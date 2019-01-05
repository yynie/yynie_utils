package com.sonf.core.session;


import android.os.SystemClock;

import com.sonf.core.IOController;
import com.sonf.core.IOProcessor;
import com.sonf.core.buffer.IoBuffer;
import com.sonf.core.buffer.SimpleIoBuffer;
import com.sonf.core.filter.DefaultFilterChain;
import com.sonf.core.filter.IFilterChain;
import com.sonf.core.filter.IFilterChainMatcher;
import com.sonf.core.future.ICloseFuture;
import com.sonf.core.future.IConnectFuture;
import com.sonf.core.future.IOFuture;
import com.sonf.core.future.IWriteFuture;
import com.sonf.core.write.IWritePacket;
import com.sonf.core.write.WritePacket;

import java.io.IOException;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;


public abstract class AbstractIOSession<CH, CG extends AbstractIOConfig> implements IOSession<CH, CG> {
    /** An id generator guaranteed to generate unique IDs for the session */
    private static AtomicLong idGenerator = new AtomicLong(0L);
    /** The session ID */
    private long sessionId;
    private CH channel;
    private CG config;
    private IOController controller;
    /** The NioSession processor */
    protected final IOProcessor<? extends AbstractIOSession> processor;
    private final Object lock = new Object();
    private IOSessionAttribute attributeMap;
    private Queue<IWritePacket> writePacketQueue;
    private IWritePacket currentWritePacket;
    /** The FilterChain created for this session */
    private final IFilterChain filterChain;
    private IFilterChainMatcher filterChainMatcher = null;
    // Status variables
    private final AtomicBoolean scheduledForFlush = new AtomicBoolean();

    private IoBuffer readIoBuffer = new SimpleIoBuffer();

    private long lastReadTime;
    private long lastWriteTime;
    private AtomicInteger idleCountForBoth = new AtomicInteger();

    private AtomicInteger idleCountForRead = new AtomicInteger();

    private AtomicInteger idleCountForWrite = new AtomicInteger();

    private long lastIdleTimeForBoth;

    private long lastIdleTimeForRead;

    private long lastIdleTimeForWrite;

    /**
     * An internal write request object that triggers session close.
     *
     * @see #writePacketQueue
     */
    public static final WritePacket CLOSE_REQUEST = new WritePacket(new Object(), null);


    private enum SState{
        NEW(0),
        CONNECTING(1),
        CONNECTTED(2),
        READY(3),
        CLOSING(4),
        INVALID(5);

        private int value;
        SState(int value) {
            this.value = value;
        }

        private static SState fromIntValue(int v) {
            for(int i = 0; i < values().length; ++i) {
                SState lev = values()[i];
                if (v == lev.value) {
                    return lev;
                }
            }

            return null;
        }
    }

    private AtomicReference<SState> stateRef = new AtomicReference<SState>(SState.NEW);

    public AbstractIOSession(IOController controller, IOProcessor<? extends AbstractIOSession> processor){
        this.controller = controller;
        this.processor = processor;
        this.filterChain = new DefaultFilterChain(this);
        // Set a new ID for this session
        sessionId = idGenerator.incrementAndGet();
        if(sessionId >= (Long.MAX_VALUE -1)) idGenerator.set(0L);
        long elapsedTime = SystemClock.elapsedRealtime();
        lastReadTime = elapsedTime;
        lastWriteTime = elapsedTime;
        lastIdleTimeForBoth = elapsedTime;
        lastIdleTimeForRead = elapsedTime;
        lastIdleTimeForWrite = elapsedTime;
    }

    /**
     * {@inheritDoc}
     *
     * We use an AtomicLong to guarantee that the session ID are unique.
     */
    @Override
    public final long getId() {
        return sessionId;
    }


    private void _SWITCH_STATE_(SState expected, SState set){
        if(false == stateRef.compareAndSet(expected, set)){
            throw new RuntimeException("Unexpected Session state:" + stateRef.get() + " when set from " + expected + " to " + set + "!");
        }
    }

    /**
     * {@inheritDoc}
     * */
    @Override
    public IOController getController(){
        return controller;
    }


