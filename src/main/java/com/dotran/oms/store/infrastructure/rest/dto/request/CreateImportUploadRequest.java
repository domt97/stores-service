package com.dotran.oms.store.infrastructure.rest.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateImportUploadRequest {

    /**
     * Exact size in bytes of the file the client will upload (it is signed into the URL). A {@code BigDecimal}, not a
     * {@code Long}: Jackson would read {@code 1.5} into a Long as 1 and sign the wrong length. At most 18 digits, so it
     * always fits a long; the upper limit (BR-CAT-13) is checked by the use case.
     */
    @NotNull
    @DecimalMin("1")
    @Digits(integer = 18, fraction = 0)
    private BigDecimal sizeBytes;
}
