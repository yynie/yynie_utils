package com.sonf.filter;

import com.sonf.core.buffer.IoBuffer;
import com.sonf.core.filter.IFilterAdapter;
import com.sonf.core.filter.IFilterChain;
import com.sonf.core.session.AttributeKey;
import com.sonf.core.session.IOSession;
import com.sonf.core.write.IWritePacket;
import com.yynie.myutils.Logger;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

/**
 * An filter which translates binary or protocol specific data into
 * message objects and vice versa using {@link IProtocolEncoder}, or {@link IProtocolDecoder}.
 */
public class ProtocolFilter extends IFilterAdapter {
    private final Logger log = Logger.get(ProtocolFilter.class, Logger.Level.INFO);
    public static final AttributeKey DECODER_OUT = new AttributeKey(ProtocolFilter.class, "decoderOut");
    public static final AttributeKey ENCODER_OUT = new AttributeKey(ProtocolFilter.class, "encoderOut");
    private IProtocolEncoder encoder;
    private IProtocolDecoder decoder;

    /**
     * Set a encoder instance
     * @param encoder
     */
    public void setEncoder(IProtocolEncoder encoder) {
        this.encoder = encoder;
    }

    /**
     * Set a decoder instance
     * @param decoder
     */
    public void setDecoder(IProtocolDecoder decoder) {
        this.decoder = decoder;
    }

    private void disposeCodec(IOSession session) {
        try {
            encoder.dispose(session);
            decoder.dispose(session);
            session.removeAttribute(DECODER_OUT);
            session.removeAttribute(ENCODER_OUT);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private IProtocolOutput getDecoderOut(IOSession session) {
        IProtocolOutput out = (IProtocolOutput) session.getAttribute(DECODER_OUT);
        if (out == null) {
            // Create a new instance, and stores it into the session
            out = new IProtocolOutputImpl();
            session.setAttribute(DECODER_OUT, out);
        }
        return out;
    }

    private IProtocolOutput getEncoderOut(IOSession session) {
        IProtocolOutput out = (IProtocolOutput) session.getAttribute(ENCODER_OUT);
        if (out == null) {
            // Create a new instance, and stores it into the session
            out = new IProtocolOutputImpl();
            session.setAttribute(ENCODER_OUT, out);
        }
        return out;
    }

    /**
     * implementation of {@link IProtocolOutput}
     */
    private class IProtocolOutputImpl implements IProtocolOutput{
        private AtomicReference<Object> message = new AtomicReference<Object>();

        /**
         * {@inheritDoc}
         */
        @Override
        public void write(Object message) {
            this.message.set(message);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Object get() {
            return message.getAndSet(null);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onPreAdd(IFilterChain parent, String name) throws Exception {
        if (parent.contains(this)) {
            throw new IllegalArgumentException(
                    "You can't add the same filter instance more than once.  Create another instance and add it.");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onPostRemove(IFilterChain parent, String name) throws Exception {
        disposeCodec(parent.getSession());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void sessionClosed(IFilterChain.Entry next, IOSession session) throws Exception{
        // Call finishDecode() first when a connection is closed.
        IProtocolOutput decoderOut = getDecoderOut(session);

        try {
            decoder.finishDecode(session, decoderOut);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            // Dispose everything
            disposeCodec(session);
            Object remain = decoderOut.get();
            if(remain != null){
                next.getFilter().messageReceived(next.getNextEntry(), session, remain);
            }
        }
        // Call the next filter
        next.getFilter().sessionClosed(next.getNextEntry(), session);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void filterWrite(IFilterChain.Entry prev, IOSession session, IWritePacket writePacket) throws Exception {
        Object message = writePacket.getMessage();

        // Bypass the encoding if the message is contained in a IoBuffer,
        // as it has already been encoded before
        if (message instanceof IoBuffer) {
            prev.getFilter().filterWrite(prev.getPrevEntry(), session, writePacket);
            return;
        }
        if (encoder == null) {
            throw new Exception("The encoder is null for the session " + session.getId());
        }

        IProtocolOutput encoderOut = getEncoderOut(session);
        encoder.encode(session, message, encoderOut);
        Object encoded = encoderOut.get();
        if(encoded != null){
            writePacket.setMessage(encoded);
        }else{
            throw new IOException("Invalid message :" + writePacket.getOrigMessage());
        }
        prev.getFilter().filterWrite(prev.getPrevEntry(), session, writePacket);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void messageReceived(IFilterChain.Entry next, IOSession session, Object message) throws Exception {
        if (!(message instanceof IoBuffer)) {
            next.getFilter().messageReceived(next.getNextEntry(), session, message);
            return;
        }
        IoBuffer in = (IoBuffer) message;
        IProtocolOutput decoderOut = getDecoderOut(session);
        while (in.hasRemaining()) {
            int oldPos = in.position();
            synchronized (session) {
                // Call the decoder with the read bytes
                try {
                    decoder.decode(session, in, decoderOut);
                    if(oldPos == in.position()){
                        log.e("messageReceived: message buf stays in the old position after Decoder. Your Decoder may not do its job correctly!");
                    }
                    Object decoded = decoderOut.get();
                    if (decoded != null) {
                        next.getFilter().messageReceived(next.getNextEntry(), session, decoded);
                    }
                }catch (Exception e) {
                    next.getFilter().exceptionCaught(next.getNextEntry(), session, e);
                }
            }
        }
    }
}
