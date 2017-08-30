package com.zgiot.dataengine.service;

import com.zgiot.common.pojo.MetricModel;
import com.zgiot.common.pojo.ThingModel;
import com.zgiot.dataengine.repository.ThingMetricLabel;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface DataEngineService {

    void initCache();

    List<ThingMetricLabel> findAllTML();

    ThingMetricLabel getTMLByLabel(String label);

    /**
     * Get label path, like '1111/PR/XX/0', including thing code.
     *
     * @param thingCode
     * @param metricCode
     * @return
     */
    String getLabelByTM(String thingCode, String metricCode);

    ThingModel getThing(String code);

    MetricModel getMetric(String code);

}
