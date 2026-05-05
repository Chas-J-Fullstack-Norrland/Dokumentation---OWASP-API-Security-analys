package com.example.library;

import com.example.library.dto.v1.AuthorDtoV1;
import com.example.library.dto.v1.BookDtoV1;
import com.example.library.dto.v1.CreateAuthorRequestV1;
import com.example.library.dto.v1.CreateBookRequestV1;
import com.example.library.dto.v1.CreateLoanRequestV1;
import com.example.library.dto.v1.LoanDtoV1;
import com.example.library.entity.Author;
import com.example.library.entity.Book;
import com.example.library.entity.Loan;
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

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;



@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
// Covers the main v1 HTTP flows end-to-end against the Spring context and database.
class ApiIntegrationTests {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private AuthorRepository authorRepository;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private LoanRepository loanRepository;

    @BeforeEach
    void cleanDatabase() {
        // Clear child tables first so foreign-key constraints do not fail between tests.
        loanRepository.deleteAll();
        bookRepository.deleteAll();
        authorRepository.deleteAll();
    }


    @Test
    void postAuthors_returns201AndCreatedAuthor() {
        ResponseEntity<AuthorDtoV1> response = restTemplate.postForEntity(
                "/api/v1/authors",
                jsonEntity(new CreateAuthorRequestV1("Astrid Lindgren")),
                AuthorDtoV1.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getHeaders().getContentType()).isNotNull();
        assertThat(response.getHeaders().getContentType()).satisfies(contentType ->
                assertThat(contentType.isCompatibleWith(MediaType.APPLICATION_JSON)).isTrue());
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().id()).isNotNull();
        assertThat(response.getBody().name()).isEqualTo("Astrid Lindgren");
        assertThat(response.getBody().numberOfBooks()).isZero();
    }

    @Test
    void getAuthorById_returns200AndAuthor() {
        Author author = saveAuthor("Tove Jansson");

        ResponseEntity<AuthorDtoV1> response = restTemplate.getForEntity(
                "/api/v1/authors/{id}",
                AuthorDtoV1.class,
                author.getId()
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().id()).isEqualTo(author.getId());
        assertThat(response.getBody().name()).isEqualTo("Tove Jansson");
        assertThat(response.getBody().numberOfBooks()).isZero();
    }

    @Test
    void getBooksByAuthorId_returns200AndBooks() {
        Author author = saveAuthor("Selma Lagerlöf");
        saveBook(author, "Gösta Berlings saga", "9780000000001", 1891);

        ResponseEntity<List<BookDtoV1>> response = restTemplate.exchange(
                "/api/v1/authors/{id}/books",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<>() {},
                author.getId()
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).hasSize(1);
        BookDtoV1 book = response.getBody().getFirst();
        assertThat(book.id()).isNotNull();
        assertThat(book.title()).isEqualTo("Gösta Berlings saga");
        assertThat(book.authorName()).isEqualTo("Selma Lagerlöf");
        assertThat(book.isbn()).isEqualTo("9780000000001");
        assertThat(book.publicationYear()).isEqualTo(1891);
    }

    @Test
    void postLoans_returns201AndCreatedLoan() {
        LocalDate today = LocalDate.now();

        Author author = saveAuthor("Vilhelm Moberg");
        Book book = saveBook(author, "Utvandrarna", "9780000000002", 1949);

        ResponseEntity<LoanDtoV1> response = restTemplate.postForEntity(
                "/api/v1/loans",
                jsonEntity(new CreateLoanRequestV1(book.getId())),
                LoanDtoV1.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().id()).isNotNull();
        assertThat(response.getBody().bookId()).isEqualTo(book.getId());
        assertThat(response.getBody().bookTitle()).isEqualTo("Utvandrarna");
        assertThat(response.getBody().loanDate()).isEqualTo(today);
        assertThat(response.getBody().returnDate()).isNull();
    }

    @Test
    void getLoans_returns200AndActiveLoans() {
        LocalDate today = LocalDate.now();

        Author author = saveAuthor("Christopher Paolini");
        Book book = saveBook(author, "Eragon", "9780000000003", 2005);
        saveLoan(book, today);

        ResponseEntity<List<LoanDtoV1>> response = restTemplate.exchange(
                "/api/v1/loans",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<>() {}
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).hasSize(1);
        LoanDtoV1 loan = response.getBody().getFirst();
        assertThat(loan.id()).isNotNull();
        assertThat(loan.bookId()).isEqualTo(book.getId());
        assertThat(loan.bookTitle()).isEqualTo("Eragon");
        assertThat(loan.loanDate()).isEqualTo(today);
        assertThat(loan.returnDate()).isNull();
    }

    @Test
    void getAuthorById_whenAuthorDoesNotExist_returns404() {
        ResponseEntity<ApiErrorResponse> response = restTemplate.getForEntity(
                "/api/v1/authors/{id}",
                ApiErrorResponse.class,
                999L
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(404);
        assertThat(response.getBody().error()).isEqualTo("Not Found");
        assertThat(response.getBody().message()).isEqualTo("Author with id 999 not found");
        assertThat(response.getBody().path()).isEqualTo("/api/v1/authors/999");
    }

    @Test
    void postLoans_whenBookDoesNotExist_returns404() {
        ResponseEntity<ApiErrorResponse> response = restTemplate.postForEntity(
                "/api/v1/loans",
                jsonEntity(new CreateLoanRequestV1(999L)),
                ApiErrorResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo("Book with id 999 not found");
        assertThat(response.getBody().path()).isEqualTo("/api/v1/loans");
    }

    @Test
    void postLoans_whenBookAlreadyOnLoan_returns400() {
        Author author = saveAuthor("Vilhelm Moberg");
        Book book = saveBook(author, "Utvandrarna", "9780000000099", 1949);

        ResponseEntity<LoanDtoV1> firstResponse = restTemplate.postForEntity(
                "/api/v1/loans",
                jsonEntity(new CreateLoanRequestV1(book.getId())),
                LoanDtoV1.class
        );

        ResponseEntity<ApiErrorResponse> secondResponse = restTemplate.postForEntity(
                "/api/v1/loans",
                jsonEntity(new CreateLoanRequestV1(book.getId())),
                ApiErrorResponse.class
        );

        assertThat(firstResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(secondResponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(secondResponse.getBody()).isNotNull();
        assertThat(secondResponse.getBody().message()).isEqualTo("Book is already on loan");
        assertThat(secondResponse.getBody().path()).isEqualTo("/api/v1/loans");
    }




    // Edge cases validate the error contract, not just the happy path.

    @Test
    void postAuthors_whenNameIsBlank_returns400AndValidationError() {
        ResponseEntity<ApiErrorResponse> response = restTemplate.postForEntity(
                "/api/v1/authors",
                jsonEntity(new CreateAuthorRequestV1("")),
                ApiErrorResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().error()).isEqualTo("Bad Request");
        assertThat(response.getBody().message()).isEqualTo("name: Author name is required");
        assertThat(response.getBody().path()).isEqualTo("/api/v1/authors");
    }

    @Test
    void getAllBooks_returns200AndBooks() {
        Author author = saveAuthor("Astrid Lindgren");
        saveBook(author, "Mio min Mio", "9780000000101", 1954);

        ResponseEntity<List<BookDtoV1>> response = restTemplate.exchange(
                "/api/v1/books",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<>() {}
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).hasSize(1);
        BookDtoV1 book = response.getBody().getFirst();
        assertThat(book.id()).isNotNull();
        assertThat(book.title()).isEqualTo("Mio min Mio");
        assertThat(book.authorName()).isEqualTo("Astrid Lindgren");
        assertThat(book.isbn()).isEqualTo("9780000000101");
        assertThat(book.publicationYear()).isEqualTo(1954);
    }

    @Test
    void getBookById_returns200AndBook() {
        Author author = saveAuthor("Selma Lagerlöf");
        Book book = saveBook(author, "Jerusalem", "9780000000102", 1901);

        ResponseEntity<BookDtoV1> response = restTemplate.getForEntity(
                "/api/v1/books/{id}",
                BookDtoV1.class,
                book.getId()
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().id()).isEqualTo(book.getId());
        assertThat(response.getBody().title()).isEqualTo("Jerusalem");
        assertThat(response.getBody().authorName()).isEqualTo("Selma Lagerlöf");
        assertThat(response.getBody().isbn()).isEqualTo("9780000000102");
        assertThat(response.getBody().publicationYear()).isEqualTo(1901);
    }

    @Test
    void postBooks_returns201AndCreatedBook() {
        Author author = saveAuthor("Astrid Lindgren");

        ResponseEntity<BookDtoV1> response = restTemplate.postForEntity(
                "/api/v1/books",
                jsonEntity(new CreateBookRequestV1(
                        "Bröderna Lejonhjärta",
                        author.getId(),
                        "9780000000103",
                        1973
                )),
                BookDtoV1.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().id()).isNotNull();
        assertThat(response.getBody().title()).isEqualTo("Bröderna Lejonhjärta");
        assertThat(response.getBody().authorName()).isEqualTo("Astrid Lindgren");
        assertThat(response.getBody().isbn()).isEqualTo("9780000000103");
        assertThat(response.getBody().publicationYear()).isEqualTo(1973);
    }

    @Test
    void getBookById_whenBookDoesNotExist_returns404() {
        ResponseEntity<ApiErrorResponse> response = restTemplate.getForEntity(
                "/api/v1/books/{id}",
                ApiErrorResponse.class,
                999L
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo("Book with id 999 not found");
        assertThat(response.getBody().path()).isEqualTo("/api/v1/books/999");
    }

    private Author saveAuthor(String name) {
        return authorRepository.saveAndFlush(new Author(name));
    }


    private Book saveBook(Author author, String title, String isbn, int publicationYear) {
        // Tests use the shared entity model, so genre must be populated even for v1 scenarios.
        Book book = new Book();
        book.setTitle(title);
        book.setAuthor(author);
        book.setGenre("Unknown");
        book.setIsbn(isbn);
        book.setPublicationYear(publicationYear);
        return bookRepository.saveAndFlush(book);
    }

    private void saveLoan(Book book, LocalDate loanDate) {
        Loan loan = new Loan();
        loan.setBook(book);
        loan.setLoanDate(loanDate);
        loan.setReturnDate(null);
        loanRepository.saveAndFlush(loan);
    }

    private <T> HttpEntity<T> jsonEntity(T body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(body, headers);
    }


}
