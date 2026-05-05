package com.example.library.repository;

import com.example.library.entity.Loan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

// Repository methods are centered around the project's definition of an active loan.
public interface LoanRepository extends JpaRepository <Loan, Long> {

    // Used before creating a loan to reject duplicate active borrows for the same book.
    boolean existsByBookIdAndReturnDateIsNull(Long bookId);

    // The list endpoint returns only active loans, not historical returned loans.
    List<Loan> findByReturnDateIsNull ();

}
