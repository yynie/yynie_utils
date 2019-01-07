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
     * @param filter The filter we are looking for
     *
     * @return <tt>true</tt> if this chain contains the specified <tt>filter</tt>.
     */
    boolean contains(IFilter filter);

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

        /**
         * @return the next Entry
         */
        Entry getNextEntry();

        /**
         * @return the previous Entry
         */
        Entry getPrevEntry();
    }

    /**
     * Fire a {@link IOHandler#sessionOpened(IOSession)} event.
     */
    void fireSessionOpened();

    /**
     * Fire a {@link IOHandler#sessionClosed(IOSession)} event.
     */
    void fireSessionClosed();

    /**
     * Fire a {@link IOHandler#exceptionCaught(IOSession, Throwable)} event.
     *
     * @param cause The exception cause
     */
    void fireExceptionCaught(Throwable cause);

    /**
     * Fire a {@link IOHandler#messageSent(IOSession, Object)} event.
     *
     * @param packet The sent packet
     */
    void fireMessageSent(IWritePacket packet);

    /**
     * Fire a {@link IOHandler#inputClosed(IOSession)} event.
     */
    void fireInputClosed();

    /**
     * Fire a {@link IOHandler#messageReceived(IOSession, Object)} event.
     *
     * @param message The received message
     */
    void fireMessageReceived(Object message);

    /**
     * Fire a {@link IOSession#closeNow()} or a {@link IOSession#closeOnFlush()} event.
     */
    void fireFilterClose();

    /**
     * Fire a {@link IOSession#write(Object)} event.
     *
     * @param writePacket The message to write
     */
    void fireFilterWrite(IWritePacket writePacket);

    /**
     * Fire a {@link IOHandler#sessionIdle(IOSession, IdleStatus)} event.
     *
     * @param status The current status to propagate
     */
    void fireSessionIdle(IdleStatus status);

}
