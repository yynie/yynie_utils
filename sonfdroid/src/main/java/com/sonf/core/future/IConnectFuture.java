package com.sonf.core.future;

public interface IConnectFuture extends IOFuture {
    /**
     * Cancels the connection attempt and notifies all threads waiting for
     * this future.
     *
     * @return {@code true} if the future has been cancelled by this call, {@code false}
     * if the future was already cancelled.
     */
    boolean cancel();

    /**
     * Sets the newly connected session and notifies all threads waiting for
     * this future.  This method is invoked internally.  Please do not
     * call this method directly.
     */
    void setConnected();

    /**
     * @return {@code true} if the connect operation is finished successfully.
     */
    boolean isConnected();

    /**
     * @return {@code true} if the connect operation has been canceled by
     * {@link #cancel()} method.
     */
    boolean isCanceled();
}
