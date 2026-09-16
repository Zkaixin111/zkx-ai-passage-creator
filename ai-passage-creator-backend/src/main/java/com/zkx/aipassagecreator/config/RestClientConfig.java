package com.zkx.aipassagecreator.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

/**
 * RestClient 超时配置
 * <p>
 * Spring AI 内部通过 RestClient 调用 DashScope API，默认无超时限制。
 * 配置连接超时和读取超时，防止 AI 接口卡死导致线程池耗尽。
 * 流式响应（大纲/正文）的 readTimeout 针对单次读取间隔，不影响正常流式推送。
 */
@Configuration
public class RestClientConfig {

    @Bean
    public RestClient.Builder restClientBuilder() {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(Duration.ofSeconds(60));
        return RestClient.builder().requestFactory(factory);
    }
}
