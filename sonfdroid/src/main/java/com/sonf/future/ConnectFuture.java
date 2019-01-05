package com.sonf.future;

import com.sonf.core.future.DefaultIOFuture;
import com.sonf.core.future.IConnectFuture;
import com.sonf.core.session.IOSession;

public class ConnectFuture extends DefaultIOFuture implements IConnectFuture {

    public ConnectFuture(IOSession session) {
        super(session);
    }

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

    @Override
    public void setConnected(){
        if (!isDone()) {
            setValue(new Boolean(true));
        }
    }

    @Override
    public boolean isConnected() {
        Object value = getValue();
        if(value instanceof Boolean){
            Boolean bval = (Boolean) value;
            return bval;
        }
        return false;
    }

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
