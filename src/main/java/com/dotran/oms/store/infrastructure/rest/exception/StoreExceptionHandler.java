package com.dotran.oms.store.infrastructure.rest.exception;

import com.dotran.oms.store.domain.exception.StoreAlreadyClosedException;
import com.dotran.oms.store.domain.exception.StoreNotFoundException;
import com.dotran.oms.core.rest.ErrorResponse;
import com.dotran.oms.core.rest.GlobalExceptionHandler;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;

@RestControllerAdvice
@Slf4j
public class StoreExceptionHandler extends GlobalExceptionHandler {

    @ExceptionHandler(value = {StoreNotFoundException.class})
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse storeNotFoundException(Exception ex, HttpServletRequest request) {
        this.logError(ex);

        return ErrorResponse.builder()
                .error(ex.getMessage())
                .timestamp(Instant.now())
                .status(HttpStatus.NOT_FOUND.value())
                .path(request.getRequestURI())
                .build();
    }

    @ExceptionHandler(value = {StoreAlreadyClosedException.class})
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse storeAlreadyClosedException(StoreAlreadyClosedException ex, HttpServletRequest request) {
        this.logError(ex);

        return ErrorResponse.builder()
                .error(ex.getMessage())
                .timestamp(Instant.now())
                .status(HttpStatus.CONFLICT.value())
                .path(request.getRequestURI())
                .build();
    }
}
