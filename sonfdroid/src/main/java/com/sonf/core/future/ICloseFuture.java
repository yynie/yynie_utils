package com.sonf.core.future;

public interface ICloseFuture extends IOFuture {
    /**
     * @return <tt>true</tt> if the close request is finished and the session is closed.
     */
    boolean isClosed();

    /**
     * Mark this future was finished and the close request has been perfectly done.
     */
    void setClosed();
}
