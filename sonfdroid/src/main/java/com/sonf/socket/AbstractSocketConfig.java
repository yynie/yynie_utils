package com.sonf.socket;

import com.sonf.core.session.AbstractIOConfig;
import com.sonf.core.session.IOConfig;

import java.net.Socket;

public abstract class AbstractSocketConfig extends AbstractIOConfig {
    private long connectTimeoutMs = 60 * 1000L;
    /**
     * {@inheritDoc}
     */
    @Override
    public void setAll(IOConfig config) {
        super.setAll(config);
        if(config instanceof AbstractSocketConfig){
            AbstractSocketConfig imp = (AbstractSocketConfig)config;
            setConnectTimeoutMs(imp.getConnectTimeoutMs());
            setSoLinger(imp.getSoLinger());
        }
    }

    public long getConnectTimeoutMs(){
        return connectTimeoutMs;
    }

    public void setConnectTimeoutMs(long connectTimeoutMs) {
        this.connectTimeoutMs = connectTimeoutMs;
    }

    /**
     * Please note that enabling <tt>SO_LINGER</tt> in Java NIO can result
     * in platform-dependent behavior and unexpected blocking of I/O thread.
     *
     * @see Socket#getSoLinger()
     * @see <a href="http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6179351">Sun Bug Database</a>
     *
     * @return The value for <tt>SO_LINGER</tt>
     */
    public abstract int getSoLinger();

    /**
     * Please note that enabling <tt>SO_LINGER</tt> in Java NIO can result
     * in platform-dependent behavior and unexpected blocking of I/O thread.
     *
     * @param soLinger Please specify a negative value to disable <tt>SO_LINGER</tt>.
     *
     * @see Socket#setSoLinger(boolean, int)
     * @see <a href="http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6179351">Sun Bug Database</a>
     */
    public abstract void setSoLinger(int soLinger);
}
