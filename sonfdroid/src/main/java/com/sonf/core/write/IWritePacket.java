package com.sonf.core.write;

import com.sonf.core.future.IWriteFuture;

public interface IWritePacket {

    IWriteFuture getFuture();

    Object getMessage();

    Object getOrigMessage();

    void setMessage(Object newMessage);

    long getStartTime();

    void setStartTime(long elapsedRealTime);
}
