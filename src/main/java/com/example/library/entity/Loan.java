package com.example.library.entity;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(
        name = "Loans",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_loan_book", columnNames = "book_id")
        }
)

@Getter
@Setter
@NoArgsConstructor

// Each loan represents one borrow operation; the unique constraint prevents two loans for the same book row.
public class Loan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false)
    @JoinColumn(name = "book_id", nullable = false,unique = true)
    private Book book;

    // loanDate is set when the loan is created, while returnDate stays null for active loans.
    @Column
    private LocalDate loanDate;

    @Column
    private LocalDate returnDate;

}
