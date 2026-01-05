package com.canyapan.sample.springbtpfxsample.services;

import com.canyapan.sample.springbtpfxsample.exceptions.S4IntegrationException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.format.DateTimeFormatter;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class S4HanaClient {

    private final RestClient s4HanaRestClient;

    public void sendExchangeRate(FxClient.ExchangeRate exchangeRate) {
        try {
            if (null == exchangeRate
                    || null == exchangeRate.getBase()
                    || null == exchangeRate.getTarget()
                    || null == exchangeRate.getMid()
                    || null == exchangeRate.getTimestamp()) {
                throw new IllegalArgumentException("ExchangeRate object or its properties cannot be null");
            }

            Map<String, Object> payload = Map.of(
                    "ExchangeRateType", "M",
                    "SourceCurrency", exchangeRate.getBase(),
                    "TargetCurrency", exchangeRate.getTarget(),
                    "ExchangeRate", exchangeRate.getMid(),
                    "ValidityStartDate", exchangeRate.getTimestamp().toLocalDate().format(DateTimeFormatter.ISO_DATE)
            );

            s4HanaRestClient.post()
                    .uri("/API_EXCHANGE_RATE_SRV/A_ExchangeRate")
                    .body(payload)
                    .retrieve()
                    .toBodilessEntity();

        } catch (Exception e) {
            throw new S4IntegrationException("Exchange rate couldn't be updated on S/4HANA", e);
        }
    }

}
