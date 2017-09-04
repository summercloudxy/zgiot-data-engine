package com.zgiot.dataengine.config;

import com.zgiot.dataengine.dataprocessor.DataProcessorManager;
import com.zgiot.dataengine.dataprocessor.mongo.DataPersistMongoDbDataListener;
import com.zgiot.dataengine.upforwarder.UpforwarderDataListener;
import com.zgiot.dataengine.upforwarder.UpforwarderHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DataProcessorConfig {

    @Bean
    public UpforwarderDataListener newUpforwarderDataListener() {
        return new UpforwarderDataListener();
    }

    @Bean
    public UpforwarderHandler newUpforwarderHandler() {
        UpforwarderHandler obj = new UpforwarderHandler();
        obj.setUpforwarderDataListener(newUpforwarderDataListener());
        return obj;
    }

    @Autowired
    DataPersistMongoDbDataListener dataPersistMongoDbDataListener;

    @Bean
    public DataProcessorManager newDataProcessorManager() {
        DataProcessorManager obj = new DataProcessorManager();
        obj.getDataListeners().add(newUpforwarderDataListener());
        obj.getDataListeners().add(dataPersistMongoDbDataListener);
        return obj;
    }

}
