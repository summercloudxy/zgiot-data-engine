package com.zgiot.dataengine.controller;

import com.alibaba.fastjson.JSON;
import com.zgiot.common.pojo.DataModel;
import com.zgiot.dataengine.common.queue.QueueManager;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;

/**
 * Created by xiayun on 2017/9/21.
 */
@RestController
public class DataController {
    @GetMapping("/data")
    public ResponseEntity<String> getInitialDatas(){
        BlockingQueue<DataModel> buffer = (BlockingQueue) QueueManager.getPreWss();
        List<DataModel> list = new ArrayList(buffer.size()+1000);
        buffer.drainTo(list);
        return new ResponseEntity<String>(
                JSON.toJSONString(list)
                , HttpStatus.OK);
    }
}
