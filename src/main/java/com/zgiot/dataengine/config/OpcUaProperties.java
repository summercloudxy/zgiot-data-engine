package com.zgiot.dataengine.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "opcua")
public class OpcUaProperties {
    private String endpointUrl;

    public String getEndpointUrl() {
        return endpointUrl;
    }

    public void setEndpointUrl(String endpointUrl) {
        this.endpointUrl = endpointUrl;
    }
}

