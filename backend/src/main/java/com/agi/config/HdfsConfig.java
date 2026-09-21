package com.agi.config;

import org.apache.hadoop.fs.FileSystem;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.URI;

/**
 * HDFS 客户端配置
 */
@Configuration
public class HdfsConfig {

    @Value("${hdfs.uri}")
    private String hdfsUri;

    @Value("${hdfs.user}")
    private String hdfsUser;

    @Bean
    public FileSystem fileSystem() throws Exception {
        // 使用全限定名避免与 Spring 的 Configuration 冲突
        org.apache.hadoop.conf.Configuration conf = new org.apache.hadoop.conf.Configuration();
        conf.set("dfs.replication", "1");
        conf.set("dfs.client.use.datanode.hostname", "true");
        // 解决 Windows 下 HDFS 客户端权限问题
        System.setProperty("HADOOP_USER_NAME", hdfsUser);
        return FileSystem.get(new URI(hdfsUri), conf, hdfsUser);
    }
}
