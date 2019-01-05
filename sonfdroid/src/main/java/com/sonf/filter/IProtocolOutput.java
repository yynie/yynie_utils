package com.sonf.filter;

public interface IProtocolOutput {
    /**
     * @param message the output message from encoder/decoder
     */
    void write(Object message);

    /**
     * @return  the output message from encoder/decoder
     */
    Object get();

}
