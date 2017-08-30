package com.zgiot.dataengine.common;

import com.zgiot.common.pojo.DataModel;

public interface DataProcessListener {
    void onChanged(DataModel data);
}

