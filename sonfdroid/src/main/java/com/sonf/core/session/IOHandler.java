package com.sonf.core.session;

public interface IOHandler {
    void sessionOpened(IOSession session);
    void sessionClosed(IOSession session);
    void exceptionCaught(IOSession session, Throwable throwable);
    void messageSent(IOSession session, Object message);
    /**
     * Handle the closure of an half-duplex TCP channel
     *
     * @param session The session which input is being closed
     */
    void inputClosed(IOSession session);
    /**
     * Invoked when a message is received.
     *
     * @param session The session that is receiving a message
     * @param message The received message
     * @throws Exception If we get an exception while processing the received message
     */
    void messageReceived(IOSession session, Object message) throws Exception;

    /**
     * Invoked with the related {@link IdleStatus} when a connection becomes idle.
     * This method is not invoked if the transport type is UDP; it's a known bug,
     * and will be fixed in 2.0.
     *
     * @param session The idling session
     * @param status The session's status
     * @throws Exception If we get an exception while processing the idle event
     */
    void sessionIdle(IOSession session, IdleStatus status) throws Exception;
}
