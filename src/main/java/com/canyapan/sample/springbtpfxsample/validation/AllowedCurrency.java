package com.canyapan.sample.springbtpfxsample.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Documented
@Constraint(validatedBy = AllowedCurrencyValidator.class)
@Target({PARAMETER, FIELD, ANNOTATION_TYPE})
@Retention(RUNTIME)
public @interface AllowedCurrency {
    String message() default "must be one of the allowed currencies";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
