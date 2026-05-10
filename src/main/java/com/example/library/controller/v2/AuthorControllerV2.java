package com.example.library.controller.v2;




import com.example.library.dto.v2.AuthorDtoV2;
import com.example.library.dto.v2.BookDtoV2;
import com.example.library.dto.v2.CreateAuthorRequestV2;
import com.example.library.entity.Author;
import com.example.library.entity.Book;
import com.example.library.exception.ApiErrorResponse;
import com.example.library.service.AuthorService;
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
@RequestMapping("/api/v2/authors")
// v2 keeps a separate author namespace so the response shape can evolve independently of v1.
@Tag(name = "Authors V2", description = "Endpoints for managing authors in API version 2")
@Validated
public class AuthorControllerV2 {


    private final AuthorService authorService;
    private final BookService bookService;


    public AuthorControllerV2(AuthorService authorService, BookService bookService) {
        this.authorService = authorService;
        this.bookService = bookService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create author", description = "Creates a new author using the v2 contract")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Author created successfully"),
            @ApiResponse(
                    responseCode = "400",
                    description = "Validation failed",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Unexpected server error",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))
            )
    })
    public AuthorDtoV2 createAuthor(@Valid @RequestBody CreateAuthorRequestV2 request) {
        Author savedAuthor = authorService.createAuthor(request.name());
        return toAuthorDto(savedAuthor);
    }


    @GetMapping("/{id}")
    @Operation(summary = "Get author by id", description = "Returns a single author by id in the v2 format")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Author found"),
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
    public AuthorDtoV2 getAuthorById(@PathVariable @Min(value = 1, message = "id must be >= 1") Long id) {
        Author author = authorService.getAuthorById(id);
        return toAuthorDto(author);
    }

    @GetMapping("/{id}/books")
    @Operation(summary = "Get books by author", description = "Returns all books written by the specified author in the v2 format")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Books returned successfully"),
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
    public List<BookDtoV2> getBooksByAuthorId(
            @PathVariable @Min(value = 1, message = "id must be >= 1") Long id,
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "page must be >= 0") int page,
            @RequestParam(defaultValue = "20") @Min(value = 1, message = "size must be >= 1") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        // v2 reuses the same author-book relation but returns the richer book DTO with genre and availability.
        return authorService.getBooksByAuthorId(id, pageable)
                .map(this::toBookDto)
                .getContent();
    }


    private AuthorDtoV2 toAuthorDto(Author author) {
        return new AuthorDtoV2(
                author.getId(),
                author.getName(),
                author.getBooks().size()
        );
    }

    private BookDtoV2 toBookDto(Book book) {
        // The DTO adds computed availability so clients do not need a second request to know if a book can be borrowed.
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