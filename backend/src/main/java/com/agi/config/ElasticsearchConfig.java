package com.agi.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Elasticsearch 客户端配置
 */
@Configuration
public class ElasticsearchConfig {

    @Value("${elasticsearch.uris}")
    private String esUris;

    @Bean
    public ElasticsearchClient elasticsearchClient() {
        // 解析 URI,支持逗号分隔多节点
        String[] uriArr = esUris.split(",");
        HttpHost[] hosts = new HttpHost[uriArr.length];
        for (int i = 0; i < uriArr.length; i++) {
            String uri = uriArr[i].trim();
            if (uri.startsWith("http://")) uri = uri.substring(7);
            if (uri.startsWith("https://")) uri = uri.substring(8);
            String[] parts = uri.split(":");
            hosts[i] = new HttpHost(parts[0], Integer.parseInt(parts[1]), "http");
        }

        RestClient restClient = RestClient.builder(hosts).build();
        RestClientTransport transport = new RestClientTransport(restClient, new JacksonJsonpMapper());
        return new ElasticsearchClient(transport);
    }
}
