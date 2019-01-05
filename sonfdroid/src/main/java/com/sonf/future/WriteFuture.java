package com.sonf.future;

import com.sonf.core.future.DefaultIOFuture;
import com.sonf.core.future.IWriteFuture;
import com.sonf.core.session.IOSession;

public class WriteFuture extends DefaultIOFuture implements IWriteFuture {
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
