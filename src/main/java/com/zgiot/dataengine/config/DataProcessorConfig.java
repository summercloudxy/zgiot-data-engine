package com.zgiot.dataengine.config;

import com.zgiot.dataengine.dataprocessor.DataListener;
import com.zgiot.dataengine.dataprocessor.DataProcessorManager;
import com.zgiot.dataengine.dataprocessor.upforwarder.UpforwarderDataListener;
import com.zgiot.dataengine.dataprocessor.upforwarder.UpforwarderHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class DataProcessorConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger(DataProcessorConfig.class);
    @Value("${dataengine.datalisteners}")
    String configDataListeners;

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

    @Bean
    public DataProcessorManager newDataProcessorManager() {
        Map<String, DataListener> map = new HashMap<>();
        map.put("NONE", null);
        map.put("WSS", newUpforwarderDataListener());

        DataProcessorManager obj = new DataProcessorManager();
        String[] configArr = this.configDataListeners.split(",");
        for (String str : configArr) {
            DataListener dl = map.get(str.trim());
            if (dl == null) {
                continue;
            }

            obj.getDataListeners().add(dl);
            LOGGER.info("DataListener added: {}", dl.getClass());
        }

        return obj;
    }

}
