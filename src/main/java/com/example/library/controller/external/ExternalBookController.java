package com.example.library.controller.external;

import com.example.library.dto.external.ExternalBookDto;
import com.example.library.service.ExternalBookService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/external-books")
@Tag(name = "External Books ", description = "Endpoints for fetching books from Open Library")
public class ExternalBookController {

    private final ExternalBookService externalBookService;

    public ExternalBookController(ExternalBookService externalBookService) {
        this.externalBookService = externalBookService;
    }

    @GetMapping("/isbn/{isbn}")
    @Operation(summary = "Get external book by ISBN", description = "Fetches book data from Open Library and maps it to the app's own DTO")
    public ExternalBookDto getByIsbn(@PathVariable String isbn) {
        return externalBookService.getBookByIsbn(isbn);
    }
}
