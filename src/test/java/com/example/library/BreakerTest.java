package com.example.library;

import com.example.library.exception.ExternalBookNotFoundException;
import com.example.library.exception.ExternalServiceUnavailableException;
import com.example.library.service.ExternalBookService;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class ExternalBookCircuitBreakerTests {

    private static MockWebServer mockWebServer;

    @Autowired
    private ExternalBookService externalBookService;

    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) throws Exception {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
        registry.add("app.external.open-library.base-url", () -> mockWebServer.url("/").toString());
    }

    @BeforeEach
    void resetBreaker() {
        circuitBreakerRegistry.circuitBreaker("openLibraryApi").reset();
    }

    @AfterEach
    void shutdown() throws Exception {
        mockWebServer.shutdown();
    }

    @Test
    void repeatedTechnicalFailures_openCircuitBreaker() {
        for (int i = 0; i < 10; i++) {
            mockWebServer.enqueue(new MockResponse().setResponseCode(500));
        }

        for (int i = 0; i < 5; i++) {
            assertThatThrownBy(() -> externalBookService.getBookByIsbn("9780140328721"))
                    .isInstanceOf(ExternalServiceUnavailableException.class);
        }

        CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker("openLibraryApi");
        assertThat(cb.getState()).isEqualTo(CircuitBreaker.State.OPEN);
    }

    @Test
    void emptyResult_doesNotOpenCircuitBreaker() {
        for (int i = 0; i < 5; i++) {
            mockWebServer.enqueue(new MockResponse()
                    .setResponseCode(200)
                    .setBody("{}")
                    .addHeader("Content-Type", "application/json"));
        }

        for (int i = 0; i < 5; i++) {
            assertThatThrownBy(() -> externalBookService.getBookByIsbn("0000000000000"))
                    .isInstanceOf(ExternalBookNotFoundException.class);
        }

        CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker("openLibraryApi");
        assertThat(cb.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
    }
}

