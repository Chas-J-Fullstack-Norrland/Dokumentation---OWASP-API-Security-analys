package com.example.library.exception;

// Signals the business rule that one book can only have one active loan at a time.
public class BookAlreadyOnLoanException extends RuntimeException {
    public BookAlreadyOnLoanException(Long bookId) {
        super("Book is already on loan");
    }
}
