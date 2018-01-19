package com.zgiot.dataengine.dataplugin.excel.dao;

import com.zgiot.common.pojo.CoalAnalysisRecord;
import com.zgiot.dataengine.dataplugin.excel.pojo.ExcelRange;
import com.zgiot.dataengine.dataplugin.excel.pojo.RecordTimeRange;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * Created by xiayun on 2017/9/1.
 */
@Mapper
public interface ExcelMapper {
    List<ExcelRange> getExcelRange();
    List<CoalAnalysisRecord> getExistRecord(RecordTimeRange timeRange);
}
