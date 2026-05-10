package com.example.library;

import com.example.library.dto.auth.LoginRequest;
import com.example.library.dto.auth.TokenPairResponse;
import com.example.library.dto.v2.AuthorDtoV2;
import com.example.library.dto.v2.BookDtoV2;
import com.example.library.dto.v2.BookResponseV2;
import com.example.library.dto.v2.CreateBookRequestV2;
import com.example.library.dto.v2.CreateAuthorRequestV2;
import com.example.library.dto.v2.CreateLoanRequestV2;
import com.example.library.dto.v2.LoanDtoV2;
import com.example.library.dto.v2.PatchBookRequestV2;
import com.example.library.entity.Author;
import com.example.library.entity.Book;
import com.example.library.exception.ApiErrorResponse;
import com.example.library.repository.AuthorRepository;
import com.example.library.repository.BookRepository;
import com.example.library.repository.LoanRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
// Verifies the richer v2 book contract, including genre and patch support.
class ApiIntegrationV2Tests {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private AuthorRepository authorRepository;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private LoanRepository loanRepository;

    private String accessToken;

    @BeforeEach
    void cleanDatabase() {
        // Reset state between tests so each scenario controls its own data completely.
        loanRepository.deleteAll();
        bookRepository.deleteAll();
        authorRepository.deleteAll();
        accessToken = loginAndGetAccessToken();
    }

    @Test
    void postBooksV2_returns201AndCreatedBookWithGenreAndAvailability() {
        Author author = saveAuthor("Ursula K. Le Guin");

        ResponseEntity<BookDtoV2> response = restTemplate.exchange(
                "/api/v2/books",
                HttpMethod.POST,
                authJsonEntity(new CreateBookRequestV2(
                        "A Wizard of Earthsea",
                        author.getId(),
                        "Fantasy",
                        "9780000000201",
                        1968
                )),
                BookDtoV2.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().id()).isNotNull();
        assertThat(response.getBody().title()).isEqualTo("A Wizard of Earthsea");
        assertThat(response.getBody().authorName()).isEqualTo("Ursula K. Le Guin");
        assertThat(response.getBody().genre()).isEqualTo("Fantasy");
        assertThat(response.getBody().isbn()).isEqualTo("9780000000201");
        assertThat(response.getBody().publicationYear()).isEqualTo(1968);
        assertThat(response.getBody().available()).isTrue();
    }

