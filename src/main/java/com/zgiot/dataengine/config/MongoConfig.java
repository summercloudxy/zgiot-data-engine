package com.zgiot.dataengine.config;

import com.mongodb.MongoClientURI;
import com.zgiot.dataengine.common.DEConstants;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.MongoDbFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoDbFactory;

@Configuration
@EnableAutoConfiguration(exclude = {MongoAutoConfiguration.class
        , MongoDataAutoConfiguration.class})
public class MongoConfig {

    @Value("${spring.data.mongodb.uri:"+ DEConstants.NA+"}")
    String mongoUri;

    @Bean
    public MongoDbFactory mongoDbFactory() throws Exception {
        String uri = this.mongoUri;
        if (mongoUri.equals("0")) {
            uri = "mongodb://localhost:27017/test";
        }
        return new SimpleMongoDbFactory(new MongoClientURI(uri));
    }

    @Bean
    public MongoTemplate genMongoTemplate() throws Exception {
        return new MongoTemplate(mongoDbFactory());
    }

}
