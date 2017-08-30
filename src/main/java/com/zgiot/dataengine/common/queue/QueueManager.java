package com.zgiot.dataengine.common.queue;

import com.zgiot.common.pojo.DataModel;

import java.util.List;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

public class QueueManager {

    private static final Queue<DataModel> fromCollecter = new LinkedBlockingQueue();

    public static Queue<DataModel> getQueueCollected() {
        return fromCollecter;
    }


}
