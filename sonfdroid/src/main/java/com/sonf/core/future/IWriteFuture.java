package com.sonf.core.future;

public interface IWriteFuture extends IOFuture {
    /**
     * Sets the message is written,
     * This method is invoked internally.  Please do
     * not call this method directly.
     */
    void setWritten();

    /**
     * @return <tt>true</tt> if the write operation is finished successfully.
     */
    boolean isWritten();
}
