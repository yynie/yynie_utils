package com.sonf.polling;

import android.os.SystemClock;

import com.sonf.core.IOProcessor;
import com.sonf.core.NamedRunnable;
import com.sonf.core.buffer.IoBuffer;
import com.sonf.core.filter.DefaultFilterChain;
import com.sonf.core.future.DefaultIOFuture;
import com.sonf.core.session.AbstractIOSession;
import com.sonf.core.session.IOConfig;
import com.sonf.core.session.IOSession;
import com.sonf.core.write.IWritePacket;
import com.sonf.core.write.WriteException;
import com.sonf.future.ConnectFuture;
import com.yynie.myutils.Logger;

import java.io.IOException;
import java.net.PortUnreachableException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 *  Polling implementation of {@link IOProcessor} to handle I/O operations for sessions
 */
public abstract class AbstractPollingIoProcessor <S extends AbstractIOSession> implements IOProcessor<S> {
    private Logger log = Logger.get(AbstractPollingIoProcessor.class, Logger.Level.INFO);
    private final Object disposalLock = new Object();
    private volatile boolean disposing;
    private volatile boolean disposed;
    private final DefaultIOFuture disposalFuture = new DefaultIOFuture(null);

    /** The executor to run the inner Processor thread */
    private final Executor executor;

    /** Reference to hold the inner Processor thread */
    private AtomicReference<ProcessorBee> beeRef = new AtomicReference<ProcessorBee>();

    /** A Session queue store the newly connected sessions */
    private final Queue<S> newSessions = new ConcurrentLinkedQueue<S>();

    /** A queue used to store the sessions to be removed */
    private final Queue<S> removingSessions = new ConcurrentLinkedQueue<S>();

    /** A queue used to store the sessions to be flushed */
    private final Queue<S> flushingSessions = new ConcurrentLinkedQueue<S>();

    protected AtomicBoolean wakeupCalled = new AtomicBoolean(false);

    /** Tracks managed sessions. */
    private final ConcurrentMap<Long, S> managedSessions = new ConcurrentHashMap<Long, S>();

