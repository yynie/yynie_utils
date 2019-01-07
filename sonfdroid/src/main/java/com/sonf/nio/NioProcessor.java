package com.sonf.nio;

import com.sonf.core.RuntimeIoException;
import com.sonf.core.buffer.IoBuffer;
import com.sonf.polling.AbstractPollingIoProcessor;
import com.sonf.polling.SessionState;
import com.yynie.myutils.Logger;

import java.io.IOException;
import java.nio.channels.ByteChannel;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.Executor;

/**
 * NIO TCP socket Channel Processor
 * Inheriting class of {@link AbstractPollingIoProcessor}.
 */
public class NioProcessor extends AbstractPollingIoProcessor<NioSession> {
    private final Logger log = Logger.get(NioProcessor.class, Logger.Level.INFO);
    /** The selector associated with this processor */
    private Selector selector;

    /**
     * Constructor
     *
     * @param executor The executor to use.
     *                 It should be the same one with {@limk IOController}
     */
    public NioProcessor(Executor executor) {
        super(executor);

        try {
            // Open a new selector
            selector = Selector.open();
        } catch (IOException e) {
            throw new RuntimeIoException("Failed to open a selector.", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void registerNewSelector() throws IOException {
        synchronized (selector) {
            Set<SelectionKey> keys = selector.keys();

            // Open a new selector
            Selector newSelector = Selector.open();

            // Loop on all the registered keys, and register them on the new selector
            for (SelectionKey key : keys) {
                SelectableChannel ch = key.channel();

                // Don't forget to attache the session, and back !
                NioSession session = (NioSession) key.attachment();
                SelectionKey newKey = ch.register(newSelector, key.interestOps(), session);
                session.setSelectionKey(newKey);
            }

            // Now we can close the old selector and switch it
            selector.close();
            selector = newSelector;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean isBrokenConnection() throws IOException {
        // A flag set to true if we find a broken session
        boolean brokenSession = false;

        synchronized (selector) {
            // Get the selector keys
            Set<SelectionKey> keys = selector.keys();

            // Loop on all the keys to see if one of them has a closed channel
            for (SelectionKey key : keys) {
                SelectableChannel channel = key.channel();

                if (((channel instanceof DatagramChannel) && !((DatagramChannel) channel).isConnected())
                        || ((channel instanceof SocketChannel) && !((SocketChannel) channel).isConnected())) {
                    // The channel is not connected anymore. Cancel
                    // the associated key then.
                    key.cancel();

                    // Set the flag to true to avoid a selector switch
                    brokenSession = true;
                }
            }
        }

        return brokenSession;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected SessionState getState(NioSession session) {
        SelectionKey key = session.getSelectionKey();

        if (key == null) {
            // The channel is not yet registred to a selector
            return SessionState.OPENING;
        }

        if (key.isValid()) {
            // The session is opened
            return SessionState.OPENED;
        } else {
            // The session still as to be closed
            return SessionState.CLOSING;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void initToRead(NioSession session) throws Exception {
        SelectableChannel ch = session.getChannel();
        ch.configureBlocking(false);
        session.setSelectionKey(ch.register(selector, SelectionKey.OP_READ, session));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void destroy(NioSession session) throws Exception {
        SelectableChannel ch = session.getChannel();

        SelectionKey key = session.getSelectionKey();

        if (key != null) {
            log.i("destroy  key canceled");
            key.cancel();
        }

        if ( ch.isOpen() ) {
            ch.close();
            log.i("destroy  channel closed");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected int select(long timeout) throws Exception {
        return selector.select(timeout);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean isSelectorEmpty() {
        return selector.keys().isEmpty();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void wakeup() {
        wakeupCalled.getAndSet(true);
        selector.wakeup();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Iterator<NioSession> allSessions() {
        return new IOSessionIterator(selector.keys());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Iterator<NioSession> selectedSessions() {
        return new IOSessionIterator(selector.selectedKeys());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void setInterestedInWrite(NioSession session, boolean isInterested){
        SelectionKey key = session.getSelectionKey();

        if ((key == null) || !key.isValid()) {
            return;
        }

        int newInterestOps = key.interestOps();

        if (isInterested) {
            newInterestOps |= SelectionKey.OP_WRITE;
        } else {
            newInterestOps &= ~SelectionKey.OP_WRITE;
        }

        key.interestOps(newInterestOps);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected int read(NioSession session, IoBuffer buf) throws Exception {
        ByteChannel channel = session.getChannel();

        return channel.read(buf.buf());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected int write(NioSession session, IoBuffer buf, int length) throws IOException {
        if (buf.remaining() <= length) {
            return session.getChannel().write(buf.buf());
        }

        int oldLimit = buf.limit();
        buf.limit(buf.position() + length);
        try {
            return session.getChannel().write(buf.buf());
        } finally {
            buf.limit(oldLimit);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean isReadable(NioSession session) {
        SelectionKey key = session.getSelectionKey();

        return (key != null) && key.isValid() && key.isReadable();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void doDispose() throws Exception {
        selector.close();
        selector = null;
        log.i("doDispose: selector closed");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean isWritable(NioSession session) {
        SelectionKey key = session.getSelectionKey();

        return (key != null) && key.isValid() && key.isWritable();
    }

    protected static class IOSessionIterator<NioSession> implements Iterator<NioSession> {
        private final Iterator<SelectionKey> iterator;

        /**
         * Create this iterator as a wrapper on top of the selectionKey Set.
         *
         * @param keys The set of selected sessions
         */
        private IOSessionIterator(Set<SelectionKey> keys) {
            iterator = keys.iterator();
        }

        /**
         * {@inheritDoc}
         */
        public boolean hasNext() {
            return iterator.hasNext();
        }

        /**
         * {@inheritDoc}
         */
        public NioSession next() {
            SelectionKey key = iterator.next();
            NioSession nioSession = (NioSession) key.attachment();
            return nioSession;
        }

        /**
         * {@inheritDoc}
         */
        public void remove() {
            iterator.remove();
        }
    }
}
