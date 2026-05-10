package com.example.library.service;


import com.example.library.entity.Book;
import com.example.library.entity.Loan;
import com.example.library.exception.BookAlreadyOnLoanException;
import com.example.library.repository.LoanRepository;
import jakarta.transaction.Transactional;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@Transactional
// Encapsulates the loan rules, especially the guarantee that a book can only have one active loan.
public class LoanService {
    private final LoanRepository loanRepository;
    private final BookService bookService;


    public LoanService(LoanRepository loanRepository, BookService bookService) {
        this.loanRepository = loanRepository;
        this.bookService = bookService;
    }


    @CacheEvict(value = "bookAvailability", key = "#bookId")
    public Loan createLoan(Long bookId){
        // The book is loaded with a write lock so concurrent requests serialize around the same row.
        Book book = bookService.getBookByIdForUpdate(bookId);

        if (loanRepository.existsByBookIdAndReturnDateIsNull(bookId)) {
            throw new BookAlreadyOnLoanException(bookId);
        }

        // An active loan is represented by a null returnDate until the book is returned.
        Loan loan = new Loan();
        loan.setBook(book);
        loan.setLoanDate(LocalDate.now());
        loan.setReturnDate(null);

        return loanRepository.save(loan);

    }

    public List<Loan> getActiveLoans(){
        // The API only exposes active loans, which in this model means `returnDate is null`.
        return loanRepository.findByReturnDateIsNull();
    }

    public Page<Loan> getActiveLoans(Pageable pageable) {
        // Pagination keeps list endpoints bounded for larger datasets.
        return loanRepository.findByReturnDateIsNull(pageable);
    }

}
