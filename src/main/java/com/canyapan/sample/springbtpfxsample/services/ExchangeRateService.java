package com.canyapan.sample.springbtpfxsample.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ExchangeRateService {

    private final FxClient fxClient;
    private final S4HanaClient s4HanaClient;

    public void updateRate(String base, String target) {
        FxClient.ExchangeRate er = fxClient.fetchExchangeRate(base, target);
        s4HanaClient.sendExchangeRate(er);
    }

}
