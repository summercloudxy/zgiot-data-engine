package com.zgiot.dataengine.repository;

import com.zgiot.common.pojo.MetricModel;
import com.zgiot.common.pojo.ThingModel;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface TMLMapper {
    @Select("SELECT * FROM `rel_thing_metric_label` ")
    List<ThingMetricLabel> findAllDeviceMetricLabels();

    @Select("SELECT * FROM `tb_metric` ")
    List<MetricModel> findAllMetrics();

    @Select("SELECT * FROM `tb_thing` ")
    List<ThingModel> findAllThings();

}