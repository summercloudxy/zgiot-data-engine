package com.zgiot.dataengine.controller;

import com.zgiot.common.pojo.DataModel;
import com.zgiot.common.pojo.MetricModel;
import com.zgiot.dataengine.repository.ThingMetricLabel;
import org.junit.Assert;
import org.junit.Test;

import java.util.Date;

import static org.junit.Assert.*;

public class CmdSendControllerTest {

    CmdSendController testObj = new CmdSendController();

    @Test
    public void convertData() throws Exception {
        // prepare data
        String srcValue = "TRUE";
        DataModel dm = new DataModel("OK", "2492", "SIG", "mc", srcValue, new Date());

        MetricModel mm = new MetricModel();
        mm.setMetricCode(dm.getMetricCode());
        mm.setValueType(MetricModel.VALUE_TYPE_BOOL);

        ThingMetricLabel tml = new ThingMetricLabel();
        tml.setBoolReverse(1);

        // do test
        testObj.convertData(dm, mm, tml);

        // assert
        Assert.assertEquals("false", dm.getValue());
        Assert.assertEquals(false, dm.getValueObj());

    }

}