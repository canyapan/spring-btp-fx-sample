package com.canyapan.sample.springbtpfxsample.services;

import org.junit.jupiter.api.Test;
import org.mockserver.client.MockServerClient;
import org.mockserver.springtest.MockServerTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;
import static org.mockserver.model.JsonBody.json;

@SpringBootTest
@ActiveProfiles("test")
@MockServerTest({"fx.api.base-url=http://localhost:${mockServerPort}/api/"})
public class FxClientIntegrationTest {

    private MockServerClient mockServer;

    @Autowired
    private FxClient fxClient;

    @Test
    public void shouldFetchExchangeRateWhenServiceReturns200() {
        mockServer.when(
                request()
                        .withMethod("GET")
                        .withPath("/api/rates/USD/EUR/latest")
        ).respond(
                response().withStatusCode(200).withBody(json("""
                        {"status_code":200,"data":{"base":"USD","target":"EUR","mid":0.849365,"unit":1,"timestamp":"2025-12-30T00:05:24.876Z"}}"""))
        );

        FxClient.ExchangeRate er = fxClient.fetchExchangeRate("USD", "EUR");
        assertEquals("USD", er.getBase());
        assertEquals("EUR", er.getTarget());
        assertEquals(new BigDecimal("0.849365"), er.getMid());
        assertEquals(ZonedDateTime.of(2025, 12, 30, 00, 05, 24, 876000000, ZoneOffset.UTC), er.getTimestamp());
    }

    @Test
    public void shouldThrowWhenServiceReturnsNon200StatusInBody() {
        mockServer.when(
                request()
                        .withMethod("GET")
                        .withPath("/api/rates/EUR/UNK/latest")
        ).respond(
                response().withStatusCode(422).withBody(json("""
                        {"status_code":422,"data":{"message":"Invalid currency code: UNK"}}"""))
        );

        assertThrows(com.canyapan.sample.springbtpfxsample.exceptions.FxIntegrationException.class,
                () -> fxClient.fetchExchangeRate("EUR", "UNK"));
    }

    @Test
    public void shouldThrowWhenResponseBodyIsNull() {
        mockServer.when(
                request()
                        .withMethod("GET")
                        .withPath("/api/rates/EUR/USD/latest")
        ).respond(
                response().withStatusCode(200).withBody("")
        );

        assertThrows(com.canyapan.sample.springbtpfxsample.exceptions.FxIntegrationException.class,
                () -> fxClient.fetchExchangeRate("EUR", "USD"));
    }

}
