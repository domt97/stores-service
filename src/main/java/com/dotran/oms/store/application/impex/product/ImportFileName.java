package com.dotran.oms.store.application.impex.product;

import com.dotran.oms.core.exception.ValidationException;
import com.dotran.oms.store.common.constants.Constants;

import java.nio.charset.StandardCharsets;

/**
 * Name of a product import file inside the store's import folder (BR-CAT-16). The server places it at
 * {@code {merchantId}/{storeId}/imports/{name}} in its own bucket, so the name must be a plain file name: 1–255 UTF-8
 * bytes (S3 keys are at most 1024 bytes), no {@code /} or {@code \}, no control, format, line or paragraph separator
 * characters (Unicode Cc, Cf, Zl, Zp: they would break log lines or disguise the name), no unpaired surrogate, and not
 * {@code .} or {@code ..}. The name is not normalised: it must match the uploaded object's name byte for byte.
 * Anything else → {@link ValidationException} (400).
 */
public record ImportFileName(String value) {

    public static final int MAX_BYTES = 255;

    public ImportFileName {
        if (!isValid(value)) {
            throw new ValidationException(String.format(Constants.ERROR_MSG_IMPORT_FILE_NAME_INVALID, MAX_BYTES));
        }
    }

    public static ImportFileName of(String value) {
        return new ImportFileName(value);
    }

    private static boolean isValid(String value) {
        if (value == null || value.isEmpty() || ".".equals(value) || "..".equals(value)) {
            return false;
        }
        int bytes = value.getBytes(StandardCharsets.UTF_8).length;
        if (bytes > MAX_BYTES) {
            return false;
        }
        int i = 0;
        while (i < value.length()) {
            int cp = value.codePointAt(i);
            if (!isAllowed(cp)) {
                return false;
            }
            i += Character.charCount(cp);
        }
        return !value.isBlank();
    }

    private static boolean isAllowed(int cp) {
        if (cp == '/' || cp == '\\') {
            return false;
        }
        // A lone surrogate is returned by codePointAt as itself and has type SURROGATE; a valid pair is one code point.
        return switch (Character.getType(cp)) {
            case Character.CONTROL, Character.FORMAT, Character.LINE_SEPARATOR, Character.PARAGRAPH_SEPARATOR,
                 Character.SURROGATE -> false;
            default -> true;
        };
    }

    @Override
    public String toString() {
        return value;
    }
}
