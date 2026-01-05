package com.canyapan.sample.springbtpfxsample.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class AllowedCurrencyValidator implements ConstraintValidator<AllowedCurrency, String> {

    @Value("${fx.api.allowed-currencies:}")
    private Set<String> allowedCurrencies;

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {

        return allowedCurrencies.contains(value);
    }
}