    /**
     * Constructor with the given executor.
     * Note that we use the same executor with the IOController
     *
     * @param executor the {@link Executor} for handling I/O events
     */
    protected AbstractPollingIoProcessor(Executor executor) {
        if (executor == null) {
            throw new IllegalArgumentException("executor");
        }

        this.executor = executor;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final boolean isDisposing() {
        return disposing;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final boolean isDisposed() {
        return disposed;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final void dispose() {
        if (disposed || disposing) {
            return;
        }
        log.i("dispose");
        synchronized (disposalLock) {
            disposing = true;
            runWorkerBee();
        }

        disposalFuture.awaitUninterruptibly();
        disposed = true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final void add(S session) {
        if (disposed || disposing) {
            throw new IllegalStateException("Already disposed.");
        }

        // Adds the session to the newSession queue and starts the worker
        newSessions.add(session);
        runWorkerBee();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final void remove(S session) {
        scheduleRemove(session);
        runWorkerBee();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final void flush(S session) {
        // add the session to the queue if it's not already
        // in the queue, then wake up the select()
        if (session.setScheduledForFlush(true)) {
            flushingSessions.add(session);
            wakeup();
        }
    }

    private void scheduleFlush(S session) {
        // add the session to the queue if it's not already
        // in the queue
        if (session.setScheduledForFlush(true)) {
            flushingSessions.add(session);
        }
    }

    private void scheduleRemove(S session) {
        if (!removingSessions.contains(session)) {
            removingSessions.add(session);
        }
    }

    private void runWorkerBee() {
        ProcessorBee bee = beeRef.get();

        if (bee == null) {
            bee = new ProcessorBee();

            if (beeRef.compareAndSet(null, bee)) {
                String name = ProcessorBee.class.getSimpleName();
                executor.execute(new NamedRunnable(bee, name));
            }
        }
        wakeup();
    }

    class ProcessorBee implements Runnable{
        private static final long SELECT_TIMEOUT = 1000L;
        private long lastIdleCheckTime;
        @Override
        public void run() {
            assert (beeRef.get() == this);

            int nSessions = 0;
            int nbTries = 10;
            lastIdleCheckTime = SystemClock.elapsedRealtime();
            for (;;) {
                try {
                    long t_s = SystemClock.elapsedRealtime();
                    int selected = select(SELECT_TIMEOUT);
                    long t_e = SystemClock.elapsedRealtime();
                    long delta = t_e - t_s;
                    /*---Followed refer to mina but may not happened on a Android device.------------
                    mina is for internet server where the environment might be more complex-----------------*/
                    if (!wakeupCalled.getAndSet(false) && (selected == 0) && (delta < 100)) {
                        // the select() may have been interrupted because we have had an closed channel.
                        if (isBrokenConnection()) {
                            log.w("Broken connection");
                        } else {
                            // Ok, we are hit by the nasty epoll spinning.
                            // Basically, there is a race condition which causes a closing file descriptor not to be
                            // considered as available as a selected channel,
                            // but
                            // it stopped the select. The next time we will call select(), it will exit immediately for the
                            // same reason, and do so forever, consuming 100% CPU.
                            // We have to destroy the selector, and register all the socket on a new one.
                            if (nbTries == 0) {
                                log.w("Create a new selector. Selected is 0, delta = " + delta);
                                registerNewSelector();
                                nbTries = 10;
                            } else {
                                nbTries--;
                            }
                        }
                    }else {
                        nbTries = 10;
                    }
                    /*------------------------------------------------------------------------------------*/

                    nSessions += registerNewSessions();
                    if (selected > 0) {
                        process();
                    }
                    long curElapsedTime = SystemClock.elapsedRealtime();
                    flush(curElapsedTime);

                    nSessions -= removeSessions();
                    notifyIdleSessions(curElapsedTime);
                    if (nSessions <= 0 && newSessions.isEmpty() && isSelectorEmpty()) {
                        break;
                    }

                    if (isDisposing()) {
                        boolean hasKeys = false;
                        for (Iterator<S> i = allSessions(); i.hasNext();) {
                            IOSession session = i.next();
                            if (session.isActive()) {
                                scheduleRemove((S)session);
                                hasKeys = true;
                            }
                        }
                        if (hasKeys) {
                            wakeup();
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            beeRef.set(null);
            try {
                synchronized (disposalLock) {
                    if (disposing) {
                        doDispose();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if(disposing) {
                    disposalFuture.setValue(true);
                }
            }
        }

        private void process() throws Exception {
            for (Iterator<S> i = selectedSessions(); i.hasNext();) {
                S session = i.next();
                // Process Reads
                if (isReadable(session)) {
                    readFrom(session);
                }

                // Process writes
                if (isWritable(session) && session.setScheduledForFlush(true)) {
                    // add the session to the queue, if it's not already there
                    flushingSessions.add(session);
                }
                i.remove();
            }
        }

        private void readFrom(S session) {
            log.d("readFrom: session id=" + session.getId());
            IoBuffer buf = session.getReadIOBuffer();
            buf.clear();
            try {
                int readBytes = 0;
                int ret;
                try {
                    while ((ret = read(session, buf)) > 0) {
                        readBytes += ret;
                        if (!buf.hasRemaining()) {
                            break;
                        }
                    }
                } finally {
                    buf.flip();
                }
                log.i("readFrom:readBytes = " + readBytes);
                if (readBytes > 0) {
                    session.updateReadTime(SystemClock.elapsedRealtime());
                    session.getFilterChain().fireMessageReceived(buf);
                }
                if (ret < 0) {
                    session.getFilterChain().fireInputClosed();
                }
            }catch (Exception e) {
                if (e instanceof IOException) {
                    if (!(e instanceof PortUnreachableException)) {
                        scheduleRemove(session);
                    }
                }
                session.getFilterChain().fireExceptionCaught(e);
            }

        }

        private void flush(long curElapsedTime) {
            if (flushingSessions.isEmpty()) {
                return;
            }
            do {
                S session = flushingSessions.poll();
                if (session == null) {
                    //should not happen.
                    break;
                }

                session.setScheduledForFlush(false);
                SessionState state = getState(session);
                switch (state) {
                    case OPENED:
                        try {
                            boolean flushedAll = flushNow(session, curElapsedTime);
                            if (flushedAll && !session.getWriteQueue().isEmpty()
                                    && !session.isScheduledForFlush()) {
                                scheduleFlush(session);
                            }
                        } catch (Exception e) {
                            scheduleRemove(session);
                            session.closeNow();
                            session.getFilterChain().fireExceptionCaught(e);
                        }
                        break;

                    case CLOSING:
                        // Skip if the channel is already closed.
                        break;

                    case OPENING:
                        // Retry later if session is not yet fully initialized.
                        // (In case that Session.write() is called before addSession()
                        // is processed)
                        scheduleFlush(session);
                        return;

                    default:
                        throw new IllegalStateException(String.valueOf(state));
                }
            }while (!flushingSessions.isEmpty());
        }

        private boolean flushNow(S session, long curElapsedTime) {
            if (!session.isReady()) {
                scheduleRemove(session);
                return false;
            }
            Queue<IWritePacket> queue = session.getWriteQueue();

            final int maxWrittenBytes = session.getConfig().getMaxWriteBytes();
            int writtenBytes = 0;
            IWritePacket packet = null;

            // Clear OP_WRITE
            setInterestedInWrite(session, false);
            do {
                // Check for pending writes.
                packet = session.getCurrentWritePacket();
                if (packet == null) {
                    packet = queue.poll();
                    if(packet == AbstractIOSession.CLOSE_REQUEST){
                        session.closeNow();
                        packet = null;
                    }

                    if (packet == null) {
                        break;
                    }

                    session.setCurrentWritePacket(packet);
                }
                Object message = packet.getMessage();
                if (message instanceof IoBuffer) {
                    IoBuffer buffer = (IoBuffer) message;
                    int perWrittenBytes = writeBuffer(session, buffer, maxWrittenBytes - writtenBytes, curElapsedTime);
                    // Now, forward the original message
                    if (!buffer.hasRemaining()) {
                        session.setCurrentWritePacket(null);
                        session.getFilterChain().fireMessageSent(packet);
                    }
                    if ((perWrittenBytes > 0) && buffer.hasRemaining()) {
                        // the buffer isn't empty, we re-interest it in writing
                        setInterestedInWrite(session, true);
                        return false;
                    }

                    if (perWrittenBytes == 0) {
                        // Kernel buffer is full.
                        setInterestedInWrite(session, true);
                        return false;
                    } else {
                        writtenBytes += perWrittenBytes;
                        if (writtenBytes >= maxWrittenBytes) {
                            // Wrote too much
                            scheduleFlush(session);
                            return false;
                        }
                    }
                }else {
                    throw new IllegalStateException("Don't know how to handle message of type '"
                            + message.getClass().getName() + "'.  Are you missing a protocol encoder?");
                }
            }while (writtenBytes < maxWrittenBytes);
            return true;
        }

        private int writeBuffer(S session, IoBuffer buf, int maxLength, long curElapsedTime) {
            int writtenBytes = 0;
            if (buf.hasRemaining()) {
                int length = Math.min(buf.remaining(),maxLength);
                try {
                    writtenBytes = write(session, buf, length);
                    log.i("writeBuffer:"+ writtenBytes);
                } catch (IOException ioe) {
                    // We have had an issue while trying to send data to the
                    // peer : let's close the session.
                    buf.free();
                    session.closeNow();
                    removeNow(session);
                    return -1;
                }
            }
            if(writtenBytes > 0) session.updateWrittenTime(curElapsedTime);
            return writtenBytes;
        }

        private int removeSessions(){
            int count = 0;
            for (;;) {
                S session = removingSessions.poll();
                if(session == null){
                    break;
                }
                SessionState state = getState(session);
                switch (state) {
                    case OPENED:
                        if (removeNow(session)) {
                            count++;
                        }
                        break;

                    case CLOSING:
                        // Skip if channel is already closed
                        count++;
                        break;

                    case OPENING:
                        // Remove session from the newSessions queue
                        newSessions.remove(session);
                        if (removeNow(session)) {
                            count++;
                        }
                        break;

                    default:
                        throw new IllegalStateException(String.valueOf(state));
                }
            }
            return count;
        }

        private int registerNewSessions() {
            int count = 0;
            for (;;) {
                S session = newSessions.poll();
                if(session == null){
                    break;
                }
                try {
                    initToRead(session);
                    // build chain here ??
                    session.getController().getFilterChainBuilder().buildChain(session.getFilterChainMatcher(), session.getFilterChain());
                    addManagedSession(session);
                    count++;
                } catch (Exception e) {
                    e.printStackTrace();
                    ConnectFuture future = (ConnectFuture) session.removeAttribute(DefaultFilterChain.SESSION_CREATED_FUTURE);
                    if (future != null) future.setException(e);
                    try {
                        destroy(session);
                    } catch (Exception e1) {
                        e.printStackTrace();
                    }finally {
                        session.setStateClosed();
                    }
                }
            }
            return count;
        }

        private void notifyIdleSessions(long curElapsedTime) throws Exception {
            if (curElapsedTime - lastIdleCheckTime >= SELECT_TIMEOUT) {
                lastIdleCheckTime = curElapsedTime;
                Iterator<S> it = allSessions();
                while (it.hasNext()) {
                    S session = it.next();
                    if (!session.getCloseFuture().isClosed()) {
                        session.notifyIdleSession(curElapsedTime);
                    }
                }
            }
        }
    }

    private boolean removeNow(S session) {
        clearWriteQueue(session);

        try {
            destroy(session);
            return true;
        } catch (Exception e) {
            session.getFilterChain().fireExceptionCaught(e);
        } finally {
            try {
                clearWriteQueue(session);
                sessionDestroyed(session);
            } catch (Exception e) {
                session.getFilterChain().fireExceptionCaught(e);
            }
        }

        return false;
    }

    private void clearWriteQueue(S session) {
        Queue<IWritePacket> queue = session.getWriteQueue();
        IWritePacket packet;

        List<IWritePacket> failedList = new ArrayList<IWritePacket>();

        if ((packet = queue.poll()) != null) {
            Object message = packet.getMessage();
            if (message instanceof IoBuffer) {
                IoBuffer buf = (IoBuffer) message;
                if (buf.hasRemaining()) {
                    failedList.add(packet);
                } else {
                    session.getFilterChain().fireMessageSent(packet);
                }
            } else {
                failedList.add(packet);
            }
            // Discard others.
            while ((packet = queue.poll()) != null) {
                failedList.add(packet);
            }
        }

        if (!failedList.isEmpty()) {
            Throwable cause = new WriteException("Trying to write a message to a closed session");
            for (IWritePacket p : failedList) {
                p.getFuture().setException(cause);
            }
            session.getFilterChain().fireExceptionCaught(cause);
        }
    }

    private void addManagedSession(S session) {
        boolean firstAdded = managedSessions.isEmpty();
        if (managedSessions.putIfAbsent(session.getId(), session) != null) {
            return;
        }
        log.d("addManagedSession: session added id=" + session.getId());
        if(firstAdded) session.getController().activate();
        session.getFilterChain().fireSessionOpened();
    }

    private void sessionDestroyed(S session){
        // Try to remove the remaining empty session set after removal.
        if (managedSessions.remove(session.getId()) == null) {
            return;
        }
        if(managedSessions.isEmpty()) session.getController().deactivate();
        // Fire session events.
        session.getFilterChain().fireSessionClosed();
    }

    /**
     * In the case we are using the java select() method, this method is used to
     * trash the buggy selector and create a new one, registring all the sockets
     * on it.
     *
     * @throws IOException If we got an exception
     */
    protected abstract void registerNewSelector() throws IOException;

    /**
     * Check that the select() has not exited immediately just because of a
     * broken connection. In this case, this is a standard case, and we just
     * have to loop.
     *
     * @return <tt>true</tt> if a connection has been brutally closed.
     * @throws IOException If we got an exception
     */
    protected abstract boolean isBrokenConnection() throws IOException;

    /**
     * Get the state of a session (One of OPENING, OPEN, CLOSING)
     *
     * @param session the {@link IOSession} to inspect
     * @return the state of the session
     */
    protected abstract SessionState getState(S session);

    /**
     * poll sessions for the given timeout
     *
     * @param timeout milliseconds before the call timeout if no event appear
     * @return The number of session ready for read or for write
     * @throws Exception if some low level IO error occurs
     */
    protected abstract int select(long timeout) throws Exception;

    /**
     * @return whether the list of sessions polled by this {@link IOProcessor} is empty
     */
    protected abstract boolean isSelectorEmpty();

    /**
     * Interrupt the {@link #select(long)} call.
     */
    protected abstract void wakeup();

    /**
     * Get an {@link Iterator} for all the sessions polled by this {@link IOProcessor}
     *
     * @return {@link Iterator} of {@link IOSession}
     */
    protected abstract Iterator<S> allSessions();

    /**
     * Get an {@link Iterator} for the list of {@link IOSession} with data ready
     * during the last call of {@link #select(long)}
     *
     * @return {@link Iterator} of {@link IOSession} ready for I/O operation
     */
    protected abstract Iterator<S> selectedSessions();

    /**
     * Set the session to be imformed when there's data ready for read on it.
     *
     * @param session the session for which we want to wait for readable events
     * @throws Exception any exception thrown by the underlying system calls
     */
    protected abstract void initToRead(S session) throws Exception;

    /**
     * Destroy the underlying client socket channel
     *
     * @param session
     * @throws Exception any exception thrown by the underlying system calls
     */
    protected abstract void destroy(S session) throws Exception;

    /**
     * Set the session to be informed when it's writable
     *
     * @param session the session for which we want to wait for writable events
     * @param isInterested <tt>true</tt> for registering, <tt>false</tt> for removing
     */
    protected abstract void setInterestedInWrite(S session, boolean isInterested);

    /**
     * Reads a sequence of bytes from a {@link IOSession} into the given
     * {@link IoBuffer}.
     *
     * @param session the session to read
     * @param buf the buffer to fill
     * @return the number of bytes read
     * @throws Exception any exception thrown by the underlying system calls
     */
    protected abstract int read(S session, IoBuffer buf) throws Exception;

    /**
     * Write a sequence of bytes to a {@link IOSession},
     *
     * @param session the session to write
     * @param buf the buffer to write
     * @param length the number of bytes to write can be superior to the number of
     *            bytes remaining in the buffer
     * @return the number of byte written
     * @throws IOException any exception thrown by the underlying system calls
     */
    protected abstract int write(S session, IoBuffer buf, int length) throws IOException;

    /**
     * Tells if the session ready for writing
     *
     * @param session the queried session
     * @return <tt>true</tt> is ready, <tt>false</tt> if not ready
     */
    protected abstract boolean isWritable(S session);

    /**
     * Tells if the session ready for reading
     *
     * @param session the queried session
     * @return <tt>true</tt> is ready, <tt>false</tt> if not ready
     */
    protected abstract boolean isReadable(S session);

    /**
     * Dispose the resources used by this {@link IOProcessor}
     *
     * @throws Exception if some low level IO error occurs
     */
    protected abstract void doDispose() throws Exception;
}
