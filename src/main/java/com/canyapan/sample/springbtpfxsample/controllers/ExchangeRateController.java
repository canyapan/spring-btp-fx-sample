package com.canyapan.sample.springbtpfxsample.controllers;

import com.canyapan.sample.springbtpfxsample.services.ExchangeRateService;
import com.canyapan.sample.springbtpfxsample.validation.AllowedCurrency;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@Validated
@RestController
@RequestMapping("api/v1/rate")
@RequiredArgsConstructor
public class ExchangeRateController {

    private final ExchangeRateService service;

    @PostMapping("/{base}/{target}/sync")
    public void syncRate(
            @PathVariable @NotBlank @AllowedCurrency String base,
            @PathVariable @NotBlank @AllowedCurrency String target) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String userName = auth.getName();

        log.info("exchange sync is triggered for {}/{} by user {}", base, target, userName);

        service.updateRate(base, target);
    }

}