    /**
     * update the idle time
     *
     * @param curElapsedTime The current time
     */
    public final void updateWrittenTime(long curElapsedTime) {
        lastWriteTime = curElapsedTime;
        idleCountForBoth.set(0);
        idleCountForWrite.set(0);
    }

    /**
     * update the idle time
     *
     * * @param currentTime The current time
     */
    public final void updateReadTime(long curElapsedTime) {
        lastReadTime = curElapsedTime;
        idleCountForBoth.set(0);
        idleCountForRead.set(0);
    }

    private final long getLastIdleTime(IdleStatus status) {
        if (status == IdleStatus.BOTH_IDLE) {
            return lastIdleTimeForBoth;
        }

        if (status == IdleStatus.READER_IDLE) {
            return lastIdleTimeForRead;
        }

        if (status == IdleStatus.WRITER_IDLE) {
            return lastIdleTimeForWrite;
        }

        throw new IllegalArgumentException("Unknown idle status: " + status);
    }

    private final long getLastIoTime() {
        return Math.max(lastReadTime, lastWriteTime);
    }

    private final long getLastReadTime() {
        return lastReadTime;
    }

    private final long getLastWriteTime() {
        return lastWriteTime;
    }

    /**
     * Fires a SESSION_IDLE if applicable for the
     * specified {@code session}.
     *
     * @param curElapsedTime the current elapsed real time (i.e. {@link SystemClock#elapsedRealtime()()})
     */
    public void notifyIdleSession(long curElapsedTime) {
        notifyIdleSession0(curElapsedTime, getConfig().getIdleTimeInMillis(IdleStatus.BOTH_IDLE),
                IdleStatus.BOTH_IDLE, Math.max(getLastIoTime(), getLastIdleTime(IdleStatus.BOTH_IDLE)));

        notifyIdleSession0(curElapsedTime, getConfig().getIdleTimeInMillis(IdleStatus.READER_IDLE),
                IdleStatus.READER_IDLE, Math.max(getLastReadTime(), getLastIdleTime(IdleStatus.READER_IDLE)));

        notifyIdleSession0(curElapsedTime, getConfig().getIdleTimeInMillis(IdleStatus.WRITER_IDLE),
                IdleStatus.WRITER_IDLE, Math.max(getLastWriteTime(), getLastIdleTime(IdleStatus.WRITER_IDLE)));

        notifyWriteTimeout(curElapsedTime);
    }

    private void notifyIdleSession0(long curElapsedTime, long idleTime, IdleStatus status,
                                           long lastIoTime) {
        if ((idleTime > 0) && (lastIoTime != 0) && (curElapsedTime - lastIoTime >= idleTime)) {
            getFilterChain().fireSessionIdle(status);
        }
    }

    private void notifyWriteTimeout(long curElapsedTime) {
        long writeTimeout = getConfig().getWriteTimeoutInMillis();
        //if ((writeTimeout > 0) && (curElapsedTime - getLastWriteTime() >= writeTimeout)
        //        && !getWriteQueue().isEmpty()) {
            IWritePacket packet = getCurrentWritePacket();
            if (packet != null && (curElapsedTime - packet.getStartTime()) >= writeTimeout) {
                setCurrentWritePacket(null);
                Throwable cause = new IOException("Write Timeout");
                packet.getFuture().setException(cause);
                getFilterChain().fireExceptionCaught(cause);
                // WriteException is an IOException, so we close the session.
                closeNow();
            }
       // }
    }

    /**
     * Increase the count of the various Idle counter
     *
     * @param status The current status
     * @param curElapsedTime The current time
     */
    public void increaseIdleCount(IdleStatus status, long curElapsedTime) {
        if (status == IdleStatus.BOTH_IDLE) {
            idleCountForBoth.incrementAndGet();
            lastIdleTimeForBoth = curElapsedTime;
        } else if (status == IdleStatus.READER_IDLE) {
            idleCountForRead.incrementAndGet();
            lastIdleTimeForRead = curElapsedTime;
        } else if (status == IdleStatus.WRITER_IDLE) {
            idleCountForWrite.incrementAndGet();
            lastIdleTimeForWrite = curElapsedTime;
        } else {
            throw new IllegalArgumentException("Unknown idle status: " + status);
        }
    }

