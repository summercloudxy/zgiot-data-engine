package com.zgiot.dataengine.service;

import com.zgiot.common.pojo.MetricModel;
import com.zgiot.common.pojo.ThingModel;
import com.zgiot.common.reloader.Reloader;
import com.zgiot.dataengine.repository.TMLMapper;
import com.zgiot.dataengine.repository.ThingMetricLabel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class DataEngineServiceImpl implements DataEngineService, Reloader {

    private static final Logger LOGGER = LoggerFactory.getLogger(DataEngineServiceImpl.class);

    @Autowired
    private TMLMapper tmlMapper;

    private final static ConcurrentHashMap<String, MetricModel> METRIC_CACHE = new ConcurrentHashMap(100);
    private final static ConcurrentHashMap<String, ThingModel> THING_CACHE = new ConcurrentHashMap(100);

    private final static ConcurrentHashMap<String, ThingMetricLabel> LABEL_THING_METRIC_CACHE = new ConcurrentHashMap(100);
    private final static ConcurrentHashMap<String, ThingMetricLabel> TM_L_CACHE = new ConcurrentHashMap(100);
    private final static List<ThingMetricLabel> T_M_L_CACHE = new ArrayList(100);

    @Override
    public void initCache() {
        synchronized (this) {
            List<ThingMetricLabel> list = tmlMapper.findAllDeviceMetricLabels();
            T_M_L_CACHE.clear();
            LABEL_THING_METRIC_CACHE.clear();
            TM_L_CACHE.clear();
            for (ThingMetricLabel item : list) {
                T_M_L_CACHE.add(item);
                LABEL_THING_METRIC_CACHE.put(item.getLabelPath(), item);
                TM_L_CACHE.put(item.getThingCode() + "_" + item.getMetricCode(), item);
            }

            List<ThingModel> things = this.tmlMapper.findAllThings();
            THING_CACHE.clear();
            for (ThingModel t : things) {
                THING_CACHE.put(t.getThingCode(), t);
            }

            List<MetricModel> metrics = this.tmlMapper.findAllMetrics();
            METRIC_CACHE.clear();
            for (MetricModel m : metrics) {
                METRIC_CACHE.put(m.getMetricCode(), m);
            }

            LOGGER.info("Cache inited. ");
        }
    }

    @Override
    public ThingMetricLabel getTMLByLabel(String labelPath) {
        synchronized (this) {
        }

        ThingMetricLabel o = LABEL_THING_METRIC_CACHE.get(labelPath);
        if (o == null) {
            LOGGER.warn("Not found metricCode for label '{}'", labelPath);
        }
        return o;
    }

    @Override
    public List<ThingMetricLabel> findAllTML() {
        synchronized (this) {
        }
        return T_M_L_CACHE;
    }

    @Override
    public ThingMetricLabel getTMLByTM(String thingCode, String metricCode) {
        synchronized (this) {
        }
        ThingMetricLabel o = TM_L_CACHE.get(thingCode + "_" + metricCode);
        if (o == null) {
            LOGGER.warn("Not found label for thingCode {} and metricCode '{}'", thingCode, metricCode);
        }
        return o;
    }

    @Override
    public ThingModel getThing(String code) {
        synchronized (this) {
        }

        ThingModel o = null;

        if (THING_CACHE.containsKey(code)) {
            o = THING_CACHE.get(code);
        } else {
            LOGGER.warn("Thing code '{}' not found. ", code);
        }

        return o;
    }

    @Override
    public MetricModel getMetric(String code) {
        synchronized (this) {
        }

        MetricModel o = null;

        if (METRIC_CACHE.containsKey(code)) {
            o = METRIC_CACHE.get(code);
        } else {
            LOGGER.warn("Metric code '{}' not found. ", code);
        }

        return o;
    }

    @Override
    public void reload() {
        initCache();
    }
}
