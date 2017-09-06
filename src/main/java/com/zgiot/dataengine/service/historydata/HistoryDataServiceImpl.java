package com.zgiot.dataengine.service.historydata;

import com.zgiot.common.pojo.DataModel;
import com.zgiot.dataengine.common.DEConstants;
import com.zgiot.dataengine.common.pojo.MongoData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
public class HistoryDataServiceImpl implements HistoryDataService {

    private static final Logger logger = LoggerFactory.getLogger(HistoryDataServiceImpl.class);

    @Autowired
    MongoTemplate mongoTemplate;

    @Value("${spring.data.mongodb.uri:"+ DEConstants.NA+"}")
    String mongoUri;

    public List<DataModel> findHistoryData(List<String> thingCodes, List<String> metricCodes, Date endDate) {
        return findHistoryDataList(thingCodes, metricCodes
                , new Date(0), endDate);
    }

    public List<DataModel> findHistoryDataList(List<String> thingCodes, List<String> metricCodes
            , Date startDate, Date endDate) {

        if (DEConstants.NA.equals(this.mongoUri)){
            return new ArrayList<>();
        }
        /* required index as  ts_tc_mc */

        Query q = new Query();
        Criteria c = new Criteria();

        // for tc
        if (thingCodes != null && thingCodes.size() > 0) {
            c.and("tc").in(thingCodes);
        }

        // for mc
        if (metricCodes != null && metricCodes.size() > 0) {
            c.and("mc").in(metricCodes);
        }

        // for end date
        if (endDate == null) {
            throw new IllegalArgumentException("enddate required.");
        }

        // for start date
        long startDateL = 0;
        if (startDate != null) {
            startDateL = startDate.getTime();
        }

        // join ts
        Criteria tsC = new Criteria("ts");
        tsC.lte(endDate.getTime()).gte(startDateL);
        c.andOperator(tsC);

        q.addCriteria(c);

        Sort tsSort = new Sort(new Sort.Order(Sort.Direction.DESC, "ts"));
        q.with(tsSort);

        logger.debug("Query is `{}`", q.toString());

        List<MongoData> dblist = this.mongoTemplate.find(q, MongoData.class);
        List<DataModel> retList = new ArrayList<>(dblist.size());
        for (MongoData data : dblist) {
            DataModel dest = new DataModel();
            dest.setDataTimeStamp(new Date(data.getTs()));
            dest.setMetricCategoryCode(data.getMcc());
            dest.setMetricCode(data.getMc());
            dest.setThingCategoryCode(data.getTcc());
            dest.setThingCode(data.getTc());
            dest.setValue(data.getV());

            retList.add(dest);
        }

        return retList;
    }

    public List<DataModel> findHistoryData(List<String> thingCodes, List<String> metricCodes
            , Date startDate, long durationMs) {
        Date endDate = new Date(startDate.getTime() + durationMs);
        return findHistoryDataList(thingCodes, metricCodes
                , startDate, endDate);
    }

    @Override
    public int insertBatch(List<MongoData> list) {
        if (list == null || list.size() == 0) {
            return 0;
        }
        try {
            if (DEConstants.NA.equals(this.mongoUri)){
                return 0;
            }
            this.mongoTemplate.insert(list, MongoData.class);
        } catch (Exception e) {
            logger.error("Insert history data error: `{}`. ", e.getMessage());
            return -1;
        }
        return list.size();
    }

}
