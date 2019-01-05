package com.sonf.core.filter;

import com.sonf.core.session.IOHandler;
import com.sonf.core.session.IOSession;
import com.sonf.core.filter.IFilterChain.Entry;
import com.sonf.core.session.IdleStatus;
import com.sonf.core.write.IWritePacket;

public interface IFilter {

    /**
     * Invoked before this filter is added to the specified <tt>parent</tt>.
     * Please note that this method can be invoked more than once if
     * this filter is added to more than one parents.
     *
     * @param parent the parent who called this method
     * @param name the name assigned to this filter
     * @throws Exception If an error occurred while processing the event
     */
    void onPreAdd(IFilterChain parent, String name) throws Exception;

    /**
     * Invoked after this filter is added to the specified <tt>parent</tt>.
     * Please note that this method can be invoked more than once if
     * this filter is added to more than one parents.
     *
     * @param parent the parent who called this method
     * @param name the name assigned to this filter

     * @throws Exception If an error occurred while processing the event
     */
    void onPostAdd(IFilterChain parent, String name) throws Exception;

    /**
     * Invoked before this filter is removed from the specified <tt>parent</tt>.
     * Please note that this method can be invoked more than once if
     * this filter is removed from more than one parents.
     *
     * @param parent the parent who called this method
     * @param name the name assigned to this filter
     * @throws Exception If an error occurred while processing the event
     */
    void onPreRemove(IFilterChain parent, String name) throws Exception;

    /**
     * Invoked after this filter is removed from the specified <tt>parent</tt>.
     * Please note that this method can be invoked more than once if
     * this filter is removed from more than one parents.
     *
     * @param parent the parent who called this method
     * @param name the name assigned to this filter
     * @throws Exception If an error occurred while processing the event
     */
    void onPostRemove(IFilterChain parent, String name) throws Exception;

    /**
     * Filters {@link IOHandler#sessionOpened(IOSession)} event.
     *
     * @param next next filter entry
     * @param session The {@link IOSession} which has received this event
     * @throws Exception If an error occurred while processing the event
     */
    void sessionOpened(Entry next, IOSession session);

    /**
     * Filters {@link IOHandler#sessionClosed(IOSession)} event.
     *
     * @param next next filter entry
     * @param session The {@link IOSession} which has received this event
     * @throws Exception If an error occurred while processing the event
     */
    void sessionClosed(Entry next, IOSession session) throws Exception;

    /**
     * Filters {@link IOHandler#exceptionCaught(IOSession,Throwable)} event.
     *
     * @param next next filter entry
     * @param session The {@link IOSession} which has received this event
     * @param cause The exception that cause this event to be received
     * @throws Exception If an error occurred while processing the event
     */
    void exceptionCaught(Entry next, IOSession session, Throwable cause);

    /**
     * Forwards <tt>messageSent</tt> event to next filter.
     *
     * @param next next filter entry
     * @param session The {@link IOSession} which has to process this invocation
     * @param packet The {@link IWritePacket} to process
     */
    void messageSent(Entry next, IOSession session, IWritePacket packet);

    /**
     * Filters {@link IOHandler#inputClosed(IOSession)} event.
     *
     * @param next next filter entry
     * @param session The {@link IOSession} which has received this event
     * @throws Exception If an error occurred while processing the event
     */
    void inputClosed(Entry next, IOSession session);

    /**
     * Filters {@link IOSession#closeNow()} or a {@link IOSession#closeOnFlush()} method invocations.
     *
     * @param prev prev filter entry
     * @param session
     *            The {@link IOSession} which has to process this method
     *            invocation
     * @throws Exception If an error occurred while processing the event
     */
    void filterClose(Entry prev, IOSession session);

    /**
     * Filters {@link IOSession#write(Object)} method invocation.
     *
     * @param prev prev filter entry
     * @param session The {@link IOSession} which has to process this invocation
     * @param writePacket The {@link IWritePacket} to process
     * @throws Exception If an error occurred while processing the event
     */
    void filterWrite(Entry prev, IOSession session, IWritePacket writePacket) throws Exception;

    /**
     * Filters {@link IOHandler#messageReceived(IOSession,Object)} event.
     *
     * @param next next filter entry
     * @param message The received message
     * @throws Exception If an error occurred while processing the event
     */
    void messageReceived(Entry next, IOSession session, Object message) throws Exception;

    /**
     * Filters {@link IOHandler#sessionIdle(IOSession,IdleStatus)} event.
     *
     * @param next next filter entry
     * @param session The {@link IOSession} which has received this event
     * @param status The {@link IdleStatus} type
     * @throws Exception If an error occurred while processing the event
     */
    void sessionIdle(Entry next, IOSession session, IdleStatus status) throws Exception;
}
