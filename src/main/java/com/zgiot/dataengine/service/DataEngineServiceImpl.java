package com.zgiot.dataengine.service;

import com.zgiot.common.pojo.MetricModel;
import com.zgiot.common.pojo.ThingModel;
import com.zgiot.common.reloader.Reloader;
import com.zgiot.common.reloader.ServerReloadManager;
import com.zgiot.dataengine.repository.TMLMapper;
import com.zgiot.dataengine.repository.ThingMetricLabel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class DataEngineServiceImpl implements DataEngineService, Reloader {

    private static final Logger logger = LoggerFactory.getLogger(DataEngineServiceImpl.class);

    @Autowired
    private TMLMapper tmlMapper;

    private final static ConcurrentHashMap<String, MetricModel> metricCache = new ConcurrentHashMap(100);
    private final static ConcurrentHashMap<String, ThingModel> thingCache = new ConcurrentHashMap(100);

    private final static ConcurrentHashMap<String, ThingMetricLabel> label_ThingMetricCache = new ConcurrentHashMap(100);
    private final static ConcurrentHashMap<String, ThingMetricLabel> thingMetric_LabelCache = new ConcurrentHashMap(100);
    private final static List<ThingMetricLabel> thingMetricLabelCache = new ArrayList(100);

    @Override
    public void initCache() {
        synchronized (this) {
            List<ThingMetricLabel> list = tmlMapper.findAllDeviceMetricLabels();
            thingMetricLabelCache.clear();
            label_ThingMetricCache.clear();
            thingMetric_LabelCache.clear();
            for (ThingMetricLabel item : list) {
                thingMetricLabelCache.add(item);
                label_ThingMetricCache.put(item.getLabelPath(), item);
                thingMetric_LabelCache.put(item.getThingCode() + "_" + item.getMetricCode(), item);
            }

            List<ThingModel> things = this.tmlMapper.findAllThings();
            thingCache.clear();
            for (ThingModel t : things) {
                thingCache.put(t.getThingCode(), t);
            }

            List<MetricModel> metrics = this.tmlMapper.findAllMetrics();
            metricCache.clear();
            for (MetricModel m : metrics) {
                metricCache.put(m.getMetricCode(), m);
            }

            logger.info("Cache inited. ");
        }
    }

    @Override
    public ThingMetricLabel getTMLByLabel(String labelPath) {
        synchronized (this) {
        }

        ThingMetricLabel o = label_ThingMetricCache.get(labelPath);
        if (o == null) {
            logger.warn("Not found metricCode for label '{}'", labelPath);
        }
        return o;
    }

    @Override
    public List<ThingMetricLabel> findAllTML() {
        synchronized (this) {
        }
        return thingMetricLabelCache;
    }

    @Override
    public ThingMetricLabel getTMLByTM(String thingCode, String metricCode) {
        synchronized (this) {
        }
        ThingMetricLabel o = thingMetric_LabelCache.get(thingCode + "_" + metricCode);
        if (o == null) {
            logger.warn("Not found label for thingCode {} and metricCode '{}'", thingCode, metricCode);
        }
        return o;
    }

    @Override
    public ThingModel getThing(String code) {
        synchronized (this) {
        }

        ThingModel o = null;

        if (thingCache.containsKey(code)) {
            o = thingCache.get(code);
        } else {
            logger.warn("Thing code '{}' not found. ", code);
        }

        return o;
    }

    @Override
    public MetricModel getMetric(String code) {
        synchronized (this) {
        }

        MetricModel o = null;

        if (metricCache.containsKey(code)) {
            o = metricCache.get(code);
        } else {
            logger.warn("Metric code '{}' not found. ", code);
        }

        return o;
    }

    @Override
    public void reload() {
        initCache();
    }
}
