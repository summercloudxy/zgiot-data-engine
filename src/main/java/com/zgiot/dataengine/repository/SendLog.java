package com.zgiot.dataengine.repository;

import java.util.Date;

public class SendLog {
    private long id;
    private String userUuid;
    private Date sendTime; // 服务器时间
    private String thingCode;
    private String metricCode;
    private String value; // 数据值
    private Date dmTime; //datamodel中的时间戳

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getUserUuid() {
        return userUuid;
    }

    public void setUserUuid(String userUuid) {
        this.userUuid = userUuid;
    }

    public Date getSendTime() {
        return sendTime;
    }

    public void setSendTime(Date sendTime) {
        this.sendTime = sendTime;
    }

    public String getThingCode() {
        return thingCode;
    }

    public void setThingCode(String thingCode) {
        this.thingCode = thingCode;
    }

    public String getMetricCode() {
        return metricCode;
    }

    public void setMetricCode(String metricCode) {
        this.metricCode = metricCode;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public Date getDmTime() {
        return dmTime;
    }

    public void setDmTime(Date dmTime) {
        this.dmTime = dmTime;
    }
}
