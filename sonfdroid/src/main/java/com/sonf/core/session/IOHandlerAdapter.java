package com.sonf.core.session;

import com.sonf.core.buffer.IoBuffer;
import com.yynie.myutils.Logger;

import java.nio.charset.Charset;

public class IOHandlerAdapter implements IOHandler {
    private Logger log = Logger.get(IOHandlerAdapter.class, Logger.Level.WARN);
    @Override
    public void sessionOpened(IOSession session) {
        log.w("sessionOpened : id = " + session.getId() + ", dest = " + session.getUniqueKey());
    }

    @Override
    public void sessionClosed(IOSession session) {
        log.w("sessionClosed : id = " + session.getId() + ", dest = " + session.getUniqueKey());
    }

    @Override
    public void exceptionCaught(IOSession session, Throwable throwable) {
        if(throwable instanceof Error){
            throwable.printStackTrace();
            log.e("session " + session.getId() + " caught:" + throwable.getMessage());
        }else{
            log.w("EXCEPTION, please implement " + getClass().getName()
                    + ".exceptionCaught() for proper handling:" + throwable);
        }
    }

    @Override
    public void messageSent(IOSession session, Object message) {
        log.w("messageSent : id = " + session.getId() + ", message = " + message);
    }

    @Override
    public void inputClosed(IOSession session) {
        log.w("inputClosed : session id = " + session.getId());
        session.closeNow();
    }

    @Override
    public void messageReceived(IOSession session, Object message) throws Exception {
        if(message instanceof IoBuffer){
            String info = ((IoBuffer)message).getString(Charset.forName("UTF-8").newDecoder());
            log.w("messageReceived : id = " + session.getId() + " message =" + info);
            ((IoBuffer)message).clear();
        }else {
            log.w("messageReceived : id = " + session.getId() + " message =" + message);
        }
    }

    @Override
    public void sessionIdle(IOSession session, IdleStatus status) throws Exception {
       // log.w("sessionIdle : id = " + session.getId() + ", status =" + status);
    }
}
