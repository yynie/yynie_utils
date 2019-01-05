/**
 * BSD 2-Clause License
 *
 * Copyright (c) 2018, yynie
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.

 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 */

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
 *
 *  @author <a href="mailto:yy_nie@hotmail.com">Yan.Nie</a>
 */
public class NioChannelController extends AbstractPollingIoController<NioSession, SocketChannel> {
    private Logger log = Logger.get(NioChannelController.class, Logger.Level.INFO);
    private volatile Selector selector;
    //private Object createLock = new Object();


    /**
     * Constructor for {@link NioChannelController} using default parameters (multiple thread model).
     */
    public NioChannelController() {
        this(null);
    }

    /**
     * Constructor for {@link NioChannelController} using default parameters (multiple thread model).
     */
    public NioChannelController(Executor executor) {
        this(executor, new NioSocketConfig());
    }

    /**
     * Constructor for {@link NioChannelController} using default parameters (multiple thread model).
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
            log.i("destroy: selector closed");
        }
        setSelectable(false);
    }

    public IOSession buildSession(String host, Integer port){
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
//        NioSession session;
        if(isSecure()){
            if(port == null) port = 443;
            throw new RuntimeException("Not support Secure socket by now!");
        }else{
            if(port == null) port = 80;
        }
//        String ip = host;
//
//        if(!DNSCache.isIpV4(host)){
//            log.i("buildSession: resolve address for host " + host);
//            try {
//                InetAddress inetAddress = InetAddress.getByName(host);
//                ip = inetAddress.getHostAddress();
//            } catch (UnknownHostException e) {
//                e.printStackTrace();
//                log.e("buildSession: can NOT resolve address for host " + host);
//                return null;
//            }
//        }

//        synchronized (createLock) {
//            session = getSessionByUnique(ip + ":" + port);
//            if (session != null) {
//                log.i("buildSession >> find session to " + ip + ":" + port);
//                return session;
//            }
//            log.i("buildSession >> create new session to " + ip + ":" + port);
//            return createSession(new InetSocketAddress(ip, port), config);
//        }
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
    protected void closeChannel(SocketChannel channel) throws IOException {
        log.i("closeChannel in" );
        SelectionKey key = channel.keyFor(selector);

        if (key != null) {
            log.i("closeChannel  key canceled");
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

    @Override
    protected int select() throws Exception {
        return selector.select();
    }

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
