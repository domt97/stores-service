package com.dotran.oms.store.application.service.impex;

import com.dotran.oms.core.domain.id.MerchantId;
import com.dotran.oms.core.domain.id.StoreId;
import com.dotran.oms.core.exception.NotFoundException;
import com.dotran.oms.core.exception.ValidationException;
import com.dotran.oms.store.application.command.impex.CreateImportUploadCmd;
import com.dotran.oms.store.application.config.ProductImportLimits;
import com.dotran.oms.store.application.dto.impex.ImportUploadDto;
import com.dotran.oms.store.application.impex.product.ImportFileName;
import com.dotran.oms.store.application.impex.upload.ImportUploadSigner;
import com.dotran.oms.store.application.impex.upload.SignedUpload;
import com.dotran.oms.store.application.repository.MerchantRepository;
import com.dotran.oms.store.application.repository.StoreRepository;
import com.dotran.oms.store.common.constants.Constants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CreateImportUploadService (BR-CAT-17)")
class CreateImportUploadServiceTest {

    @Mock private MerchantRepository merchantRepository;
    @Mock private StoreRepository storeRepository;
    @Mock private ImportUploadSigner signer;

    private final ProductImportLimits limits = new ProductImportLimits();
    private CreateImportUploadService service;

    private final MerchantId merchantId = MerchantId.newMerchantId();
    private final StoreId storeId = StoreId.newStoreId();
    private final SignedUpload signed = new SignedUpload("http://s3/url", "PUT",
            Map.of("content-type", "x", "x-amz-tagging", "purpose=import-upload"), Instant.parse("2026-11-14T10:15:00Z"));

    @BeforeEach
    void setUp() {
        service = new CreateImportUploadService(merchantRepository, storeRepository, signer, limits);
    }

    private CreateImportUploadCmd cmd(long size) {
        return CreateImportUploadCmd.builder().merchantId(merchantId).storeId(storeId).sizeBytes(size).build();
    }

    private void merchantAndStoreExist() {
        when(merchantRepository.existsById(merchantId)).thenReturn(true);
        when(storeRepository.existsByMerchantIdAndStoreId(merchantId, storeId)).thenReturn(true);
    }

    @ParameterizedTest
    @ValueSource(longs = {0, -1, 5L * 1024 * 1024 + 1})
    @DisplayName("size below 1 or above the import limit → 400 before anything else: no lookup, nothing signed")
    void badSize_400_nothingTouched(long size) {
        assertThatThrownBy(() -> service.create(cmd(size)))
                .isInstanceOf(ValidationException.class)
                .hasMessage(String.format(Constants.ERROR_MSG_IMPORT_UPLOAD_SIZE_INVALID, limits.getMaxBytes()));

        verifyNoInteractions(merchantRepository, storeRepository, signer);
    }

    @Test
    @DisplayName("unknown merchant → 404: store never checked, nothing signed")
    void unknownMerchant_404() {
        when(merchantRepository.existsById(merchantId)).thenReturn(false);

        assertThatThrownBy(() -> service.create(cmd(100)))
                .isInstanceOf(NotFoundException.class).hasMessage(Constants.ERROR_MSG_MERCHANT_NOT_FOUND);

        verifyNoInteractions(storeRepository, signer);
    }

    @Test
    @DisplayName("store not the merchant's → 404, nothing signed")
    void storeOfAnotherMerchant_404() {
        when(merchantRepository.existsById(merchantId)).thenReturn(true);
        when(storeRepository.existsByMerchantIdAndStoreId(merchantId, storeId)).thenReturn(false);

        assertThatThrownBy(() -> service.create(cmd(100)))
                .isInstanceOf(NotFoundException.class).hasMessage(Constants.ERROR_MSG_STORE_NOT_FOUND);

        verify(signer, never()).sign(any(), any(), any(), anyLong(), any());
    }

    @ParameterizedTest
    @ValueSource(longs = {1, 5L * 1024 * 1024})
    @DisplayName("1 byte and exactly the limit are signed for this store, with the exact size and the configured lifetime")
    void signsExactSizeAndTtl(long size) {
        merchantAndStoreExist();
        limits.setUploadUrlTtl(Duration.ofMinutes(7));
        ArgumentCaptor<ImportFileName> name = ArgumentCaptor.forClass(ImportFileName.class);
        when(signer.sign(eq(merchantId), eq(storeId), name.capture(), eq(size), eq(Duration.ofMinutes(7)))).thenReturn(signed);

        ImportUploadDto dto = service.create(cmd(size));

        assertThat(dto.fileName()).isEqualTo(name.getValue().value());
        assertThat(dto.uploadUrl()).isEqualTo("http://s3/url");
        assertThat(dto.method()).isEqualTo("PUT");
        assertThat(dto.headers()).isEqualTo(signed.headers());
        assertThat(dto.expiresAt()).isEqualTo(signed.expiresAt());
        assertThat(dto.maxBytes()).isEqualTo(limits.getMaxBytes());
        assertThat(dto.toString()).doesNotContain("http://s3/url");
    }

    @Test
    @DisplayName("the server chooses the name: {uuid}.xlsx, a valid import file name, new on every call")
    void serverChosenName() {
        merchantAndStoreExist();
        when(signer.sign(any(), any(), any(), anyLong(), any())).thenReturn(signed);

        String first = service.create(cmd(10)).fileName();
        String second = service.create(cmd(10)).fileName();

        assertThat(first).matches("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.xlsx$");
        assertThat(ImportFileName.of(first).value()).isEqualTo(first);
        assertThat(second).isNotEqualTo(first);
        verify(signer, times(2)).sign(eq(merchantId), eq(storeId), any(), eq(10L), any());
    }
}
