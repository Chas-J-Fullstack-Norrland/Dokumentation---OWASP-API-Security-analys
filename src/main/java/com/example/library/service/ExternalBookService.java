package com.example.library.service;

import com.example.library.dto.external.ExternalBookDto;
import com.example.library.dto.external.OpenLibraryBookResponse;
import com.example.library.exception.ExternalBookNotFoundException;
import com.example.library.exception.ExternalServiceUnavailableException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Map;

@Service
public class ExternalBookService {

    private final RestClient restClient;

    public ExternalBookService(
            RestClient.Builder builder,
            @Value("${app.external.open-library.base-url}") String baseUrl
    ) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .build();

        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofSeconds(5));

        this.restClient = builder
                .requestFactory(requestFactory)
                .baseUrl(baseUrl)
                .build();
    }

    @Retry(name = "openLibraryApi", fallbackMethod = "getBookByIsbnFallback")
    @CircuitBreaker(name = "openLibraryApi", fallbackMethod = "getBookByIsbnFallback")
    public ExternalBookDto getBookByIsbn(String isbn) {
        Map<String, OpenLibraryBookResponse> result = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/books")
                        .queryParam("bibkeys", "ISBN:" + isbn)
                        .queryParam("format", "json")
                        .queryParam("jscmd", "data")
                        .build())
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});

        if (result == null || result.isEmpty()) {
            throw new ExternalBookNotFoundException(isbn);
        }

        OpenLibraryBookResponse book = result.get("ISBN:" + isbn);

        if (book == null) {
            throw new ExternalBookNotFoundException(isbn);
        }

        String authorName = "Unknown";
        if (book.authors() != null && !book.authors().isEmpty() && book.authors().getFirst() != null) {
            authorName = book.authors().getFirst().name();
        }

        return new ExternalBookDto(
                isbn,
                book.title(),
                authorName,
                book.publish_date()
        );
    }
    public ExternalBookDto getBookByIsbnFallback(String isbn, Throwable throwable) {
        if (throwable instanceof ExternalBookNotFoundException externalBookNotFoundException) {
            throw externalBookNotFoundException;
        }

        throw new ExternalServiceUnavailableException(
                "Open Library is currently unavailable for ISBN: " + isbn,
                throwable
        );
    }
}