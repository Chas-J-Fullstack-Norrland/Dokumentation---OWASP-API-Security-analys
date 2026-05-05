package com.example.library.service;

import com.example.library.entity.Author;
import com.example.library.exception.BookNotFoundException;
import com.example.library.entity.Book;
import com.example.library.repository.BookRepository;
import com.example.library.repository.LoanRepository;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Transactional
// Handles book business rules, cache coordination, and version-specific create/update behavior.
public class BookService {

    private static final String DEFAULT_GENRE_FOR_V1 = "Unknown";
    private final BookRepository bookRepository;
    private final AuthorService authorService;
    private final LoanRepository loanRepository;
    private static final Logger log = LoggerFactory.getLogger(BookService.class);


    public BookService(BookRepository bookRepository,
                       AuthorService authorService,
                       LoanRepository loanRepository) {

        this.bookRepository = bookRepository;
        this.authorService = authorService;
        this.loanRepository = loanRepository;
    }

    @Cacheable("books")
    public List<Book> getAllBooks() {
        // Repeated reads can be served from cache until a create/update operation invalidates the list.
        log.info("Fetching all books from database");
        return bookRepository.findAll();
    }

    @Cacheable(value = "bookById", key = "#id")
    public Book getBookById(Long id) {
        // The explicit not-found exception keeps the REST contract stable for both v1 and v2.
        log.info("Fetching book from database: {}", id);

        return bookRepository.findById(id)
                .orElseThrow(() -> new BookNotFoundException(id));
    }

    public Book getBookByIdForUpdate(Long id) {
        // Loan creation uses a pessimistic lock to prevent two requests from borrowing the same book at once.
        return bookRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new BookNotFoundException(id));
    }

    // v1 keeps its original contract, so genre is filled with a default value on the server side.
    @Caching(evict = {
            @CacheEvict(value = "books", allEntries = true),
            @CacheEvict(value = "bookById", allEntries = true),
            @CacheEvict(value = "bookAvailability", allEntries = true)
    })
    public Book createBook(String title, Long authorId, String isbn, int publicationYear) {
        return createBook(title, authorId, DEFAULT_GENRE_FOR_V1, isbn, publicationYear);
    }

    // v2 makes genre part of the public API and persists it explicitly.
    @Caching(evict = {
            @CacheEvict(value = "books", allEntries = true),
            @CacheEvict(value = "bookById", allEntries = true),
            @CacheEvict(value = "bookAvailability", allEntries = true)
    })
    public Book createBook(String title, Long authorId,String genre, String isbn, int publicationYear) {
        Author author = authorService.getAuthorById(authorId);

        Book book = new Book();
        book.setTitle(title);
        book.setAuthor(author);
        book.setGenre(genre);
        book.setIsbn(isbn);
        book.setPublicationYear(publicationYear);

        return bookRepository.save(book);
    }

    @Cacheable(value = "bookAvailability", key = "#bookId")
    public boolean isAvailable(Long bookId) {
        // Availability is derived from active loans instead of being stored on the book itself.
        getBookById(bookId);
        return !loanRepository.existsByBookIdAndReturnDateIsNull(bookId);
    }

    @Caching(evict = {
            @CacheEvict(value = "books", allEntries = true),
            @CacheEvict(value = "bookById", key = "#id"),
            @CacheEvict(value = "bookAvailability", key = "#id")
    })
    public Book patchBook(Long id,
                          String title,
                          Long authorId,
                          String genre,
                          String isbn,
                          Integer publicationYear) {

        // PATCH only applies the fields that are present so older books can be enriched incrementally.
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new BookNotFoundException(id));

        if (title != null) {
            book.setTitle(title);
        }

        if (authorId != null) {
            Author author = authorService.getAuthorById(authorId);
            book.setAuthor(author);
        }

        if (genre != null) {
            book.setGenre(genre);
        }

        if (isbn != null) {
            book.setIsbn(isbn);
        }

        if (publicationYear != null) {
            book.setPublicationYear(publicationYear);
        }

        return bookRepository.save(book);
    }


}


