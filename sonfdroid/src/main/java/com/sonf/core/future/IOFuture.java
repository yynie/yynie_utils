package com.sonf.core.future;

import com.sonf.core.session.IOSession;

import java.util.concurrent.TimeUnit;

public interface IOFuture {
    /**
     * @return the {@link IOSession} which is associated with this future.
     */
    IOSession getSession();
    /**
     * Sets the exception caught due to connection failure and notifies all
     * threads waiting for this future.  This method is invoked by MINA
     * internally.  Please do not call this method directly.
     *
     * @param exception The exception to store in the ConnectFuture instance
     */
    void setException(Throwable exception);

    /**
     * Returns the cause of the connection failure.
     *
     * @return <tt>null</tt> if the connect operation is not finished yet,
     *         or if the connection attempt is successful, otherwise returns
     *         teh cause of the exception
     */
    Throwable getException();

    /**
     * @return <tt>true</tt> if the operation is completed.
     */
    boolean isDone();

    /**
     * Wait for the asynchronous operation to complete uninterruptibly.
     * The attached listeners will be notified when the operation is
     * completed.
     *
     * @return the current IoFuture
     */
    void awaitUninterruptibly();


    /**
     * Wait for the asynchronous operation to complete.
     * The attached listeners will be notified when the operation is
     * completed.
     *
     * @exception InterruptedException If the thread is interrupted while waiting
     */
    void await() throws InterruptedException;

    /**
     * Wait for the asynchronous operation to complete with the specified timeout.
     *
     * @param timeout The maximum delay to wait before getting out
     * @param unit the type of unit for the delay (seconds, minutes...)
     * @return <tt>true</tt> if the operation is completed.
     * @exception InterruptedException If the thread is interrupted while waiting
     */
    boolean await(long timeout, TimeUnit unit) throws InterruptedException;

    /**
     * Wait for the asynchronous operation to complete with the specified timeout.
     *
     * @param timeoutMillis The maximum milliseconds to wait before getting out
     * @return <tt>true</tt> if the operation is completed.
     * @exception InterruptedException If the thread is interrupted while waiting
     */
    boolean await(long timeoutMillis) throws InterruptedException;

    /**
     * Adds an event <tt>listener</tt> which is notified when
     * this future is completed. If the listener is added
     * after the completion, the listener is directly notified.
     *
     * @param listener The listener to add
     * @return the current IoFuture
     */
    void setListener(IoFutureListener listener);

    /**
     * Removes an existing event <tt>listener</tt> so it won't be notified when
     * the future is completed.
     *
     * @return the current IoFuture
     */
    void removeListener();
}
