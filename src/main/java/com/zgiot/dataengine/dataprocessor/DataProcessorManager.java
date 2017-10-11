package com.zgiot.dataengine.dataprocessor;

import com.zgiot.common.pojo.DataModel;
import com.zgiot.dataengine.common.ThreadManager;
import com.zgiot.dataengine.common.queue.QueueManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class DataProcessorManager {

    private static final Logger logger = LoggerFactory.getLogger(DataProcessorManager.class);

    private List<DataListener> dataListeners = new ArrayList<>();

    public List<DataListener> getDataListeners() {
        return dataListeners;
    }

    public void start() {
        ThreadManager.getThreadPool().submit((Runnable) () -> {
            while (true) {
                /* wait for queue values and send out */
                DataModel data = QueueManager.getQueueCollected().poll();
                try {
                    if (data == null) {
                        Thread.sleep(100);
                        continue;
                    }

                    if (logger.isTraceEnabled()){
                        logger.trace("Got data: {}" , data.toString());
                    }

                    invokeListeners(data);

                } catch (Exception e) {
                    logger.error("Error: ", e);
                }

            }
        });
    }

    private void invokeListeners(DataModel data) {
        ThreadManager.getThreadPool().execute(
                () -> {
                    for (DataListener dl : dataListeners) {
                        dl.onData(data);
                    }
                }
        );
    }

}
