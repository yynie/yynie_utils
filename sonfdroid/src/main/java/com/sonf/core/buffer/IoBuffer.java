package com.sonf.core.buffer;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;

public abstract class IoBuffer {
    /**
     * Returns the direct or heap buffer which is capable to store the specified
     * amount of bytes.
     *
     * @param capacity the capacity of the buffer
     * @return a IoBuffer which can hold up to capacity bytes
     *
     */
    public IoBuffer allocate(int capacity) {
        if (capacity < 0) {
            throw new IllegalArgumentException("capacity: " + capacity);
        }
        ByteBuffer buf = allocateNioBuffer(capacity, false);
        return wrap(buf);
    }

    protected abstract ByteBuffer allocateNioBuffer(int capacity, boolean direct);

    protected abstract IoBuffer wrap(ByteBuffer buf);

    public boolean available(){
        return (buf() != null);
    }

    /**
     * @see java.nio.Buffer#remaining()
     *
     * @return The remaining bytes in the buffer
     */
    public final int remaining() {
        ByteBuffer byteBuffer = buf();

        return byteBuffer.limit() - byteBuffer.position();
    }

    /**
     * @see java.nio.Buffer#hasRemaining()
     *
     * @return <tt>true</tt> if there are some remaining bytes in the buffer
     */
    public final boolean hasRemaining() {
        ByteBuffer byteBuffer = buf();

        return byteBuffer.limit() > byteBuffer.position();
    }

    public final void flip() {
        buf().flip();
    }

    /**
     * Declares this buffer and all its derived buffers are not used anymore
     * It is not mandatory to call this method, but you might want to invoke
     * this method for maximum performance.
     */
    public void free() {
    }

    public void clear(){
        buf().clear();
    }

    /**
     * @return the underlying NIO {@link ByteBuffer} instance.
     */
    public abstract ByteBuffer buf();

    /**
     * @see java.nio.Buffer#limit()
     *
     * @return the modified IoBuffer
    's limit
     */
    public int limit(){
        return buf().limit();
    }

    /**
     * @see java.nio.Buffer#limit(int)
     *
     * @param newLimit The new buffer's limit
     * @return the modified IoBuffer

     */
    public void limit(int newLimit){
        buf().limit(newLimit);
    }

    /**
     * @see java.nio.Buffer#position()
     * @return The current position in the buffer
     */
    public int position(){
        return buf().position();
    }

    public int capacity(){
        return buf().capacity();
    }
    /**
     * @see java.nio.Buffer#position(int)
     *
     * @param newPosition Sets the new position in the buffer
     * @return the modified IoBuffer

     */
    public void position(int newPosition){
        buf().position(newPosition);
    }

    /**
     * Returns the first occurrence position of the specified byte from the
     * current position to the current limit.
     *
     * @param b The byte we are looking for
     * @return <tt>-1</tt> if the specified byte is not found
     */
    public abstract int indexOf(byte b);

    /**
     * @see ByteBuffer#get()
     *
     * @return The byte at the current position
     */
    public abstract byte get();

    /**
     * @see ByteBuffer#get(int)
     *
     * @param index The position for which we want to read a byte
     * @return the byte at the given position
     */
    public abstract byte get(int index);

    /**
     * Reads a <code>NUL</code>-terminated string from this buffer using the
     * specified <code>decoder</code> and returns it. This method reads until
     * the limit of this buffer if no <tt>NUL</tt> is found.
     *
     * @param decoder The {@link CharsetDecoder} to use
     * @return the read String
     * @exception CharacterCodingException Thrown when an error occurred while decoding the buffer
     */
    public abstract String getString(CharsetDecoder decoder) throws CharacterCodingException;

    /**
     * Writes the content of <code>in</code> into this buffer using the
     * specified <code>encoder</code>. This method doesn't terminate string with
     * <tt>NUL</tt>. You have to do it by yourself.
     *
     * @param val The CharSequence to put in the IoBuffer
     * @param encoder The CharsetEncoder to use
     * @return The modified IoBuffer
     * @throws CharacterCodingException When we have an error while decoding the String
     */
    public abstract void putString(CharSequence val, CharsetEncoder encoder) throws CharacterCodingException;

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        if(buf() == null){
            buf.append("[]");
        }else {
            buf.append("[pos=");
            buf.append(position());
            buf.append(" lim=");
            buf.append(limit());
            buf.append(" cap=");
            buf.append(capacity());
            buf.append(']');
        }
        return buf.toString();
    }
}
