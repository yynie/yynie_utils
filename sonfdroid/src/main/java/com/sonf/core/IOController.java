package com.sonf.core;

import com.sonf.core.filter.FilterChainBuilder;
import com.sonf.core.filter.IFilterChain;
import com.sonf.core.future.IOFuture;
import com.sonf.core.session.IOHandler;
import com.sonf.core.session.IOSession;

public interface IOController<S> {
    void activate();
    void deactivate();
    boolean isActive();

    /**
     * get the select timeout intervel for OP_CONNECT
     *
     * @return interval time in millisecond
     */
    long getConnectCheckIntervalMs();

    /**
     * set the select timeout intervel for OP_CONNECT
     * the value greater than 1000L will not take effect
     *
     * @return interval time in millisecond
     */
    void setConnectCheckIntervalMs(long connectCheckIntervalMs);
    boolean connect(IOFuture future);
    void cancelConnect(IOFuture future);

    /**
     * @return the {@link FilterChainBuilder} which will build the
     * {@link IFilterChain} of all {@link IOSession}s which is created
     * by this service.
     */
    FilterChainBuilder getFilterChainBuilder();

    /**
     * @return the handler which will handle all connections managed by this service.
     */
    IOHandler getHandler();

    /**
     * Sets the handler which will handle all connections managed by this service.
     *
     * @param handler The IoHandler to use
     */
    void setHandler(IOHandler handler);

    /**
     * Releases any resources allocated by this service.  Please note that
     * this method might block as long as there are any sessions managed by
     * this service.
     */
    void dispose();
    /**
     * @return <tt>true</tt> if and if only {@link #dispose()} method has
     * been called.  Please note that this method will return <tt>true</tt>
     * even after all the related resources are released.
     */
    boolean isDisposing();

}
