package com.sonf.future;

import com.sonf.core.future.DefaultIOFuture;
import com.sonf.core.future.ICloseFuture;
import com.sonf.core.session.IOSession;

/**
 * Inheriting class of {@link DefaultIOFuture} used for an async close request
 */
public class CloseFuture extends DefaultIOFuture implements ICloseFuture {

    /**
     * Constructor
     *
     * @param session the associated session
     */
    public CloseFuture(IOSession session) {
        super(session);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isClosed() {
        if (isDone()) {
            return ((Boolean) getValue()).booleanValue();
        } else {
            return false;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setClosed() {
        setValue(Boolean.TRUE);
    }
}
