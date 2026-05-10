package com.example.library.service;


import com.example.library.entity.Author;
import com.example.library.entity.Book;
import com.example.library.exception.AuthorNotFoundException;
import com.example.library.repository.AuthorRepository;
import com.example.library.repository.BookRepository;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;


@Slf4j
@Service
@Transactional
// Centralizes author-related business rules so controllers stay focused on HTTP concerns.
public class AuthorService {

    private final AuthorRepository authorRepository;
    private final BookRepository bookRepository;

    public AuthorService(AuthorRepository authorRepository, BookRepository bookRepository) {
        this.authorRepository = authorRepository;
        this.bookRepository = bookRepository;
    }

    // Creating the entity through the constructor keeps the name validation in one place.
    public Author createAuthor(String name){
        Author author = new Author(name);
        return authorRepository.save(author);
    }

    // A missing author is treated as a domain error and translated to 404 by the global handler.
    public Author getAuthorById(Long id){
        return authorRepository.findById(id)
                .orElseThrow(() -> new AuthorNotFoundException(id));
    }

    // The author lookup fails fast before querying books so `/authors/{id}/books` returns 404 for unknown authors.

    public Page<Book> getBooksByAuthorId(Long authorId, Pageable pageable) {
        if (!authorRepository.existsById(authorId)) {
            throw new AuthorNotFoundException(authorId);
        }

        log.info("Fetching books page for author {}: page={}, size={}, sort={}",
                authorId,
                pageable.getPageNumber(),
                pageable.getPageSize(),
                pageable.getSort());

        return bookRepository.findAllByAuthorId(authorId, pageable);
    }
}