    @Override
    public IoBuffer getIOBuffer(){
        synchronized (readIoBuffer){
            if(!readIoBuffer.available()){
                readIoBuffer.allocate(config.getReadBufferSize());
            }
            return readIoBuffer;
        }
    }

    /**
     * {@inheritDoc}
     * */
    @Override
    public IFilterChain getFilterChain(){
        return filterChain;
    }

    /**
     * {@inheritDoc}
     * */
    @Override
    public void setFilterChainMatcher(IFilterChainMatcher filterChainMatcher){
        this.filterChainMatcher = filterChainMatcher;
    }

    /**
     * {@inheritDoc}
     * */
    @Override
    public IFilterChainMatcher getFilterChainMatcher(){
        return filterChainMatcher;
    }

    public void prepare(){
        stateRef.set(SState.CONNECTTED);
        applySessionConfig();
        prepareAttributeMap();
        prepareWriteQueue();
    }


    private void prepareAttributeMap(){
        this.attributeMap = new DefaultAttributeMap();
    }

    private void prepareWriteQueue(){
        writePacketQueue = new ConcurrentLinkedQueue<IWritePacket>();
    }

    /**
     * {@inheritDoc}
     */
    public final IWritePacket getCurrentWritePacket() {
        return currentWritePacket;
    }

    /**
     * {@inheritDoc}
     */
    public final void setCurrentWritePacket(IWritePacket currentWritePacket) {
        this.currentWritePacket = currentWritePacket;
    }

    @Override
    public Queue<IWritePacket> getWriteQueue(){
        return writePacketQueue;
    }

    public IOProcessor getProcessor(){
        return processor;
    }

    @Override
    public void setChannel(CH channel) {
        this.channel = channel;
    }

    @Override
    public CH getChannel(){
        return this.channel;
    }

    @Override
    public void setConfig(CG config){
        this.config = config;
    }

    @Override
    public CG getConfig(){
        return config;
    }

    @Override
    public IOFuture connect(){
        _SWITCH_STATE_(SState.NEW, SState.CONNECTING);
        IOFuture f = getConnectFuture();
        if(controller.connect(f)){
            setConnectDeadLine();
        }else{
            stateRef.set(SState.INVALID);
        }
        return f;
    }

    @Override
    public void setStateClosed(){
        stateRef.set(SState.INVALID);
    }

    @Override
    public void setStateReady(){
        stateRef.set(SState.READY);
    }

    @Override
    public boolean isReady(){
        return stateRef.get().equals(SState.READY);
    }

    @Override
    public boolean isConnecting(){
        return stateRef.get().equals(SState.CONNECTING);
    }

    @Override
    public boolean isClosing(){
        return stateRef.get().equals(SState.CLOSING);
    }

    @Override
    public boolean isInvalid(){
        return stateRef.get().equals(SState.INVALID);
    }

    @Override
    public boolean isNew() { return  stateRef.get().equals(SState.NEW); }

    @Override
    public void cancelConnect(IOFuture future){
        if(stateRef.get().equals(SState.CONNECTING)) {
            stateRef.set(SState.CLOSING);
            controller.cancelConnect(future);
        }
    }

    private class DefaultAttributeMap implements IOSessionAttribute{
        private final ConcurrentMap<Object, Object> attributes = new ConcurrentHashMap<Object, Object>(4);

