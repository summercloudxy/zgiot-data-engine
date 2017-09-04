package com.zgiot.dataengine.controller.tools;

import com.alibaba.fastjson.JSON;
import com.zgiot.common.pojo.DataModel;
import com.zgiot.dataengine.common.pojo.MongoData;
import com.zgiot.dataengine.common.queue.QueueManager;
import com.zgiot.dataengine.service.historydata.HistoryDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.xml.crypto.Data;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

@RestController
@RequestMapping(value = "/test")
@Profile("dev")
public class TestController {

    @RequestMapping(
            value = "/it",
            method = RequestMethod.GET)
    public ResponseEntity<String> testit() {


        int size = 3000;
        long startTime = System.currentTimeMillis();
            DataModel d = new DataModel("DEV","1111","SIG","CR0",null,null);
        for (int i = 0; i < size; i++) {
            DataModel dd =  d.clone();
            dd.setValue(String.valueOf(i));
            dd.setDataTimeStamp(new Date(startTime+i));
            QueueManager.getQueueCollected().add(dd);
        }
        return new ResponseEntity<String>(
                "testit"
                , HttpStatus.OK);
    }

}
