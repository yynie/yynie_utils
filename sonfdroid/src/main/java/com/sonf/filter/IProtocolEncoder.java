package com.sonf.filter;

import com.sonf.core.session.IOSession;

public interface IProtocolEncoder {
    /**
     * Encode higher-level message into binary or protocol-specific data.
     * Invoked before a message fired to write to the remote endpoint
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
