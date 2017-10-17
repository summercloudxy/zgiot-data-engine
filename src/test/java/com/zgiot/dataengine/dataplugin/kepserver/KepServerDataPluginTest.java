package com.zgiot.dataengine.dataplugin.kepserver;

import com.zgiot.common.pojo.MetricModel;
import com.zgiot.dataengine.repository.ThingMetricLabel;
import org.junit.Assert;
import org.junit.Test;

public class KepServerDataPluginTest {
    KepServerDataPlugin test = new KepServerDataPlugin();

    @Test
    public void parseOpcValueToString() throws Exception {

        Object value = Boolean.valueOf(false);

        MetricModel metricModel = new MetricModel();
        metricModel.setValueType(MetricModel.VALUE_TYPE_BOOL);

        ThingMetricLabel tml = new ThingMetricLabel();
        tml.setBoolReverse(1);

        String dest = test.parseOpcValueToString(value, metricModel, tml);

        Assert.assertEquals("true", dest);
    }

}