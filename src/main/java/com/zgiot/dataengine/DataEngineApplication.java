package com.zgiot.dataengine;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.stereotype.Component;

@Component
@SpringBootApplication
@EnableScheduling
public class DataEngineApplication {

    public static void main(String[] args) {
        try {
            SpringApplication.run(DataEngineApplication.class, args);
        } catch (Throwable e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}
