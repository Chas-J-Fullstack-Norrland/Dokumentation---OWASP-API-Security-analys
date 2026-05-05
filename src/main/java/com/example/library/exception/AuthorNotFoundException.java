package com.example.library.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;


@ResponseStatus(HttpStatus.NOT_FOUND)
// Thrown when an API request targets an author id that does not exist.
public class AuthorNotFoundException extends RuntimeException {


    public AuthorNotFoundException(Long id){
            super("Author with id " + id + " not found");
    }
}
