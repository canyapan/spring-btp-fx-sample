package com.canyapan.sample.springbtpfxsample.services;

import com.canyapan.sample.springbtpfxsample.exceptions.S4IntegrationException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockserver.client.MockServerClient;
import org.mockserver.springtest.MockServerTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;
import static org.mockserver.model.JsonBody.json;

@SpringBootTest
@ActiveProfiles("test")
@MockServerTest({"sap.s4.base-url=http://localhost:${mockServerPort}/odata/v2/"})
public class S4HanaClientIntegrationTest {

    private MockServerClient mockServer;

    @Autowired
    private S4HanaClient s4HanaClient;

    @Autowired
    private S4HanaCsrfTokenCache tokenCache;

    @BeforeEach
    void setUp(){
        tokenCache.update("dummy-csrf-token", List.of("_session=dummy-session-cookie"));
    }

    @AfterEach
    void cleanUp() {
        mockServer.reset();
    }

    @Test
    public void shouldUpdateExchangeRateForEurUsdOnS4Hana() {
        mockServer.when(request()
                        .withMethod("POST")
                        .withPath("/odata/v2/API_EXCHANGE_RATE_SRV/A_ExchangeRate")
                        .withHeader("x-csrf-token", "dummy-csrf-token")
                        .withCookie("_session", "dummy-session-cookie"))
                .respond(response()
                        .withStatusCode(204)
                );

        FxClient.ExchangeRate er = new FxClient.ExchangeRate();
        er.setBase("EUR");
        er.setTarget("USD");
        er.setMid(new BigDecimal("1.23"));
        er.setTimestamp(ZonedDateTime.of(LocalDateTime.of(2025, 12, 30, 12, 13, 30), ZoneOffset.UTC));

        s4HanaClient.sendExchangeRate(er);

        // verify request was received with expected JSON fields
        mockServer.verify(request()
                .withMethod("POST")
                .withPath("/odata/v2/API_EXCHANGE_RATE_SRV/A_ExchangeRate")
                .withHeader("x-csrf-token", "dummy-csrf-token")
                .withCookie("_session", "dummy-session-cookie")
                .withBody(json("""
                        {
                          "ExchangeRateType" : "M",
                          "SourceCurrency" : "EUR",
                          "TargetCurrency" : "USD",
                          "ExchangeRate" : 1.23,
                          "ValidityStartDate" : "2025-12-30"
                        }"""))
        );
    }

    @Test
    public void shouldThrowWhenServerReturns500() {
        mockServer.when(request()
                        .withMethod("POST")
                        .withPath("/odata/v2/API_EXCHANGE_RATE_SRV/A_ExchangeRate")
                        .withHeader("x-csrf-token", "dummy-csrf-token")
                        .withCookie("_session", "dummy-session-cookie"))
                .respond(response()
                        .withStatusCode(500));

        FxClient.ExchangeRate er = new FxClient.ExchangeRate();
        er.setBase("EUR");
        er.setTarget("USD");
        er.setMid(new BigDecimal("1.23"));
        er.setTimestamp(ZonedDateTime.of(LocalDateTime.of(2025, 12, 30, 12, 13, 30), ZoneOffset.UTC));

        S4IntegrationException exception = assertThrows(S4IntegrationException.class,
                () -> s4HanaClient.sendExchangeRate(er));

        assertEquals("Exchange rate couldn't be updated on S/4HANA", exception.getMessage());
    }

}
