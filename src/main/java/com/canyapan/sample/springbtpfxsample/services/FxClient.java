package com.canyapan.sample.springbtpfxsample.services;

import com.canyapan.sample.springbtpfxsample.exceptions.FxIntegrationException;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.ZonedDateTime;

@Service
@RequiredArgsConstructor
public class FxClient {

    private final RestClient fxRestClient;

    public ExchangeRate fetchExchangeRate(String base, String target) {
        try {
            ResponseEntity<Response> entity = fxRestClient.get()
                    .uri("/rates/{base}/{target}/latest", base, target)
                    .retrieve()
                    .toEntity(Response.class);

            Response response = entity.getBody();

            if (null == response || null == response.getStatusCode() || null == response.getData()) {
                throw new FxIntegrationException("Failed to fetch fx rates due to null response from Fx service.");
            }

            if (!response.getStatusCode().equals(200)) {
                throw new FxIntegrationException("Failed to fetch fx rates due to non-success response from Fx service.");
            }

            return response.getData();

        } catch (Exception e) {
            throw new FxIntegrationException("Failed to fetch fx rates.", e);
        }

    }

    @Data
    public static class Response {

        @JsonProperty("status_code")
        private Integer statusCode;

        private ExchangeRate data;
    }

    @Data
    public static class ExchangeRate {

        private String base;
        private String target;
        private String date;
        private BigDecimal mid;
        private Integer unit;
        private ZonedDateTime timestamp;
    }

}


