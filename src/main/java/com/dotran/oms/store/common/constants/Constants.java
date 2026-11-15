package com.dotran.oms.store.common.constants;

import lombok.experimental.UtilityClass;

@UtilityClass
public class Constants {

    // Resource
    public static final String MERCHANT = "Merchant";
    public static final String STORE = "Store";
    public static final String PRODUCT = "Product";

    // VND has no decimals: a VND price must be whole đồng. SKUs may carry other currencies (BR-MER-01, BR-STO-09).
    public static final String CURRENCY_VND = "VND";

    // Common error
    public static final String ERROR_MSG_NOT_FOUND = "Resource not found";

    // Store business error
    public static final String ERROR_MSG_MERCHANT_NOT_FOUND = "Merchant not found";
    public static final String ERROR_MSG_MERCHANT_CODE_ALREADY_EXISTS = "Merchant code already exists";
    public static final String ERROR_MSG_STORE_NOT_FOUND = "Store not found";
    public static final String ERROR_MSG_PRODUCT_NOT_FOUND = "Product not found";
    public static final String ERROR_MSG_SKU_CURRENCY_INVALID = "Currency must be a 3-letter code (SKU %s)";
    public static final String ERROR_MSG_SKU_PRICE_INVALID = "Price must be zero or more, below 10^16, with at most 2 decimals (SKU %s)";
    public static final String ERROR_MSG_SKU_PRICE_NOT_WHOLE_VND = "A VND price must be a whole number (SKU %s)";
    public static final String ERROR_MSG_PRODUCT_LIMITS = "A product has at most %d SKUs, %d images and a description of %d characters";
    public static final String ERROR_MSG_PRODUCT_NEEDS_SKU = "A product needs at least one SKU";
    public static final String ERROR_MSG_PRODUCT_NAME_REQUIRED = "Product name is required";
    public static final String ERROR_MSG_PRODUCT_NAME_TOO_LONG = "Product name has at most %d characters";
    public static final String ERROR_MSG_SKU_STATUS_REQUIRED = "A SKU status is required";
    public static final String ERROR_MSG_SKU_CODE_CONCURRENT = "Another product of the store is being saved with the same SKU code at the same time; try again";
    public static final String ERROR_MSG_PRODUCT_STATUS_REQUIRED = "Product status is required";
    public static final String ERROR_MSG_PRODUCT_STATUS_CHANGE = "A product cannot go from %s to %s";
    public static final String ERROR_MSG_PRODUCT_ACTIVE_NEEDS_ACTIVE_SKU = "An ACTIVE product needs at least one ACTIVE SKU";
    public static final String ERROR_MSG_SKU_CODE_REQUIRED = "A SKU code is required";
    public static final String ERROR_MSG_SKU_CODE_DUPLICATE = "SKU code %s appears more than once";
    public static final String ERROR_MSG_SKU_ID_MISMATCH = "The id sent for SKU %s is not the id of that SKU";
    public static final String ERROR_MSG_SKU_CODE_IN_USE = "SKU code(s) already used by another product of the store: %s";
    public static final String ERROR_MSG_PRODUCT_VERSION_CONFLICT = "The product was changed by someone else (current version %d); reload it and try again";
    public static final String ERROR_MSG_PRODUCT_CONCURRENT_UPDATE = "The product was changed by someone else at the same time; reload it and try again";
    public static final String ERROR_MSG_IMPORT_REFERENCE_REQUIRED = "Every row of the %s sheet needs a product reference";
    public static final String ERROR_MSG_IMPORT_REFERENCE_DUPLICATE = "Product reference %s appears more than once in the Product sheet";
    public static final String ERROR_MSG_IMPORT_FILE_TOO_LARGE = "The import file is larger than %d MB";
    public static final String ERROR_MSG_IMPORT_TOO_MANY_ROWS = "The %s sheet has more than %d rows";
    public static final String ERROR_MSG_IMPORT_TOO_MANY_ROW_ELEMENTS = "The %s sheet has more than %d rows including blank formatted rows; delete the unused rows";
    public static final String ERROR_MSG_IMPORT_TOO_MUCH_TEXT = "The import file has more text than allowed (%s)";
    public static final String ERROR_MSG_IMPORT_STYLES_TOO_LARGE = "The import file has too much formatting (styles part larger than %d MB)";
    public static final String ERROR_MSG_IMPORT_MISSING_SHEET = "The import file has no %s sheet";
    public static final String ERROR_MSG_IMPORT_BAD_HEADER = "Invalid header in sheet %s, column %d: expected '%s' but got '%s'";
    public static final String ERROR_MSG_IMPORT_BAD_VALUE = "Invalid %s in sheet %s, row %d, column %s";
    public static final String ERROR_MSG_IMPORT_UNREADABLE = "Unable to read Excel file";
    public static final String ERROR_MSG_IMPORT_BUSY = "The server is busy with other imports; try again in %d seconds";
    public static final String ERROR_MSG_IMPORT_STORE_RUNNING = "An import for this store is already running; wait for it to finish";
    public static final String ERROR_MSG_IMPORT_FILE_NAME_INVALID = "fileName must be a plain file name of at most %d bytes, without / or \\, control characters, or . or ..";
    public static final String ERROR_MSG_IMPORT_UPLOAD_SIZE_INVALID = "sizeBytes must be the file size, between 1 and %d bytes";
    public static final String ERROR_MSG_IMPORT_FILE_NOT_FOUND = "The import file %s was not found in this store's import folder";
    public static final String ERROR_MSG_IMPORT_FAILED = "The import failed because of an internal error; nothing was imported. Contact support if it persists.";
    public static final String ERROR_MSG_IMPORT_FIELD_TOO_LONG = "%s has at most %d characters (product reference %s)";
    public static final String ERROR_MSG_IMPORT_IMAGE_URL_REQUIRED = "Every image needs a URL (product reference %s)";
    public static final String ERROR_MSG_IMPORT_DIMENSION_INVALID = "%s must be zero or more, below 10^8, with at most 2 decimals (SKU %s)";
    public static final String ERROR_MSG_COLLECTION_NOT_FOUND = "Collection not found";
    public static final String ERROR_MSG_COLLECTION_PRODUCT_LIST_EMPTY = "Product list cannot be null or empty";
    public static final String ERROR_MSG_COLLECTION_CONCURRENT_CHANGE = "The collection was changed by another request at the same time; try again";
    public static final String ERROR_MSG_INVALID_SORT_FIELD = "sortBy must be one of createdAt, updatedAt, name, id";
    public static final String ERROR_MSG_INVALID_SORT_DIRECTION = "direction must be ASC or DESC";
    public static final String ERROR_MSG_INVALID_PAGE = "pageNumber must be 0 or more and pageSize 1 or more";
    public static final String ERROR_MSG_INVALID_PAGE_MAX = "pageNumber must be 0 or more and pageSize between 1 and %d";
    public static final String ERROR_MSG_PRODUCT_ID_REQUIRED = "Product id must not be null";
    public static final String ERROR_MSG_STORE_AVAILABILITY_NOT_FOUND = "Store availability not found";
    public static final String ERROR_MSG_STORE_ALREADY_CLOSED = "Store is already closed";
    public static final String ERROR_MSG_STORE_IS_NOT_CLOSED = "Store is not closed";
    public static final String ERROR_MSG_STORE_MISSING_BUSINESS_HOUR_CONFIG = "Missing business hour config for %s";

}
