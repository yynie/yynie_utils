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

/**
 * A implementation of {@link IOController} with some basic functions fulfilled.
 */
public abstract class AbstractIOController<S extends AbstractIOSession> implements IOController<S> {
    private Logger log = Logger.get(AbstractPollingIoProcessor.class, Logger.Level.INFO);

    /**
     * IOController should keep a executor which will handle or the Runnable thread created
     * in this controller.
     * */
    private Executor executor;

    /**
     * A flag marked that the executor was created locally by the controller
     * and so it should be shutdown when disposing
     */
    private final boolean createdExecutor;

    private FilterChainBuilder filterChainBuilder = new FilterChainBuilder();

    private final AtomicBoolean activated = new AtomicBoolean(false);

    private IOHandler handler = new IOHandlerAdapter();

    protected final Object disposalLock = new Object();

    private volatile boolean disposing;

    private volatile boolean disposed;

    protected final DefaultIOFuture disposalFuture = new DefaultIOFuture(null);

    private static final AtomicInteger id = new AtomicInteger(0);

    /**
     * Constructor for {@link AbstractIOController}.
     * A <code>null</code> {@link Executor} provided will cause a default one created inside
     * this controller. If you provide an {@link Executor} created by yourself, you must also
     * manage it by yourself.
     *
     * @param executor
     *            the {@link Executor} used for handling all threads. Can be <code>null</code>.
     */
    public AbstractIOController(Executor executor) {
        if(executor == null){
            this.executor = getDefaultThreadPollExecutor();
            this.createdExecutor = true;
        }else{
            this.executor = executor;
            this.createdExecutor = false;
        }
    }

    /**
     * Get the {@link Executor}.
     * Use this API only if you really know what you can do with it.
     * */
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
     * {@inheritDoc}
     */
    @Override
    public boolean isActive() {
        return activated.get();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void activate(){
        activated.compareAndSet(false, true);
    }

    /**
     * {@inheritDoc}
     */
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
    @Override
    public final boolean isDisposing() {
        return disposing;
    }

    /**
     * This method will release any resources. It is invoked only once by {@link #dispose()}.
     *
     * @throws Exception If the dispose failed
     */
    protected abstract void dispose0() throws Exception;

    /**
     * This method will create and @return a default executor.
     * It is invoke only by the constructor.
     * */
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
