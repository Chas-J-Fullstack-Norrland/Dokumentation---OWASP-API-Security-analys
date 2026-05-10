package com.example.library.config;

import com.example.library.entity.Author;
import com.example.library.repository.BookRepository;
import com.example.library.service.AuthorService;
import com.example.library.service.BookService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.transaction.annotation.Transactional;


@Configuration
@Profile("dev")
public class DataInitializer {

    @Bean
    @Transactional
    CommandLineRunner seedLibraryData(
            AuthorService authorService,
            BookService bookService,
            BookRepository bookRepository
    ) {
        return args -> {
            // Seed only once, avoids duplicates on restart.
            if (bookRepository.count() > 0) {
                return;
            }

            Author author1 = authorService.createAuthor("Astrid Lindgren");
            Author author2 = authorService.createAuthor("Tove Jansson");
            Author author3 = authorService.createAuthor("George Orwell");

            // Uses v2 create method (explicit genre) to keep seed data rich for v2 responses.
            bookService.createBook("Pippi Longstocking", author1.getId(), "Children", "9789129698313", 1945);
            bookService.createBook("Mio, My Son", author1.getId(), "Fantasy", "9789129696487", 1954);
            bookService.createBook("The Brothers Lionheart", author1.getId(), "Fantasy", "9789129688314", 1973);
            bookService.createBook("Ronia, the Robber's Daughter", author1.getId(), "Fantasy", "9789129689915", 1981);

            bookService.createBook("Comet in Moominland", author2.getId(), "Children", "9780000000203", 1946);
            bookService.createBook("Finn Family Moomintroll", author2.getId(), "Children", "9780000000204", 1948);
            bookService.createBook("Moominpappa at Sea", author2.getId(), "Children", "9780000000205", 1965);

            bookService.createBook("1984", author3.getId(), "Dystopian", "9780451524935", 1949);
            bookService.createBook("Animal Farm", author3.getId(), "Satire", "9780451526342", 1945);
            bookService.createBook("Homage to Catalonia", author3.getId(), "Memoir", "9780156421171", 1938);
        };
    }
}
