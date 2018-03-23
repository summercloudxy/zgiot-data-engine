package com.zgiot.dataengine.controller;

import com.alibaba.fastjson.JSON;
import com.zgiot.common.constants.GlobalConstants;
import com.zgiot.common.exceptions.SysException;
import com.zgiot.common.pojo.DataModel;
import com.zgiot.common.pojo.MetricModel;
import com.zgiot.common.pojo.ThingModel;
import com.zgiot.common.restcontroller.ServerResponse;
import com.zgiot.dataengine.dataplugin.kepserver.KepServerDataPlugin;
import com.zgiot.dataengine.repository.ThingMetricLabel;
import com.zgiot.dataengine.service.DataEngineService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.*;

@RestController
@RequestMapping(value = "/cmd")
public class CmdSendController {
    private static final Logger LOGGER = LoggerFactory.getLogger(CmdSendController.class);
    private static final int DEFAULT_BUFFER_SIZE = 1000;

    @Autowired
    private KepServerDataPlugin kepServerDataCollecter;
    @Autowired
    private DataEngineService dataEngineService;

    @RequestMapping(
            value = "/send",
            method = RequestMethod.POST,
            produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<String> send(HttpServletRequest req, @RequestBody String bodyStr) {
        String reqId = req.getHeader(GlobalConstants.REQUEST_ID_HEADER_KEY);
        long startMs = System.currentTimeMillis();
        logAccepted(req, reqId, bodyStr);

        List<DataModel> list = JSON.parseArray(bodyStr, DataModel.class);

        ResponseEntity<String> errRes = verifyIncomingList(bodyStr, reqId, startMs, list);
        if (errRes != null) return errRes;

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
            ThingMetricLabel tml = this.dataEngineService.getTMLByTM(d.getThingCode(), d.getMetricCode());

            // validate category
            if (MetricModel.CATEGORY_SIGNAL.equals(metric.getMetricCategoryCode())) {
                // convert data type
                convertData(d, metric, tml);
                continue;
            }

            // invalid action
            categoryFails.add("Thing(code='" + thing.getThingCode()
                    + "') and Metric(category='" + metric.getMetricCategoryCode()
                    + "', code='" + metric.getMetricCode()
                    + "') is not supported to send. ");
        }

        // do send
        int okCount = 0;
        if (categoryFails.size() > 0) {
            String msg = "Only support KepServer cmd sending so far. Pls check your requests if contain other category. ";
            logEnd(reqId, msg, startMs);
            throw new SysException(msg
                    , SysException.EC_UNKNOWN, 0);
        } else {
            // send via KepServer
            List<String> errors = new ArrayList<>();
            try {
                // core
                okCount = this.kepServerDataCollecter.sendCommands(list, errors);
            } catch (Exception e) {
                logEnd(reqId, e.getMessage(), startMs);
                throw new SysException(e.getMessage(), SysException.EC_UNKNOWN, 0); // NOPMD
            }

            if (okCount < list.size()) {
                String msg = "There are '" + okCount + "/"
                        + list.size()
                        + "' successful commands, failed happened. Errors from opc: `" + joinMsg(errors) + "` ";
                ServerResponse res = new ServerResponse<>(msg, SysException.EC_UNKNOWN
                        , okCount);
                logEnd(reqId, msg, startMs);
                return new ResponseEntity<>(
                        JSON.toJSONString(res)
                        , HttpStatus.OK);
            }
        }

        logEnd(reqId, "OK for all "+okCount, startMs);
        return new ResponseEntity<>(
                ServerResponse.buildOkJson(okCount)
                , HttpStatus.OK);
    }

    private ResponseEntity<String> verifyIncomingList(@RequestBody String bodyStr, String reqId, long startMs, List<DataModel> list) {
        if (list.size() == 0) {
            String msg = "Not valid request data.";
            logEnd(reqId, msg, startMs);
            return new ResponseEntity<>(
                    JSON.toJSONString(new ServerResponse(msg
                            , SysException.EC_UNKNOWN, 0))
                    , HttpStatus.OK);
        } else {
            for (DataModel data : list) {
                if (data.getThingCode() == null) { // means request body invalid
                    ServerResponse res = new ServerResponse(
                            "Not valid request data. The incoming req body: `" + bodyStr + "`", SysException.EC_UNKNOWN, 0);
                    String resJson = JSON.toJSONString(res);
                    logEnd(reqId, resJson, startMs);
                    return new ResponseEntity<String>(resJson, HttpStatus.OK);
                }
            }
        }
        return null;
    }

    private void logAccepted(HttpServletRequest req, String reqId, String bodyStr) {
        if (LOGGER.isDebugEnabled()){
            // headers, body
            Map map = new LinkedHashMap();
            map.put("reqId", req.getHeader(GlobalConstants.REQUEST_ID_HEADER_KEY));

            Enumeration<String> hNames = req.getHeaderNames();
            Map headerMap = new LinkedHashMap();
            while (hNames.hasMoreElements()) {
                String name = hNames.nextElement();
                String hValue = req.getHeader(name);
                headerMap.put(name, hValue);
            }

            LOGGER.debug("CmdSendAccepted: reqId=`{}`, headers=`{}`, body=`{}` ",
                    reqId
                    , JSON.toJSONString(headerMap)
                    , bodyStr
            );
        }
    }

    private void logEnd(String reqId, String msg, long startMs) {
        if (LOGGER.isDebugEnabled()) {
            long duration = System.currentTimeMillis() - startMs;
            LOGGER.debug("CmdSendEnded: reqId=`{}`, msg=`{}`, drMs=`{}`",
                    reqId,
                    msg,
                    duration);
        }
    }

    void convertData(DataModel d, MetricModel metric, ThingMetricLabel tml) {
        // parse from string
        if (d.getValueObj() == null) {
            d.setValueObj(DataModel.parseValueFromString(d.getValue(), metric.getValueType()));
        }

        if (MetricModel.VALUE_TYPE_BOOL.equals(metric.getValueType())
                && tml.getBoolReverse() == 1) { // if boolean value, do revert or not
            Boolean src = (Boolean) d.getValueObj();
            Boolean dest = !src.booleanValue();
            d.setValueObj(dest);
            d.setValue(String.valueOf(dest));
        }
    }

    private String joinMsg(List<String> errors) {
        if (errors == null || errors.size() == 0) {
            return "";
        }

        StringBuilder sb = new StringBuilder(DEFAULT_BUFFER_SIZE);
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
