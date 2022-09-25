package com.winllc.pki.ra.config;

import com.winllc.acme.common.cache.CachedCertificateService;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.common.settings.Settings;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.client.RestClients;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;

@Configuration
//@EnableElasticsearchRepositories(
//        basePackages = "com.winllc.acme.common.cache"
//)
public class CacheConfig {

    //@Value("${elasticsearch.url}")
    String elasticSearchUrl;

    //@Bean
    public RestHighLevelClient client() {
        ClientConfiguration clientConfiguration
                = ClientConfiguration.builder()
                .connectedTo(elasticSearchUrl)
                .build();

        return RestClients.create(clientConfiguration).rest();
    }

    //@Bean
    public ElasticsearchOperations elasticsearchTemplate() {
        return new ElasticsearchRestTemplate(client());
    }

    @Bean
    public CachedCertificateService cachedCertificateService(ElasticsearchOperations operations){
        return new CachedCertificateService(operations);
    }

    @Bean
    public boolean createTestIndex(RestHighLevelClient restHighLevelClient) throws Exception {

        try {
            CreateIndexRequest createIndexRequest = new CreateIndexRequest("cachedcertificate");
            createIndexRequest.settings(
                    Settings.builder()
                            .put("index.number_of_shards", 1)
                            .put("index.number_of_replicas", 0));
            restHighLevelClient.indices()
                    .create(createIndexRequest, RequestOptions.DEFAULT); // 2
        }catch (Exception e){
            e.printStackTrace();
        }

        return true;
    }
}
