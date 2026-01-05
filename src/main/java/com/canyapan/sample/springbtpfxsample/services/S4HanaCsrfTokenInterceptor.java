package com.canyapan.sample.springbtpfxsample.services;

import com.canyapan.sample.springbtpfxsample.exceptions.S4IntegrationException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class S4HanaCsrfTokenInterceptor implements ClientHttpRequestInterceptor {

    private final S4HanaCsrfTokenCache cache;
    private final RestClient s4HanaRestClientForCsrfToken;

    @Override
    public ClientHttpResponse intercept(
            HttpRequest request,
            byte[] body,
            ClientHttpRequestExecution execution) throws IOException {

        HttpMethod method = request.getMethod();

        if (requiresCsrf(method)) {
            injectToken(request);
        }

        ClientHttpResponse response = execution.execute(request, body);

        // 🔑 Authoritative invalidation
        if (isSessionInvalid(response)) {
            cache.invalidate();
        }

        return response;
    }

    private void injectToken(HttpRequest request) {
        if (!cache.isValid()) {
            CsrfToken csrfToken = fetchCsrfToken();
            cache.update(csrfToken.token(), csrfToken.cookies());
        }

        request.getHeaders().set("x-csrf-token", cache.getToken());
        request.getHeaders().put(HttpHeaders.COOKIE, cache.getCookies());
    }

    private boolean requiresCsrf(HttpMethod method) {
        return method == HttpMethod.POST
                || method == HttpMethod.PUT
                || method == HttpMethod.PATCH
                || method == HttpMethod.DELETE;
    }

    private boolean isSessionInvalid(ClientHttpResponse response) throws IOException {
        return response.getStatusCode() == HttpStatus.FORBIDDEN
                || response.getStatusCode() == HttpStatus.UNAUTHORIZED;
    }

    private CsrfToken fetchCsrfToken() {
        try {
            ResponseEntity<Void> responseEntity = s4HanaRestClientForCsrfToken.get()
                    .uri("/API_EXCHANGE_RATE_SRV")
                    .header("x-csrf-token", "Fetch")
                    .retrieve()
                    .toBodilessEntity();

            String token = responseEntity.getHeaders().getFirst("x-csrf-token");
            List<String> cookies = responseEntity.getHeaders().get(HttpHeaders.SET_COOKIE);

            if (null == token || CollectionUtils.isEmpty(cookies)) {
                throw new IllegalStateException("Could not receive a CSRF token or session.");
            }

            return new CsrfToken(token, cookies);
        } catch (Exception e) {
            throw new S4IntegrationException("Exchange rate couldn't be updated on S/4HANA", e);
        }
    }

    private record CsrfToken(String token, List<String> cookies) {
    }
}