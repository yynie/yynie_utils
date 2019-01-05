/**
 * BSD 2-Clause License
 *
 * Copyright (c) 2018, yynie
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.

 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 */
package com.sonf.polling;


import com.sonf.core.AbstractIOController;
import com.sonf.core.IOController;
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

public abstract class AbstractPollingIoController<S extends AbstractIOSession, CH> extends AbstractIOController<S> {
    private Logger log = Logger.get(AbstractPollingIoController.class, Logger.Level.INFO);

    private AbstractIOConfig config;
    private boolean isSecure = false; //default false
    private long connectCheckIntervalMs = 60 * 1000L; // 1 minute by default
//    private final ConcurrentHashMap<String, S> managedSessionByIpPort = new ConcurrentHashMap<String, S>();
//    private final Map<String, S> readOnlyManagedSessions = Collections.unmodifiableMap(managedSessionByIpPort);
    private volatile boolean selectable;
    /** A lock used to protect the selector to be waked up before it's created */
    private final Semaphore lock = new Semaphore(1);

    private AtomicReference<WorkerBee> beeRef = new AtomicReference<WorkerBee>();

    private IOProcessor<S> processor; //only one processor supported on android devices;

    private final Queue<IOFuture> connectQueue = new ConcurrentLinkedQueue<IOFuture>();
    private final Queue<IOFuture> cancelConnectQueue = new ConcurrentLinkedQueue<IOFuture>();

    /**
     * Constructor
     * @param executor if null, create Default pool
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

    @Override
    protected Executor getDefaultThreadPollExecutor(){
        return createCachedThreadPool();
    }

    public Executor createCachedThreadPool(){
        Executor executor = Executors.newCachedThreadPool();
        ((ThreadPoolExecutor)executor).setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        return executor;
    }

    public Executor createFixedThreadPool(){
        int size = Math.max(1, Runtime.getRuntime().availableProcessors() - 1);

        return Executors.newFixedThreadPool(size);
    }

    public Executor createFixedThreadPool(int nThreads){
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


    public boolean isSecure() {
        return isSecure;
    }

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

    public boolean isSelectable() {
        return selectable;
    }

    public void setSelectable(boolean selectable) {
        log.i("setSelectable " + selectable);
        this.selectable = selectable;
    }

    public AbstractIOConfig getConfig() {
        return config;
    }

//    public S getSessionByUnique(String ipColonPort){
//        return readOnlyManagedSessions.get(ipColonPort);
//    }

    public S createSession(SocketAddress remoteAddress, AbstractIOConfig config){
        S session = newSession(processor);
        session.setRemoteAddress(remoteAddress);
        session.setConfig(config);
        //managedSessionByIpPort.put(session.getUniqueKey(), session);
        return session;
    }

    public S createSession(String remoteHost, int remotePort, AbstractIOConfig config){
        S session = newSession(processor);
        session.setRemoteAddress(remoteHost, remotePort);
        session.setConfig(config);
        //managedSessionByIpPort.put(session.getUniqueKey(), session);
        return session;
    }

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
                log.i("processCancelQueue session:" + session.getId());
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
     * build a IOSession connected to specified host:port
     *
     * @return An IOSession instance. If failed, return null.
     */
    protected abstract S buildSession(String host, Integer port, AbstractIOConfig config);

    /**
     * Create a new IOSession instance
     *
     * @return a new IOSession instance
     */
    protected abstract S newSession(IOProcessor<S> processor);
    /**
     * Create a new client socket handle
     *
     * @return a new client socket handle
     * @throws Exception
     *             any exception thrown by the underlying systems calls
     */
    protected abstract CH newChannel() throws Exception;

    /**
     * Close a client socket.
     *
     * @param channel
     *            the client socket
     * @throws Exception
     *             any exception thrown by the underlying systems calls
     */
    protected abstract void closeChannel(CH channel) throws Exception;


    /**
     * Connect a newly created client socket handle to a remote address.
     * This operation is non-blocking, so at end of the
     * call the socket can be still in connection process.
     *
     * @param channel the client socket handle
     * @param remoteAddress the remote address where to connect
     * @return <tt>true</tt> if a connection was established, <tt>false</tt> if
     *         this client socket is in non-blocking mode and the connection
     *         operation is in progress
     * @throws Exception If the connect failed
     */
    protected abstract boolean connect(CH channel, SocketAddress remoteAddress) throws Exception;

    /**
     * Finish the connection process of a client socket after it was marked as
     * ready to process by the {@link #select(int)} call. The socket will be
     * connected or reported as connection failed.
     *
     * @param channel
     *            the client socket handle to finish to connect
     * @return true if the socket is connected
     * @throws Exception
     *             any exception thrown by the underlying systems calls
     */
    protected abstract boolean finishConnect(CH channel) throws Exception;

    /**
     * Register a new client socket for connection, add it to connection polling
     *
     * @param channel
     *            client socket handle

     * @throws Exception
     *             any exception thrown by the underlying systems calls
     */
    protected abstract void registerConnecting(CH channel, IOFuture future) throws Exception;

    /**
     * get the IOFuture for a given client socket channel
     *
     * @param channel
     *            the socket client handle
     * @return the connection IOFuture if the socket is connecting otherwise
     *         <code>null</code>
     */
    protected abstract IOFuture getFuture(CH channel);

    /**
     * Check for connected sockets, interrupt when at least a connection is
     * processed (connected or failed to connect). All the client socket
     * descriptors processed need to be returned by {@link #selectedChannel()}
     *
     * @param timeout The timeout for the select() method
     * @return The number of socket having received some data
     * @throws Exception any exception thrown by the underlying systems calls
     */
    protected abstract int select(int timeout) throws Exception;

    /**
     * Check for acceptable connections, interrupt when at least a server is ready for accepting.
     * All the ready server socket descriptors need to be returned by {@link #selectedChannel()}
     * @return The number of sockets having got incoming client
     * @throws Exception any exception thrown by the underlying systems calls
     */
    protected abstract int select() throws Exception;

    /**
     * Say if the list of {@link IOSession} polled by this {@link IOProcessor}
     * is empty
     *
     * @return <tt>true</tt> if at least a session is managed by this {@link IOProcessor}
     */
    protected abstract boolean isSelectorEmpty();

    /**
     * Interrupt the {@link #select(int timeout)} method. Used when the poll set need to be modified.
     */
    protected abstract void wakeup();

    /**
     * {@link Iterator} for the set of server sockets found with acceptable incoming connections
     *  during the last {@link #select(int timeout)} call.
     * @return the list of server handles ready
     */
    protected abstract Iterator<SelectableChannel> selectedChannel();

    /**
     * {@link Iterator} for all the client sockets polled for connection.
     *
     * @return the list of client sockets currently polled for connection
     */
    protected abstract Iterator<SelectableChannel> allChannels();


    /**
     * Destroy the polling system, will be called when this {@link IOController}
     * implementation will be disposed.
     * @throws Exception any exception thrown by the underlying systems calls
     */
    protected abstract void destroy() throws Exception;

}
