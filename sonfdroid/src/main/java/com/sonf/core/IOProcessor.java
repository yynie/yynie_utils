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
     * @return <tt>true</tt> if and if only all resources of this processor
     * have been disposed.
     */
    boolean isDisposed();

    /**
     * Releases any resources allocated by this processor.  Please note that
     * the resources might not be released as long as there are any sessions
     * managed by this processor.  Most implementations will close all sessions
     * immediately and release the related resources.
     */
    void dispose();


    /**
     * Adds the specified {@code session} to the I/O processor so that
     * the I/O processor starts to perform any I/O operations related
     * with the {@code session}.
     *
     * @param session The added session
     */
    void add(S session);

    /**
     * Removes and closes the specified {@code session} from the I/O
     * processor so that the I/O processor closes the connection
     * associated with the {@code session} and releases any other related
     * resources.
     *
     * @param session The session to be removed
     */
    void remove(S session);

    /**
     * Flushes the internal write request queue of the specified
     * {@code session}.
     *
     * @param session The session we want the message to be written
     */
    void flush(S session);
}
