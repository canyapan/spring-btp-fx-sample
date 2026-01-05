package com.canyapan.sample.springbtpfxsample.services;

import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;
import org.mockserver.client.MockServerClient;
import org.mockserver.springtest.MockServerTest;
import org.mockserver.verify.VerificationTimes;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.mock.http.client.MockClientHttpResponse;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

@SpringBootTest
@ActiveProfiles("test")
@MockServerTest({"sap.s4.base-url=http://localhost:${mockServerPort}/odata/v2/"})
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class S4HanaCsrfTokenInterceptorIntegrationTest {

    private MockServerClient mockServer;

    @Autowired
    private S4HanaCsrfTokenInterceptor interceptor;

    @Autowired
    private S4HanaCsrfTokenCache cache;

    @AfterEach
    void cleanUp() {
        mockServer.reset();
    }

    @Test
    @Order(1)
    void shouldGetTokenWhenThereIsNoTokenCached() throws IOException {
        assertFalse(cache.isValid());

        String csrfToken = UUID.randomUUID().toString();

        mockServer.when(request()
                        .withMethod("GET")
                        .withPath("/odata/v2/API_EXCHANGE_RATE_SRV"))
                .respond(response()
                        .withStatusCode(200)
                        .withHeader("x-csrf-token", csrfToken)
                        .withCookie("_session", "session-cookie")
                );

        URI uri = URI.create("http://localhost:%d/odata/v2/".formatted(mockServer.getPort()));
        byte[] bytes = "".getBytes(StandardCharsets.UTF_8);

        ClientHttpRequestExecution execution = mock(ClientHttpRequestExecution.class);
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        ClientHttpRequest request = requestFactory.createRequest(uri, HttpMethod.POST);
        when(execution.execute(request, bytes))
                .thenReturn(new MockClientHttpResponse(new byte[0], HttpStatus.NO_CONTENT));

        interceptor.intercept(request, bytes, execution);

        mockServer.verify(request()
                .withMethod("GET")
                .withPath("/odata/v2/API_EXCHANGE_RATE_SRV"), VerificationTimes.once());

        ArgumentCaptor<HttpRequest> captor = ArgumentCaptor.forClass(HttpRequest.class);
        verify(execution, times(1)).execute(captor.capture(), eq(bytes));

        HttpRequest capturedRequest = captor.getValue();
        HttpHeaders capturedRequestHeaders = capturedRequest.getHeaders();
        assertNotNull(capturedRequestHeaders);

        // Inject CSRF token and session
        assertEquals(csrfToken, capturedRequestHeaders.getFirst("x-csrf-token"));
        assertEquals("_session=session-cookie", capturedRequestHeaders.getFirst("Cookie"));

        // Update cache
        assertTrue(cache.isValid());
        assertEquals(csrfToken, cache.getToken());
        assertEquals("_session=session-cookie", cache.getCookies().getFirst());
    }

    @Test
    @Order(2)
    void shouldGetTokenWhenThereIsTokenCached() throws IOException {
        assertTrue(cache.isValid());

        URI uri = URI.create("http://localhost:%d/odata/v2/".formatted(mockServer.getPort()));
        byte[] bytes = "".getBytes(StandardCharsets.UTF_8);

        ClientHttpRequestExecution execution = mock(ClientHttpRequestExecution.class);
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        ClientHttpRequest request = requestFactory.createRequest(uri, HttpMethod.POST);
        when(execution.execute(request, bytes))
                .thenReturn(new MockClientHttpResponse(new byte[0], HttpStatus.NO_CONTENT));

        interceptor.intercept(request, bytes, execution);

        mockServer.verify(request()
                .withMethod("GET")
                .withPath("/odata/v2/API_EXCHANGE_RATE_SRV"), VerificationTimes.never());

        ArgumentCaptor<HttpRequest> captor = ArgumentCaptor.forClass(HttpRequest.class);
        verify(execution, times(1)).execute(captor.capture(), eq(bytes));

        HttpRequest capturedRequest = captor.getValue();
        HttpHeaders capturedRequestHeaders = capturedRequest.getHeaders();
        assertNotNull(capturedRequestHeaders);

        // Inject CSRF token and session
        assertEquals(cache.getToken(), capturedRequestHeaders.getFirst("x-csrf-token"));
        assertEquals("_session=session-cookie", capturedRequestHeaders.getFirst("Cookie"));

        // Cache still valid after
        assertTrue(cache.isValid());
    }

    @Test
    @Order(3)
    void shouldGetTokenWhenThereIsExpiredTokenCached() throws IOException {
        ReflectionTestUtils.setField(cache, "fetchedAt", Instant.now().minus(30, ChronoUnit.MINUTES));
        assertFalse(cache.isValid());

        String csrfToken = UUID.randomUUID().toString();

        mockServer.when(request()
                        .withMethod("GET")
                        .withPath("/odata/v2/API_EXCHANGE_RATE_SRV"))
                .respond(response()
                        .withStatusCode(200)
                        .withHeader("x-csrf-token", csrfToken)
                        .withCookie("_session", "session-cookie")
                );

        URI uri = URI.create("http://localhost:%d/odata/v2/".formatted(mockServer.getPort()));
        byte[] bytes = "".getBytes(StandardCharsets.UTF_8);

        ClientHttpRequestExecution execution = mock(ClientHttpRequestExecution.class);
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        ClientHttpRequest request = requestFactory.createRequest(uri, HttpMethod.POST);
        when(execution.execute(request, bytes))
                .thenReturn(new MockClientHttpResponse(new byte[0], HttpStatus.NO_CONTENT));

        interceptor.intercept(request, bytes, execution);

        mockServer.verify(request()
                .withMethod("GET")
                .withPath("/odata/v2/API_EXCHANGE_RATE_SRV"), VerificationTimes.once());

        ArgumentCaptor<HttpRequest> captor = ArgumentCaptor.forClass(HttpRequest.class);
        verify(execution, times(1)).execute(captor.capture(), eq(bytes));

        HttpRequest capturedRequest = captor.getValue();
        HttpHeaders capturedRequestHeaders = capturedRequest.getHeaders();
        assertNotNull(capturedRequestHeaders);

        // Inject CSRF token and session
        assertEquals(csrfToken, capturedRequestHeaders.getFirst("x-csrf-token"));
        assertEquals("_session=session-cookie", capturedRequestHeaders.getFirst("Cookie"));

        // Update cache
        assertTrue(cache.isValid());
        assertEquals(csrfToken, cache.getToken());
        assertEquals("_session=session-cookie", cache.getCookies().getFirst());
    }

    @Test
    @Order(4)
    void shouldGetTokenWhenServerRejectsTokenCached() throws IOException {
        assertTrue(cache.isValid());

        URI uri = URI.create("http://localhost:%d/odata/v2/".formatted(mockServer.getPort()));
        byte[] bytes = "".getBytes(StandardCharsets.UTF_8);

        ClientHttpRequestExecution execution = mock(ClientHttpRequestExecution.class);
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        ClientHttpRequest request = requestFactory.createRequest(uri, HttpMethod.POST);
        when(execution.execute(request, bytes))
                .thenReturn(new MockClientHttpResponse(new byte[0], HttpStatus.FORBIDDEN));

        interceptor.intercept(request, bytes, execution);

        mockServer.verify(request()
                .withMethod("GET")
                .withPath("/odata/v2/API_EXCHANGE_RATE_SRV"), VerificationTimes.never());

        ArgumentCaptor<HttpRequest> captor = ArgumentCaptor.forClass(HttpRequest.class);
        verify(execution, times(1)).execute(captor.capture(), eq(bytes));

        HttpRequest capturedRequest = captor.getValue();
        HttpHeaders capturedRequestHeaders = capturedRequest.getHeaders();
        assertNotNull(capturedRequestHeaders);

        // Inject CSRF token and session
        assertNotNull(capturedRequestHeaders.getFirst("x-csrf-token"));
        assertEquals("_session=session-cookie", capturedRequestHeaders.getFirst("Cookie"));

        // Invalidates cache
        assertFalse(cache.isValid());
        assertNull(cache.getToken());
        assertNull(cache.getCookies());
    }

}
