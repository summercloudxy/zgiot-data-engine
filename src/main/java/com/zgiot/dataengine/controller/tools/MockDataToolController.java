package com.zgiot.dataengine.controller.tools;

import com.alibaba.fastjson.JSON;
import com.zgiot.common.pojo.DataModel;
import com.zgiot.dataengine.common.queue.QueueManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;
import java.util.Queue;

@RestController
@RequestMapping(value = "/mockdatatool")
//@Profile("dev")
public class MockDataToolController {
    private static final Logger LOGGER = LoggerFactory.getLogger(MockDataToolController.class);

    /**
     *
     * @param bodyStr  like `[{"dt": 1505388766104, "mc": "FEED_OVER", "mcc": "SIG", "tc": "2496A", "tcc": "DVC", "v": "false"},{"dt": 1505388766104, "mc": "FEED_OVER", "mcc": "SIG", "tc": "2496", "tcc": "DVC", "v": "true"}]`
     * @return
     */
    @RequestMapping(
            value = "/mock",
            method = RequestMethod.POST)
    public ResponseEntity<String> mockBatch(@RequestBody String bodyStr) {
        List<DataModel> list = JSON.parseArray(bodyStr,DataModel.class);
        for (DataModel dd : list){
            QueueManager.getQueueCollected().add(dd);
        }

        return new ResponseEntity<String>(
                "Mocked item count: " + list.size()
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
