package com.zgiot.dataengine.controller;

import com.alibaba.fastjson.JSON;
import com.zgiot.common.exceptions.SysException;
import com.zgiot.common.pojo.DataModel;
import com.zgiot.common.pojo.MetricModel;
import com.zgiot.common.pojo.ThingModel;
import com.zgiot.common.restcontroller.ServerResponse;
import com.zgiot.dataengine.dataplugin.kepserver.KepServerDataPlugin;
import com.zgiot.dataengine.service.DataEngineService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping(value = "/cmd")
public class CmdSendController {

    @Autowired
    private KepServerDataPlugin kepServerDataCollecter;
    @Autowired
    private DataEngineService dataEngineService;

    @RequestMapping(
            value = "/send",
            method = RequestMethod.POST)
    public ResponseEntity<String> send(@RequestBody String bodyStr) {
        List<DataModel> list = JSON.parseArray(bodyStr, DataModel.class);

        if (list.size() == 0) {
            return new ResponseEntity<String>(
                    JSON.toJSONString(new ServerResponse(
                            "No request data.", SysException.EC_SUCCESS, 0))
                    , HttpStatus.OK);
        }

        // check data list are all for KepServer
        List<String> categoryFails = new ArrayList<>();
        for (DataModel d : list) {

            // overide category value
            ThingModel thing = this.dataEngineService.getThing(d.getThingCode());
            MetricModel metric = this.dataEngineService.getMetric(d.getMetricCode());

            // validate category
            if (MetricModel.CATEGORY_SIGNAL.equals(metric.getMetricCategoryCode())
                    && ThingModel.CATEGORY_DEVICE.equals(thing.getThingCategoryCode())) {
                // convert data type
                d.initValueByType(metric.getValueType());
                continue;
            }

            // invalid action
            categoryFails.add("Thing(category='" + thing.getThingCategoryCode()
                    + "', code='" + thing.getThingCode()
                    + "') and Metric(category='" + metric.getMetricCategoryCode()
                    + "', code='" + metric.getMetricCode()
                    + "') is not supported to send. ");
        }

        // do send
        int okCount = 0;
        if (categoryFails.size() > 0) {
            throw new SysException("Only support KepServer cmd sending so far. Pls check your requests if contain other category. "
                    , SysException.EC_UNKOWN, categoryFails);
        } else {
            // send via KepServer
            try {
                //// core
                okCount = this.kepServerDataCollecter.sendCommands(list);
            } catch (Exception e) {
                throw new SysException(e.getMessage(), SysException.EC_UNKOWN, e);
            }

            if (okCount < list.size()) {
                ServerResponse res = new ServerResponse<>("There are '" + okCount + "/"
                        + list.size()
                        + "' successful commands, failed happened.  ", SysException.EC_UNKOWN
                        , okCount);
                return new ResponseEntity<>(
                        JSON.toJSONString(res)
                        , HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }

        return new ResponseEntity<>(
                ServerResponse.buildOkJson(okCount)
                , HttpStatus.OK);

    }

}
