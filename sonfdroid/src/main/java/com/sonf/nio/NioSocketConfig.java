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

import com.sonf.core.session.IOConfig;
import com.sonf.socket.AbstractSocketConfig;

/**
 * A default implementation of {@link AbstractSocketConfig}.
 *
 * @author <a href="mailto:yy_nie@hotmail.com">Yan.Nie</a>
 */
public class NioSocketConfig extends AbstractSocketConfig {
    private static final int DEFAULT_SO_LINGER = -1;

    /* The SO_RCVBUF parameter. Set to -1 (ie, will default to OS default) */
    private int receiveBufferSize = -1;

    /* The SO_SNDBUF parameter. Set to -1 (ie, will default to OS default) */
    private int sendBufferSize = -1;

    private int soLinger = DEFAULT_SO_LINGER;

    /**
     * {@inheritDoc}
     */
    @Override
    public int getReceiveBufferSize() {
        return receiveBufferSize;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setReceiveBufferSize(int receiveBufferSize) {
        this.receiveBufferSize = receiveBufferSize;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setAll(IOConfig config) {
        super.setAll(config);
        if(config instanceof NioSocketConfig){
            NioSocketConfig imp = (NioSocketConfig)config;

        }

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getSoLinger() {
        return soLinger;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setSoLinger(int soLinger) {
        this.soLinger = soLinger;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getSendBufferSize() {
        return sendBufferSize;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setSendBufferSize(int sendBufferSize) {
        this.sendBufferSize = sendBufferSize;
    }

}
