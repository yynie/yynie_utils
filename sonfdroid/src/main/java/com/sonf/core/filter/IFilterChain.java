package com.sonf.core.filter;

import com.sonf.core.session.IOHandler;
import com.sonf.core.session.IOSession;
import com.sonf.core.session.IdleStatus;
import com.sonf.core.write.IWritePacket;

public interface IFilterChain {
    /**
     * @return the parent {@link IOSession} of this chain.
     */
    IOSession getSession();
    /**
     * Adds the specified filter with the specified name at the end of this chain.
     *
     * @param name The filter's name
     * @param filter The filter to add
     */
    void addLast(String name, IFilter filter);

    /**
     * Removes all filters added to this chain.
     */
    void clear();

    /**
     * Represents a name-filter pair that an {@link IFilterChain} contains.
     */
    interface Entry{
        /**
         * @return the name of the filter.
         */
        String getName();

        /**
         * @return the filter.
         */
        IFilter getFilter();

        Entry getNextEntry();

        Entry getPrevEntry();
    }

    /**
     * Fires a {@link IOHandler#sessionOpened(IOSession)} event. Most users don't need to call
     * this method at all. Please use this method only when you implement a new transport or
     * fire a virtual event.
     */
    void fireSessionOpened();

    /**
     * Fires a {@link IOHandler#sessionClosed(IOSession)} event. Most users don't need to call
     * this method at all. Please use this method only when you implement a new transport or
     * fire a virtual event.
     */
    void fireSessionClosed();

    /**
     * Fires a {@link IOHandler#exceptionCaught(IOSession, Throwable)} event. Most users don't
     * need to call this method at all. Please use this method only when you implement a new
     * transport or fire a virtual event.
     *
     * @param cause The exception cause
     */
    void fireExceptionCaught(Throwable cause);

    /**
     * Fires a {@link IOHandler#messageSent(IOSession, Object)} event. Most
     * users don't need to call this method at all. Please use this method only
     * when you implement a new transport or fire a virtual event.
     *
     * @param packet
     *            The sent request
     */
    void fireMessageSent(IWritePacket packet);

    /**
     * Fires a {@link IOHandler#inputClosed(IOSession)} event. Most users don't
     * need to call this method at all. Please use this method only when you
     * implement a new transport or fire a virtual event.
     */
    void fireInputClosed();

    /**
     * Fires a {@link IOHandler#messageReceived(IOSession, Object)} event. Most
     * users don't need to call this method at all. Please use this method only
     * when you implement a new transport or fire a virtual event.
     *
     * @param message
     *            The received message
     */
    void fireMessageReceived(Object message);

    /**
     * Fires a {@link IOSession#closeNow()} or a {@link IOSession#closeOnFlush()} event. Most users don't need to call this method at
     * all. Please use this method only when you implement a new transport or fire a virtual
     * event.
     */
    void fireFilterClose();

    /**
     * Fires a {@link IOSession#write(Object)} event. Most users don't need to
     * call this method at all. Please use this method only when you implement a
     * new transport or fire a virtual event.
     *
     * @param writePacket
     *            The message to write
     */
    void fireFilterWrite(IWritePacket writePacket);

    /**
     * Fires a {@link IOHandler#sessionIdle(IOSession, IdleStatus)} event. Most users don't
     * need to call this method at all. Please use this method only when you implement a new
     * transport or fire a virtual event.
     *
     * @param status The current status to propagate
     */
    void fireSessionIdle(IdleStatus status);

}
