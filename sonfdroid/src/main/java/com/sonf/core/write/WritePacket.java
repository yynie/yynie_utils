package com.sonf.core.write;

import com.sonf.core.future.IWriteFuture;

public class WritePacket implements IWritePacket {
    private final Object origMessage;
    private Object message;
    private final IWriteFuture future;
    private long startTime;

    /**
     * Creates a new instance with {@link IWriteFuture}.
     *
     * @param message The message that will be written
     * @param future The associated {@link IWriteFuture}
     */
    public WritePacket(Object message, IWriteFuture future) {
        this.origMessage = message;
        this.message = message;
        this.future = future;
    }

    public IWriteFuture getFuture() {
        return future;
    }

    @Override
    public Object getMessage() {
        return message;
    }

    @Override
    public void setMessage(Object newMessage){
        message = newMessage;
    }

    @Override
    public long getStartTime() {
        return startTime;
    }

    @Override
    public void setStartTime(long elapsedRealTime) {
        this.startTime = elapsedRealTime;
    }

    @Override
    public Object getOrigMessage() {
        return origMessage;
    }
}
