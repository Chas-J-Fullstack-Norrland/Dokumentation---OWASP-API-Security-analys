package com.example.library.controller.v1;

import com.example.library.dto.v1.BookDtoV1;
import com.example.library.dto.v1.CreateBookRequestV1;
import com.example.library.entity.Book;
import com.example.library.exception.ApiErrorResponse;
import com.example.library.service.BookService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/books")
@Tag(name = "Books V1", description = "Endpoints for managing books in API version 1")
@Validated
// v1 exposes the original book representation without the later genre and availability additions.
public class BookControllerV1 {

    private final BookService bookService;

    public BookControllerV1(BookService bookService) {
        this.bookService = bookService;
    }

    @GetMapping
    @Operation(
            summary = "Get all books",
            description = "Returns all books in the v1 format"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "All books returned")
    })
    public List<BookDtoV1> getAllBooks(
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "page must be >= 0") int page,
            @RequestParam(defaultValue = "20") @Min(value = 1, message = "size must be >= 1") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        return bookService.getAllBooks(pageable)
                .map(this::toBookDto)
                .getContent();
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Get book by id",
            description = "Returns one book by its id in the v1 format "
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "book found"),
            @ApiResponse(
                    responseCode = "404",
                    description = "Book not found",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))
            )
    })
    public BookDtoV1 getBookById(@PathVariable @Min(value = 1, message = "id must be >= 1") Long id) {
        return toBookDto(bookService.getBookById(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Create a book",
            description = "Creates a book in the v1 format"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Book created successfully"),
            @ApiResponse(
                    responseCode = "400",
                    description = "Validation failed or request is invalid",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Author not found",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))
            )
    })
    public BookDtoV1 createBook(@Valid @RequestBody CreateBookRequestV1 request) {
        // v1 delegates to the compatibility overload in the service, which fills in the default genre.
        Book savedBook = bookService.createBook(
                request.title(),
                request.authorId(),
                request.isbn(),
                request.publicationYear()
        );

        return toBookDto(savedBook);
    }

    private BookDtoV1 toBookDto(Book book) {
        // The v1 DTO stays intentionally small to preserve the original API contract.
        return new BookDtoV1(
                book.getId(),
                book.getTitle(),
                book.getAuthor().getName(),
                book.getIsbn(),
                book.getPublicationYear()
        );

    }
}