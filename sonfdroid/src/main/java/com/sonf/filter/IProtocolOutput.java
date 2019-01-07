package com.sonf.filter;

/**
 * used by {@link IProtocolDecoder} or {@link IProtocolEncoder} to store processed messages.
 */
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
