package com.zgiot.dataengine.common.pojo;

import com.mongodb.Mongo;
import com.zgiot.common.pojo.DataModel;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

@Document(collection = "metricdata")
public class MongoData {
    private String mc;
    private String tc;
    private String mcc;
    private String tcc;
    private String v;
    private long ts;

    public String getMc() {
        return mc;
    }

    public void setMc(String mc) {
        this.mc = mc;
    }

    public String getTc() {
        return tc;
    }

    public void setTc(String tc) {
        this.tc = tc;
    }

    public String getMcc() {
        return mcc;
    }

    public void setMcc(String mcc) {
        this.mcc = mcc;
    }

    public String getTcc() {
        return tcc;
    }

    public void setTcc(String tcc) {
        this.tcc = tcc;
    }

    public String getV() {
        return v;
    }

    public void setV(String v) {
        this.v = v;
    }

    public long getTs() {
        return ts;
    }

    public void setTs(long ts) {
        this.ts = ts;
    }

    public static DataModel convertToDataModel(MongoData obj){
        DataModel dest = new DataModel();
        dest.setDataTimeStamp(new Date(obj.getTs()));
        dest.setMetricCategoryCode(obj.getMcc());
        dest.setMetricCode(obj.getMc());
        dest.setThingCategoryCode(obj.getTcc());
        dest.setThingCode(obj.getTc());
        dest.setValue(obj.getV());
        return dest;
    }

    public static MongoData convertFromDataModel(DataModel src){
        MongoData dest = new MongoData();
        dest.setTs(src.getDataTimeStamp().getTime());
        dest.setMcc(src.getMetricCategoryCode());
        dest.setMc(src.getMetricCode());
        dest.setTcc(src.getThingCategoryCode());
        dest.setTc(src.getThingCode());
        dest.setV(src.getValue().toString());
        return dest;
    }
}
