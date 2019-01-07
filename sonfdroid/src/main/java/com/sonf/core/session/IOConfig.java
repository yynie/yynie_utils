package com.sonf.core.session;


public interface IOConfig {

    /**
     * Sets all configuration properties retrieved from the specified
     * <tt>config</tt>.
     *
     * @param config The configuration to use
     */
    void setAll(IOConfig config);

    /**
     * @return the size of the read buffer that I/O processor used for session's read operation.
     *          The default size is 2048 bytes
     */
    int getReadBufferSize();

    /**
     * Sets the size of the read buffer that I/O processor used for session's read operation
     *
     * @param readBufferSize The size of the read buffer
     */
    void setReadBufferSize(int readBufferSize);

    /**
     * @return limitation for the number of written bytes per flush
     *          the default value is calculated automatically based on the default readBufferSize
     *          for read-write fairness
     */
    int getMaxWriteBytes();

    /**
     * Set limitation for the number of written bytes for read-write fairness
     * @param maxWriteBytes The maximum write bytes
     */
    void setMaxWriteBytes(int maxWriteBytes);

    /**
     * @return idle time for the specified type of idleness in milliseconds.
     *
     * @param status The status for which we want the idle time (One of READER_IDLE,
     * WRITER_IDLE or BOTH_IDLE)
     */
    long getIdleTimeInMillis(IdleStatus status);

    /**
     * Sets idle time for the specified type of idleness in seconds.
     * @param status The status for which we want to set the idle time (One of READER_IDLE,
     * WRITER_IDLE or BOTH_IDLE)
     * @param idleTimeInMillis The time in millisecond to set
     */
    void setIdleTimeInMillis(IdleStatus status, long idleTimeInMillis);

    /**
     * @return write timeout in milliseconds.
     */
    long getWriteTimeoutInMillis();

    /**
     * Sets write timeout in milliseconds.
     *
     * @param writeTimeoutInMillis The timeout to set
     */
    void setWriteTimeoutInMillis(long writeTimeoutInMillis);
}
