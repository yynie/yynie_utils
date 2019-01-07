package com.sonf.socket;

import com.sonf.core.session.AbstractIOConfig;
import com.sonf.core.session.IOConfig;

import java.net.Socket;

/**
 * TCP Socket configuration
 * Inheriting class of {@link AbstractIOConfig}
 */
public abstract class AbstractSocketConfig extends AbstractIOConfig {
    /* connect time out used for all sessions connect operation */
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

    /**
     * Get the connect timeout milliseconds which will be used
     * for all sessions' connect operation
     *
     * @return connect timeout milliseconds
     */
    public long getConnectTimeoutMs(){
        return connectTimeoutMs;
    }

    /**
     * Set the connect timeout milliseconds,
     *
     * @param connectTimeoutMs
     */
    public void setConnectTimeoutMs(long connectTimeoutMs) {
        this.connectTimeoutMs = connectTimeoutMs;
    }

    /**
     * Please note that enabling <tt>SO_LINGER</tt> in Java NIO can result
     * in platform-dependent behavior and unexpected blocking of I/O thread.
     *
     * @see Socket#getSoLinger()
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
     */
    public abstract void setSoLinger(int soLinger);
}
