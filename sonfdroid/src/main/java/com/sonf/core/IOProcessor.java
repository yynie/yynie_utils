package com.sonf.core;

import com.sonf.core.session.IOSession;

public interface IOProcessor<S extends IOSession> {

    /**
     * @return <tt>true</tt> if and if only {@link #dispose()} method has
     * been called.  Please note that this method will return <tt>true</tt>
     * even after all the related resources are released.
     */
    boolean isDisposing();

    /**
     * @return <tt>true</tt> if all resources of this processor have been disposed.
     */
    boolean isDisposed();

    /**
     * Releases all resources allocated by this processor.  Please note that
     * this method might be blocked as long as there are any sessions
     * managed by this processor.
     */
    void dispose();


    /**
     * Adds a session to the processor to perform I/O operations with this session
     *
     * @param session The session to be added
     */
    void add(S session);

    /**
     * Remove the specified session from the processor so that the processor will closes
     * the connection with this seesion and releases all related resources.
     *
     * @param session The session to be removed
     */
    void remove(S session);

    /**
     * Flushe the internal write queue of the specified session
     *
     * @param session The session that has at lease one message to be send to the remote endpoint
     */
    void flush(S session);
}
