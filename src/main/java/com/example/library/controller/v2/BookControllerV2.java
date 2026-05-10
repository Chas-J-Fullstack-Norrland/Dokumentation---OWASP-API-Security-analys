package com.example.library.controller.v2;


import com.example.library.dto.v2.BookDtoV2;
import com.example.library.dto.v2.BookResponseV2;
import com.example.library.dto.v2.CreateBookRequestV2;
import com.example.library.dto.v2.PatchBookRequestV2;
import com.example.library.entity.Book;
import com.example.library.service.BookService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import com.example.library.exception.ApiErrorResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v2/books")
// v2 keeps its own endpoint namespace so the API can evolve without breaking v1 clients.
@Tag(name = "Books V2", description = "Endpoints for managing books in API version 2")
public class BookControllerV2 {

    private final BookService bookService;


    public BookControllerV2(BookService bookService) {
        this.bookService = bookService;
    }

    @GetMapping
    @Operation(summary = "Get all books", description = "Returns all books using the v2 response format")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Books returned successfully"),
            @ApiResponse(
                    responseCode = "500",
                    description = "Unexpected server error",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))
            )
    })
    public BookResponseV2 getAllBooks(Pageable pageable) {
        // The v2 list response wraps the books together with explicit version metadata.
        List<BookDtoV2> books = bookService.getAllBooks(pageable)
                .map(this::toBookDto)
                .getContent();

        return new BookResponseV2(books, "v2");
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get book by id", description = "Returns one book by its id in the v2 format")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Book found"),
            @ApiResponse(
                    responseCode = "404",
                    description = "Book not found",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Unexpected server error",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))
            )
    })
    public BookDtoV2 getBookById(@PathVariable Long id) {
        return toBookDto(bookService.getBookById(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create book", description = "Creates a new book using the v2 contract including genre")
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
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Unexpected server error",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))
            )
    })
    public BookDtoV2 createBook(@Valid @RequestBody CreateBookRequestV2 request) {
        // The controller only handles HTTP input/output; validation rules and persistence stay in the service layer.
        Book savedBook = bookService.createBook(
                request.title(),
                request.authorId(),
                request.genre(),
                request.isbn(),
                request.publicationYear()
        );

        return toBookDto(savedBook);
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Patch book", description = "Partially updates an existing book using the v2 contract")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Book updated successfully"),
            @ApiResponse(
                    responseCode = "400",
                    description = "Validation failed or request is invalid",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Book or author not found",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Unexpected server error",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))
            )
    })
    public BookDtoV2 patchBook(@PathVariable Long id,
                               @Valid @RequestBody PatchBookRequestV2 request) {
        // PATCH lets v2 enrich existing books, for example by adding genre to older records already stored in the database.
        Book updatedBook = bookService.patchBook(
                id,
                request.title(),
                request.authorId(),
                request.genre(),
                request.isbn(),
                request.publicationYear()
        );

        return toBookDto(updatedBook);
    }


    private BookDtoV2 toBookDto(Book book) {
        // v2 exposes more context than the entity itself, including the author's name and computed availability.
        return new BookDtoV2(
                book.getId(),
                book.getTitle(),
                book.getAuthor().getName(),
                book.getGenre(),
                book.getIsbn(),
                book.getPublicationYear(),
                bookService.isAvailable(book.getId())
        );

    }
}