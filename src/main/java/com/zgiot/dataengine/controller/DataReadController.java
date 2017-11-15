package com.zgiot.dataengine.controller;

import com.alibaba.fastjson.JSON;
import com.zgiot.common.pojo.DataModel;
import com.zgiot.common.restcontroller.ServerResponse;
import com.zgiot.dataengine.common.queue.QueueManager;
import com.zgiot.dataengine.dataplugin.kepserver.KepServerDataPlugin;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.Queue;

@RestController
@RequestMapping(value = "/data")
public class DataReadController {

    @Autowired
    private KepServerDataPlugin kepServerDataCollecter;

    /**
     *
     * @param thingCode
     * @param metricCode
     * @return
     * @deprecated replace to: #adhocLoadData
     */
    @RequestMapping(
            value = "/sync/{thingCode}/{metricCode}",
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<String> syncRead(@PathVariable String thingCode, @PathVariable String metricCode) {
        DataModel dm = kepServerDataCollecter.syncRead(thingCode, metricCode);

        return new ResponseEntity<>(
                ServerResponse.buildOkJson(dm.getValue())
                , HttpStatus.OK);
    }

    /**
     * On demand sync load data from device and also notify upforwarders.
     * @param thingCode
     * @param metricCode
     * @return
     */
    @RequestMapping(
            value = "/adhocLoad/{thingCode}/{metricCode}",
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<String> adhocLoadData(@PathVariable String thingCode, @PathVariable String metricCode) {
        DataModel dm = kepServerDataCollecter.syncRead(thingCode, metricCode);
        Queue q = QueueManager.getQueueCollected();
        q.add(dm);

        return new ResponseEntity<>(
                ServerResponse.buildOkJson(JSON.toJSONString(dm))
                , HttpStatus.OK);
    }

}
