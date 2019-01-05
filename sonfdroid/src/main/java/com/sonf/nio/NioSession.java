package com.sonf.nio;



import android.os.SystemClock;

import com.sonf.core.IOController;
import com.sonf.core.IOProcessor;
import com.sonf.core.RuntimeIoException;
import com.sonf.core.future.ICloseFuture;
import com.sonf.core.future.IConnectFuture;
import com.sonf.core.future.IWriteFuture;
import com.sonf.core.session.AbstractIOSession;
import com.sonf.future.CloseFuture;
import com.sonf.future.ConnectFuture;
import com.sonf.future.WriteFuture;
import com.yynie.myutils.Logger;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

public class NioSession extends AbstractIOSession<SocketChannel, NioSocketConfig> {
    private Logger log = Logger.get(NioSession.class, Logger.Level.INFO);
    private InetSocketAddress remoteAddress;
    private String host;
    private int port;
    private final long UN_SET = 0L;
    private long connectDeadLine = UN_SET;
    private SelectionKey selectionKey;
    /**
     * A future that will be set 'closed' when the connection is closed.
     */
    private final CloseFuture closeFuture = new CloseFuture(this);

    public NioSession(IOController controller, IOProcessor<NioSession> processor) {
        super(controller, processor);
    }

    @Override
    protected IConnectFuture getConnectFuture(){
        return new ConnectFuture(this);
    }

    @Override
    public ICloseFuture getCloseFuture(){
        return closeFuture;
    }

    @Override
    protected IWriteFuture getWriteFuture(){
        return new WriteFuture(this);
    }

    @Override
    protected void applySessionConfig() {
        NioSocketConfig config = new NioSessionConfigImpl();
        config.setAll(getConfig());
        setConfig(config);
    }

    @Override
    public void setRemoteAddress(SocketAddress remoteAddress) {
        if(!isNew()){
            throw new IllegalStateException("Can change remote address of a session in use");
        }
        if(remoteAddress instanceof InetSocketAddress) {
            this.remoteAddress = (InetSocketAddress) remoteAddress;
        }else{
            throw new RuntimeException("Unsupported remoteAddress class!");
        }
    }

    @Override
    public void setRemoteAddress(String remoteHost, int remotePort) {
        if(!isNew()){
            throw new IllegalStateException("Can change remote address of a session in use");
        }
        this.remoteAddress = null;
        this.host = remoteHost;
        this.port = remotePort;
    }

    @Override
    public void parseRemoteAddress() throws UnknownHostException {
        if(remoteAddress != null) return;

        if(!DNSCache.isIpV4(host)) {
            log.i("parseRemoteAddress:" + host);
            InetAddress inetAddress = InetAddress.getByName(host);
            String ip = inetAddress.getHostAddress();
            remoteAddress = new InetSocketAddress(ip, port);
        }else{
            remoteAddress = new InetSocketAddress(host, port);
        }
    }

    @Override
    public final SocketAddress getRemoteAddress(){
        return remoteAddress;
    }

    private Socket getSocket() {
        return getChannel().socket();
    }

    @Override
    public String getUniqueKey(){
        InetAddress inetAddress = remoteAddress.getAddress();
        return inetAddress.getHostAddress() + ":" + remoteAddress.getPort();
    }

    @Override
    public void setConnectDeadLine(){
        connectDeadLine = SystemClock.elapsedRealtime() + getConfig().getConnectTimeoutMs();
    }

    @Override
    public boolean isConnectTimeout(){
        if(connectDeadLine == UN_SET){
            return false; //never timeout
        }
        return (SystemClock.elapsedRealtime() >= connectDeadLine);
    }

    /**
     * @return The {@link SelectionKey} associated with this session
     */
    public SelectionKey getSelectionKey() {
        return selectionKey;
    }

    /**
     * Sets the {@link SelectionKey} for this session
     *
     * @param key The new {@link SelectionKey}
     */
    public void setSelectionKey(SelectionKey key) {
        this.selectionKey = key;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final boolean isActive() {
        return this.selectionKey.isValid();
    }

    private class NioSessionConfigImpl extends NioSocketConfig{
        /**
         * {@inheritDoc}
         */
        @Override
        public int getSoLinger() {
            try {
                return getSocket().getSoLinger();
            } catch (SocketException e) {
                throw new RuntimeIoException(e);
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void setSoLinger(int linger) {
            super.setSoLinger(linger);
            log.i("setSoLinger: old = " + getSoLinger());
            try {
                if (linger < 0) {
                    getSocket().setSoLinger(false, 0);
                } else {
                    getSocket().setSoLinger(true, linger);
                }
            } catch (SocketException e) {
                throw new RuntimeIoException(e);
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int getSendBufferSize() {
            try {
                return getSocket().getSendBufferSize();
            } catch (SocketException e) {
                throw new RuntimeIoException(e);
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void setSendBufferSize(int size) {
            super.setSendBufferSize(size);
            log.i("setSendBufferSize: old = " + getSendBufferSize());
            if(size > 0) {
                try {
                    getSocket().setSendBufferSize(size);
                } catch (SocketException e) {
                    throw new RuntimeIoException(e);
                }
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int getReceiveBufferSize() {
            try {
                return getSocket().getReceiveBufferSize();
            } catch (SocketException e) {
                throw new RuntimeIoException(e);
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void setReceiveBufferSize(int size) {
            super.setReceiveBufferSize(size);
            log.i("setReceiveBufferSize: old = " + getReceiveBufferSize());
            if(size > 0) {
                try {
                    getSocket().setReceiveBufferSize(size);
                } catch (SocketException e) {
                    throw new RuntimeIoException(e);
                }
            }
        }
    }
}
