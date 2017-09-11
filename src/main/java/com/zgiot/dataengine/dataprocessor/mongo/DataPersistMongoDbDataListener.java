package com.zgiot.dataengine.dataprocessor.mongo;

import com.zgiot.common.pojo.DataModel;
import com.zgiot.dataengine.common.ThreadManager;
import com.zgiot.dataengine.common.pojo.MongoData;
import com.zgiot.dataengine.common.queue.QueueManager;
import com.zgiot.dataengine.dataprocessor.DataListener;
import com.zgiot.dataengine.service.historydata.HistoryDataService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;

@Component
public class DataPersistMongoDbDataListener implements DataListener {
    private static final Logger logger = LoggerFactory.getLogger(DataPersistMongoDbDataListener.class);

    @Autowired
    HistoryDataService historyDataService;

    private static final int BATCH_ITEMS = 10000; // items
    private static final int TIMEOUT = 3000; // ms
    private static final int TIME_INTERVAL = 1000; // ms

    public DataPersistMongoDbDataListener() {
        ThreadManager.getThreadPool().execute(() -> {
            int timeout = 0;
            BlockingQueue<MongoData> dataBuffer = QueueManager.getMongoPrePersistBuffer();
            while (true) {
                try {
                    Thread.sleep(TIME_INTERVAL);
                    if (dataBuffer.size() >= BATCH_ITEMS
                            || timeout > TIMEOUT) {
                        // check and insert
                        synchronized (dataBuffer) {
                            int size = dataBuffer.size();
                            List<MongoData> list = new ArrayList<>(size);

                            if (size > 0) {
                                dataBuffer.drainTo(list);
                                asyncInsertBatch(historyDataService, list);
                            }

                            // clear timeout counter
                            timeout = 0;
                        }

                    } else {
                        timeout += TIME_INTERVAL;
                    }

                } catch (InterruptedException e) {
                    logger.error(e.getMessage());
                }
            }
        });

        logger.info("MongoDB flush thread started. (batch size={}, timeoutMs={}, intervalMs={})"
                , BATCH_ITEMS, TIMEOUT, TIME_INTERVAL);
    }

    @Override
    public void onData(DataModel data) {
        BlockingQueue<MongoData> dataBuffer = QueueManager.getMongoPrePersistBuffer();
        synchronized (dataBuffer) {
            dataBuffer.add(MongoData.convertFromDataModel(data));
        }
    }

    private void asyncInsertBatch(HistoryDataService historyDataService, List<MongoData> list) {
        DataInserter runner = new DataInserter();
        runner.svc = historyDataService;
        runner.list = list;
        ThreadManager.getThreadPool().execute(runner);
    }

    final class DataInserter implements Runnable {
        HistoryDataService svc;
        List<MongoData> list;

        @Override
        public void run() {
            svc.insertBatch(list);
        }
    }
}
