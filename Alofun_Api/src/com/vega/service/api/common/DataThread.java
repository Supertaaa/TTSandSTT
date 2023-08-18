package com.vega.service.api.common;

import java.util.concurrent.atomic.AtomicInteger;

import org.apache.log4j.Logger;

public class DataThread extends Thread {

    public static final String DEFAULT_NAME = "SMPPThread";
    private static volatile boolean debugLifecycle = false;
    private static final AtomicInteger created = new AtomicInteger();
    private static final AtomicInteger alive = new AtomicInteger();
    static Logger logger = Logger.getLogger(DataThread.class);

    public DataThread(Runnable r) {
        this(r, DEFAULT_NAME);
    }

    public DataThread(Runnable runnable, String name) {
        super(runnable, name + "-" + created.incrementAndGet());
        setUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {

            public void uncaughtException(Thread t, Throwable e) {
                if (logger.isDebugEnabled()) {
                    logger.debug("UNCAUGHT in thread " + t.getName(), e);
                }

            }
        });
    }

    public void run() {
        if (logger.isDebugEnabled()) {
            logger.debug("Created " + getName());
        }
        try {
            alive.incrementAndGet();
            super.run();
        } finally {
            alive.decrementAndGet();
            if (logger.isDebugEnabled()) {
                logger.debug("Exiting " + getName());
            }
        }
    }

    public static int getThreadsCreated() {
        return created.get();
    }

    public static int getThreadsAlive() {
        return alive.get();
    }

    public static boolean getDebug() {
        return debugLifecycle;
    }

    public static void setDebug(boolean b) {
        debugLifecycle = b;
    }
}
