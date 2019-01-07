package com.sonf.filter;

import com.sonf.core.buffer.IoBuffer;
import com.sonf.core.session.IOSession;

public interface IProtocolDecoder {

    /**
     * Decodes binary or protocol-specific content into higher-level message objects.
     * Invoked when read data from session channel
     *
     * @param session The current Session
     * @param in the buffer to decode
     * @param out The {@link IProtocolOutput} that will receive the decoded message
     * @throws Exception if the read data violated protocol specification
     */
    void decode(IOSession session, IoBuffer in, IProtocolOutput out) throws Exception;

    /**
     * Invoked when the specified <tt>session</tt> is closed.  This method is useful
     * when you deal with the protocol which doesn't specify the length of a message
     * such as HTTP response without <tt>content-length</tt> header. Implement this
     * method to process the remaining data that {@link #decode(IOSession, IoBuffer, IProtocolOutput)}
     * method didn't process completely.
     *
     * @param session The current Session
     * @param out The {@link IProtocolOutput} that contains the decoded message
     * @throws Exception if the read data violated protocol specification
     */
    void finishDecode(IOSession session, IProtocolOutput out) throws Exception;

    /**
     * Releases all resources related with this decoder.
     *
     * @param session The current Session
     * @throws Exception if failed to dispose all resources
     */
    void dispose(IOSession session) throws Exception;
}
