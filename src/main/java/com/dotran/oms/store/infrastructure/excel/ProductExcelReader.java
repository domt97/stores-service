package com.dotran.oms.store.infrastructure.excel;

import com.dotran.oms.core.cloud.s3.S3ObjectStorageReader;
import com.dotran.oms.core.domain.id.CategoryId;
import com.dotran.oms.core.domain.id.SKU;
import com.dotran.oms.core.exception.BusinessException;
import com.dotran.oms.core.exception.ValidationException;
import com.dotran.oms.store.application.config.ProductImportLimits;
import com.dotran.oms.store.application.impex.parser.StoreProductImportReader;
import com.dotran.oms.store.application.impex.product.ProductImageImportRow;
import com.dotran.oms.store.application.impex.product.ProductImportData;
import com.dotran.oms.store.application.impex.product.ProductImportRow;
import com.dotran.oms.store.application.impex.product.ProductSKUImportRow;
import com.dotran.oms.store.application.impex.product.column.ProductImageColumn;
import com.dotran.oms.store.application.impex.product.column.ProductImportColumn;
import com.dotran.oms.store.application.impex.product.column.ProductSKUColumn;
import com.dotran.oms.store.common.constants.Constants;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ooxml.POIXMLException;
import org.apache.poi.openxml4j.exceptions.OpenXML4JException;
import org.apache.poi.openxml4j.exceptions.OpenXML4JRuntimeException;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.openxml4j.opc.PackageAccess;
import org.apache.poi.openxml4j.util.ZipSecureFile;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.util.RecordFormatException;
import org.apache.poi.util.XMLHelper;
import org.apache.poi.xssf.eventusermodel.XSSFReader;
import org.apache.poi.xssf.model.SharedStrings;
import org.apache.poi.xssf.model.StylesTable;
import org.springframework.stereotype.Component;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

/**
 * Reads the product import workbook from S3 without loading it into memory (BR-CAT-13): the object is copied to a temp
 * file (at most {@code maxBytes}), opened lazily, and only the Product, Product SKU and Product Image sheets are
 * streamed, with row caps and a text budget. What the client sees:
 * <ul>
 *   <li>400 — file too large, too many rows, too much text or formatting, missing sheet, bad header, a cell that is not
 *       a valid number / id (sheet and row named);</li>
 *   <li>422 — not an Excel (xlsx) file, corrupt file, zip bomb;</li>
 *   <li>500 — bucket or key missing, temp storage or network failure (not the client's file).</li>
 * </ul>
 */
@Component
@Slf4j
public class ProductExcelReader implements StoreProductImportReader {

    private static final int COPY_BUFFER = 64 * 1024;
    private static final String PRODUCT_SHEET = "Product";
    private static final String SKU_SHEET = "Product SKU";
    private static final String IMAGE_SHEET = "Product Image";

    private final S3ObjectStorageReader s3ObjectStorageReader;
    private final ProductImportLimits limits;

    public ProductExcelReader(S3ObjectStorageReader s3ObjectStorageReader, ProductImportLimits limits) {
        this.s3ObjectStorageReader = s3ObjectStorageReader;
        this.limits = limits;
        // JVM-wide POI settings (only this reader uses POI): inflated size of any zip entry, and the inflation ratio.
        ZipSecureFile.setMaxEntrySize(limits.getMaxZipEntryBytes());
        ZipSecureFile.setMinInflateRatio(limits.getMinInflateRatio());
    }

    @Override
    public ProductImportData read(String path, String fileName) {
        Path file = download(path, fileName);
        try {
            return parse(file);
        } finally {
            deleteQuietly(file);
        }
    }

    private Path download(String bucket, String key) {
        Path file;
        try {
            file = Files.createTempFile("product-import-", ".xlsx");
        } catch (IOException e) {
            throw new UncheckedIOException("Could not create a temp file for the import (java.io.tmpdir must be writable)", e);
        }
        try (InputStream in = s3ObjectStorageReader.read(bucket, key)) {
            if (in instanceof ResponseInputStream<?> response && response.response() instanceof GetObjectResponse object
                    && object.contentLength() != null && object.contentLength() > limits.getMaxBytes()) {
                response.abort(); // close() would download the rest of the object
                throw fileTooLarge();
            }
            if (copyAtMost(in, file, limits.getMaxBytes()) > limits.getMaxBytes()) {
                if (in instanceof ResponseInputStream<?> response) {
                    response.abort();
                }
                throw fileTooLarge();
            }
            return file;
        } catch (IOException e) {
            deleteQuietly(file);
            throw new UncheckedIOException("Could not download the import file", e);
        } catch (RuntimeException e) {
            deleteQuietly(file);
            throw e;
        }
    }

