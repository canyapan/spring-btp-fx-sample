package com.canyapan.sample.springbtpfxsample;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
class SpringBtpFxSampleApplicationTests {

	@Test
	void contextLoads() {
        assertTrue(true, "This test is for loading application context.");
	}

    @Test
    void main() throws Exception {
        SpringBtpFxSampleApplication.main(new String[]{"--spring.profiles.active=test", "--server.port=0"});
        assertTrue(true, "This test is for loading application context.");
    }
}
