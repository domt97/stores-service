package com.dotran.oms.store.infrastructure.rest.exception;

import com.dotran.oms.core.rest.ErrorResponse;
import com.dotran.oms.store.domain.exception.StoreAlreadyClosedException;
import com.dotran.oms.store.domain.exception.StoreNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("StoreExceptionHandler")
class StoreExceptionHandlerTest {

    @InjectMocks
    private StoreExceptionHandler handler;

    @Mock
    private HttpServletRequest request;

    @Test
    @DisplayName("storeNotFoundException returns 404 with error details")
    void storeNotFoundException_returns404() {
        StoreNotFoundException ex = new StoreNotFoundException();
        when(request.getRequestURI()).thenReturn("/api/stores/123");

        ErrorResponse response = handler.storeNotFoundException(ex, request);

        assertThat(response.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(response.getPath()).isEqualTo("/api/stores/123");
        assertThat(response.getTimestamp()).isNotNull();
    }

    @Test
    @DisplayName("storeAlreadyClosedException returns 409 with error details")
    void storeAlreadyClosedException_returns409() {
        StoreAlreadyClosedException ex = new StoreAlreadyClosedException();
        when(request.getRequestURI()).thenReturn("/api/stores/123/close");

        ErrorResponse response = handler.storeAlreadyClosedException(ex, request);

        assertThat(response.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
        assertThat(response.getPath()).isEqualTo("/api/stores/123/close");
        assertThat(response.getTimestamp()).isNotNull();
    }
}
