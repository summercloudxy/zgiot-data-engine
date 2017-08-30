package com.zgiot.dataengine.dataplugin;

import com.zgiot.common.pojo.DataModel;

import java.util.List;

public interface DataPlugin {
    void init() throws Exception;
    void start() throws Exception;
    int sendCommands(List<DataModel> datalist) throws Exception;
}
