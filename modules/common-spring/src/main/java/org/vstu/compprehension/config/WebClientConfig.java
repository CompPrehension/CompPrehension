package org.vstu.compprehension.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Configuration
public class WebClientConfig {

    /**
     * TODO после обновления до Spring Framework 6.1 заменить на RestClient
     */
    @Bean
    public RestTemplate restTemplate(
            @Value("${compprehension.http.client.connect-timeout:10s}") Duration connectTimeout,
            @Value("${compprehension.http.client.read-timeout:30s}") Duration readTimeout
    ) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout((int) connectTimeout.toMillis());
        factory.setReadTimeout((int) readTimeout.toMillis());
        return new RestTemplate(factory);
    }
}
