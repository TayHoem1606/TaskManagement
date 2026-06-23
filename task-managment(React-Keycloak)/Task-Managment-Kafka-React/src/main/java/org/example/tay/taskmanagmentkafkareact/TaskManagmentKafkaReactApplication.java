package org.example.tay.taskmanagmentkafkareact;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.config.EnableReactiveMongoAuditing;
import org.springframework.data.mongodb.repository.config.EnableReactiveMongoRepositories;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableReactiveMongoRepositories
@EnableReactiveMongoAuditing
public class TaskManagmentKafkaReactApplication {

    public static void main(String[] args) {
        SpringApplication.run(TaskManagmentKafkaReactApplication.class, args);
    }

}
