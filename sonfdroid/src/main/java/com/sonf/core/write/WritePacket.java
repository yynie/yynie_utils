package com.sonf.core.write;

import com.sonf.core.future.IWriteFuture;

/**
 * implementatin of {@link IWritePacket}, represent for a data packet sent to remote endpoint
 */
public class WritePacket implements IWritePacket {
    private final Object origMessage;
    private Object message;
    private final IWriteFuture future;
    private long startTime;

    /**
     * Creates a new instance with {@link IWriteFuture}.
     *
     * @param message The original message that will be written
     * @param future The associated {@link IWriteFuture}
     */
    public WritePacket(Object message, IWriteFuture future) {
        this.origMessage = message;
        this.message = message;
        this.future = future;
    }

    /**
     *  {@inheritDoc}
     */
    @Override
    public IWriteFuture getFuture() {
        return future;
    }

    /**
     *  {@inheritDoc}
     */
    @Override
    public Object getMessage() {
        return message;
    }

    /**
     *  {@inheritDoc}
     */
    @Override
    public void setMessage(Object newMessage){
        message = newMessage;
    }

    @Override
    public long getStartTime() {
        return startTime;
    }

    /**
     *  {@inheritDoc}
     */
    @Override
    public void setStartTime(long elapsedRealTime) {
        this.startTime = elapsedRealTime;
    }

    /**
     *  {@inheritDoc}
     */
    @Override
    public Object getOrigMessage() {
        return origMessage;
    }
}
