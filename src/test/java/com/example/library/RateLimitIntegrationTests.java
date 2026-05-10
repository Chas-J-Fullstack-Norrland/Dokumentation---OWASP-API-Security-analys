package com.example.library;

import com.example.library.dto.auth.LoginRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
// Verifies the Bucket4j filter returns 429 after the per-IP login request limit is exceeded.
class RateLimitIntegrationTests {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void loginRateLimit_exceeded_returns429() {
        String uniqueIp = "203.0.113." + (int) (Math.random() * 200 + 1);
        List<Integer> statuses = new ArrayList<>();

        for (int i = 0; i < 12; i++) {
            ResponseEntity<String> response = restTemplate.exchange(
                    "/api/auth/login",
                    HttpMethod.POST,
                    loginEntity(uniqueIp),
                    String.class
            );
            statuses.add(response.getStatusCode().value());
        }

        assertThat(statuses.subList(0, 10)).doesNotContain(HttpStatus.TOO_MANY_REQUESTS.value());
        assertThat(statuses.subList(10, 12)).allMatch(code -> code == HttpStatus.TOO_MANY_REQUESTS.value());
    }

    private HttpEntity<LoginRequest> loginEntity(String ip) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Forwarded-For", ip);
        // Invalid credentials keep auth deterministic; the test only cares that pre-limit responses are not 429.
        return new HttpEntity<>(new LoginRequest("invalid-user", "invalid-pass"), headers);
    }
}
