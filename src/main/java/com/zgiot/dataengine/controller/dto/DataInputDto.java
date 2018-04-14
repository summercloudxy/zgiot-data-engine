package com.zgiot.dataengine.controller.dto;

import com.zgiot.common.pojo.DataModel;

import java.util.List;

public class DataInputDto {
    private String userUuid;
    private String accessToken;
    private String source;
    private List<DataModel> dataList;

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getUserUuid() {
        return userUuid;
    }

    public void setUserUuid(String userUuid) {
        this.userUuid = userUuid;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public List<DataModel> getDataList() {
        return dataList;
    }

    public void setDataList(List<DataModel> dataList) {
        this.dataList = dataList;
    }
}
