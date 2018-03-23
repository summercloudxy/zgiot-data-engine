package com.zgiot.dataengine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.stereotype.Component;

@Component
@SpringBootApplication
@EnableScheduling
public class DataEngineApplication {
    private static final Logger LOGGER = LoggerFactory.getLogger(DataEngineApplication.class);
    public static void main(String[] args) {
        try {
            SpringApplication.run(DataEngineApplication.class, args);
        } catch (Throwable e) {
            LOGGER.error("Fail to startup. ",e);
        }
    }
}
