package org.example.tay.springbkafkamongodbdemo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.config.EnableReactiveMongoAuditing;
import org.springframework.data.mongodb.repository.config.EnableReactiveMongoRepositories;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableReactiveMongoRepositories
@EnableReactiveMongoAuditing
@EnableAsync
public class SpringBKafkaMongoDbDemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpringBKafkaMongoDbDemoApplication.class, args);
    }
}