    @Test
    void authorV2_endpointsReturnCreatedAuthorAndBooks() {
        ResponseEntity<AuthorDtoV2> createResponse = restTemplate.exchange(
                "/api/v2/authors",
                HttpMethod.POST,
                authJsonEntity(new CreateAuthorRequestV2("Maria Gripe")),
                AuthorDtoV2.class
        );

        assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(createResponse.getBody()).isNotNull();
        assertThat(createResponse.getBody().id()).isNotNull();
        assertThat(createResponse.getBody().name()).isEqualTo("Maria Gripe");
        assertThat(createResponse.getBody().numberOfBooks()).isZero();

        Long authorId = createResponse.getBody().id();
        Author savedAuthor = authorRepository.findById(authorId).orElseThrow();
        saveBook(savedAuthor, "Fantasy", "9780000000209");

        ResponseEntity<AuthorDtoV2> getAuthorResponse = restTemplate.exchange(
                "/api/v2/authors/{id}",
                HttpMethod.GET,
                authEntity(),
                AuthorDtoV2.class,
                authorId
        );

        assertThat(getAuthorResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(getAuthorResponse.getBody()).isNotNull();
        assertThat(getAuthorResponse.getBody().id()).isEqualTo(authorId);
        assertThat(getAuthorResponse.getBody().name()).isEqualTo("Maria Gripe");
        assertThat(getAuthorResponse.getBody().numberOfBooks()).isEqualTo(1);

        ResponseEntity<java.util.List<BookDtoV2>> getBooksResponse = restTemplate.exchange(
                "/api/v2/authors/{id}/books",
                HttpMethod.GET,
                authEntity(),
                new ParameterizedTypeReference<>() {},
                authorId
        );

        assertThat(getBooksResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(getBooksResponse.getBody()).isNotNull();
        assertThat(getBooksResponse.getBody()).hasSize(1);

        BookDtoV2 book = getBooksResponse.getBody().getFirst();
        assertThat(book.id()).isNotNull();
        assertThat(book.title()).isEqualTo("Kometen kommer");
        assertThat(book.authorName()).isEqualTo("Maria Gripe");
        assertThat(book.genre()).isEqualTo("Fantasy");
        assertThat(book.isbn()).isEqualTo("9780000000209");
        assertThat(book.publicationYear()).isEqualTo(1946);
        assertThat(book.available()).isTrue();
    }

    @Test
    void getAllBooksV2_withPagination_returnsLimitedResult() {
        Author author = saveAuthor("Tove Jansson");
        saveBook(author, "Fantasy", "9780000000202");
        saveBook(author, "Fantasy", "9780000000203");

        ResponseEntity<BookResponseV2> response = restTemplate.exchange(
                "/api/v2/books?page=0&size=1",
                HttpMethod.GET,
                authEntity(),
                BookResponseV2.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().data()).hasSize(1);
    }

    @Test
    void getBooksByAuthorIdV2_withPagination_returnsLimitedResult() {
        Author author = saveAuthor("Ursula K. Le Guin");
        saveBook(author, "Fantasy", "9780000000210");
        saveBook(author, "Fantasy", "9780000000211");

        ResponseEntity<java.util.List<BookDtoV2>> response = restTemplate.exchange(
                "/api/v2/authors/{id}/books?page=0&size=1",
                HttpMethod.GET,
                authEntity(),
                new ParameterizedTypeReference<>() {},
                author.getId()
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).hasSize(1);
    }

    @Test
    void getLoansV2_withPagination_returnsLimitedResult() {
        Author author = saveAuthor("Maria Gripe");
        Book firstBook = saveBook(author, "Fantasy", "9780000000212");
        Book secondBook = saveBook(author, "Fantasy", "9780000000213");

        ResponseEntity<LoanDtoV2> firstLoan = restTemplate.exchange(
                "/api/v2/loans",
                HttpMethod.POST,
                authJsonEntity(new CreateLoanRequestV2(firstBook.getId())),
                LoanDtoV2.class
        );
        ResponseEntity<LoanDtoV2> secondLoan = restTemplate.exchange(
                "/api/v2/loans",
                HttpMethod.POST,
                authJsonEntity(new CreateLoanRequestV2(secondBook.getId())),
                LoanDtoV2.class
        );

        assertThat(firstLoan.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(secondLoan.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        ResponseEntity<java.util.List<LoanDtoV2>> response = restTemplate.exchange(
                "/api/v2/loans?page=0&size=1",
                HttpMethod.GET,
                authEntity(),
                new ParameterizedTypeReference<>() {}
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).hasSize(1);
    }

    @Test
    void getBookByIdV2_returns200AndBookWithGenreAndAvailability() {
        Author author = saveAuthor("Tove Jansson");
        Book book = saveBook(author, "Fantasy", "9780000000202");

        ResponseEntity<BookDtoV2> response = restTemplate.exchange(
                "/api/v2/books/{id}",
                HttpMethod.GET,
                authEntity(),
                BookDtoV2.class,
                book.getId()
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().id()).isEqualTo(book.getId());
        assertThat(response.getBody().title()).isEqualTo("Kometen kommer");
        assertThat(response.getBody().authorName()).isEqualTo("Tove Jansson");
        assertThat(response.getBody().genre()).isEqualTo("Fantasy");
        assertThat(response.getBody().isbn()).isEqualTo("9780000000202");
        assertThat(response.getBody().publicationYear()).isEqualTo(1946);
        assertThat(response.getBody().available()).isTrue();
    }

    @Test
    void patchBookV2_updatesGenreAndReturns200() {
        Author author = saveAuthor("Tove Jansson");
        Book book = saveBook(author, "Unknown", "9780000000203");

        ResponseEntity<BookDtoV2> patchResponse = restTemplate.exchange(
                "/api/v2/books/{id}",
                HttpMethod.PATCH,
                authJsonEntity(new PatchBookRequestV2(null, null, "Fantasy", null, null)),
                BookDtoV2.class,
                book.getId()
        );

        assertThat(patchResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(patchResponse.getBody()).isNotNull();
        assertThat(patchResponse.getBody().id()).isEqualTo(book.getId());
        assertThat(patchResponse.getBody().title()).isEqualTo("Kometen kommer");
        assertThat(patchResponse.getBody().authorName()).isEqualTo("Tove Jansson");
        assertThat(patchResponse.getBody().genre()).isEqualTo("Fantasy");
        assertThat(patchResponse.getBody().isbn()).isEqualTo("9780000000203");
        assertThat(patchResponse.getBody().publicationYear()).isEqualTo(1946);
        assertThat(patchResponse.getBody().available()).isTrue();

        ResponseEntity<BookDtoV2> getResponse = restTemplate.exchange(
                "/api/v2/books/{id}",
                HttpMethod.GET,
                authEntity(),
                BookDtoV2.class,
                book.getId()
        );

        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(getResponse.getBody()).isNotNull();
        assertThat(getResponse.getBody().id()).isEqualTo(book.getId());
        assertThat(getResponse.getBody().genre()).isEqualTo("Fantasy");
    }

    @Test
    void postBookV2_whenAuthorIdIsZero_returns400() {
        ResponseEntity<ApiErrorResponse> response = restTemplate.exchange(
                "/api/v2/books",
                HttpMethod.POST,
                authJsonEntity(new CreateBookRequestV2("Test", 0L, "Fantasy", "9780000000299", 2000)),
                ApiErrorResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().error()).isEqualTo("Bad Request");
    }

    @Test
    void getBookByIdV2_whenIdIsZero_returns400() {
        ResponseEntity<ApiErrorResponse> response = restTemplate.exchange(
                "/api/v2/books/{id}",
                HttpMethod.GET,
                authEntity(),
                ApiErrorResponse.class,
                0L
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().error()).isEqualTo("Bad Request");
    }

    private Author saveAuthor(String name) {
        return authorRepository.saveAndFlush(new Author(name));
    }

    private Book saveBook(Author author, String genre, String isbn) {
        // The helper creates a valid shared Book entity that can be reused across the v2 scenarios.
        Book book = new Book();
        book.setTitle("Kometen kommer");
        book.setAuthor(author);
        book.setGenre(genre);
        book.setIsbn(isbn);
        book.setPublicationYear(1946);
        return bookRepository.saveAndFlush(book);
    }

    private <T> HttpEntity<T> jsonEntity(T body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(body, headers);
    }

    private HttpEntity<Void> authEntity() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        return new HttpEntity<>(headers);
    }

    private <T> HttpEntity<T> authJsonEntity(T body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(body, headers);
    }

    private String loginAndGetAccessToken() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Forwarded-For", "test-client-" + System.nanoTime());

        ResponseEntity<TokenPairResponse> response = restTemplate.exchange(
                "/api/auth/login",
                HttpMethod.POST,
                new HttpEntity<>(new LoginRequest("libraryuser", "librarypass"), headers),
                TokenPairResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().accessToken()).isNotBlank();
        return response.getBody().accessToken();
    }
}