    /** Copies at most {@code max + 1} bytes and returns how many were copied (more than {@code max} = too large). */
    private static long copyAtMost(InputStream in, Path file, long max) throws IOException {
        long copied = 0;
        byte[] buffer = new byte[COPY_BUFFER];
        try (OutputStream out = Files.newOutputStream(file)) {
            int read;
            while (copied <= max && (read = in.read(buffer, 0, (int) Math.min(buffer.length, max + 1 - copied))) != -1) {
                out.write(buffer, 0, read);
                copied += read;
            }
        }
        return copied;
    }

    private ProductImportData parse(Path file) {
        TextBudget budget = new TextBudget(limits.getMaxTextChars(), limits.getMaxCellChars());
        SheetRows products = new SheetRows(PRODUCT_SHEET, columnNames(ProductImportColumn.values(), ProductImportColumn::getColumnName),
                ProductImportColumn.CATEGORY.getIndex() + 1, limits.getMaxProducts(), limits.getRowElementFactor());
        SheetRows skus = new SheetRows(SKU_SHEET, columnNames(ProductSKUColumn.values(), ProductSKUColumn::getColumnName),
                ProductSKUColumn.values().length, limits.getMaxSkus(), limits.getRowElementFactor());
        SheetRows images = new SheetRows(IMAGE_SHEET, columnNames(ProductImageColumn.values(), ProductImageColumn::getColumnName),
                ProductImageColumn.values().length, limits.getMaxImages(), limits.getRowElementFactor());
        List<SheetRows> sheets = List.of(products, skus, images);

        OPCPackage pkg = null;
        try {
            checkZipDirectory(file);
            pkg = OPCPackage.open(file.toFile(), PackageAccess.READ);
            XSSFReader reader = new XSSFReader(pkg);
            StylesTable styles = readStyles(reader);
            SharedStrings strings = readSharedStrings(reader, budget);
            DataFormatter formatter = new DataFormatter();

            XSSFReader.SheetIterator it = reader.getSheetIterator();
            while (it.hasNext()) {
                try (InputStream sheetData = it.next()) {
                    String name = it.getSheetName();
                    // case-insensitive, as XSSFWorkbook.getSheet in the previous reader
                    SheetRows target = sheets.stream().filter(s -> s.sheetName().equalsIgnoreCase(name)).findFirst().orElse(null);
                    if (target == null || target.seen()) {
                        continue; // other sheets are never read
                    }
                    target.markSeen();
                    XMLReader xml = XMLHelper.newXMLReader();
                    xml.setContentHandler(new BudgetedSheetHandler(styles, strings, target, formatter, budget));
                    xml.parse(new InputSource(sheetData));
                }
            }
            for (SheetRows sheet : sheets) {
                if (!sheet.seen()) {
                    throw new ImportFileException(String.format(Constants.ERROR_MSG_IMPORT_MISSING_SHEET, sheet.sheetName()));
                }
            }
            return ProductImportData.builder()
                    .products(products.rows().stream().map(ProductExcelReader::toProduct).toList())
                    .skus(skus.rows().stream().map(ProductExcelReader::toSku).toList())
                    .images(images.rows().stream().map(ProductExcelReader::toImage).toList())
                    .build();
        } catch (ImportFileException e) {
            throw new ValidationException(e.getMessage());
        } catch (SAXException e) {
            ImportFileException cause = findImportFileException(e);
            if (cause != null) {
                throw new ValidationException(cause.getMessage());
            }
            throw unreadable(e);
        } catch (OpenXML4JException | OpenXML4JRuntimeException | POIXMLException | RecordFormatException | IOException
                 | IllegalArgumentException | IndexOutOfBoundsException e) {
            // IllegalArgumentException covers POI's UnsupportedFileFormatException (not an xlsx) and EmptyFileException
            // malformed content (bad cell reference, row number, style or shared-string index, empty file) is 422 too
            ImportFileException cause = findImportFileException(e);
            if (cause != null) {
                throw new ValidationException(cause.getMessage());
            }
            throw unreadable(e);
        } catch (ParserConfigurationException e) {
            throw new IllegalStateException("XML parser not available", e);
        } finally {
            if (pkg != null) {
                pkg.revert(); // read-only package: revert, not close
            }
        }
    }

    /**
     * Opens the zip's central directory once. POI falls back to reading every entry into memory when the directory is
     * unreadable; such a file is refused (422) instead.
     */
    private static void checkZipDirectory(Path file) throws IOException {
        try (ZipSecureFile zip = new ZipSecureFile(file.toFile())) {
            if (!zip.getEntries().hasMoreElements()) {
                throw new IOException("empty zip");
            }
        }
    }

