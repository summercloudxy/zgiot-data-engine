package com.zgiot.dataengine.controller;

import com.alibaba.fastjson.JSON;
import com.zgiot.common.restcontroller.ServerResponse;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

public class ValidatorUtil {

    public static String validateRequestId(String requestId, List<String> errMsgs) {
        String err = null;
        if (StringUtils.isBlank(requestId)) {
            err = "RequestId is required. ";
            errMsgs.add(err);
        }
        return err;
    }

    public static ResponseEntity<String> buildResponseEntityOfValidationError(List<String> errorMsgs) {
        return new ResponseEntity<>(
                ServerResponse.buildOkJson(JSON.toJSONString(errorMsgs))
                , HttpStatus.BAD_REQUEST);
    }

}
