package com.canyapan.sample.springbtpfxsample.configs;

import com.canyapan.sample.springbtpfxsample.services.S4HanaCsrfTokenInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;


@Configuration
public class RestClientConfig {

    @Bean
    public RestClient fxRestClient(
            RestClient.Builder builder,
            @Value("${fx.api.base-url}") String baseUrl) {

        return builder
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    @Bean
    public RestClient s4HanaRestClient(
            RestClient.Builder builder,
            S4HanaCsrfTokenInterceptor csrfTokenInterceptor,
            @Value("${sap.s4.base-url}") String baseUrl) {

        return builder
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .requestInterceptor(csrfTokenInterceptor)
                .build();
    }

    @Bean
    public RestClient s4HanaRestClientForCsrfToken(
            RestClient.Builder builder,
            @Value("${sap.s4.base-url}") String baseUrl) {

        return builder
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }
}
