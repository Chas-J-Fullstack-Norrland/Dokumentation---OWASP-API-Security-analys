package com.example.library;


import com.example.library.dto.v1.CreateLoanRequestV1;
import com.example.library.entity.Author;
import com.example.library.entity.Book;
import com.example.library.entity.Loan;
import com.example.library.exception.ApiErrorResponse;
import com.example.library.repository.AuthorRepository;
import com.example.library.repository.BookRepository;
import com.example.library.repository.LoanRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;


import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
// Exercises concurrent loan requests to prove the single-active-loan rule holds under race conditions.
class LoanConcurrencyTests {

    @Autowired
    private TestRestTemplate restTemplate;

    private final ObjectMapper objectMapper = JsonMapper.builder()
            .findAndAddModules()
            .build();

    @LocalServerPort
    private int port;

    @Autowired
    private AuthorRepository authorRepository;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private LoanRepository loanRepository;

    @BeforeEach
    void cleanDatabase() {
        // Delete in dependency order so each concurrency test starts from a clean schema state.
        loanRepository.deleteAll();
        bookRepository.deleteAll();
        authorRepository.deleteAll();
    }

    @Test
    void createLoan_raceCondition_returnsOne201AndOne400() throws Exception {
        Author author = authorRepository.saveAndFlush(new Author("Concurrency Author"));
        Book book = saveBook(author, "Race Condition Book", "9781234567890");

        ExecutorService executorService = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        Callable<ResponseEntity<String>> task = () -> {
            ready.countDown();
            start.await();

            // Both requests are released together to maximize the chance of colliding on the same book.
            return restTemplate.exchange(
                    loansUrl(),
                    HttpMethod.POST,
                    jsonEntity(new CreateLoanRequestV1(book.getId())),
                    String.class
            );
        };

        Future<ResponseEntity<String>> future1 = executorService.submit(task);
        Future<ResponseEntity<String>> future2 = executorService.submit(task);

        ready.await();
        start.countDown();

        ResponseEntity<String> result1 = future1.get();
        ResponseEntity<String> result2 = future2.get();

        executorService.shutdown();
        boolean terminated = executorService.awaitTermination(5, TimeUnit.SECONDS);

        assertThat(terminated).isTrue();

        List<Loan> loans = loanRepository.findAll();
        List<Integer> results = List.of(result1.getStatusCode().value(), result2.getStatusCode().value());

        assertThat(loans).hasSize(1);
        assertThat(results).contains(201);
        assertThat(results).contains(400);
        assertThat(results).doesNotContain(500);

        ResponseEntity<String> failedResponse = result1.getStatusCode().is4xxClientError() ? result1 : result2;
        ApiErrorResponse errorResponse = objectMapper.readValue(failedResponse.getBody(), ApiErrorResponse.class);
        assertThat(errorResponse.message()).isEqualTo("Book is already on loan");
        assertThat(errorResponse.path()).isEqualTo("/api/v1/loans");
    }

    @Test
    void createLoan_manyParallelRequests_returnsOne201AndTheRest400() throws Exception {
        Author author = authorRepository.saveAndFlush(new Author("Concurrency Author"));
        Book book = saveBook(author, "Concurrency Book", "9781234567899");

        int numberOfRequests = 10;
        ExecutorService executorService = Executors.newFixedThreadPool(numberOfRequests);
        CountDownLatch ready = new CountDownLatch(numberOfRequests);
        CountDownLatch start = new CountDownLatch(1);

        Callable<ResponseEntity<String>> task = () -> {
            ready.countDown();
            start.await();

            return restTemplate.exchange(
                    loansUrl(),
                    HttpMethod.POST,
                    jsonEntity(new CreateLoanRequestV1(book.getId())),
                    String.class
            );
        };

        List<Future<ResponseEntity<String>>> futures = new ArrayList<>();
        for (int i = 0; i < numberOfRequests; i++) {
            futures.add(executorService.submit(task));
        }

        ready.await();
        start.countDown();

        List<ResponseEntity<String>> responses = new ArrayList<>();
        for (Future<ResponseEntity<String>> future : futures) {
            responses.add(future.get());
        }

        executorService.shutdown();
        boolean terminated = executorService.awaitTermination(5, TimeUnit.SECONDS);

        assertThat(terminated).isTrue();

        List<Loan> loans = loanRepository.findAll();
        List<Integer> results = responses.stream()
                .map(response -> response.getStatusCode().value())
                .toList();

        assertThat(loans).hasSize(1);
        assertThat(results).filteredOn(status -> status == 201).hasSize(1);
        assertThat(results).filteredOn(status -> status == 400).hasSize(numberOfRequests - 1);
        assertThat(results).doesNotContain(500);

        for (ResponseEntity<String> response : responses) {
            if (response.getStatusCode().value() == 400) {
                ApiErrorResponse errorResponse = objectMapper.readValue(response.getBody(), ApiErrorResponse.class);
                assertThat(errorResponse.message()).isEqualTo("Book is already on loan");
                assertThat(errorResponse.path()).isEqualTo("/api/v1/loans");
            }
        }
    }

    private Book saveBook(Author author, String title, String isbn) {
        // Genre is required by the shared entity even though these tests target the loan flow.
        Book book = new Book();
        book.setTitle(title);
        book.setAuthor(author);
        book.setGenre("Unknown");
        book.setIsbn(isbn);
        book.setPublicationYear(2025);
        return bookRepository.saveAndFlush(book);
    }

    private String loansUrl() {
        return "http://localhost:" + port + "/api/v1/loans";
    }

    private <T> HttpEntity<T> jsonEntity(T body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(body, headers);
    }
}
