package com.example.library.controller.v1;


import com.example.library.dto.v1.AuthorDtoV1;
import com.example.library.dto.v1.BookDtoV1;
import com.example.library.dto.v1.CreateAuthorRequestV1;
import com.example.library.entity.Author;
import com.example.library.entity.Book;
import com.example.library.exception.ApiErrorResponse;
import com.example.library.service.AuthorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/authors")
@Tag(name = "Authors V1", description = "Endpoints for managing authors in API v1" )
// v1 keeps the original author contract so future versions can evolve independently.
public class AuthorControllerV1 {


    private final AuthorService authorService;


    public AuthorControllerV1(AuthorService authorService) {
        this.authorService = authorService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Create author",
            description = "Creates a new author"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Author created successfully"),
            @ApiResponse(
                    responseCode = "400",
                    description = "Validation failed",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))
            )
    })
    public AuthorDtoV1 createAuthor(@Valid @RequestBody CreateAuthorRequestV1 request) {
        // Controllers map HTTP payloads to DTOs while the service layer owns the business rules.
        Author savedAuthor = authorService.createAuthor(request.name());
        return toAuthorDto(savedAuthor);
    }


    @GetMapping("/{id}")
    @Operation(
            summary = "Get author by id",
            description = "Returns one author by id"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Author found"),
            @ApiResponse(
                    responseCode = "404",
                    description = "Author not found",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))
            )
    })
    public AuthorDtoV1 getAuthorById(@PathVariable Long id) {
        Author author = authorService.getAuthorById(id);
        return toAuthorDto(author);
    }

    @GetMapping("/{id}/books")
    @Operation(
            summary = "Get books by author",
            description = "Returns all books written by the specified author"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Books returned successfully"),
            @ApiResponse(
                    responseCode = "404",
                    description = "Author not found",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))
            )
    })
    public List<BookDtoV1> getBooksByAuthorId(@PathVariable Long id) {
        // v1 book DTOs intentionally omit genre so the older contract stays stable.
        return authorService.getBooksByAuthorId(id)
                .stream()
                .map(this::toBookDto)
                .toList();
    }


    private AuthorDtoV1 toAuthorDto(Author author) {
        // The response exposes a summary count instead of embedding the full book collection.
        return new AuthorDtoV1(
                author.getId(),
                author.getName(),
                author.getBooks().size()
        );
    }

    private BookDtoV1 toBookDto(Book book) {
        return new BookDtoV1(
                book.getId(),
                book.getTitle(),
                book.getAuthor().getName(),
                book.getIsbn(),
                book.getPublicationYear()
        );

    }
}