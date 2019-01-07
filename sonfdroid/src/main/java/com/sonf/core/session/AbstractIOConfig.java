package com.sonf.core.session;

import com.sonf.core.RuntimeIoException;

import java.lang.reflect.Constructor;
import java.net.Socket;

/**
 * Base implementation of {@link IOConfig}
 */
public abstract class AbstractIOConfig implements IOConfig {
    /** The default size of the buffer used to read incoming data */
    private int readBufferSize = 2048;

    /** Set limitation for the number of written bytes for read-write
     * fairness. default is readBufferSize * 3 / 2, which yields best
     * performance normally while not breaking fairness much.
     */
    private int maxWriteBytes = readBufferSize + (readBufferSize >>> 1);

    /** The delay before we notify a session that it has been idle on read. Default to infinite */
    private long idleTimeForReadMillis;

    /** The delay before we notify a session that it has been idle on write. Default to infinite */
    private long idleTimeForWriteMillis;

    /**
     * The delay before we notify a session that it has been idle on read and write.
     * Default to infinite
     **/
    private long idleTimeForBothMillis;

    /** The delay to wait for a write operation to complete before bailing out */
    private long writeTimeoutInMillis = 60 * 1000L;

    /**
     * {@inheritDoc}
     */
    @Override
    public int getReadBufferSize() {
        return readBufferSize;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setReadBufferSize(int readBufferSize) {
        if (readBufferSize <= 0) {
            throw new IllegalArgumentException("readBufferSize: " + readBufferSize + " (expected: 1+)");
        }
        this.readBufferSize = readBufferSize;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getMaxWriteBytes() {
        return maxWriteBytes;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setMaxWriteBytes(int maxWriteBytes) {
        this.maxWriteBytes = maxWriteBytes;
    }

    /**
     * @see Socket#getReceiveBufferSize()
     *
     * @return the size of the receive buffer
     */
    public abstract int getReceiveBufferSize();

    /**
     * @see Socket#setReceiveBufferSize(int)
     *
     * @param receiveBufferSize The size of the receive buffer
     */
    public abstract void setReceiveBufferSize(int receiveBufferSize);

    /**
     * @see Socket#getSendBufferSize()
     *
     * @return the size of the send buffer
     */
    public abstract int getSendBufferSize();

    /**
     * @see Socket#setSendBufferSize(int)
     *
     * @param sendBufferSize The size of the send buffer
     */
    public abstract void setSendBufferSize(int sendBufferSize);

    /**
     * {@inheritDoc}
     */
    @Override
    public void setAll(IOConfig config) {
        if(config instanceof AbstractIOConfig) {
            AbstractIOConfig imp = (AbstractIOConfig)config;
            setReadBufferSize(imp.getReadBufferSize());
            setMaxWriteBytes(imp.getMaxWriteBytes());
            setIdleTimeInMillis(IdleStatus.BOTH_IDLE, imp.getIdleTimeInMillis(IdleStatus.BOTH_IDLE));
            setIdleTimeInMillis(IdleStatus.READER_IDLE, imp.getIdleTimeInMillis(IdleStatus.READER_IDLE));
            setIdleTimeInMillis(IdleStatus.WRITER_IDLE, imp.getIdleTimeInMillis(IdleStatus.WRITER_IDLE));
            setWriteTimeoutInMillis(imp.getWriteTimeoutInMillis());
            setReceiveBufferSize(imp.getReceiveBufferSize());
            setSendBufferSize(imp.getSendBufferSize());
        }else{
            throw new RuntimeException("Unknown IOConfig implementation");
        }
    }

    /**
     * clone a new instance, all properties set to same value of this one
     *
     * @return a new AbstractIOConfig instance
     */
    public AbstractIOConfig clone(){
        Class<? extends AbstractIOConfig> type = this.getClass();
        Constructor<? extends AbstractIOConfig> constructor = null;
        try {
            constructor = type.getConstructor();
            AbstractIOConfig config = constructor.newInstance();
            config.setAll(this);
            return config;
        } catch (Exception e) {
            String msg = "Failed to create a new instance of " + type.getName() + ":" + e.getMessage();
            throw new RuntimeIoException(msg, e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getIdleTimeInMillis(IdleStatus status) {
        if (status == IdleStatus.BOTH_IDLE) {
            return idleTimeForBothMillis;
        }

        if (status == IdleStatus.READER_IDLE) {
            return idleTimeForReadMillis;
        }

        if (status == IdleStatus.WRITER_IDLE) {
            return idleTimeForWriteMillis;
        }

        throw new IllegalArgumentException("Unknown idle status: " + status);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setIdleTimeInMillis(IdleStatus status, long idleTimeInMillis) {
        if (idleTimeInMillis < 0) {
            throw new IllegalArgumentException("Illegal idle time: " + idleTimeInMillis);
        }

        if (status == IdleStatus.BOTH_IDLE) {
            idleTimeForBothMillis = idleTimeInMillis;
        } else if (status == IdleStatus.READER_IDLE) {
            idleTimeForReadMillis = idleTimeInMillis;
        } else if (status == IdleStatus.WRITER_IDLE) {
            idleTimeForWriteMillis = idleTimeInMillis;
        } else {
            throw new IllegalArgumentException("Unknown idle status: " + status);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getWriteTimeoutInMillis() {
        return writeTimeoutInMillis;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setWriteTimeoutInMillis(long writeTimeoutInMillis) {
        if (writeTimeoutInMillis < 0) {
            throw new IllegalArgumentException("Illegal write timeout: " + writeTimeoutInMillis);
        }
        this.writeTimeoutInMillis = writeTimeoutInMillis;
    }
}
