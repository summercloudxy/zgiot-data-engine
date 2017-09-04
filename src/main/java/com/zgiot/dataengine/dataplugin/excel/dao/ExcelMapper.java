package com.zgiot.dataengine.dataplugin.excel.dao;

import com.zgiot.dataengine.dataplugin.excel.pojo.ExcelRange;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * Created by xiayun on 2017/9/1.
 */
@Mapper
public interface ExcelMapper {
    List<ExcelRange> getExcelRange();
}
