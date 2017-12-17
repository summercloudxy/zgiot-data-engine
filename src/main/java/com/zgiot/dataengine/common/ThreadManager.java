package com.zgiot.dataengine.common;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ThreadManager {

    private static final ExecutorService es = Executors.newCachedThreadPool();

    public static ExecutorService getThreadPool() {
        return es;
    }

}
