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
     * @return the size of the read buffer that I/O processor allocates
     * per each read.  It's unusual to adjust this property because
     * it's often adjusted automatically by the I/O processor.
     */
    int getReadBufferSize();

    /**
     * Sets the size of the read buffer that I/O processor allocates
     * per each read.  It's unusual to adjust this property because
     * it's often adjusted automatically by the I/O processor.
     *
     * @param readBufferSize The size of the read buffer
     */
    void setReadBufferSize(int readBufferSize);

    /**
     * @return limitation for the number of written bytes per flush
     */
    int getMaxWriteBytes();

    /**
     * Set limitation for the number of written bytes for read-write airness
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
     * Sets write timeout in seconds.
     *
     * @param writeTimeoutInMillis The timeout to set
     */
    void setWriteTimeoutInMillis(long writeTimeoutInMillis);
}
