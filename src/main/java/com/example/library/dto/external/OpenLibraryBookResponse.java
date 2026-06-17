package com.example.library.dto.external;



import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OpenLibraryBookResponse(
        String title,
        String publish_date,
        List<OpenLibraryAuthorResponse> authors
) {
}
