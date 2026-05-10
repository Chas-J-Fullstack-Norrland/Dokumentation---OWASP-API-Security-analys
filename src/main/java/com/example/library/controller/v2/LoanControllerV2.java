package com.example.library.controller.v2;

import com.example.library.dto.v2.CreateLoanRequestV2;
import com.example.library.dto.v2.LoanDtoV2;
import com.example.library.entity.Loan;
import com.example.library.service.LoanService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import com.example.library.exception.ApiErrorResponse;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v2/loans")
// v2 keeps its own loan endpoint namespace even though the current payload matches v1 closely.
@Tag(name = "Loans V2", description = "Endpoints for managing active book loans in API version 2")
@Validated
public class LoanControllerV2 {

    private final LoanService loanService;


    public LoanControllerV2(LoanService loanService) {
        this.loanService = loanService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create loan", description = "Creates a new loan for a book if it is available")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Loan created successfully"),
            @ApiResponse(
                    responseCode = "400",
                    description = "Book already on loan, validation failed, or request is invalid",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Book not found",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Unexpected server error",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))
            )
    })
    public LoanDtoV2 createLoan(@Valid @RequestBody CreateLoanRequestV2 request){
        // The service enforces the single-active-loan rule; the controller only maps request and response data.
        Loan loan = loanService.createLoan(request.bookId());
        return toDto(loan);
    }

    @GetMapping
    @Operation(summary = "Get active loans", description = "Returns all currently active loans in the v2 format")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Active loans returned successfully"),
            @ApiResponse(
                    responseCode = "500",
                    description = "Unexpected server error",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))
            )
    })
    public List <LoanDtoV2> getActiveLoans(
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "page must be >= 0") int page,
            @RequestParam(defaultValue = "20") @Min(value = 1, message = "size must be >= 1") int size
    ){
        Pageable pageable = PageRequest.of(page, size);
        // Keeping a dedicated v2 endpoint now makes later response changes easier without touching v1.
        return loanService.getActiveLoans(pageable)
                .map(this::toDto)
                .getContent();
    }

    private LoanDtoV2 toDto(Loan loan){
        return new LoanDtoV2(
                loan.getId(),
                loan.getBook().getId(),
                loan.getBook().getTitle(),
                loan.getLoanDate(),
                loan.getReturnDate()
        );
    }

}
