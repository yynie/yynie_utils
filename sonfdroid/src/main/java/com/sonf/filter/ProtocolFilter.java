package com.sonf.filter;

import com.sonf.core.buffer.IoBuffer;
import com.sonf.core.filter.DefaultFilterChain;
import com.sonf.core.filter.IFilterAdapter;
import com.sonf.core.filter.IFilterChain;
import com.sonf.core.session.IOSession;
import com.sonf.core.write.IWritePacket;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;


public class ProtocolFilter extends IFilterAdapter {
    private IProtocolEncoder encoder;
    private IProtocolDecoder decoder;

    public void setEncoder(IProtocolEncoder encoder) {
        this.encoder = encoder;
    }

    public void setDecoder(IProtocolDecoder decoder) {
        this.decoder = decoder;
    }

    private void disposeCodec(IOSession session) {
        try {
            encoder.dispose(session);
            decoder.dispose(session);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private IProtocolOutput getDecoderOut(IOSession session) {
        IProtocolOutput out = (IProtocolOutput) session.getAttribute(DefaultFilterChain.SESSION_DECODER_OUT);
        if (out == null) {
            // Create a new instance, and stores it into the session
            out = new IProtocolOutputImpl();
            session.setAttribute(DefaultFilterChain.SESSION_DECODER_OUT, out);
        }
        return out;
    }

    private IProtocolOutput getEncoderOut(IOSession session) {
        IProtocolOutput out = (IProtocolOutput) session.getAttribute(DefaultFilterChain.SESSION_ENCODER_OUT);
        if (out == null) {
            // Create a new instance, and stores it into the session
            out = new IProtocolOutputImpl();
            session.setAttribute(DefaultFilterChain.SESSION_ENCODER_OUT, out);
        }
        return out;
    }

    private class IProtocolOutputImpl implements IProtocolOutput{
        private AtomicReference<Object> message = new AtomicReference<Object>();
        @Override
        public void write(Object message) {
            this.message.set(message);
        }

        @Override
        public Object get() {
            return message.getAndSet(null);
        }
    }
    @Override
    public void onPreAdd(IFilterChain parent, String name) throws Exception {
        if (parent.contains(this)) {
            throw new IllegalArgumentException(
                    "You can't add the same filter instance more than once.  Create another instance and add it.");
        }
    }

    @Override
    public void onPostRemove(IFilterChain parent, String name) throws Exception {
        disposeCodec(parent.getSession());
    }

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
