package com.sonf.polling;


import com.sonf.core.AbstractIOController;
import com.sonf.core.IOProcessor;
import com.sonf.core.filter.DefaultFilterChain;
import com.sonf.core.future.IOFuture;
import com.sonf.core.session.AbstractIOConfig;
import com.sonf.core.session.AbstractIOSession;
import com.sonf.core.session.IOSession;
import com.sonf.future.ConnectFuture;
import com.yynie.myutils.Logger;

import java.lang.reflect.Constructor;
import java.net.ConnectException;
import java.net.SocketAddress;
import java.nio.channels.SelectableChannel;
import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Polling Controller
 * Inheriting class of {@link AbstractIOController}.
 */
public abstract class AbstractPollingIoController<S extends AbstractIOSession, CH> extends AbstractIOController<S> {
    private Logger log = Logger.get(AbstractPollingIoController.class, Logger.Level.INFO);

    private AbstractIOConfig config;
    private boolean isSecure = false; //default false
    private long connectCheckIntervalMs = 60 * 1000L; // 1 minute by default

    /** A flag set when the controller has been initialized successfully */
    private volatile boolean selectable;

    private final Semaphore lock = new Semaphore(1);

    /** A reference to hold the thread to handle connect or cancel-connect requests */
    private AtomicReference<WorkerBee> beeRef = new AtomicReference<WorkerBee>();

    /** The only one processor supported on android devices;*/
    private IOProcessor<S> processor;

    private final Queue<IOFuture> connectQueue = new ConcurrentLinkedQueue<IOFuture>();
    private final Queue<IOFuture> cancelConnectQueue = new ConcurrentLinkedQueue<IOFuture>();

    /**
     * Constructor
     * @param executor can provided by outside code.
     *                 Pass <code>null</code> if you want this controller to create a Default one.
     * @param processorType used to construct processor which will be responsible for handling
     *                     read/write I/O events.
     * @param config the base configuration used for creation of {@link IOSession}'s configuration
     */
    public AbstractPollingIoController(Executor executor, Class<? extends IOProcessor<S>> processorType, AbstractIOConfig config) {
        super(executor);
        if (processorType == null) {
            throw new IllegalArgumentException("processorType");
        }

        this.config = config;
        Constructor<? extends IOProcessor<S>> processorConstructor;
        Throwable throwable = null;
        try {
            processorConstructor = processorType.getConstructor(Executor.class);
            processor = processorConstructor.newInstance(getExecutor());
        } catch (Exception e) {
            throwable = e;
            processor = null;
        } finally {
            if(processor == null){
                throw new RuntimeException("Can NOT create IOProcessor:", throwable);
            }
        }
    }

    /***
     * {@inheritDoc}
     */
    @Override
    protected Executor getDefaultThreadPollExecutor(){
        return createCachedThreadPool();
    }

    private Executor createCachedThreadPool(){
        Executor executor = Executors.newCachedThreadPool();
        ((ThreadPoolExecutor)executor).setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        return executor;
    }

    private Executor createFixedThreadPool(){
        int size = Math.max(1, Runtime.getRuntime().availableProcessors() - 1);

        return Executors.newFixedThreadPool(size);
    }

