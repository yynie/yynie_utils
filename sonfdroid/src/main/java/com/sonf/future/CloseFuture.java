package com.sonf.future;

import com.sonf.core.future.DefaultIOFuture;
import com.sonf.core.future.ICloseFuture;
import com.sonf.core.session.IOSession;

public class CloseFuture extends DefaultIOFuture implements ICloseFuture {
    public CloseFuture(IOSession session) {
        super(session);
    }

    /**
     * {@inheritDoc}
     */
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
    public void setClosed() {
        setValue(Boolean.TRUE);
    }
}
