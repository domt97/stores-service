package com.dotran.oms.store.domain.model;

import com.dotran.oms.core.exception.ValidationException;
import com.dotran.oms.store.domain.enums.MerchantStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Merchant")
class MerchantTest {

    @Test
    @DisplayName("register: new id, ACTIVE, trimmed name, timestamps set")
    void register_createsActiveMerchant() {
        Merchant merchant = Merchant.register("m001", "  Tea Chain  ");

        assertThat(merchant.getId()).isNotNull();
        assertThat(merchant.getStatus()).isEqualTo(MerchantStatus.ACTIVE);
        assertThat(merchant.isActive()).isTrue();
        assertThat(merchant.getName()).isEqualTo("Tea Chain");
        assertThat(merchant.getCreatedAt()).isNotNull();
        assertThat(merchant.getUpdatedAt()).isEqualTo(merchant.getCreatedAt());
    }

    @Test
    @DisplayName("register: code is trimmed and upper-cased (case-insensitive uniqueness)")
    void register_normalizesCode() {
        assertThat(Merchant.register("  m001 ", "Tea").getCode()).isEqualTo("M001");
        assertThat(Merchant.normalizeCode("Ab-1")).isEqualTo("AB-1");
        assertThat(Merchant.normalizeCode(null)).isNull();
    }

    @Test
    @DisplayName("register: blank code or name is rejected")
    void register_rejectsBlankFields() {
        assertThatThrownBy(() -> Merchant.register("   ", "Tea")).isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> Merchant.register(null, "Tea")).isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> Merchant.register("M001", " ")).isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> Merchant.register("M001", null)).isInstanceOf(ValidationException.class);
    }

    @Test
    @DisplayName("register: code is 1-50 ASCII letters or digits (BR-MER-04)")
    void register_validatesCodeCharacters() {
        assertThat(Merchant.register("a".repeat(50), "Tea").getCode()).isEqualTo("A".repeat(50));
        assertThat(Merchant.register("Tea2Go", "Tea").getCode()).isEqualTo("TEA2GO");

        assertThatThrownBy(() -> Merchant.register("A".repeat(51), "Tea")).isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> Merchant.register("M-001", "Tea")).isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> Merchant.register("M 001", "Tea")).isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> Merchant.register("M_001", "Tea")).isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> Merchant.register("TR\u00C0S\u1EEEA", "Tea")).isInstanceOf(ValidationException.class);
        // "\u00DF" (sharp s) upper-cases to "SS"; rejected before upper-casing
        assertThatThrownBy(() -> Merchant.register("\u00DF", "Tea")).isInstanceOf(ValidationException.class);
    }
}
