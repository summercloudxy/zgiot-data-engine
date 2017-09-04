package com.zgiot.dataengine.dataprocessor;

import com.zgiot.common.pojo.DataModel;

public interface DataListener {
    void onData(DataModel data);
}

