package com.example.library.repository;

import com.example.library.entity.Author;
import org.springframework.data.jpa.repository.JpaRepository;

// Standard JPA access for authors; custom business rules live in AuthorService.
public interface AuthorRepository extends JpaRepository<Author, Long> {
}
