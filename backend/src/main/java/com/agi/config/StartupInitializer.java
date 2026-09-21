package com.agi.config;

import com.agi.es.EsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * 应用启动初始化 - 创建 Elasticsearch 索引
 */
@Slf4j
@Component
public class StartupInitializer implements CommandLineRunner {

    @Autowired
    private EsService esService;

    @Override
    public void run(String... args) {
        try {
            esService.ensureIndex();
            log.info("Elasticsearch 索引初始化完成");
        } catch (Exception e) {
            log.error("Elasticsearch 索引初始化失败: {}", e.getMessage());
        }
    }
}
