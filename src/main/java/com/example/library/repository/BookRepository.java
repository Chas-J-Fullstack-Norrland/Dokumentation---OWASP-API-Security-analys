package com.example.library.repository;
import com.example.library.entity.Book;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;


// Repository for catalog reads plus the lock-aware query used during loan creation.
public interface BookRepository extends JpaRepository<Book, Long> {

    // Used by the author endpoints to list only the books that belong to one author.
    List <Book> findAllByAuthorId(Long authorId);

    // This query acquires a database lock so concurrent loan requests cannot modify the same book safely in parallel.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from Book b where b.id = :id")
    Optional<Book> findByIdForUpdate(@Param("id") Long id);

    Page<Book> findAllByAuthorId(Long authorId, Pageable pageable);

}