    private StylesTable readStyles(XSSFReader reader) throws IOException {
        byte[] xml;
        try (InputStream in = reader.getStylesData()) {
            xml = in.readNBytes((int) Math.min(Integer.MAX_VALUE - 8, limits.getMaxStylesBytes() + 1));
        } catch (OpenXML4JException | POIXMLException e) {
            return null; // no styles part: cells have no styles to apply
        }
        if (xml.length > limits.getMaxStylesBytes()) {
            throw new ImportFileException(String.format(Constants.ERROR_MSG_IMPORT_STYLES_TOO_LARGE,
                    limits.getMaxStylesBytes() / (1024 * 1024)));
        }
        return new StylesTable(new ByteArrayInputStream(xml));
    }

    private SharedStrings readSharedStrings(XSSFReader reader, TextBudget budget)
            throws IOException, SAXException, ParserConfigurationException {
        InputStream sst;
        try {
            sst = reader.getSharedStringsData();
        } catch (OpenXML4JException | POIXMLException e) {
            sst = null; // no shared strings part
        }
        try (InputStream in = sst) {
            return new BudgetedSharedStrings(in, limits.getMaxSharedStrings(), budget);
        }
    }

    private static ProductImportRow toProduct(SheetRows.Row row) {
        return ProductImportRow.builder()
                .reference(row.get(ProductImportColumn.REFERENCE.getIndex()))
                .name(row.get(ProductImportColumn.NAME.getIndex()))
                .description(row.get(ProductImportColumn.DESCRIPTION.getIndex()))
                .thumbnailUrl(row.get(ProductImportColumn.THUMBNAIL_URL.getIndex()))
                .categoryId(CategoryId.of(uuid(row, PRODUCT_SHEET, ProductImportColumn.CATEGORY.getIndex(),
                        ProductImportColumn.CATEGORY.getColumnName())))
                .build();
    }

    private static ProductSKUImportRow toSku(SheetRows.Row row) {
        return ProductSKUImportRow.builder()
                .productRef(row.get(ProductSKUColumn.PRODUCT_REF.getIndex()))
                .sku(SKU.of(row.get(ProductSKUColumn.SKU.getIndex())))
                .name(row.get(ProductSKUColumn.NAME.getIndex()))
                .price(decimal(row, ProductSKUColumn.PRICE))
                .currency(row.get(ProductSKUColumn.CURRENCY.getIndex()))
                .weight(decimal(row, ProductSKUColumn.WEIGHT))
                .length(decimal(row, ProductSKUColumn.LENGTH))
                .width(decimal(row, ProductSKUColumn.WIDTH))
                .height(decimal(row, ProductSKUColumn.HEIGHT))
                .build();
    }

    private static ProductImageImportRow toImage(SheetRows.Row row) {
        return ProductImageImportRow.builder()
                .productRef(row.get(ProductImageColumn.PRODUCT_REF.getIndex()))
                .imageUrl(row.get(ProductImageColumn.URL.getIndex()))
                .build();
    }

    private static UUID uuid(SheetRows.Row row, String sheet, int column, String columnName) {
        String value = row.get(column);
        try {
            return value == null ? null : UUID.fromString(value);
        } catch (IllegalArgumentException e) {
            throw new ImportFileException(String.format(Constants.ERROR_MSG_IMPORT_BAD_VALUE, "id", sheet, row.number(), columnName));
        }
    }

    private static BigDecimal decimal(SheetRows.Row row, ProductSKUColumn column) {
        String value = row.get(column.getIndex());
        try {
            return value == null ? null : new BigDecimal(value);
        } catch (NumberFormatException e) {
            throw new ImportFileException(String.format(Constants.ERROR_MSG_IMPORT_BAD_VALUE, "number", SKU_SHEET,
                    row.number(), column.getColumnName()));
        }
    }

    private static <E> List<String> columnNames(E[] columns, Function<E, String> name) {
        return Arrays.stream(columns).map(name).toList();
    }

    private ValidationException fileTooLarge() {
        return new ValidationException(String.format(Constants.ERROR_MSG_IMPORT_FILE_TOO_LARGE, limits.getMaxBytes() / (1024 * 1024)));
    }

    private static BusinessException unreadable(Exception e) {
        log.warn("Unable to read Excel file: {}", e.toString());
        return new BusinessException(Constants.ERROR_MSG_IMPORT_UNREADABLE);
    }

    private static ImportFileException findImportFileException(Throwable e) {
        for (Throwable t = e; t != null; t = t.getCause() != t ? t.getCause() : null) {
            if (t instanceof ImportFileException ife) {
                return ife;
            }
            if (t instanceof SAXException sax && sax.getException() instanceof ImportFileException ife) {
                return ife;
            }
        }
        return null;
    }

    private static void deleteQuietly(Path file) {
        try {
            Files.deleteIfExists(file);
        } catch (IOException e) {
            log.warn("Could not delete import temp file {}", file, e);
        }
    }
}
