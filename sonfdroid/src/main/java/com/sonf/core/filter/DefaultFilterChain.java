package com.sonf.core.filter;

import android.os.SystemClock;

import com.sonf.core.buffer.IoBuffer;
import com.sonf.core.session.AbstractIOSession;
import com.sonf.core.session.AttributeKey;
import com.sonf.core.session.IOSession;
import com.sonf.core.session.IdleStatus;
import com.sonf.core.write.IWritePacket;
import com.sonf.future.ConnectFuture;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DefaultFilterChain implements IFilterChain {
    public static final AttributeKey SESSION_CREATED_FUTURE = new AttributeKey(DefaultFilterChain.class, "connectFuture");
    public static final AttributeKey SESSION_DECODER_OUT = new AttributeKey(DefaultFilterChain.class, "decoderOut");
    public static final AttributeKey SESSION_ENCODER_OUT = new AttributeKey(DefaultFilterChain.class, "encoderOut");
    private final AbstractIOSession session;
    /** The chain head */
    private final EntryImpl head;

    /** The chain tail */
    private final EntryImpl tail;

    /** The mapping between the filters and their associated name */
    private final Map<String, Entry> name2entry = new ConcurrentHashMap<String, Entry>();

    public DefaultFilterChain(AbstractIOSession session) {
        if (session == null) {
            throw new IllegalArgumentException("session");
        }
        this.session = session;
        head = new EntryImpl(null, null, "head", new HeadFilter());
        tail = new EntryImpl(head, null, "tail", new TailFilter());
        head.nextEntry = tail;
    }

    /**
     * {@inheritDoc}
     * */
    @Override
    public IOSession getSession() {
        return session;
    }

    /**
     * {@inheritDoc}
     * */
    @Override
    public synchronized void addLast(String name, IFilter filter) {
        checkAddable(name);
        register(tail.prevEntry, name, filter);
    }

    @Override
    public boolean contains(IFilter filter) {
        return getEntry(filter) != null;
    }

    @Override
    public synchronized void clear() {
        List<Entry> list = new ArrayList<Entry>(name2entry.values());

        for (Entry entry : list) {
            deregister((EntryImpl) entry);
        }
    }

    @Override
    public void fireSessionOpened() {
        callNextSessionOpened(head, session);
    }

    @Override
    public void fireSessionClosed() {
        callNextSessionClosed(head, session);
    }

    @Override
    public void fireExceptionCaught(Throwable cause) {
        callNextExceptionCaught(head, session, cause);
    }

    @Override
    public void fireMessageSent(IWritePacket packet) {
        packet.getFuture().setWritten();
        callNextMessageSent(head, session, packet);
    }

    @Override
    public void fireInputClosed() {
        callNextInputClosed(head, session);
    }

    @Override
    public void fireMessageReceived(Object message) {
        callNextMessageReceived(head, session, message);
    }

    @Override
    public void fireFilterClose() {
        callPreviousFilterClose(tail, session);
    }

    @Override
    public void fireFilterWrite(IWritePacket writePacket) {
        callPreviousFilterWrite(tail, session, writePacket);
    }

    @Override
    public void fireSessionIdle(IdleStatus status) {
        session.increaseIdleCount(status, SystemClock.elapsedRealtime());
        callNextSessionIdle(head, session, status);
    }

    private void callNextSessionOpened(Entry entry, IOSession session) {
        IFilter filter = entry.getFilter();
        filter.sessionOpened(entry.getNextEntry(), session);
    }

    private void callNextSessionClosed(Entry entry, IOSession session) {
        try {
            IFilter filter = entry.getFilter();
            filter.sessionClosed(entry.getNextEntry(), session);
        }catch (Exception e) {
            fireExceptionCaught(e);
        } catch (Error e) {
            fireExceptionCaught(e);
        }
    }

    private void callNextExceptionCaught(Entry entry, IOSession session, Throwable cause) {
        IFilter filter = entry.getFilter();
        filter.exceptionCaught(entry.getNextEntry(), session, cause);
    }

    private void callNextMessageSent(Entry entry, IOSession session, IWritePacket packet) {
        IFilter filter = entry.getFilter();
        filter.messageSent(entry.getNextEntry(), session, packet);
    }

    private void callNextInputClosed(Entry entry, IOSession session) {
        try {
            IFilter filter = entry.getFilter();
            filter.inputClosed(entry.getNextEntry(), session);
        } catch (Throwable e) {
            fireExceptionCaught(e);
        }
    }

    private void callNextMessageReceived(Entry entry, IOSession session, Object message) {
        try {
            IFilter filter = entry.getFilter();
            filter.messageReceived(entry.getNextEntry(), session, message);
        } catch (Exception e) {
            fireExceptionCaught(e);
        } catch (Error e) {
            fireExceptionCaught(e);
            throw e;
        }
    }

    private void callNextSessionIdle(Entry entry, IOSession session, IdleStatus status) {
        try {
            IFilter filter = entry.getFilter();
            filter.sessionIdle(entry.getNextEntry(), session, status);
        }catch (Exception e) {
            fireExceptionCaught(e);
        } catch (Error e) {
            fireExceptionCaught(e);
            throw e;
        }
    }


    private void callPreviousFilterClose(Entry entry, IOSession session) {
        IFilter filter = entry.getFilter();
        filter.filterClose(entry.getPrevEntry(), session);
    }

    private void callPreviousFilterWrite(Entry entry, IOSession session, IWritePacket writePacket) {
        try {
            IFilter filter = entry.getFilter();
            filter.filterWrite(entry.getPrevEntry(), session, writePacket);
        } catch (Exception e) {
            writePacket.getFuture().setException(e);
            fireExceptionCaught(e);
        } catch (Error e) {
            writePacket.getFuture().setException(e);
            fireExceptionCaught(e);
            throw e;
        }
    }
    /**
     * Register the newly added filter, inserting it between the previous and
     * the next filter in the filter's chain. We also call the preAdd and
     * postAdd methods.
     */
    private void register(EntryImpl prevEntry, String name, IFilter filter) {
        EntryImpl newEntry = new EntryImpl(prevEntry, prevEntry.nextEntry, name, filter);

        try {
            filter.onPreAdd(this, name);
        } catch (Exception e) {
            throw new RuntimeException("onPreAdd(): " + name + ':' + filter + " in " + getSession(), e);
        }

        prevEntry.nextEntry.prevEntry = newEntry;
        prevEntry.nextEntry = newEntry;
        name2entry.put(name, newEntry);

        try {
            filter.onPostAdd(this, name);
        } catch (Exception e) {
            deregister0(newEntry);
            throw new RuntimeException("onPostAdd(): " + name + ':' + filter + " in " + getSession(), e);
        }
    }

    private void deregister(EntryImpl entry) {
        IFilter filter = entry.getFilter();

        try {
            filter.onPreRemove(this, entry.getName());
        } catch (Exception e) {
            throw new RuntimeException("onPreRemove(): " + entry.getName() + ':' + filter + " in "
                    + getSession(), e);
        }

        deregister0(entry);

        try {
            filter.onPostRemove(this, entry.getName());
        } catch (Exception e) {
            throw new RuntimeException("onPostRemove(): " + entry.getName() + ':' + filter + " in "
                    + getSession(), e);
        }
    }

    private void deregister0(EntryImpl entry) {
        EntryImpl prevEntry = entry.prevEntry;
        EntryImpl nextEntry = entry.nextEntry;
        prevEntry.nextEntry = nextEntry;
        nextEntry.prevEntry = prevEntry;

        name2entry.remove(entry.name);
    }

    /**
     * Checks the specified filter name is already taken and throws an exception if already taken.
     */
    private void checkAddable(String name) {
        if (name2entry.containsKey(name)) {
            throw new IllegalArgumentException("Other filter is using the same name '" + name + "'");
        }
    }

    private Entry getEntry(IFilter filter) {
        EntryImpl e = head.nextEntry;

        while (e != tail) {
            if (e.getFilter() == filter) {
                return e;
            }
            e = e.nextEntry;
        }
        return null;
    }

    private class HeadFilter extends IFilterAdapter{
        @Override
        public void filterClose(Entry next, IOSession session) {
            ((AbstractIOSession) session).getProcessor().remove(session);
        }
        @Override
        public void filterWrite(IFilterChain.Entry next, IOSession session, IWritePacket writePacket) throws Exception {
            Object message = writePacket.getMessage();
            if(message == null){
                throw new IOException("empty message should not be sent");
            }
            if(message instanceof IoBuffer){
                AbstractIOSession s = (AbstractIOSession) session;
                s.getWriteQueue().offer(writePacket);
                s.getProcessor().flush(s);
            }else{
                throw new IOException("Don't know how to handle message of type '"
                        + message.getClass().getName() + "'.  Are you missing a protocol encoder?");
            }
        }
    }

    private class TailFilter extends IFilterAdapter{
        @Override
        public void sessionOpened(Entry next, IOSession session) {
            session.setStateReady();
            session.getController().getHandler().sessionOpened(session);
            ConnectFuture future = (ConnectFuture) session.removeAttribute(SESSION_CREATED_FUTURE);
            if (future != null) {
                future.setConnected();
            }
        }

        @Override
        public void sessionClosed(Entry next, IOSession session) {
            session.setStateClosed();
            session.getFilterChain().clear();
            session.removeAttribute(SESSION_DECODER_OUT);
            session.removeAttribute(SESSION_ENCODER_OUT);
            session.getController().getHandler().sessionClosed(session);
        }

        @Override
        public void exceptionCaught(Entry next, IOSession session, Throwable cause){
            session.getController().getHandler().exceptionCaught(session, cause);
        }

        @Override
        public void messageSent(Entry next, IOSession session, IWritePacket packet) {
            // Propagate the message
            session.getController().getHandler().messageSent(session, packet.getOrigMessage());
        }

        @Override
        public void inputClosed(Entry next, IOSession session){
            session.getController().getHandler().inputClosed(session);
        }

        @Override
        public void messageReceived(IFilterChain.Entry next, IOSession session, Object message) throws Exception {
            session.getController().getHandler().messageReceived(session, message);
        }

        @Override
        public void sessionIdle(IFilterChain.Entry next, IOSession session, IdleStatus status) throws Exception {
            session.getController().getHandler().sessionIdle(session, status);
        }
    }

    private final class EntryImpl implements Entry {
        private final String name;
        private final IFilter filter;
        private EntryImpl prevEntry;
        private EntryImpl nextEntry;

        private EntryImpl(EntryImpl prevEntry, EntryImpl nextEntry, String name, IFilter filter) {
            if (filter == null) {
                throw new IllegalArgumentException("filter");
            }

            if (name == null) {
                throw new IllegalArgumentException("name");
            }

            this.prevEntry = prevEntry;
            this.nextEntry = nextEntry;
            this.name = name;
            this.filter = filter;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public IFilter getFilter() {
            return filter;
        }

        @Override
        public Entry getNextEntry() {
            return nextEntry;
        }

        @Override
        public Entry getPrevEntry() {
            return prevEntry;
        }
    }
}
