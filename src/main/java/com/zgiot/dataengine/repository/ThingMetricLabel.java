package com.zgiot.dataengine.repository;

public class ThingMetricLabel {
    private String thingCode;
    private String metricCode;
    private String labelPath;

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
}
