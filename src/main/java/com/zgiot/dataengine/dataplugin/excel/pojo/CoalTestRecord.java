package com.zgiot.dataengine.dataplugin.excel.pojo;

import java.util.Date;

/**
 * 煤质化验记录
 */
public class CoalTestRecord {

    private String sample;

    private Double aad;

    private Double mt;

    private Double stad;

    private Double qnetar;

    private Date time;

    private String target;

    public String getSample() {
        return sample;
    }

    public void setSample(String sample) {
        this.sample = sample;
    }

    public Double getAad() {
        return aad;
    }

    public void setAad(Double aad) {
        this.aad = aad;
    }

    public Double getMt() {
        return mt;
    }

    public void setMt(Double mt) {
        this.mt = mt;
    }

    public Double getStad() {
        return stad;
    }

    public void setStad(Double stad) {
        this.stad = stad;
    }

    public Double getQnetar() {
        return qnetar;
    }

    public void setQnetar(Double qnetar) {
        this.qnetar = qnetar;
    }

    public Date getTime() {
        return time;
    }

    public void setTime(Date time) {
        this.time = time;
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }
}
