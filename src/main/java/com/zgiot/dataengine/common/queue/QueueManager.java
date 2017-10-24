package com.zgiot.dataengine.common.queue;

import com.zgiot.common.pojo.DataModel;

import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

public class QueueManager {

    private static final Queue<DataModel> fromCollecter = new LinkedBlockingQueue(1000000);
    private static final Queue<DataModel> preWss = new LinkedBlockingQueue(1000000); // before send to websocket session , buffer here

    public static Queue<DataModel> getQueueCollected() {
        return fromCollecter;
    }

    public static Queue<DataModel> getPreWss() {
        return preWss;
    }

}
