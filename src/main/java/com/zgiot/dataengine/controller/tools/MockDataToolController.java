package com.zgiot.dataengine.controller.tools;

import com.zgiot.common.pojo.DataModel;
import com.zgiot.dataengine.common.queue.QueueManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;
import java.util.Queue;

@RestController
@RequestMapping(value = "/mockdatatool")
@Profile("dev")
public class MockDataToolController {
    private static final Logger logger = LoggerFactory.getLogger(MockDataToolController.class);

    @RequestMapping(
            value = "/press",
            method = RequestMethod.GET)
    public ResponseEntity<String> testit() {

        int size = 10;
        long startTime = System.currentTimeMillis();
        DataModel d = new DataModel("DEV", "1111", "SIG", "CR0", null, null);
        for (int i = 0; i < size; i++) {
            DataModel dd = d.clone();
            dd.setValue(String.valueOf(i));
            dd.setDataTimeStamp(new Date(startTime + i));
            QueueManager.getQueueCollected().add(dd);
        }
        return new ResponseEntity<String>(
                "testit"
                , HttpStatus.OK);
    }

    @RequestMapping(
            value = "/mock/{sizePerLoop}/{loops}",
            method = RequestMethod.GET)
    public ResponseEntity<String> mock(@PathVariable int sizePerLoop
            , @PathVariable int loops) {

        long startTime = System.currentTimeMillis();
        DataModel d = new DataModel("DEV", "1111", "SIG", "CR0", null, null);

        Queue<DataModel> q1 = QueueManager.getQueueCollected();

        for (int j = 0; j < loops; j++) {
            for (int i = 0; i < sizePerLoop; i++) {
                DataModel dd = d.clone();
                dd.setValue(String.valueOf(i));
                dd.setDataTimeStamp(new Date(startTime + i));
                q1.add(dd);
            }
        }

        return new ResponseEntity<String>(
                "Done: " + sizePerLoop * loops
                , HttpStatus.OK);
    }

}
