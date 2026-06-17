package com.example.library.dto.external;

public record ExternalBookDto(
        String isbn,
        String title,
        String author,
        String publishDate
) {
}
