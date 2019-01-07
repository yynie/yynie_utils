package com.sonf.core.future;

import java.util.EventListener;

public interface IoFutureListener<F extends IOFuture> extends EventListener {
    /**
     * Invoked when the operation associated with the {@link IOFuture}
     * has been completed
     *
     * @param future  The source {@link IOFuture} which called this
     *                callback.
     */
    void onComplete(F future);
}
