package com.canyapan.sample.springbtpfxsample.services;

import com.canyapan.sample.springbtpfxsample.exceptions.FxIntegrationException;
import com.canyapan.sample.springbtpfxsample.exceptions.S4IntegrationException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
public class ExchangeRateServiceTest {

    @MockitoBean
    private FxClient fxClient;

    @MockitoBean
    private S4HanaClient s4HanaClient;

    @Autowired
    private ExchangeRateService service;

    private FxClient.ExchangeRate sampleRate() {
        FxClient.ExchangeRate r = new FxClient.ExchangeRate();
        r.setBase("EUR");
        r.setTarget("USD");
        r.setDate("2025-12-30");
        r.setMid(new BigDecimal("1.23"));
        r.setTimestamp(ZonedDateTime.parse("2025-12-30T12:00:00Z"));
        return r;
    }

    @Test
    public void shouldFetchFromFxAndSendToS4WhenUpdateRateCalled() {
        FxClient.ExchangeRate r = sampleRate();
        when(fxClient.fetchExchangeRate("EUR", "USD")).thenReturn(r);

        service.updateRate("EUR", "USD");

        verify(fxClient, times(1)).fetchExchangeRate("EUR", "USD");
        ArgumentCaptor<FxClient.ExchangeRate> captor = ArgumentCaptor.forClass(FxClient.ExchangeRate.class);
        verify(s4HanaClient, times(1)).sendExchangeRate(captor.capture());

        FxClient.ExchangeRate passed = captor.getValue();
        assertEquals(r.getBase(), passed.getBase());
        assertEquals(r.getTarget(), passed.getTarget());
        assertEquals(r.getMid(), passed.getMid());
    }

    @Test
    public void shouldNotInvokeS4AndPropagateWhenFxClientFails() {
        when(fxClient.fetchExchangeRate("EUR", "USD")).thenThrow(new FxIntegrationException("fx failed"));

        assertThrows(FxIntegrationException.class, () -> service.updateRate("EUR", "USD"));

        verifyNoInteractions(s4HanaClient);
    }

    @Test
    public void shouldPropagateS4IntegrationExceptionWhenS4ClientFails() {
        FxClient.ExchangeRate r = sampleRate();
        when(fxClient.fetchExchangeRate("EUR", "USD")).thenReturn(r);
        doThrow(new S4IntegrationException("s4 failed", new RuntimeException("cause"))).when(s4HanaClient).sendExchangeRate(r);

        assertThrows(S4IntegrationException.class, () -> service.updateRate("EUR", "USD"));
    }

}
