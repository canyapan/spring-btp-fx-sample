package com.canyapan.sample.springbtpfxsample.controllers;

import com.canyapan.sample.springbtpfxsample.services.ExchangeRateService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class ExchangeRateControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ExchangeRateService serviceMock;

    @Test
    void shouldSyncRateForCurrencies() throws Exception {
        String base = "USD";
        String target = "EUR";

        mockMvc.perform(post("/api/v1/rate/{base}/{target}/sync", base, target)
                        .with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_ExchangeRate.Sync"))))
                .andDo(print())
                .andExpect(status().is2xxSuccessful());

        verify(serviceMock).updateRate(base, target);
    }

    @Test
    void shouldRejectUnknownCurrencies() throws Exception {
        String base = "USD";
        String target = "UNK";

        mockMvc.perform(post("/api/v1/rate/{base}/{target}/sync", base, target)
                        .with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_ExchangeRate.Sync"))))
                .andDo(print())
                .andExpect(status().isBadRequest());

        verify(serviceMock, never()).updateRate(base, target);
    }

    @Test
    void shouldRejectEmptyCurrencies() throws Exception {
        String base = "USD";
        String target = " ";

        mockMvc.perform(post("/api/v1/rate/{base}/{target}/sync", base, target)
                        .with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_ExchangeRate.Sync"))))
                .andDo(print())
                .andExpect(status().isBadRequest());

        verify(serviceMock, never()).updateRate(base, target);
    }

    @Test
    void shouldRejectWhenNoAuthentication() throws Exception {
        String base = "USD";
        String target = "EUR";

        mockMvc.perform(post("/api/v1/rate/{base}/{target}/sync", base, target))
                .andDo(print())
                .andExpect(status().isForbidden());

        verify(serviceMock, never()).updateRate(base, target);
    }

    @Test
    void shouldRejectWhenUnauthenticated() throws Exception {
        String base = "USD";
        String target = "EUR";

        mockMvc.perform(post("/api/v1/rate/{base}/{target}/sync", base, target)
                        .with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_Another"))))
                .andDo(print())
                .andExpect(status().isForbidden());

        verify(serviceMock, never()).updateRate(base, target);
    }
}