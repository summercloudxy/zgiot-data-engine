package com.zgiot.dataengine.controller;

import com.alibaba.fastjson.JSON;
import com.zgiot.common.pojo.DataModel;
import com.zgiot.dataengine.common.queue.QueueManager;
import com.zgiot.dataengine.repository.TMLMapper;
import com.zgiot.dataengine.repository.ThingMetricLabel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;

@RestController
@RequestMapping(value = "/test")
public class TestController {

    @Autowired
    TMLMapper mapper;

    @RequestMapping(
            value = "/it",
            method = RequestMethod.GET)
    public ResponseEntity<String> testit() {

        List<ThingMetricLabel> list = mapper.findAllDeviceMetricLabels();

        return new ResponseEntity<String>(
                JSON.toJSONString(list)
                , HttpStatus.OK);
    }

}
