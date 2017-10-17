package com.zgiot.dataengine.repository;

public class ThingMetricLabel {
    private String thingCode;
    private String metricCode;
    private String labelPath;
    private int boolReverse;  // 1 yes, 0 not
    private int enabled;

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

    public String getLabelPath() {
        return labelPath;
    }

    public void setLabelPath(String labelPath) {
        this.labelPath = labelPath;
    }

    public int getEnabled() {
        return enabled;
    }

    public void setEnabled(int enabled) {
        this.enabled = enabled;
    }

    public int getBoolReverse() {
        return boolReverse;
    }

    public void setBoolReverse(int boolReverse) {
        this.boolReverse = boolReverse;
    }
}
