package com.canyapan.sample.springbtpfxsample.controllers;

import com.canyapan.sample.springbtpfxsample.exceptions.BaseException;
import com.canyapan.sample.springbtpfxsample.exceptions.InternalException;
import com.canyapan.sample.springbtpfxsample.exceptions.ValidationException;
import jakarta.servlet.http.Cookie;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.validation.Validator;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class GlobalExceptionHandlerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldHandleManagedException() throws Exception {
        mockMvc.perform(get("/test/throw-internal-exception")
                        .with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_User.Read"))))
                .andDo(print())
                .andExpect(status().isInternalServerError())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.reason").value("Internal Server Error"))
                .andExpect(jsonPath("$.message").value("Test InternalException"));
    }

    @Test
    void shouldHandleManagedValidationException() throws Exception {
        mockMvc.perform(get("/test/throw-validation-exception")
                        .with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_ExchangeRate.Sync"))))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.reason").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Test ValidationException"));
    }

    @Test
    void shouldHandleManagedUnannotatedException() throws Exception {
        mockMvc.perform(get("/test/throw-unannotated-exception")
                        .with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_ExchangeRate.Sync"))))
                .andDo(print())
                .andExpect(status().isInternalServerError())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.reason").value("Internal Server Error"))
                .andExpect(jsonPath("$.message").value("Test UnannotatedException"));
    }

    @Test
    void shouldHandleUnmanagedException() throws Exception {
        mockMvc.perform(get("/test/throw-unmanaged-exception")
                        .with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_ExchangeRate.Sync"))))
                .andDo(print())
                .andExpect(status().isInternalServerError())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.reason").value("Internal Server Error"))
                .andExpect(jsonPath("$.message").value("A server-side error occurred. Please reach out to the application support team if this issue persists."));
    }

    @Test
    void shouldHandleUnknownPath() throws Exception {
        mockMvc.perform(get("/test/not-found")
                        .with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_ExchangeRate.Sync"))))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.reason").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Requested content not found."));
    }

    @Test
    void shouldHandleAccessDeniedException() throws Exception {
        mockMvc.perform(get("/test/throw-access-denied-exception")
                        .with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_ExchangeRate.Sync"))))
                .andDo(print())
                .andExpect(status().isForbidden())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.reason").value("Forbidden"))
                .andExpect(jsonPath("$.message").value("Forbidden access to content."));
    }

    @Test
    void shouldHandleValidationExceptionWhenUnsupportedMethod() throws Exception {
        mockMvc.perform(get("/test/validated-request-payload")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_ExchangeRate.Sync"))))
                .andDo(print())
                .andExpect(status().isMethodNotAllowed())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.reason").value("Method Not Allowed"))
                .andExpect(jsonPath("$.message").value("Invalid http method received. It may be wrong or not supported by this operation. Try, [POST]."));
    }

    @Test
    void shouldHandleValidationExceptionWhenNullPayload() throws Exception {
        mockMvc.perform(post("/test/validated-request-payload")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_ExchangeRate.Sync"))))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.reason").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Cannot parse request body. Malformed input received."));
    }

    @Test
    void shouldHandleValidationExceptionWhenNullField() throws Exception {
        mockMvc.perform(post("/test/validated-request-payload")
                        .content("""
                                {}""")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_ExchangeRate.Sync"))))
                .andDo(print())
                .andExpect(status().isUnprocessableContent())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.reason").value("Unprocessable Content"))
                .andExpect(jsonPath("$.message").value("Validation exception occurred. [1], on field 'name', name cannot be null."));
    }

    @Test
    void shouldHandleValidationExceptionWhenInvalidDataTypeOnField() throws Exception {
        mockMvc.perform(post("/test/validated-request-payload")
                        .content("""
                                {"name": "Superman", "age": "thirty"}""")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_ExchangeRate.Sync"))))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.reason").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Cannot parse request body. Malformed input received."));
    }

    @Test
    void shouldHandleValidationExceptionWhenInvalidDataTypeOnPathVar() throws Exception {
        mockMvc.perform(get("/test/required-path-var/test")
                        .with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_ExchangeRate.Sync"))))
                .andDo(print())
                .andExpect(status().isNotAcceptable())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.reason").value("Not Acceptable"))
                .andExpect(jsonPath("$.message").value("A validation error occurred. Malformed value on field 'intVar'."));
    }

    @Test
    void shouldHandleValidationExceptionWhenUnknownPathVar() throws Exception {
        mockMvc.perform(get("/test/unknown-path-var/test")
                        .with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_ExchangeRate.Sync"))))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.reason").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("A validation error occurred. Missing required path parameter 'intVar'."));
    }

    @Test
    void shouldHandleValidationExceptionWhenMissingQueryParam() throws Exception {
        mockMvc.perform(get("/test/required-query-param")
                        .with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_ExchangeRate.Sync"))))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.reason").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("A validation error occurred. Missing required query parameter 'intVal'."));
    }

    @Test
    void shouldHandleValidationExceptionWhenInvalidDataTypeOnQueryParam() throws Exception {
        mockMvc.perform(get("/test/required-query-param")
                        .queryParam("intVal", "a-string-value")
                        .with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_ExchangeRate.Sync"))))
                .andDo(print())
                .andExpect(status().isNotAcceptable())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.reason").value("Not Acceptable"))
                .andExpect(jsonPath("$.message").value("A validation error occurred. Malformed value on field 'intVal'."));
    }

    @Test
    void shouldHandleValidationExceptionWhenMissingHeader() throws Exception {
        mockMvc.perform(get("/test/required-header")
                        .with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_ExchangeRate.Sync"))))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.reason").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("A validation error occurred. Missing required header 'intHeader'."));
    }

    @Test
    void shouldHandleValidationExceptionWhenInvalidDataTypeOnHeader() throws Exception {
        mockMvc.perform(get("/test/required-header")
                        .header("intHeader", "a-string-value")
                        .with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_ExchangeRate.Sync"))))
                .andDo(print())
                .andExpect(status().isNotAcceptable())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.reason").value("Not Acceptable"))
                .andExpect(jsonPath("$.message").value("A validation error occurred. Malformed value on field 'intHeader'."));
    }

    @Test
    void shouldHandleValidationExceptionWhenInvalidValueOnHeader() throws Exception {
        mockMvc.perform(get("/test/required-header")
                        .header("intHeader", "-1")
                        .with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_ExchangeRate.Sync"))))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.reason").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Validation exception occurred. [1], on property 'throwValidationExceptionWhenMissingRequestHeader.intHeader', must be greater than 0."));
    }

    @Test
    void shouldHandleValidationExceptionWhenMissingCookie() throws Exception {
        mockMvc.perform(get("/test/required-cookie")
                        .with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_ExchangeRate.Sync"))))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.reason").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("A validation error occurred. Missing required cookie 'intCookie'."));
    }

    @Test
    void shouldHandleValidationExceptionWhenInvalidDataTypeOnCookie() throws Exception {
        mockMvc.perform(get("/test/required-cookie")
                        .cookie(new Cookie("intCookie", "a-string-value"))
                        .with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_ExchangeRate.Sync"))))
                .andDo(print())
                .andExpect(status().isNotAcceptable())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.reason").value("Not Acceptable"))
                .andExpect(jsonPath("$.message").value("A validation error occurred. Malformed value on field 'intCookie'."));
    }

    @TestConfiguration
    public static class TestConfig {

        @Validated
        @RestController
        @RequestMapping("/test")
        public static class TestApi {

            @Autowired
            private Validator validator;

            @GetMapping("/throw-internal-exception")
            public String throwInternalException() {
                throw new InternalException("Test InternalException");
            }

            @GetMapping("/throw-validation-exception")
            public String throwValidationException() {
                throw new ValidationException("Test ValidationException");
            }

            @GetMapping("/throw-unannotated-exception")
            public String throwUnannotatedException() {
                throw new UnannotatedException("Test UnannotatedException");
            }

            @GetMapping("/throw-unmanaged-exception")
            public String throwUnmanagedException() {
                throw new RuntimeException("Test RuntimeException");
            }

            @GetMapping("/throw-access-denied-exception")
            public String throwUAccessDeniedException() {
                throw new AccessDeniedException("Test AccessDeniedException");
            }

            @PostMapping("/validated-request-payload")
            public String throwValidationExceptionWithValidatedRequestPayload(
                    @RequestBody @Valid @NotNull TestRequest req) {
                return "should fail during validation. payload= " + req;
            }

            @GetMapping("/required-path-var/{intVar}")
            public String throwValidationExceptionWhenMissingPathParam(
                    @PathVariable Integer intVar) {
                return "should fail during validation. path param= " + intVar;
            }

            @GetMapping("/unknown-path-var/{unknownVar}")
            public String throwValidationExceptionWhenUnknownPathParam(
                    @PathVariable Integer intVar) {
                return "should fail during validation. path param= " + intVar;
            }

            @GetMapping("/required-query-param")
            public String throwValidationExceptionWhenMissingQueryParam(
                    @RequestParam Integer intVal) {
                return "should fail during validation. query param= " + intVal;
            }

            @GetMapping("/required-header")
            public String throwValidationExceptionWhenMissingRequestHeader(
                    @RequestHeader @Positive Integer intHeader) {
                return "should fail during validation. header= " + intHeader;
            }

            @GetMapping("/required-cookie")
            public String throwValidationExceptionWhenMissingCookie(
                    @CookieValue Integer intCookie) {
                return "should fail during validation. cookie= " + intCookie;
            }
        }

        public static class TestRequest {

            @NotNull(message = "name cannot be null")
            private String name;

            @PositiveOrZero(message = "age cannot be negative")
            private Integer age = 0;

            public String getName() {
                return name;
            }

            public void setName(String name) {
                this.name = name;
            }

            public Integer getAge() {
                return age;
            }

            public void setAge(Integer age) {
                this.age = age;
            }
        }

        public static class UnannotatedException extends BaseException {
            public UnannotatedException(String message) {
                super(message);
            }
        }
    }

}