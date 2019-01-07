package com.sonf.core;

import com.sonf.core.filter.FilterChainBuilder;
import com.sonf.core.filter.IFilterChain;
import com.sonf.core.future.IOFuture;
import com.sonf.core.session.IOHandler;
import com.sonf.core.session.IOSession;
import com.sonf.future.ConnectFuture;

import java.nio.channels.Selector;


public interface IOController<S> {
    /**
     * Set the IOController to be activated when the first sessions
     * will be added to process, and this status will be quit only when all
     * sessions closed
     *
     * This API is used internal, never call it from the outside code.
     */
    void activate();

    /**
     * Set the IOController to be deactivated when the last session
     * was removed(closed) from the processor.
     *
     * This API is used internal, never call it from the outside code.
     */
    void deactivate();

    /**
     * @return a value of whether or not this Controller is active
     */
    boolean isActive();

    /**
     * Get the select timeout interval for OP_CONNECT
     * used for {@link Selector#select(long timeout)}
     *
     * @return interval time in millisecond
     */
    long getConnectCheckIntervalMs();

    /**
     * Set the select timeout intervel for OP_CONNECT
     * used for {@link Selector#select(long timeout)}
     * the value greater than 1000L will not take effect
     *
     * @param connectCheckIntervalMs interval time in millisecond
     */
    void setConnectCheckIntervalMs(long connectCheckIntervalMs);

    /**
     * Offer a Connect request to Controller,
     *
     * @param future the {@link ConnectFuture} implementation bound to
     *               the {@link IOSession} which should be built a connection
     *               to some remote endpoint.
     * @return <tt>true</tt> if success, <tt>false</tt> otherwise
     */
    boolean connect(IOFuture future);

    /**
     * Offer a cancel-connect request to Controller
     *
     * @param future the {@link ConnectFuture} implementation should be canceled
     */
    void cancelConnect(IOFuture future);

    /**
     * @return the {@link FilterChainBuilder} which will build the
     * {@link IFilterChain} of all {@link IOSession}s which is created
     * by this Controller.
     */
    FilterChainBuilder getFilterChainBuilder();

    /**
     * @return the handler which will handle all all callback events about the sessions
     * managed by this Controller.
     */
    IOHandler getHandler();

    /**
     * Sets the handler which will handle all callback events about the sessions
     * managed by this Controller.
     *
     * @param handler The IoHandler to use
     */
    void setHandler(IOHandler handler);

    /**
     * Releases all resources allocated by this Controller.  Please note that
     * this method might block as long as there are any sessions managed by
     * this Controller.
     */
    void dispose();

    /**
     * @return <tt>true</tt> if and if only {@link #dispose()} method has
     * been called.  Please note that this method will return <tt>true</tt>
     * even after all the related resources are released.
     */
    boolean isDisposing();

}
