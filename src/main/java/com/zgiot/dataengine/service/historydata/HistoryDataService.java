package com.zgiot.dataengine.service.historydata;

import com.zgiot.common.pojo.DataModel;
import com.zgiot.dataengine.common.pojo.MongoData;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;

public interface HistoryDataService {
    /**
     * @see
     */
    List<DataModel> findHistoryData(List<String> thingCodes, List<String> metricCodes, Date endDate);

    /**
     * @param thingCodes  null means no limit
     * @param metricCodes null means no limit
     * @param startDate
     * @param endDate
     * @return key is 'thingcode-metriccode' ; sort desc on timestatmp;  if no data, return map size 0 .
     * returned DataModel value is all type of 'String'.
     * @see #
     */
    List<DataModel> findHistoryDataList(List<String> thingCodes, List<String> metricCodes
            , Date startDate, Date endDate);

    /**
     * * @see #findHistoryData(List, List, Date, Date)
     *
     * @param thingCodes
     * @param metricCodes
     * @param startDate
     * @param durationMs
     * @return
     */
    List<DataModel> findHistoryData(List<String> thingCodes, List<String> metricCodes
            , Date startDate, long durationMs);

    /**
     * @param list MongoData use MongoData directly to avoid another loop to convert obj.
     * @return count of success.
     */
    int insertBatch(List<MongoData> list);

}
