package com.sonf.future;

import com.sonf.core.future.DefaultIOFuture;
import com.sonf.core.future.IWriteFuture;
import com.sonf.core.session.IOSession;

/**
 * Inheriting class of {@link DefaultIOFuture} used for an async write request
 */
public class WriteFuture extends DefaultIOFuture implements IWriteFuture {

    /**
     * Constructor
     *
     * @param session the associated session
     */
    public WriteFuture(IOSession session) {
        super(session);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setWritten() {
        setValue(Boolean.TRUE);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isWritten() {
        if (isDone()) {
            Object v = getValue();

            if (v instanceof Boolean) {
                return ((Boolean) v).booleanValue();
            }
        }

        return false;
    }
}