    private Executor createFixedThreadPool(int nThreads){
        int size = Math.max(nThreads, 1);

        return Executors.newFixedThreadPool(size);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void dispose0() throws Exception {
        runWorkerBee();
    }

    /**
     * Note : we don't support SSL/TLS protocol by now
     * @return whether this controller is used for secure connection(SSL/TLS)
     * */
    public boolean isSecure() {
        return isSecure;
    }

    /**
     * Set this controller to be secure mode (SSL/TLS)
     * Note : we don't support SSL/TLS protocol by now
     *
     * @param  secure <tt>true</tt> for secure mode. <tt>false</tt> as default
     */
    public void setSecure(boolean secure) {
        isSecure = secure;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getConnectCheckIntervalMs() {
        return connectCheckIntervalMs;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setConnectCheckIntervalMs(long connectCheckIntervalMs) {
        this.connectCheckIntervalMs = connectCheckIntervalMs;
    }

    /**
     * Set selectable.
     *
     * @param selectable
     *          Set to true when the controller has been initialized
     *          Set to false when the controller will be destroyed.
     */
    public void setSelectable(boolean selectable) {
        log.d("setSelectable " + selectable);
        this.selectable = selectable;
    }

    /***
     * @return the base configuration instance of this controller.
     *          Any modification to this instance will be applied to the sessions created after.
     */
    public AbstractIOConfig getConfig() {
        return config;
    }

    /**
     * Create a new {@link IOSession}
     *
     * @param remoteAddress the {@link SocketAddress} of remote endpoint this session will
     *                      connect to lately.
     * @param config session's configuration which used only by this session.
     *               Modification to this configuration will effect on the session immediately.
     * @return An IOSession instance.
     */
    public S createSession(SocketAddress remoteAddress, AbstractIOConfig config){
        S session = newSession(processor);
        session.setRemoteAddress(remoteAddress);
        session.setConfig(config);
        //managedSessionByIpPort.put(session.getUniqueKey(), session);
        return session;
    }

    /**
     * Create a new {@link IOSession}
     *
     * @param remoteHost the host domain of remote endpoint this session will
     *                      connect to lately.
     * @param remotePort the port on which the remote endpoint listened for connections.
     * @param config session's configuration which used only by this session.
     *               Modification to this configuration will effect on the session immediately.
     * @return An IOSession instance.
     */
    public S createSession(String remoteHost, int remotePort, AbstractIOConfig config){
        S session = newSession(processor);
        session.setRemoteAddress(remoteHost, remotePort);
        session.setConfig(config);
        //managedSessionByIpPort.put(session.getUniqueKey(), session);
        return session;
    }

    /**
     *{@inheritDoc}
     */
    @Override
    public boolean connect(IOFuture future){
        if(isDisposing()){
            future.setException(new IllegalStateException("connect failed when Controller is disposing !"));
            return false;
        }

        try {
            connectQueue.add(future);
            runWorkerBee();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return true;
    }

    /**
     *{@inheritDoc}
     */
    @Override
    public void cancelConnect(IOFuture future){
        log.i("cancelConnect");
        scheduleCancelConnect(future);
        try {
            runWorkerBee();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void scheduleCancelConnect(IOFuture future){
        if(!cancelConnectQueue.contains(future)){
            cancelConnectQueue.add(future);
        }
    }

    private void runWorkerBee() throws InterruptedException {
        if(!selectable){
            log.e("Working Processor can NOY be invoked, selectable = " + selectable);
            return;
        }
        // start the acceptor if not already started
        WorkerBee bee = beeRef.get();
        if (bee == null) {
            lock.acquire();
            bee = new WorkerBee();

            if (beeRef.compareAndSet(null, bee)) {
                executeRunnable(bee, null);
            } else {
                lock.release();
            }
        }
        wakeup();
    }

    class WorkerBee implements Runnable {
        @Override
        public void run() {
            assert (beeRef.get() == this);

            lock.release();
            int nConnectSession = 0;
            while (selectable) {
                try {
                    int selected = 0;

                    if (nConnectSession > 0) { //TODO !!! may be select again after last channel closed
                        int timeout = (int) Math.min(getConnectCheckIntervalMs(), 1000L);
                        selected = select(timeout);
                    }else{
                        // Detect if we have some keys ready to be processed
                        // The select() will be woke up if some new connection
                        // have occurred, or if the selector has been explicitly
                        // woke up
                        selected = select(1000);
                    }
                    nConnectSession += doConnectThenRegister();
                    if(nConnectSession == 0 && connectQueue.isEmpty() && cancelConnectQueue.isEmpty() &&  isSelectorEmpty()){
                        break;
                    }
                    if (selected > 0) {
                        nConnectSession -= process(selectedChannel());
                    }

                    checkTimeOut(allChannels());
                    nConnectSession -= processCancelQueue();
                }catch (Exception e) {
                    e.printStackTrace();
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e1) {
                        e1.fillInStackTrace();
                    }
                }
            }

            beeRef.set(null);
            if(selectable && isDisposing()){
                selectable = false;
                cancelConnectQueue.clear();
                connectQueue.clear();
                try{
                    processor.dispose();
                }finally {
                    try {
                        synchronized (disposalLock) {
                            destroy();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }finally {
                        disposalFuture.setValue(true);
                    }
                }
            }
        }

        private int doConnectThenRegister(){
            int nRegisterHandles = 0;
            for (;;) {
                IOFuture future = connectQueue.poll();
                if (future == null) {
                    break;
                }
                S session = (S) future.getSession();
                CH channel = null;
                try {
                    session.parseRemoteAddress();
                    channel = newChannel();
                    if (connect(channel, session.getRemoteAddress())) {
                        session.setChannel(channel);
                        session.prepare();
                        session.setAttribute(DefaultFilterChain.SESSION_CREATED_FUTURE, future);
                        session.getProcessor().add(session);
                    }else {
                        // no-blocking register select
                        registerConnecting(channel, future);
                        session.setChannel(channel);
                        nRegisterHandles++;
                    }
                } catch (Exception e) {
                    future.setException(e);
                    if(channel != null) {
                        try {
                            closeChannel(channel);
                        } catch (Exception e2) {
                            e2.printStackTrace();
                        }
                    }
                }
            }
            return nRegisterHandles;
        }

        private int process(Iterator<SelectableChannel> channelIt) {
            int finishedCount = 0;
            while (channelIt.hasNext()) {
                CH ch = (CH) channelIt.next();
                channelIt.remove();

                if(ch != null){
                    IOFuture future = getFuture(ch);
                    assert(future instanceof ConnectFuture);

                    S session = (S) future.getSession();
                    if(session == null) {
                        log.e("process::Invalid future with a NULL session");
                        continue;
                    }
                    boolean success = false;
                    try {
                        if (finishConnect(ch)) {
                            //do something
                            if(((ConnectFuture) future).isCanceled()){
                               // cancelConnectQueue.offer(future);
                            }else {
                                session.prepare();
                                session.setAttribute(DefaultFilterChain.SESSION_CREATED_FUTURE, future);
                                session.getProcessor().add(session);
                                finishedCount++;
                            }
                        }
                        success = true;
                    } catch (Exception e) {
                        future.setException(e);
                    } finally {
                        if(!success){
                            log.e("The connection failed, we have to cancel it");
                            scheduleCancelConnect(future);
                        }
                    }
                }
            }
            return finishedCount;
        }

        private void checkTimeOut(Iterator<SelectableChannel> channelIt){
            while (channelIt.hasNext()) {
                CH ch = (CH) channelIt.next();
                if(ch != null){
                    IOFuture future = getFuture(ch);
                    if(isDisposing()){
                        scheduleCancelConnect(future);
                    }else {
                        S session = (S) future.getSession();
                        if ((session != null) && session.isConnectTimeout()) {
                            future.setException(new ConnectException("Connection timed out."));
                            scheduleCancelConnect(future);
                        }
                    }
                }
            }
        }

        private int processCancelQueue(){
            int count = 0;

            for (;;) {
                IOFuture future = cancelConnectQueue.poll();
                if(future == null){
                    break;
                }
                S session = (S) future.getSession();
                if (session == null) {
                    log.e("processCancelQueue::Invalid future with a NULL session");
                    break;
                }
                log.d("processCancelQueue session:" + session.getId());
                CH ch = (CH) session.getChannel();
                try {
                    closeChannel(ch);
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    count ++;
                    session.setStateClosed();
                    ((ConnectFuture)future).cancel();
                }
            }

            if (count > 0) {
                wakeup();
            }
            return count;
        }
    }

    /**
     * build a IOSession connected to specified host:port,
     * use the controller's configuration  as the base config
     *
     * @param host the host domain of remote endpoint this session will
     *                      connect to lately.
     * @param port the port on which the remote endpoint listened for connections.
     * @return An IOSession instance.
     */
    protected abstract S buildSession(String host, Integer port);

    /**
     * build a IOSession connected to specified host:port
     * with the specified configuration
     *
     * @param host the host domain of remote endpoint this session will
     *                      connect to lately.
     * @param port the port on which the remote endpoint listened for connections.
     * @param config session's configuration which used only by this session.
     *               Modification to this configuration will effect on the session immediately.
     * @return An IOSession instance.
     */
    protected abstract S buildSession(String host, Integer port, AbstractIOConfig config);

    /**
     * Create a new IOSession instance
     *
     * @param processor The processor the session will offer to after connection built.
     * @return a new IOSession instance
     */
    protected abstract S newSession(IOProcessor<S> processor);

    /**
     * Create a new client socket channel
     *
     * @return a new client socket channel
     * @throws Exception any exception thrown by the underlying systems calls
     */
    protected abstract CH newChannel() throws Exception;

    /**
     * Close a client socket.
     *
     * @param channel
     *            the client socket
     * @throws Exception any exception thrown by the underlying systems calls
     */
    protected abstract void closeChannel(CH channel) throws Exception;


    /**
     * Connect a newly created client socket channel to a remote address.
     * This operation is non-blocking,
     *
     * @param channel the client socket channel
     * @param remoteAddress the remote address where to connect
     * @return <tt>true</tt> if a connection was established immediately,
     *          <tt>false</tt> if the connection operation is in progress
     * @throws Exception If the connect failed
     */
    protected abstract boolean connect(CH channel, SocketAddress remoteAddress) throws Exception;

    /**
     * Finish the connection of a client socket after it was reported to be ready by the
     * {@link #select(int)} call.
     *
     * @param channel the client socket channel to finish the connection
     * @return true if the socket is connected, false if connection failed.
     * @throws Exception any exception thrown by the underlying systems calls
     */
    protected abstract boolean finishConnect(CH channel) throws Exception;

    /**
     * Register a new client socket for connection operation, add it to polling
     *
     * @param channel client socket channel
     * @param future the {@link ConnectFuture} as the attachment
     * @throws Exception any exception thrown by the underlying systems calls
     */
    protected abstract void registerConnecting(CH channel, IOFuture future) throws Exception;

    /**
     * get the IOFuture for a given client socket channel
     *
     * @param channel the socket client channel
     * @return the connection IOFuture
     */
    protected abstract IOFuture getFuture(CH channel);

    /**
     * Check for connected sockets, interrupt when at least one connection is
     * processed (connected or failed to connect).
     * Use {@link #selectedChannel()} to get all the client socket processed.
     *
     * @param timeout The timeout for the select
     * @return The number of processed socket
     * @throws Exception any exception thrown by the underlying systems calls
     */
    protected abstract int select(int timeout) throws Exception;

    /**
     * @return whether the list of polled sessions is empty
     */
    protected abstract boolean isSelectorEmpty();

    /**
     * Interrupt the {@link #select(int timeout)} method.
     * Used when the polled sessions queue changed
     */
    protected abstract void wakeup();

    /**
     * {@link Iterator} for the set of sockets with connection processed.
     *  during the last {@link #select(int timeout)} call.
     *
     * @return the list of processed sockets channels
     */
    protected abstract Iterator<SelectableChannel> selectedChannel();

    /**
     * {@link Iterator} for all the client sockets polled for connection.
     *
     * @return the list of client sockets currently polled for connection
     */
    protected abstract Iterator<SelectableChannel> allChannels();


    /**
     * Destroy the controller, will be called by {@link #dispose()}
     *
     * @throws Exception any exception thrown by the underlying systems calls
     */
    protected abstract void destroy() throws Exception;

}
