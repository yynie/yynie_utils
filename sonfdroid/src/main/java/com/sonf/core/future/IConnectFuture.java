package com.sonf.core.future;

public interface IConnectFuture extends IOFuture {
    /**
     * Try to cancel the connect request
     *
     * @return {@code true} if the future has been cancelled by this call, {@code false}
     * if the future was already cancelled or already done.
     */
    boolean cancel();

    /**
     * Sets the connect operation to be successfully done.
     * This method is invoked internally.  Please do not
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
