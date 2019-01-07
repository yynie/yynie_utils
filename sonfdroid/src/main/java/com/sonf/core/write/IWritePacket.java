package com.sonf.core.write;

import android.os.SystemClock;

import com.sonf.core.filter.IFilter;
import com.sonf.core.future.IWriteFuture;

public interface IWritePacket {

    /**
     * @return the associated IWriteFuture with this packet
     */
    IWriteFuture getFuture();

    /**
     * @return the message to be written to then remote endpoint
     */
    Object getMessage();

    /**
     * @return the original message object from the very start of the write request.
     *        which is not transformed by any {@link IFilter}.
     */
    Object getOrigMessage();

    /**
     * Set the stored message object.
     * the message will be transformed by all chained {@link IFilter}s when passing by
     * @param newMessage transformed object.
     */
    void setMessage(Object newMessage);

    /**
     * @return the start time in milliseconds at which the packet is fired to write
     */
    long getStartTime();

    /**
     * Set the start time at which the packet is fired to write
     * @param elapsedRealTime milliseconds {@link SystemClock#elapsedRealtime()}
     */
    void setStartTime(long elapsedRealTime);
}
