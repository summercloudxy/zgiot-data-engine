package com.zgiot.dataengine.controller.tools;

import com.alibaba.fastjson.JSON;
import com.zgiot.common.pojo.DataModel;
import com.zgiot.dataengine.common.pojo.MongoData;
import com.zgiot.dataengine.common.queue.QueueManager;
import com.zgiot.dataengine.service.historydata.HistoryDataService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

@RestController
@RequestMapping(value = "/historytool")
@Profile("dev")
public class HistoryDataToolController {

    private static final Logger logger = LoggerFactory.getLogger(HistoryDataToolController.class);
    @Autowired
    HistoryDataService historyDataService;

    @RequestMapping(
            value = "/testFind",
            method = RequestMethod.GET)
    public ResponseEntity<String> testFind() {

        List<String> tcList = new ArrayList<>();
        tcList.add("3333");
        tcList.add("2222");

        List<String> mcList = new ArrayList<>();
        mcList.add("CR1");
        mcList.add("CR2");

        Date startDate = new Date(1504091999490l);
        Date endDate = new Date(1504091999502l);

        long start = System.nanoTime();
        int times = 100;
        for (int i = 0; i < times; i++) {
            List list = historyDataService.findHistoryDataList(tcList, null, startDate, endDate);
        }
        long end = System.nanoTime();

        System.out.println((end - start) / times);

        return new ResponseEntity<String>(
                ""
                , HttpStatus.OK);
    }

    /**
     * @param mode        q: fill to collected queue; svc: call svc to insert db only
     * @param sizePerLoop
     * @param loops
     * @return
     * @throws InterruptedException
     */
    @RequestMapping(
            value = "/testNthreadInsertBatch/{mode}/{sizePerLoop}/{loops}",
            method = RequestMethod.GET)

    public ResponseEntity<String> testInsertBatchMultiThreads(@PathVariable String mode
            , @PathVariable int sizePerLoop
            , @PathVariable int loops) throws InterruptedException {
        int size = sizePerLoop;
        int loop = loops;

        CountDownLatch countDown = new CountDownLatch(loop);

        AtomicLong loopTotalTime = new AtomicLong(0);

        ExecutorService es = Executors.newFixedThreadPool(1);

        for (int j = 0; j < loop; j++) {
            Queue<DataModel> q1 = QueueManager.getQueueCollected();
            Queue q2 = QueueManager.getMongoPrePersistBuffer();

            while (q1.size() > 10000 || q2.size() > 10000) {
                try {
                    Thread.sleep(1000);
                    logger.warn("Wait for buffer flush...");
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            es.execute(new Runnable() {
                @Override
                public void run() {
                    long startDate = System.currentTimeMillis();
                    List<MongoData> list = new ArrayList<>(size);
                    for (int i = 0; i < size; i++) {
                        DataModel dest = new DataModel();
                        dest.setDataTimeStamp(new Date(startDate + 1));
                        dest.setMetricCategoryCode("SIG");
                        dest.setMetricCode("CR0");
                        dest.setThingCategoryCode("DEV");
                        dest.setThingCode("1111");
                        dest.setValue(String.valueOf(i));

                        if ("q".equals(mode)) {
                            QueueManager.getQueueCollected().add(dest);
                        } else if ("svc".equals(mode)) {
                            list.add(MongoData.convertFromDataModel(dest));
                        }
                    }

                    long startNs = System.nanoTime();
                    if ("svc".equals(mode)) {
                        historyDataService.insertBatch(list);
                    }
                    long endNs = System.nanoTime();
                    long ds = endNs - startNs;

                    loopTotalTime.addAndGet(ds);
                    logger.debug("Loop time {} ns", ds);
                    countDown.countDown();
                }
            });
        }

        countDown.await();

        long loopAvgInsertTime = loopTotalTime.get() / loop;
        Map res = new LinkedHashMap();

        res.put("loopAvgInsertMsTime", loopAvgInsertTime / 1000000);


        return new ResponseEntity<String>(
                JSON.toJSONString(res)
                , HttpStatus.OK);
    }

    @RequestMapping(
            value = "/syncinsert/{sizePerLoop}/{loops}",
            method = RequestMethod.GET)
    public ResponseEntity<String> testInsert1Thread(@PathVariable int sizePerLoop
            , @PathVariable int loops) throws Exception {

        List<MongoData> list = new ArrayList<>(sizePerLoop);

        for (int j = 0; j < loops; j++) {
            list.clear();

            for (int i = 0; i < sizePerLoop; i++) {
                DataModel dest = new DataModel();
                dest.setDataTimeStamp(new Date(System.currentTimeMillis() + i));
                dest.setMetricCategoryCode("SIG");
                dest.setMetricCode("CR0");
                dest.setThingCategoryCode("DEV");
                dest.setThingCode("1111");
                dest.setValue(String.valueOf(i));

                list.add(MongoData.convertFromDataModel(dest));
            }

            historyDataService.insertBatch(list);
        }

        return new ResponseEntity<String>(
                ""
                , HttpStatus.OK);
    }

}
