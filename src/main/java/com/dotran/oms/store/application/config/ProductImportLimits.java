package com.dotran.oms.store.application.config;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

/**
 * Limits of the Excel product import (BR-CAT-13). Business limits (size, rows) are what a seller sees; the technical
 * budgets bound the heap one import can use, so that {@code permits} imports at once fit in the service. Imports of
 * different stores are independent; one store runs one import at a time.
 */
@Configuration(proxyBeanMethods = false)
@ConfigurationProperties(prefix = "app.import")
@Validated
@Getter
@Setter
public class ProductImportLimits {

    /** File size: 5 MB. */
    @Positive
    private long maxBytes = 5L * 1024 * 1024;

    /** Counted (non-blank) rows per sheet, header excluded. */
    @Positive
    private int maxProducts = 1_000;
    @Positive
    private int maxSkus = 10_000;
    @Positive
    private int maxImages = 20_000;

    /** Row elements per sheet, blank rows included, as a multiple of the sheet's row cap (bounds parsing work). */
    @Positive
    private int rowElementFactor = 10;

    /** Distinct shared strings in the file. */
    @Positive
    private int maxSharedStrings = 200_000;

    /** Characters of text in the whole file: shared strings, inline strings, values and formulas. */
    @Positive
    private long maxTextChars = 20_000_000L;

    /** Characters in one cell or one shared string. */
    @Positive
    private int maxCellChars = 10_000;

    /** Size of the styles part (XML), parsed into an in-memory tree. */
    @Positive
    private long maxStylesBytes = 5L * 1024 * 1024;

    /** Inflated size of any one zip entry (POI ZipSecureFile, JVM-wide); ~2× the largest part of a file at the limits. */
    @Positive
    private long maxZipEntryBytes = 16L * 1024 * 1024;

    /** Smallest compressed/inflated ratio of a zip entry (POI ZipSecureFile, JVM-wide): 0.01 = at most 100× inflation. */
    @Positive
    private double minInflateRatio = 0.01;

    /**
     * Imports running at the same time on one instance (all stores together). 0 = derived from the heap: one per
     * {@code heapPerImportBytes}, keeping one share free for the rest of the service, at least 1.
     */
    @PositiveOrZero
    private int permits = 0;

    /** Upper bound of the derived slot count: each import uses a database connection (pool of 10) and CPU. */
    @Positive
    private int maxPermits = 4;

    /** Imports allowed to wait for a slot at once; more get 503 at once (a waiting import holds a request thread). */
    @PositiveOrZero
    private int maxWaiting = 20;

    /** Heap one import at the limits may use (measured: ~110 MB for 1,000 products / 10,000 SKUs / 20,000 images). */
    @Positive
    private long heapPerImportBytes = 128L * 1024 * 1024;

    /** How long an import waits for a free slot before the server answers 503. */
    @PositiveOrZero
    private int slotWaitSeconds = 40;

    /** Retry-After sent with 503. */
    @Positive
    private int retryAfterSeconds = 10;

    /** The slot count to use: {@link #permits}, or derived from the max heap when it is 0 (at most {@link #maxPermits}). */
    public int effectivePermits(long maxHeapBytes) {
        if (permits > 0) {
            return permits;
        }
        return (int) Math.min(maxPermits, Math.max(1, maxHeapBytes / heapPerImportBytes - 1));
    }
}
