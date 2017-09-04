package com.zgiot.dataengine.controller.tools;

import com.alibaba.fastjson.JSON;
import com.zgiot.common.pojo.DataModel;
import com.zgiot.dataengine.common.pojo.MongoData;
import com.zgiot.dataengine.service.historydata.HistoryDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

    @RequestMapping(
            value = "/testNthreadInsertBatch",
            method = RequestMethod.GET)
    public ResponseEntity<String> testInsertBatchMultiThreads() throws InterruptedException {
        int size = 10000;
        int loop= 20*10000;

        CountDownLatch countDown = new CountDownLatch(loop);

        AtomicLong loopTotalTime = new AtomicLong(0);

        ExecutorService es = Executors.newFixedThreadPool(6);

        for (int j = 0; j < loop; j++) {
            es.execute(new Runnable() {
                @Override
                public void run() {
                    long startDate = System.currentTimeMillis();
                    List<MongoData> list = new ArrayList<>(size);
                    for (int i = 0; i < size; i++) {
                        DataModel dest = new DataModel();
                        dest.setDataTimeStamp(new Date(startDate+1));
                        dest.setMetricCategoryCode("SIG");
                        dest.setMetricCode("CR0");
                        dest.setThingCategoryCode("DEV");
                        dest.setThingCode("1111");
                        dest.setValue(String.valueOf(i));

                        list.add(MongoData.convertFromDataModel(dest));
                    }

                    long startNs = System.nanoTime();
                    historyDataService.insertBatch(list);
                    long endNs = System.nanoTime();
//                    System.out.println(Thread.currentThread().getId()+",list size:"+ list.size());

                    loopTotalTime.addAndGet((endNs - startNs));
                    countDown.countDown();
                }
            });
        }

        countDown.await();

        long loopAvgInsertTime = loopTotalTime.get() / loop ;
        Map res = new LinkedHashMap();

        res.put("loopAvgInsertMsTime", loopAvgInsertTime/1000000);


        return new ResponseEntity<String>(
                JSON.toJSONString(res)
                , HttpStatus.OK);
    }

    @RequestMapping(
            value = "/syncinsert",
            method = RequestMethod.GET)
    public ResponseEntity<String> testInsert1Thread() throws Exception {

        List<MongoData> list = new ArrayList<>();

        DataModel dest = new DataModel();
        dest.setDataTimeStamp(new Date(System.currentTimeMillis()+1));
        dest.setMetricCategoryCode("SIG");
        dest.setMetricCode("CR0");
        dest.setThingCategoryCode("DEV");
        dest.setThingCode("1111");
        dest.setValue(String.valueOf(81));

        list.add(MongoData.convertFromDataModel(dest));

        historyDataService.insertBatch(list);

        return new ResponseEntity<String>(
                ""
                , HttpStatus.OK);
    }

}
