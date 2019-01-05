package com.sonf.core.filter;

import com.sonf.core.session.IOSession;
import com.sonf.core.session.IdleStatus;
import com.sonf.core.write.IWritePacket;

/**
 * An adapter class for {@link IFilter}.  You can extend
 * this class and selectively override required event filter methods only.  All
 * methods forwards events to the next filter by default.
 *
 */
public class IFilterAdapter implements IFilter {
    @Override
    public void onPreAdd(IFilterChain parent, String name) throws Exception {

    }

    @Override
    public void onPostAdd(IFilterChain parent, String name) throws Exception {

    }

    @Override
    public void onPreRemove(IFilterChain parent, String name) throws Exception {

    }

    @Override
    public void onPostRemove(IFilterChain parent, String name) throws Exception {

    }

    @Override
    public void sessionOpened(IFilterChain.Entry next, IOSession session) {
        next.getFilter().sessionOpened(next.getNextEntry(), session);
    }

    @Override
    public void sessionClosed(IFilterChain.Entry next, IOSession session) {
        next.getFilter().sessionClosed(next.getNextEntry(), session);
    }

    @Override
    public void exceptionCaught(IFilterChain.Entry next, IOSession session, Throwable cause) {
        next.getFilter().exceptionCaught(next.getNextEntry(), session, cause);
    }

    @Override
    public void messageSent(IFilterChain.Entry next, IOSession session, IWritePacket packet) {
        next.getFilter().messageSent(next.getNextEntry(), session, packet);
    }

    @Override
    public void inputClosed(IFilterChain.Entry next, IOSession session) {
        next.getFilter().inputClosed(next.getNextEntry(),session);
    }

    @Override
    public void filterClose(IFilterChain.Entry next, IOSession session) {
        next.getFilter().filterClose(next.getPrevEntry(), session);
    }

    @Override
    public void filterWrite(IFilterChain.Entry next, IOSession session, IWritePacket writePacket) throws Exception {
        next.getFilter().filterWrite(next.getPrevEntry(), session, writePacket);
    }

    @Override
    public void messageReceived(IFilterChain.Entry next, IOSession session, Object message) throws Exception {
        next.getFilter().messageReceived(next.getNextEntry(), session, message);
    }

    @Override
    public void sessionIdle(IFilterChain.Entry next, IOSession session, IdleStatus status) throws Exception {
        next.getFilter().sessionIdle(next.getNextEntry(),session, status);
    }


}
