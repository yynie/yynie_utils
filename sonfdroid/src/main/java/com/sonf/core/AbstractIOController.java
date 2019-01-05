package com.sonf.core;

import com.sonf.core.filter.FilterChainBuilder;
import com.sonf.core.future.DefaultIOFuture;
import com.sonf.core.session.AbstractIOSession;
import com.sonf.core.session.IOHandler;
import com.sonf.core.session.IOHandlerAdapter;
import com.sonf.polling.AbstractPollingIoProcessor;
import com.yynie.myutils.Logger;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class AbstractIOController<S extends AbstractIOSession> implements IOController<S> {
    private Logger log = Logger.get(AbstractPollingIoProcessor.class, Logger.Level.INFO);
    private Executor executor;
    /**
     * A flag used to indicate that the local executor has been created
     * inside this instance, and not passed by a caller.
     *
     * If the executor is locally created, then it will be an instance
     * of the ThreadPoolExecutor class.
     */
    private final boolean createdExecutor;

    /**
     * Current filter chain builder.
     */
    private FilterChainBuilder filterChainBuilder = new FilterChainBuilder();

    private final AtomicBoolean activated = new AtomicBoolean(false);

    private IOHandler handler = new IOHandlerAdapter();

    /**
     * A lock object which must be acquired when related resources are
     * destroyed.
     */
    protected final Object disposalLock = new Object();

    private volatile boolean disposing;

    private volatile boolean disposed;

    protected final DefaultIOFuture disposalFuture = new DefaultIOFuture(null);

    /**
     * The unique number identifying the Service. It's incremented
     * for each new IoService created.
     */
    private static final AtomicInteger id = new AtomicInteger(0);

    public AbstractIOController(Executor executor) {
        if(executor == null){
            this.executor = getDefaultThreadPollExecutor();
            this.createdExecutor = true;
        }else{
            this.executor = executor;
            this.createdExecutor = false;
        }
    }

    protected Executor getExecutor(){
        return executor;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FilterChainBuilder getFilterChainBuilder(){
        return filterChainBuilder;
    }

    /**
     * @return true if the instance is active
     */
    @Override
    public boolean isActive() {
        return activated.get();
    }

    @Override
    public void activate(){
        activated.compareAndSet(false, true);
    }

    @Override
    public void deactivate(){
        activated.compareAndSet(true, false);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final void dispose() {
        if (disposed) {
            return;
        }
        log.i("dispose");
        synchronized (disposalLock) {
            if (!disposing) {
                disposing = true;

                try {
                    dispose0();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        disposalFuture.awaitUninterruptibly();

        if (createdExecutor) {
            ExecutorService e = (ExecutorService) executor;
            e.shutdownNow();
        }
        disposed = true;
    }

    /**
     * {@inheritDoc}
     */
    public final boolean isDisposing() {
        return disposing;
    }


    /**
     * Implement this method to release any acquired resources.  This method
     * is invoked only once by {@link #dispose()}.
     *
     * @throws Exception If the dispose failed
     */
    protected abstract void dispose0() throws Exception;

    protected abstract Executor getDefaultThreadPollExecutor();

    protected final void executeRunnable(Runnable runnable, String suffix) {
        String name = runnable.getClass().getSimpleName() + '-' + id.incrementAndGet();
        if (suffix != null) {
            name += '[' + suffix + "]";
        }
        executor.execute(new NamedRunnable(runnable, name));
        if(id.get() >= (Integer.MAX_VALUE - 1)){
            id.set(0);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final IOHandler getHandler() {
        return handler;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final void setHandler(IOHandler handler) {
        if (handler == null) {
            throw new IllegalArgumentException("handler cannot be null");
        }

        if (isActive()) {
            throw new IllegalStateException("handler cannot be set while the service is active.");
        }

        this.handler = handler;
    }
}
