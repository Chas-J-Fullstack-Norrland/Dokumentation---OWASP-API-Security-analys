package com.example.library.controller.v1;


import com.example.library.dto.v1.CreateLoanRequestV1;
import com.example.library.dto.v1.LoanDtoV1;
import com.example.library.entity.Loan;
import com.example.library.exception.ApiErrorResponse;
import com.example.library.service.LoanService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/loans")
@Tag(name = "Loans v1", description = "Endpoints for managing Loans in API v1")
@Validated
// v1 exposes the original loan API while delegating all concurrency-sensitive rules to the service layer.
public class LoanControllerV1 {

    private final LoanService loanService;


    public LoanControllerV1(LoanService loanService) {
        this.loanService = loanService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Create loan",
            description = "Creat a loan if a book is available"
    )

    @ApiResponses({
            @ApiResponse(responseCode = "201",description = "loan created successfully"),
            @ApiResponse(
                    responseCode = "400",
                    description = "Book is already on loan",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))
            ),
    @ApiResponse(
            responseCode = "404",
            description = "Book not found",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))
    )

    })
    public LoanDtoV1 createLoan(@Valid @RequestBody CreateLoanRequestV1 request){
        // The controller stays thin so duplicate-loan protection remains centralized in LoanService.
        Loan loan = loanService.createLoan(request.bookId());
        return toDto(loan);
    }

    @GetMapping
    @Operation(
            summary = "Get active loans",
            description = "Returns all currently active loans"
    )

    @ApiResponses({
            @ApiResponse(responseCode = "200", description = " All Active loans returned successfully")
    })

    public List <LoanDtoV1> getActiveLoans(
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "page must be >= 0") int page,
            @RequestParam(defaultValue = "20") @Min(value = 1, message = "size must be >= 1") int size
    ){
        Pageable pageable = PageRequest.of(page, size);
        // Only active loans are returned because returned books are modeled by a non-null returnDate.
        return loanService.getActiveLoans(pageable)
                .map(this::toDto)
                .getContent();
    }

    private LoanDtoV1 toDto(Loan loan){
        return new LoanDtoV1(
                loan.getId(),
                loan.getBook().getId(),
                loan.getBook().getTitle(),
                loan.getLoanDate(),
                loan.getReturnDate()
        );
    }

}
