package com.example.library.entity;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "authors")
@Getter
@Setter
@NoArgsConstructor
// Author is the inverse side of the author-books relationship and validates its name eagerly.
public class Author {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    // `mappedBy` tells JPA that Book owns the foreign key column in the database.
    @OneToMany(mappedBy = "author")
    private List<Book> books = new ArrayList<>();

    public Author(String name){
        setName(name);
    }

    public void setName(String name) {
         // The entity protects its own invariant so invalid authors cannot be persisted accidentally.
         if(name == null || name.isBlank()) {
             throw new IllegalArgumentException("Author name cannot be empty");
         }
         this.name = name;
    }

    public void addBook(Book book) {
        // Convenience methods keep both sides of the bidirectional relation synchronized in memory.
        if (book == null) {
            throw new IllegalArgumentException("Book cannot be null");
        }
        books.add(book);
        book.setAuthor(this);
    }

    public void removeBook(Book book) {
        if (book == null) {
            return;
        }
        books.remove(book);
        book.setAuthor(null);
    }
}


