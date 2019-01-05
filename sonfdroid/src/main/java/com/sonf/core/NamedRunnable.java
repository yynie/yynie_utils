package com.sonf.core;

import com.yynie.myutils.Logger;

public class NamedRunnable implements Runnable {
    private final static Logger log = Logger.get(NamedRunnable.class, Logger.Level.INFO);

    /** The runnable name */
    private final String newName;

    /** The runnable task */
    private final Runnable runnable;

    /**
     * Creates a new instance of NamedRunnable.
     *
     * @param runnable The underlying runnable
     * @param newName The runnable's name
     */
    public NamedRunnable(Runnable runnable, String newName) {
        this.runnable = runnable;
        this.newName = newName;
    }

    /**
     * Run the runnable after having renamed the current thread's name
     * to the new name. When the runnable has completed, set back the
     * current thread name back to its origin.
     */
    public void run() {
        Thread currentThread = Thread.currentThread();
        String oldName = currentThread.getName();

        if (newName != null) {
            setName(currentThread, newName);
        }

        try {
            log.i(newName + " run!!!");
            runnable.run();
            log.i(newName + " quit!!!");
        } finally {
            setName(currentThread, oldName);
        }
    }

    /**
     * Wraps {@link Thread#setName(String)} to catch a possible {@link Exception}s such as
     * {@link SecurityException} in sandbox environments, such as applets
     */
    private void setName(Thread thread, String name) {
        try {
            thread.setName(name);
        } catch (SecurityException e) {
            log.w("Failed to set the thread name:" + e);
        }
    }
}
