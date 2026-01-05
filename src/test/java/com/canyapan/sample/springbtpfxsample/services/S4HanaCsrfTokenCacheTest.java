package com.canyapan.sample.springbtpfxsample.services;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class S4HanaCsrfTokenCacheTest {

    @Autowired
    private S4HanaCsrfTokenCache cache;

    @Test
    @Order(1)
    public void initiallyInvalid() {
        assertFalse(cache.isValid());
        assertNull(cache.getToken());
        assertNull(cache.getCookies());
    }

    @Test
    @Order(2)
    public void updateMakesValid() {
        cache.update("token-value", List.of("cookie1"));
        assertTrue(cache.isValid());
        assertEquals("token-value", cache.getToken());
        assertEquals(List.of("cookie1"), cache.getCookies());
    }

    @Test
    @Order(3)
    public void invalidateClearsCache() {
        cache.update("t", List.of("c"));
        cache.invalidate();
        assertFalse(cache.isValid());
        assertNull(cache.getToken());
        assertNull(cache.getCookies());
    }

    @Test
    @Order(4)
    public void expiresAfterMaxAge() throws InterruptedException {
        cache.update("t", List.of());
        Thread.sleep(1100);
        assertFalse(cache.isValid());
    }

}
