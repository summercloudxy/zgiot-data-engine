package com.zgiot.dataengine.controller;

import com.alibaba.fastjson.JSON;
import com.zgiot.common.constants.GlobalConstants;
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

import javax.servlet.http.HttpServletRequest;
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
    public ResponseEntity<String> send(HttpServletRequest req, @RequestBody String bodyStr) {
        List<DataModel> list = JSON.parseArray(bodyStr, DataModel.class);

        if (list.size() == 0) {
            return new ResponseEntity<>(
                    JSON.toJSONString(new ServerResponse(
                            "Not valid request data.", SysException.EC_UNKOWN, 0))
                    , HttpStatus.OK);
        } else {
            for (DataModel data : list) {
                if (data.getThingCode() == null) { // means request body invalid
                    ServerResponse res = new ServerResponse(
                            "Not valid request data. The incoming req body: `" + bodyStr + "`", SysException.EC_UNKOWN, 0);
                    String resJson = JSON.toJSONString(res);
                    return new ResponseEntity<String>(resJson, HttpStatus.OK);
                }
            }
        }

        Integer mockValue = handleMockExpectation(req);
        if (mockValue != null) {
            return new ResponseEntity<>(
                    ServerResponse.buildOkJson(mockValue)
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
            List<String> errors = new ArrayList<>(10);
            try {
                // core
                okCount = this.kepServerDataCollecter.sendCommands(list, errors);
            } catch (Exception e) {
                throw new SysException(e.getMessage(), SysException.EC_UNKOWN, e);
            }

            if (okCount < list.size()) {
                ServerResponse res = new ServerResponse<>("There are '" + okCount + "/"
                        + list.size()
                        + "' successful commands, failed happened. Errors from opc: `" + joinMsg(errors) + "` ", SysException.EC_UNKOWN
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

    private String joinMsg(List<String> errors) {
        if (errors == null || errors.size() == 0) {
            return "";
        }

        StringBuffer sb = new StringBuffer(1000);
        boolean first = true;
        for (String str : errors) {
            if (!first) {
                sb.append(" | ");
            }
            sb.append(str);
            first = false;
        }

        return sb.toString();
    }

    private Integer handleMockExpectation(HttpServletRequest req) {
        String exp = req.getHeader(GlobalConstants.REQUEST_MOCK_EXP);
        if (exp == null) {
            return null;
        }

        return Integer.valueOf(exp);
    }

}
