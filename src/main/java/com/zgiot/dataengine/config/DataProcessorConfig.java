package com.zgiot.dataengine.config;

import com.alibaba.rocketmq.client.exception.MQClientException;
import com.alibaba.rocketmq.client.producer.DefaultMQProducer;
import com.alibaba.rocketmq.client.producer.MQProducer;
import com.zgiot.dataengine.dataprocessor.DataListener;
import com.zgiot.dataengine.dataprocessor.DataProcessorManager;
import com.zgiot.dataengine.dataprocessor.rocketmqupforwarder.RocketMqUpforwarderDataListener;
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

    @Value("#{new Boolean('${dataengine.rocketmq.enabled}')}" )
    private Boolean rktMqEnabled;

    @Value("${dataengine.rocketmq.nameservers}")
    private String rktMqNameservers;

    @Value("${dataengine.rocketmq.producer.name}")
    private String rktMqProducerName;

    @Bean
    public RocketMqUpforwarderDataListener newRocketUpforwarderDataListener() {
        if (!this.rktMqEnabled){
            return null;
        }
        return new RocketMqUpforwarderDataListener();
    }

    @Bean
    public MQProducer newDefaultMQProducer() {
        if (!this.rktMqEnabled){
            LOGGER.info("RocketMQ disabled.");
            return null;
        }

        DefaultMQProducer producer = new DefaultMQProducer(this.rktMqProducerName);
        producer.setNamesrvAddr(this.rktMqNameservers);
        producer.setRetryTimesWhenSendAsyncFailed(0);
        try {
            producer.start();
            LOGGER.info("RocketMQ started. nameservers=`{}`, producer=`{}` ", this.rktMqNameservers
                    , this.rktMqProducerName);
        } catch (MQClientException e) {
            LOGGER.error("Failed to start MQ producer. ", e);
            System.exit(2);
        }

        Runtime.getRuntime().addShutdownHook(new Thread(() -> producer.shutdown()));
        return producer;
    }

    @Bean
    public DataProcessorManager newDataProcessorManager() {
        Map<String, DataListener> map = new HashMap<>();
        map.put("NONE", null);
        map.put("WSS", newUpforwarderDataListener());
        map.put("ROCKETMQ", newRocketUpforwarderDataListener());

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
