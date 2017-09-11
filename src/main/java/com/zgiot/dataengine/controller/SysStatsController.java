package com.zgiot.dataengine.controller;

import com.alibaba.fastjson.JSON;
import com.zgiot.dataengine.common.queue.QueueManager;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;

@RestController
@RequestMapping(value = "/sys-stats")
public class SysStatsController {
    @RequestMapping(
            value = "/all",
            method = RequestMethod.GET)
    public ResponseEntity<String> sysStats() {
        LinkedHashMap map = new LinkedHashMap();
        map.put("q.fromplugin.size", QueueManager.getQueueCollected().size());
        map.put("q.wss-buffer.size", QueueManager.getPreWss().size());

        return new ResponseEntity<String>(
                JSON.toJSONString(map)
                , HttpStatus.OK);
    }

    @RequestMapping(
            value = "/q-fromplugin",
            method = RequestMethod.GET)
    public ResponseEntity<String> listQueueDataFromPlugin() {
        return new ResponseEntity<String>(
                JSON.toJSONString(QueueManager.getQueueCollected())
                , HttpStatus.OK);
    }
}
