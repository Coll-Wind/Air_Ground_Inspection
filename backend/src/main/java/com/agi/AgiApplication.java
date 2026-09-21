package com.agi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 空地协同巡检集成平台 - 主启动类
 * 内嵌设备仿真器(@Scheduled),集成 Kafka/MongoDB/HDFS/Elasticsearch
 */
@SpringBootApplication
@EnableScheduling
public class AgiApplication {

    public static void main(String[] args) {
        SpringApplication.run(AgiApplication.class, args);
    }
}
