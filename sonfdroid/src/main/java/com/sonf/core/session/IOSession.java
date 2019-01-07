package com.sonf.core.session;

import com.sonf.core.IOController;
import com.sonf.core.IOProcessor;
import com.sonf.core.buffer.IoBuffer;
import com.sonf.core.filter.IFilter;
import com.sonf.core.filter.IFilterChain;
import com.sonf.core.filter.IFilterChainMatcher;
import com.sonf.core.future.ICloseFuture;
import com.sonf.core.future.IOFuture;
import com.sonf.core.future.IWriteFuture;
import com.sonf.core.write.IWritePacket;
import com.sonf.future.ConnectFuture;

import java.util.Queue;
import java.util.Set;


public interface IOSession<CH, CG extends IOConfig> {
    /**
     * @return a unique identifier for this session.  Every session has its own
     * ID which is different from each other.
     */
    long getId();

    /**
     * Set the channel associated with this session
     *
     * @param channel
     */
    void setChannel(CH channel);

    /**
     * Get the channel associated with this session
     *
     * @return  channel
     */
    CH getChannel();


    /**
     * Set the configuration of this session.
     * @param  config  the {@link IOConfig}
     */
    void setConfig(CG config);

    /**
     * Get the configuration of this session.
     * Session's configuration will be applied only to this session itself.
     *
     * @return the {@link IOConfig} of this session.
     */
    CG getConfig();

    /**
     * Start a Connect-request to connect this session to it's remote address
     * This method should be called only when the session is in SState.NEW state.
     *
     * @return the {@link ConnectFuture} of the Connect-request
     */
    IOFuture connect();

    /**
     * Cancel the Connect-request associate with the specified {@link ConnectFuture} if
     * the session is in SState.CONNECTING state.
     *
     * @param future {@link ConnectFuture} of the Connect-request to be canceled
     */
    void cancelConnect(IOFuture future);

    /**
     * Unique key is of form:  ip:port
     *
     * @return  "ip:port " string
     *          or <tt>null</tt> if the remoteAddress has not been parsed successfully
     */
    String getUniqueKey();

    /**
     * Set the session to SState.INVALID means the session channel closed
     */
    void setStateClosed();

    /**
     * Set the session to SState.READY means the session channel connected ok
     * and is ready for I/O operations.
     */
    void setStateReady();

    /**
     * @return whether this session is ready for I/O operations.
     */
    boolean isReady();

    /**
     * @return whether this session is in connecting now
     */
    boolean isConnecting();

    /**
     * @return whether this session is in closing now
     */
    boolean isClosing();

    /**
     * @return whether this session is closed already
     */
    boolean isInvalid();

    /**
     * @return whether this session is a new one,
     *          you can use @link IOFuture connect()} method to start a connect-request
     */
    boolean isNew();

    /**
     * @param status {@link IdleStatus}
     * @return the count of the specified {@link IdleStatus} counter
     */
    int getIdleCount(IdleStatus status);

    /**
     * @return the IoBuffer used to read data from the session channel
     */
    IoBuffer getReadIOBuffer();

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
     * is not set yet.
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
     * Closes this session after all queued write packets are flushed.  This operation
     * is asynchronous.  Wait for the returned {@link ICloseFuture} if you want to wait
     * for the session actually closed.
     *
     * @return The associated {@link ICloseFuture}
     */
    ICloseFuture closeOnFlush();

    /**
     * Writes the specified <code>message</code> to remote endpoint.  This
     * operation is asynchronous; {@link IOHandler#messageSent(IOSession,Object)}
     * will be invoked when the message is actually sent.
     * You can also wait for the returned {@link IWriteFuture} if you want
     * to wait for the message actually written.
     *
     * @param message The message to write.
     *                Note that the message of IoBuffer class will be considered as a raw data
     *                and sent directly, all {@link IFilter} encoders on the session's FilterChain
     *                will be ignored.
     * @return The associated WriteFuture
     */
    IWriteFuture write(Object message);

    /**
     * Get the queue that contains the message waiting for being written.
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
