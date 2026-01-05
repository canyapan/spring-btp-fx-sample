package com.canyapan.sample.springbtpfxsample.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Component
public final class S4HanaCsrfTokenCache {

    @Value("${sap.s4.max-token-age:10m}")
    private Duration maxTokenAge;

    private String csrfToken;
    private List<String> cookies;
    private Instant fetchedAt;

    synchronized boolean isValid() {
        return csrfToken != null
                && cookies != null
                && fetchedAt != null
                && Instant.now().isBefore(fetchedAt.plus(maxTokenAge));
    }

    synchronized void update(String token, List<String> cookies) {
        this.csrfToken = token;
        this.cookies = cookies;
        this.fetchedAt = Instant.now();
    }

    synchronized void invalidate() {
        this.csrfToken = null;
        this.cookies = null;
        this.fetchedAt = null;
    }

    synchronized String getToken() {
        return csrfToken;
    }

    synchronized List<String> getCookies() {
        return cookies;
    }
}