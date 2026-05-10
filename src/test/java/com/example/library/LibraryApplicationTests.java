package com.example.library;

import com.example.library.dto.auth.LoginRequest;
import com.example.library.dto.auth.TokenPairResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
// Smoke test that verifies the Spring application context can start successfully and serve real HTTP traffic.
class LibraryApplicationTests {

	@Autowired
	private TestRestTemplate restTemplate;

	@Test
	void contextLoads() {
		ResponseEntity<TokenPairResponse> loginResponse = restTemplate.postForEntity(
				"/api/auth/login",
				new HttpEntity<>(new LoginRequest("libraryuser", "librarypass")),
				TokenPairResponse.class
		);

		assertThat(loginResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(loginResponse.getBody()).isNotNull();

		HttpHeaders headers = new HttpHeaders();
		headers.setBearerAuth(loginResponse.getBody().accessToken());

		ResponseEntity<String> response = restTemplate.exchange(
				"/api/v1/books",
				HttpMethod.GET,
				new HttpEntity<>(headers),
				String.class
		);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).isNotNull();
	}

}
