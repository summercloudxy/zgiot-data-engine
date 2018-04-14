package com.zgiot.dataengine.repository;

import com.zgiot.common.pojo.MetricModel;
import com.zgiot.common.pojo.ThingModel;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.SelectKey;

import java.util.List;

@Mapper
public interface TMLMapper {
    @Select("SELECT * FROM `rel_thing_metric_label` ")
    List<ThingMetricLabel> findAllDeviceMetricLabels();

    @Select("SELECT * FROM `tb_metric` ")
    List<MetricModel> findAllMetrics();

    @Select("SELECT * FROM `tb_thing` ")
    List<ThingModel> findAllThings();

    @Insert("INSERT INTO `tb_dae_send_log` (user_uuid,  send_time,  thing_code,  metric_code,  value,  dmtime) " +
            "VALUES (#{userUuid},#{sendTime},#{thingCode},#{metricCode},#{value},#{dmTime}) ")
    @SelectKey(statement = "select LAST_INSERT_ID()", keyProperty = "id",
            before = false, resultType = long.class)
    void insertSendLog(SendLog log);

}