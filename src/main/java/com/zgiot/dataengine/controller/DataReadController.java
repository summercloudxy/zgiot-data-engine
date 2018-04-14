package com.zgiot.dataengine.controller;

import com.alibaba.fastjson.JSON;
import com.zgiot.common.constants.GlobalConstants;
import com.zgiot.common.pojo.DataModel;
import com.zgiot.common.pojo.MetricModel;
import com.zgiot.common.pojo.ThingModel;
import com.zgiot.common.restcontroller.ServerResponse;
import com.zgiot.dataengine.common.queue.QueueManager;
import com.zgiot.dataengine.controller.dto.DataInputDto;
import com.zgiot.dataengine.dataplugin.kepserver.KepServerDataPlugin;
import com.zgiot.dataengine.service.DataEngineService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Queue;

@RestController
@RequestMapping(value = "/data")
public class DataReadController {

    private static final Logger LOGGER = LoggerFactory.getLogger(DataReadController.class);

    @Autowired
    private KepServerDataPlugin kepServerDataCollecter;

    @Autowired
    private DataEngineService dataEngineService;

    /**
     * @param thingCode
     * @param metricCode
     * @return
     * @deprecated replace to: #adhocLoadData
     */
    @Deprecated
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
     *
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

    /**
     * As data plugin, accept external data into collector queue.
     *
     * @return
     */
    @RequestMapping(
            value = "/input",
            method = RequestMethod.POST,
            produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<String> inputData(@RequestBody String bodyStr, HttpServletRequest req) {
        List<String> errMsgs = new ArrayList<>();

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Input Data Request Body: `" + bodyStr + "`");
        }

        ValidatorUtil.validateRequestId(req.getHeader(GlobalConstants.REQUEST_ID_HEADER_KEY), errMsgs);

        DataInputDto dataInput = JSON.parseObject(bodyStr, DataInputDto.class);

        if (StringUtils.isBlank(dataInput.getUserUuid())
                && StringUtils.isBlank(dataInput.getAccessToken())) {
            errMsgs.add("userUuid or accessToken cannot be both blank.");
        }

        if (StringUtils.isBlank(dataInput.getSource()) ){
            errMsgs.add("source is required.");
        }

        if (dataInput.getDataList() == null){
            errMsgs.add("datalist is required.");
        }else{

            // Validate data list
            for (DataModel dm : dataInput.getDataList()) {
                // validate tc, mc
                ThingModel thing = dataEngineService.getThing(dm.getThingCode());
                if (thing == null) errMsgs.add("Thing `" + dm.getThingCode() + "` not exists.");

                MetricModel metric = dataEngineService.getMetric(dm.getMetricCode());
                if (metric == null) errMsgs.add("Metric `" + dm.getMetricCode() + "` not exists.");

                if (dm.getDataTimeStamp() == null) {
                    dm.setDataTimeStamp(new Date());
                }

            }
        }

        if (errMsgs.size() > 0) {
            return ValidatorUtil.buildResponseEntityOfValidationError(errMsgs);
        }

        // add to q
        Queue q = QueueManager.getQueueCollected();
        q.addAll(dataInput.getDataList());

        return new ResponseEntity<>(
                ServerResponse.buildOkJson("Collected " + dataInput.getDataList().size() + " items. ")
                , HttpStatus.OK);
    }

}
