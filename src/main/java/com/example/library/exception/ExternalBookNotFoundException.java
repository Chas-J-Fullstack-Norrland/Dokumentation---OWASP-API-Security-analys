package com.example.library.exception;

public class ExternalBookNotFoundException extends RuntimeException {
    public ExternalBookNotFoundException(String isbn) {
        super("Book not found in Open Library for ISBN: " + isbn);
    }
}

