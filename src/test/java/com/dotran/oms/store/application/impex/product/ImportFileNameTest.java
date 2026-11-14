package com.dotran.oms.store.application.impex.product;

import com.dotran.oms.core.exception.ValidationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ImportFileName (BR-CAT-16)")
class ImportFileNameTest {

    @ParameterizedTest
    @ValueSource(strings = {"products.xlsx", "sản phẩm tháng 11.xlsx", "my file (1).xlsx", "no-extension", ".hidden.xlsx",
            "a..b.xlsx", "...", "🙂.xlsx"})
    @DisplayName("accepts plain file names, Vietnamese and emoji included")
    void acceptsPlainNames(String name) {
        assertThat(ImportFileName.of(name).value()).isEqualTo(name);
    }

    @Test
    @DisplayName("accepts exactly 255 UTF-8 bytes")
    void accepts255Bytes() {
        String name = "a".repeat(250) + ".xlsx";
        assertThat(name.getBytes(StandardCharsets.UTF_8)).hasSize(255);
        assertThat(ImportFileName.of(name).value()).isEqualTo(name);
    }

    @Test
    @DisplayName("rejects 256 UTF-8 bytes, also when it is only 128 UTF-16 units (64 emoji)")
    void rejectsOver255Bytes() {
        String ascii = "a".repeat(251) + ".xlsx";
        String emoji = "🙂".repeat(64);
        assertThat(emoji.getBytes(StandardCharsets.UTF_8)).hasSize(256);
        assertThat(emoji.length()).isEqualTo(128);

        assertThatThrownBy(() -> ImportFileName.of(ascii)).isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> ImportFileName.of(emoji)).isInstanceOf(ValidationException.class);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", ".", "..", "a/b.xlsx", "/products.xlsx", "imports/", "..\\x.xlsx", "a\\b.xlsx",
            "line\nbreak.xlsx", "tab\t.xlsx", "nul\u0000.xlsx", "del\u007f.xlsx", "nel\u0085.xlsx", "ls\u2028.xlsx",
            "ps\u2029.xlsx", "xlsx\u202e.exe", "zw\u200b.xlsx", "lone\ud800.xlsx", "lone\udc00.xlsx"})
    @DisplayName("rejects blank, . and .., separators, control/format/line/paragraph characters and lone surrogates")
    void rejectsInvalidNames(String name) {
        assertThatThrownBy(() -> ImportFileName.of(name))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("plain file name");
    }
}
