package com.example.library.entity;

import jakarta.persistence.*;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.Getter;

@Entity
@Table(name = "books")
@Getter
@NoArgsConstructor
@Setter
// Book owns the foreign key to Author and stores the core catalog data used by both API versions.
public class Book {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable =false)
    private String title;

    // The owning side writes `author_id` to the books table.
    @ManyToOne(optional = false)
    @JoinColumn(name = "author_id", nullable = false)
    private Author author;

    // Genre became part of the public contract in v2, but it is stored on the shared entity.
    @Column(nullable = false)
    private String genre;

    @Column(nullable = false, unique = true)
    private String isbn;

    @Column(nullable = false)
    private int publicationYear;



}
