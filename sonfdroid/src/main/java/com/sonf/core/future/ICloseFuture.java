package com.sonf.core.future;

public interface ICloseFuture extends IOFuture {
    /**
     * @return <tt>true</tt> if the close request is finished and the session is closed.
     */
    boolean isClosed();

    /**
     * Marks this future as closed and notifies all threads waiting for this
     * future. This method is invoked by MINA internally.  Please do not call
     * this method directly.
     */
    void setClosed();
}
