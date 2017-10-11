package com.zgiot.dataengine.common.queue;

import com.zgiot.common.pojo.DataModel;
import com.zgiot.dataengine.common.pojo.MongoData;

import java.util.List;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class QueueManager {

    private static final Queue<DataModel> fromCollecter = new LinkedBlockingQueue(1000000);
    private static final Queue<DataModel> preWss = new LinkedBlockingQueue(1000000); // before send to websocket session , buffer here
    private static final BlockingQueue<MongoData> mongoPrePersistBuffer = new ArrayBlockingQueue(
            1000000);

    public static Queue<DataModel> getQueueCollected() {
        return fromCollecter;
    }

    public static Queue<DataModel> getPreWss() {
        return preWss;
    }

    public static BlockingQueue<MongoData> getMongoPrePersistBuffer() {
        return mongoPrePersistBuffer;
    }

}
