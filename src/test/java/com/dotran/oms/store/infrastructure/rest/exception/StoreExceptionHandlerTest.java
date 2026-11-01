package com.dotran.oms.store.infrastructure.rest.exception;

import com.dotran.oms.core.rest.ErrorResponse;
import com.dotran.oms.store.application.impex.product.ImportBusyException;
import com.dotran.oms.store.application.impex.product.StoreImportRunningException;
import com.dotran.oms.store.domain.exception.StoreAlreadyClosedException;
import com.dotran.oms.store.domain.exception.ProductVersionConflictException;
import com.dotran.oms.store.domain.exception.SkuCodeInUseException;
import com.dotran.oms.store.domain.exception.StoreNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

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

    @Test
    @DisplayName("productConflict returns 409 with the message for a version conflict and a SKU code in use")
    void productConflict_returns409() {
        when(request.getRequestURI()).thenReturn("/v1/store/merchants/m/stores/s/products/p");

        ErrorResponse version = handler.productConflict(new ProductVersionConflictException("changed"), request);
        ErrorResponse code = handler.productConflict(new SkuCodeInUseException("in use"), request);

        assertThat(version.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
        assertThat(version.getError()).isEqualTo("changed");
        assertThat(code.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
        assertThat(code.getError()).isEqualTo("in use");
    }

    @Test
    @DisplayName("importBusy returns 503 with Retry-After and the message")
    void importBusy_returns503_withRetryAfter() {
        when(request.getRequestURI()).thenReturn("/v1/store/merchants/m/stores/s/products/import");

        ResponseEntity<ErrorResponse> response = handler.importBusy(new ImportBusyException(10), request);

        assertThat(response.getStatusCode().value()).isEqualTo(503);
        assertThat(response.getHeaders().getFirst(HttpHeaders.RETRY_AFTER)).isEqualTo("10");
        assertThat(response.getBody().getError()).isEqualTo("The server is busy with other imports; try again in 10 seconds");
        assertThat(response.getBody().getStatus()).isEqualTo(503);
    }

    @Test
    @DisplayName("storeImportRunning returns 409 with the message")
    void storeImportRunning_returns409() {
        when(request.getRequestURI()).thenReturn("/v1/store/merchants/m/stores/s/products/import");

        ErrorResponse response = handler.storeImportRunning(new StoreImportRunningException(), request);

        assertThat(response.getStatus()).isEqualTo(409);
        assertThat(response.getError()).isEqualTo("An import for this store is already running; wait for it to finish");
    }
}
