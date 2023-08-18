package com.vega.service.api.common;

import java.util.concurrent.ThreadFactory;

public class DataThreadFactory implements ThreadFactory {

    private final String poolName;

    public DataThreadFactory(String poolName) {
        this.poolName = poolName;
    }

    public Thread newThread(Runnable runnable) {
        return new DataThread(runnable, poolName);
    }
}
