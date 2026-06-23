package com.jobtracker.controller;

import com.jobtracker.service.AnalysisException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * Maps domain/validation failures to clean HTTP responses (RFC 7807
 * ProblemDetail) so clients never see a raw 500 stack trace.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** Bean-validation failure on the request body -> 400. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        String detail = ex.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .collect(Collectors.joining("; "));
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
        problem.setTitle("Invalid request");
        return problem;
    }

    /** Downstream Claude failure -> 502 (not a leaked 500). */
    @ExceptionHandler(AnalysisException.class)
    public ProblemDetail handleAnalysis(AnalysisException ex) {
        log.warn("Analysis failed: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_GATEWAY,
                "The analysis service is temporarily unavailable. Please try again.");
        problem.setTitle("Analysis failed");
        return problem;
    }
}
