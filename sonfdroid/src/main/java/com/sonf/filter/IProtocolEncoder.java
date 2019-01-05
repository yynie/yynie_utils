package com.sonf.filter;

import com.sonf.core.session.IOSession;

public interface IProtocolEncoder {
    /**
     * Encodes higher-level message objects into binary or protocol-specific data.
     * MINA invokes {@link #encode(IOSession, Object, IProtocolOutput)}
     * method with message which is popped from the session write queue, and then
     * the encoder implementation puts encoded messages into {@link IProtocolOutput}.
     *
     * @param session The current Session
     * @param message the message to encode
     * @param out The {@link IProtocolOutput} that will receive the encoded message
     * @throws Exception if the message violated protocol specification
     */
    void encode(IOSession session, Object message, IProtocolOutput out) throws Exception;

    /**
     * Releases all resources related with this encoder.
     *
     * @param session The current Session
     * @throws Exception if failed to dispose all resources
     */
    void dispose(IOSession session) throws Exception;
}
