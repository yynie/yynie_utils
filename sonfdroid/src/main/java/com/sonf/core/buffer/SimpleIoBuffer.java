package com.sonf.core.buffer;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;

/**
 * Simple Inheriting class of {@link IoBuffer}
 */
public class SimpleIoBuffer extends IoBuffer{
    private ByteBuffer nioBuffer;

    /**
     * {@inheritDoc}
     */
    @Override
    protected ByteBuffer allocateByteBuffer(int capacity, boolean direct) {
        ByteBuffer nioBuffer;
        if (direct) {
            nioBuffer = ByteBuffer.allocateDirect(capacity);
        } else {
            nioBuffer = ByteBuffer.allocate(capacity);
        }
        nioBuffer.clear();
        return nioBuffer;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void wrap(ByteBuffer buf){
        nioBuffer = buf;
        nioBuffer.order(ByteOrder.BIG_ENDIAN);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ByteBuffer buf(){
        return nioBuffer;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final byte get() {
        return buf().get();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final byte get(int index) {
        return buf().get(index);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int indexOf(byte b) {
        if (nioBuffer.hasArray()) {
            int arrayOffset = nioBuffer.arrayOffset();
            int beginPos = arrayOffset + position();
            int limit = arrayOffset + limit();
            byte[] array = nioBuffer.array();
            for (int i = beginPos; i < limit; i++) {
                if (array[i] == b) {
                    return i - arrayOffset;
                }
            }
        } else {
            int beginPos = position();
            int limit = limit();
            for (int i = beginPos; i < limit; i++) {
                if (get(i) == b) {
                    return i;
                }
            }
        }
        return -1;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getString(CharsetDecoder decoder) throws CharacterCodingException {
        if (!hasRemaining()) {
            return "";
        }
        boolean utf16 = decoder.charset().name().startsWith("UTF-16");

        int oldPos = position();
        int oldLimit = limit();
        int end = -1;
        int newPos;

        if (!utf16) {
            end = indexOf((byte) 0x00);
            if (end < 0) {
                newPos = end = oldLimit;
            } else {
                newPos = end + 1;
            }
        } else {
            int i = oldPos;
            for (;;) {
                boolean wasZero = get(i) == 0;
                i++;
                if (i >= oldLimit) {
                    break;
                }
                if (get(i) != 0) {
                    i++;
                    if (i >= oldLimit) {
                        break;
                    }
                    continue;
                }
                if (wasZero) {
                    end = i - 1;
                    break;
                }
            }
            if (end < 0) {
                newPos = end = oldPos + (oldLimit - oldPos & 0xFFFFFFFE);
            } else {
                if (end + 2 <= oldLimit) {
                    newPos = end + 2;
                } else {
                    newPos = end;
                }
            }
        }

        if (oldPos == end) {
            position(newPos);
            return "";
        }

        limit(end);
        decoder.reset();

        int expectedLength = (int) (remaining() * decoder.averageCharsPerByte()) + 1;
        CharBuffer out = CharBuffer.allocate(expectedLength);
        for (;;) {
            CoderResult cr;
            if (hasRemaining()) {
                cr = decoder.decode(buf(), out, true);
            } else {
                cr = decoder.flush(out);
            }

            if (cr.isUnderflow()) {
                break;
            }

            if (cr.isOverflow()) {
                CharBuffer o = CharBuffer.allocate(out.capacity() + expectedLength);
                out.flip();
                o.put(out);
                out = o;
                continue;
            }

            if (cr.isError()) {
                // Revert the buffer back to the previous state.
                limit(oldLimit);
                position(oldPos);
                cr.throwException();
            }
        }

        limit(oldLimit);
        position(newPos);
        return out.flip().toString();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void putString(CharSequence val, CharsetEncoder encoder) throws CharacterCodingException {
        if (val.length() == 0) {
            return;
        }
        CharBuffer in = CharBuffer.wrap(val);
        encoder.reset();
        for (;;) {
            CoderResult cr;
            if (in.hasRemaining()) {
                cr = encoder.encode(in, buf(), true);
            } else {
                cr = encoder.flush(buf());
            }

            if (cr.isUnderflow()) {
                break;
            }
            if (cr.isOverflow()) {
                throw new RuntimeException("but that wasn't enough for '" + val + "'");
            }
            cr.throwException();
        }
    }

}
