package com.dotran.oms.store.infrastructure.rest.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImportStoreProductsRequest {

    /**
     * Name of the .xlsx file in the store's import folder (BR-CAT-16): the server reads
     * {@code {merchantId}/{storeId}/imports/{fileName}} from its own bucket. The client never chooses the bucket or the
     * folder; a {@code path} sent by older clients is ignored. These checks give an early 400 for the obvious cases;
     * ImportFileName is the full rule (UTF-8 bytes, Unicode categories, {@code .}/{@code ..}).
     */
    @NotBlank
    @Size(max = 255)
    @Pattern(regexp = "[^/\\\\\\p{Cntrl}]+", message = "must be a plain file name, without a slash, a backslash or control characters")
    private String fileName;
}