        /**
         * {@inheritDoc}
         */
        @Override
        public Object get(Object key) {
            if (key == null) throw new IllegalArgumentException("key");
            return attributes.get(key);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Object set(Object key, Object value) {
            if (key == null) throw new IllegalArgumentException("key");

            if (value == null) {
                return attributes.remove(key);
            }

            return attributes.put(key, value);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Object setIfAbsent(Object key, Object value) {
            if (key == null) throw new IllegalArgumentException("key");
            if (value == null) {
                return null;
            }

            return attributes.putIfAbsent(key, value);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Object remove(Object key) {
            if (key == null) throw new IllegalArgumentException("key");
            return attributes.remove(key);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean contains(Object key) {
            if (key == null) throw new IllegalArgumentException("key");
            return attributes.containsKey(key);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Set<Object> getKeys() {
            synchronized (attributes) {
                return new HashSet<Object>(attributes.keySet());

            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final Object setAttribute(Object key, Object value) {
        return attributeMap.set(key, value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final Object getAttribute(Object key) {
        return attributeMap.get(key);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final Object setAttributeIfAbsent(Object key, Object value) {
        return attributeMap.setIfAbsent(key, value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final Object removeAttribute(Object key) {
        return attributeMap.remove(key);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final boolean containsAttribute(Object key) {
        return attributeMap.contains(key);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final Set<Object> getAttributeKeys() {
        return attributeMap.getKeys();
    }

    /**
     * {@inheritDoc}
     */
    public final ICloseFuture closeOnFlush() {
        if (!isClosing() && !isInvalid()) {
            getWriteQueue().offer(CLOSE_REQUEST);
            getProcessor().flush(this);
        }

        return getCloseFuture();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final ICloseFuture closeNow() {
        synchronized (lock) {
            if (isClosing() || isInvalid()) {
                return getCloseFuture();
            }

            stateRef.set(SState.CLOSING);

            try {
                destroyWriteQueue();
            } catch (Exception e) {
                getFilterChain().fireExceptionCaught(e);
            }
        }

        getFilterChain().fireFilterClose();

        return getCloseFuture();
    }

    /**
     * Destroy the session queue
     */
    private void destroyWriteQueue(){
        if (writePacketQueue != null) {
            while (!writePacketQueue.isEmpty()) {
                IWritePacket packet = writePacketQueue.poll();

                if (packet != null) {
                    IWriteFuture writeFuture = packet.getFuture();

                    // The WriteRequest may not always have a future : The CLOSE_REQUEST
                    // and MESSAGE_SENT_REQUEST don't.
                    if (writeFuture != null) {
                        Throwable cause = new IOException("session closed!");
                        writeFuture.setException(cause);
                    }
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IWriteFuture write(Object message){
        if (message == null) {
            throw new IllegalArgumentException("Trying to write a null message : not allowed");
        }

        IWriteFuture future = getWriteFuture();
        if(!isReady()){
            future.setException(new IllegalStateException("Trying to write a message to a closed session"));
            return future;
        }

        if ((message instanceof IoBuffer) && !((IoBuffer) message).hasRemaining()) {
            throw new IllegalArgumentException("message is empty. Forgot to call flip()?");
        }

        WritePacket packet = new WritePacket(message, future);
        packet.setStartTime(SystemClock.elapsedRealtime());
        getFilterChain().fireFilterWrite(packet);
        return future;
    }

    /**
     * Set the scheduledForFLush flag. As we may have concurrent access to this
     * flag, we compare and set it in one call.
     *
     * @param schedule
     *            the new value to set if not already set.
     * @return true if the session flag has been set, and if it wasn't set
     *         already.
     */
    public final boolean setScheduledForFlush(boolean schedule) {
        if (schedule) {
            // If the current tag is set to false, switch it to true,
            // otherwise, we do nothing but return false : the session
            // is already scheduled for flush
            return scheduledForFlush.compareAndSet(false, schedule);
        }

        scheduledForFlush.set(schedule);
        return true;
    }

    /**
     * Tells if the session is scheduled for flushed
     *
     * @return true if the session is scheduled for flush
     */
    public final boolean isScheduledForFlush() {
        return scheduledForFlush.get();
    }

    public abstract void setRemoteAddress(SocketAddress remoteAddress);

    public abstract void setRemoteAddress(String remoteHost, int remotePort);

    public abstract SocketAddress getRemoteAddress();

    public abstract void parseRemoteAddress() throws UnknownHostException;

    public abstract void setConnectDeadLine();

    public abstract boolean isConnectTimeout();

    protected abstract IConnectFuture getConnectFuture();

    public abstract ICloseFuture getCloseFuture();

    protected abstract IWriteFuture getWriteFuture();

    protected abstract void applySessionConfig();
}
