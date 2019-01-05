package com.sonf.core.session;

import com.sonf.core.IOController;
import com.sonf.core.IOProcessor;
import com.sonf.core.buffer.IoBuffer;
import com.sonf.core.filter.IFilterChain;
import com.sonf.core.filter.IFilterChainMatcher;
import com.sonf.core.future.ICloseFuture;
import com.sonf.core.future.IOFuture;
import com.sonf.core.future.IWriteFuture;
import com.sonf.core.write.IWritePacket;

import java.util.Queue;
import java.util.Set;


public interface IOSession<CH, CG extends IOConfig> {
    /**
     * @return a unique identifier for this session.  Every session has its own
     * ID which is different from each other.
     */
    long getId();
    void setChannel(CH channel);
    CH getChannel();
    void setConfig(CG config);
    CG getConfig();
    IOFuture connect();
    /** {@hide} */
    void cancelConnect(IOFuture future);
    String getUniqueKey();

    void setStateClosed();
    void setStateReady();
    boolean isReady();
    boolean isConnecting();
    boolean isClosing();
    boolean isInvalid();
    boolean isNew();

    IoBuffer getIOBuffer();

    /**
     * @return the {@link IOController} which provides I/O service to this session.
     */
    IOController getController();

    /**
     * @return the filter chain that only affects this session.
     */
    IFilterChain getFilterChain();

    /**
     * set a filter chain matcher only used in this session
     */
    void setFilterChainMatcher(IFilterChainMatcher filterChainMatcher);

    /**
     * @return the filter chain matcher that only affects this session.
     */
    IFilterChainMatcher getFilterChainMatcher();

    /**
     * Sets a user-defined attribute.
     *
     * @param key the key of the attribute
     * @param value the value of the attribute
     * @return The old value of the attribute.  <tt>null</tt> if it is new.
     */
    Object setAttribute(Object key, Object value);

    /**
     * Returns the value of the user-defined attribute of this session.
     *
     * @param key the key of the attribute
     * @return <tt>null</tt> if there is no attribute with the specified key
     */
    Object getAttribute(Object key);

    /**
     * Sets a user defined attribute if the attribute with the specified key
     * is not set yet.  This method is same with the following code except
     * that the operation is performed atomically.
     * <pre>
     * if (containsAttribute(key)) {
     *     return getAttribute(key);
     * } else {
     *     return setAttribute(key, value);
     * }
     * </pre>
     *
     * @param key The key of the attribute we want to set
     * @param value The value we want to set
     * @return The old value of the attribute.  <tt>null</tt> if not found.
     */
    Object setAttributeIfAbsent(Object key, Object value);

    /**
     * Removes a user-defined attribute with the specified key.
     *
     * @param key The key of the attribute we want to remove
     * @return The old value of the attribute.  <tt>null</tt> if not found.
     */
    Object removeAttribute(Object key);
    /**
     * @param key The key of the attribute we are looking for in the session
     * @return <tt>true</tt> if this session contains the attribute with
     * the specified <tt>key</tt>.
     */
    boolean containsAttribute(Object key);

    /**
     * @return the set of keys of all user-defined attributes.
     */
    Set<Object> getAttributeKeys();


    /**
     * Closes this session immediately.  This operation is asynchronous, it
     * returns a {@link ICloseFuture}.
     *
     * @return The {@link ICloseFuture} that can be use to wait for the completion of this operation
     */
    ICloseFuture closeNow();

    /**
     * Closes this session after all queued write requests are flushed.  This operation
     * is asynchronous.  Wait for the returned {@link ICloseFuture} if you want to wait
     * for the session actually closed.
     *
     * @return The associated {@link ICloseFuture}
     */
    ICloseFuture closeOnFlush();

    /**
     * Writes the specified <code>message</code> to remote peer.  This
     * operation is asynchronous; {@link IOHandler#messageSent(IOSession,Object)}
     * will be invoked when the message is actually sent to remote peer.
     * You can also wait for the returned {@link IWriteFuture} if you want
     * to wait for the message actually written.
     *
     * @param message The message to write
     * @return The associated WriteFuture
     */
    IWriteFuture write(Object message);

    /**
     * Get the queue that contains the message waiting for being written.
     * As the reader might not be ready, it's frequent that the messages
     * aren't written completely, or that some older messages are waiting
     * to be written when a new message arrives. This queue is used to manage
     * the backlog of messages.
     *
     * @return The queue containing the pending messages.
     */
    Queue<IWritePacket> getWriteQueue();

    /**
     * Returns the {@link IWritePacket} which is being processed by
     * {@link IOProcessor}.
     *
     * @return <tt>null</tt> if and if only no message is being written
     */
    IWritePacket getCurrentWritePacket();

    /**
     *
     * Associate the current write request with the session
     *
     * @param writePacket the current write packet
     */
    void setCurrentWritePacket(IWritePacket writePacket);


    /**
     * @return a value of whether or not this service is active
     */
    boolean isActive();
}
