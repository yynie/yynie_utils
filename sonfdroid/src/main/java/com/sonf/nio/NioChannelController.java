package com.sonf.nio;

import com.sonf.polling.AbstractPollingIoController;
import com.sonf.core.IOProcessor;
import com.sonf.core.future.IOFuture;
import com.sonf.core.session.AbstractIOConfig;
import com.sonf.core.session.IOSession;
import com.yynie.myutils.Logger;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.Executor;

/**
 *  NIO TCP socket Channel Controller
 *  Inheriting class of {@link AbstractPollingIoController}.
 */
public class NioChannelController extends AbstractPollingIoController<NioSession, SocketChannel> {
    private Logger log = Logger.get(NioChannelController.class, Logger.Level.INFO);
    private volatile Selector selector;

    /**
     * Constructor for {@link NioChannelController} using default parameters
     */
    public NioChannelController() {
        this(null);
    }

    /**
     * Constructor for {@link NioChannelController} using provided executor
     */
    public NioChannelController(Executor executor) {
        this(executor, new NioSocketConfig());
    }

    /**
     * Constructor for {@link NioChannelController} using provided executor and configuration
     */
    public NioChannelController(Executor executor, NioSocketConfig config) {
        super(executor, NioProcessor.class, config);
        try {
            selector = Selector.open();

            setSelectable(true);
        } catch (IOException e) {
            e.printStackTrace();
            try {
                destroy();
            } catch (Exception e1) {
                e1.printStackTrace();
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void destroy() throws Exception {
        if (selector != null) {
            selector.close();
            selector = null;
            log.d("destroy: selector closed");
        }
        setSelectable(false);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NioSession buildSession(String host, Integer port){
        return buildSession(host, port, getConfig().clone());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected NioSession buildSession(String host, Integer port, AbstractIOConfig config) {
        if(isDisposing()){
            throw new IllegalStateException("Controller is disposing !");
        }
        if(isSecure()){
            if(port == null) port = 443;
            throw new RuntimeException("Not support Secure socket by now!");
        }else{
            if(port == null) port = 80;
        }

        if(DNSCache.isIpV4(host)){
            return createSession(new InetSocketAddress(host, port), config);
        }else{
            return createSession(host, port, config);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected NioSession newSession(IOProcessor<NioSession> processor){
        return new NioSession(this, processor);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected SocketChannel newChannel() throws IOException {
        SocketChannel ch = SocketChannel.open();
        int receiveBufferSize = getConfig().getReceiveBufferSize();
        if (receiveBufferSize > 65535) {
            ch.socket().setReceiveBufferSize(receiveBufferSize);
        }

        ch.configureBlocking(false);
        return ch;
    }

    /**
     *  {@inheritDoc}
     */
    @Override
    protected void closeChannel(SocketChannel channel) throws IOException {
        log.d("closeChannel in" );
        SelectionKey key = channel.keyFor(selector);

        if (key != null) {
            log.d("closeChannel  key canceled");
            key.cancel();
        }

        channel.close();
        log.i("closeChannel  channel closed");
    }

    /**
     *  {@inheritDoc}
     */
    @Override
    protected boolean connect(SocketChannel channel, SocketAddress remoteAddress) throws Exception {
        return channel.connect(remoteAddress);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean finishConnect(SocketChannel channel) throws IOException {
        if (channel.finishConnect()) {
            SelectionKey key = channel.keyFor(selector);
            if (key != null) {
                log.i("finishConnect  key canceled");
                key.cancel();
            }
            return true;
        }
        return false;
    }

    /**
     *  {@inheritDoc}
     */
    @Override
    protected void registerConnecting(SocketChannel channel, IOFuture future) throws ClosedChannelException {
        channel.register(selector, SelectionKey.OP_CONNECT, future);
    }

    /**
     *  {@inheritDoc}
     */
    @Override
    protected IOFuture getFuture(SocketChannel channel){
        SelectionKey key = channel.keyFor(selector);

        if ((key == null) || (!key.isValid())) {
            return null;
        }

        return (IOFuture) key.attachment();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected int select(int timeout) throws Exception {
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
    protected void wakeup(){
        selector.wakeup();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Iterator<SelectableChannel> selectedChannel(){
        return new ChannelIterator(selector.selectedKeys());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Iterator<SelectableChannel> allChannels() {
        return new ChannelIterator(selector.keys());
    }

    private static class ChannelIterator implements Iterator<SelectableChannel> {
        private final Iterator<SelectionKey> iterator;

        private ChannelIterator(Collection<SelectionKey> selectedKeys) {
            iterator = selectedKeys.iterator();
        }

        public boolean hasNext() {
            return iterator.hasNext();
        }

        public SelectableChannel next() {
            SelectionKey key = iterator.next();

            if(key.isValid() && key.isConnectable()) {
                SelectableChannel ch =  key.channel();
                return ch;
            }
            return null;
        }

        public void remove() {
            iterator.remove();
        }
    }
}
