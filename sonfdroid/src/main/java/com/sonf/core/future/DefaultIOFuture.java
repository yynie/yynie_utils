package com.sonf.core.future;

import android.os.SystemClock;

import com.sonf.core.session.IOSession;

import java.util.concurrent.TimeUnit;

/**
 * A Default implementation of {@link IOFuture}
 */
public class DefaultIOFuture implements IOFuture {
    private static final long DEAD_LOCK_CHECK_INTERVAL = 5000L;
    /** A lock used by the wait() method */
    private final Object lock;
    /** The associated session */
    private final IOSession session;
    /** The flag used to determinate if the Future is completed or not */
    private boolean ready;
    /** A counter for the number of threads waiting on this future */
    private int waiters;

    private Object result;

    private IoFutureListener<DefaultIOFuture> listener;

    /**
     * Constructor
     * @param session session associated with thi future
     */
    public DefaultIOFuture(IOSession session) {
        assert (session != null);
        this.session = session;
        this.lock = this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IOSession getSession() {
        return session;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setException(Throwable exception) {
        if (exception == null) {
            throw new IllegalArgumentException("exception");
        }

        setValue(exception);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Throwable getException() {
        Object v = getValue();

        if (v instanceof Throwable) {
            return (Throwable) v;
        } else {
            return null;
        }
    }

    /**
     * Set the result value and set this future to be done (ready = true).
     * The notify all waiters and listener
     *
     * @param newValue
     * @return false if the future is already done
     */
    public boolean setValue(Object newValue) {
        synchronized (lock) {
            // Allowed only once.
            if (ready) {
                return false;
            }

            result = newValue;
            ready = true;

            // Now, if we have waiters, notify them that the operation has completed
            if (waiters > 0) {
                lock.notifyAll();
            }
        }

        // Last, not least, inform the listeners
        notifyListener();
        return true;
    }

    /**
     * @return the result of the asynchronous operation.
     */
    protected Object getValue() {
        synchronized (lock) {
            return result;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isDone() {
        synchronized (lock) {
            return ready;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setListener(IoFutureListener listener) {
        this.listener = listener;
        if(isDone()){
            notifyListener();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeListener() {
        this.listener = null;
    }

    private void notifyListener() {
        if (listener != null) {
            listener.onComplete(this);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void awaitUninterruptibly() {
        try {
            await0(Long.MAX_VALUE, false);
        } catch (InterruptedException ie) {
            // Do nothing : this catch is just mandatory by contract
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void await() throws InterruptedException {
        synchronized (lock) {
            while (!ready) {
                waiters++;

                try {
                    // Wait for a notify, or if no notify is called,
                    // assume that we have a deadlock and exit the
                    // loop to check for a potential deadlock.
                    lock.wait(DEAD_LOCK_CHECK_INTERVAL);
                } finally {
                    waiters--;

                    if (!ready) {
                        checkDeadLock();
                    }
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean await(long timeout, TimeUnit unit) throws InterruptedException {
        return await0(unit.toMillis(timeout), true);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean await(long timeoutMillis) throws InterruptedException {
        return await0(timeoutMillis, true);
    }

    private boolean await0(long timeoutMillis, boolean interruptable) throws InterruptedException {
        long endTime = SystemClock.elapsedRealtime() + timeoutMillis;
        if (endTime < 0) {
            endTime = Long.MAX_VALUE;
        }
        synchronized (lock) {
            // We can quit if the ready flag is set to true, or if
            // the timeout is set to 0 or below : we don't wait in this case.
            if (ready||(timeoutMillis <= 0)) {
                return ready;
            }

            // The operation is not completed : we have to wait
            waiters++;

            try {
                for (;;) {
                    try {
                        long timeOut = Math.min(timeoutMillis, DEAD_LOCK_CHECK_INTERVAL);
                        // but every DEAD_LOCK_CHECK_INTERVAL seconds, we will check dead lock
                        lock.wait(timeOut);
                    } catch (InterruptedException e) {
                        if (interruptable) {
                            throw e;
                        }
                    }

                    if (ready || (endTime <= SystemClock.elapsedRealtime())) {
                        return ready;
                    } else {
                        // Take a chance, detect a potential deadlock
                        checkDeadLock();
                    }
                }
            } finally {
                // We get here for 3 possible reasons :
                // 1) We have been notified (the operation has completed a way or another)
                // 2) We have reached the timeout
                // 3) The thread has been interrupted
                // In any case, we decrement the number of waiters, and we get out.
                waiters--;

                if (!ready) {
                    checkDeadLock();
                }
            }
        }
    }

    /**
     * Check for a deadlock, avoid to await in a thread which is responsible for
     * commiting a result
     *
     * I haven't implement it by now, so you should be careful when use an IOFuture.
     * As what I mentioned above, Never wait and commit it in the same thread.
     */
    private void checkDeadLock() {

    }

}
