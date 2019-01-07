package com.sonf.future;

import com.sonf.core.future.DefaultIOFuture;
import com.sonf.core.future.IConnectFuture;
import com.sonf.core.session.IOSession;

/**
 * Inheriting class of {@link DefaultIOFuture} used for an async connect request
 */
public class ConnectFuture extends DefaultIOFuture implements IConnectFuture {

    /**
     * Constructor
     *
     * @param session the associated session
     */
    public ConnectFuture(IOSession session) {
        super(session);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean cancel() {
        if (!isDone()) {
            boolean justCancelled = setValue(new Boolean(false));
            if(justCancelled) {
                getSession().cancelConnect(this);
            }
        }
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setConnected(){
        if (!isDone()) {
            setValue(new Boolean(true));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isConnected() {
        Object value = getValue();
        if(value instanceof Boolean){
            Boolean bval = (Boolean) value;
            return bval;
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isCanceled() {
        Object value = getValue();
        if(value instanceof Boolean){
            Boolean bval = (Boolean) value;
            return !bval;
        }
        return false;
    }

}
