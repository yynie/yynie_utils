package com.sonf.core.future;

import com.sonf.core.session.IOSession;

import java.util.concurrent.TimeUnit;

public interface IOFuture {

    /**
     * @return the {@link IOSession} which is associated with this future.
     */
    IOSession getSession();

    /**
     * Sets the exception as result
     *
     * @param exception
     */
    void setException(Throwable exception);

    /**
     * @return <tt>null</tt> if the operation is not finished
     *         or if the operation is successful, otherwise return the exception
     */
    Throwable getException();

    /**
     * @return <tt>true</tt> if the operation is completed whether or not successfully.
     */
    boolean isDone();

    /**
     * Wait for the asynchronous operation to complete uninterruptibly.
     */
    void awaitUninterruptibly();


    /**
     * Wait for the asynchronous operation to complete.
     * Return when the operation is completed or interrupted.
     *
     * @exception InterruptedException If the thread is interrupted while waiting
     */
    void await() throws InterruptedException;

    /**
     * Wait for the asynchronous operation to complete with the specified timeout.
     *
     * @param timeout The maximum delay to wait
     * @param unit the type of unit for the delay
     * @return <tt>true</tt> if the operation is completed.
     * @exception InterruptedException If the thread is interrupted while waiting
     */
    boolean await(long timeout, TimeUnit unit) throws InterruptedException;

    /**
     * Wait for the asynchronous operation to complete with the specified timeout.
     *
     * @param timeoutMillis The maximum milliseconds to wait
     * @return <tt>true</tt> if the operation is completed.
     * @exception InterruptedException If the thread is interrupted while waiting
     */
    boolean await(long timeoutMillis) throws InterruptedException;

    /**
     * Set a <tt>listener</tt> which is notified when
     * this future is completed. If the listener is added
     * after the completion, the listener is directly notified.
     *
     * @param listener The listener to set
     */
    void setListener(IoFutureListener listener);

    /**
     * Removes an existing <tt>listener</tt>
     */
    void removeListener();
}
