package com.dotran.oms.store.infrastructure.excel;

import com.dotran.oms.core.cloud.s3.S3ObjectStorageReader;
import com.dotran.oms.core.exception.BusinessException;
import com.dotran.oms.core.exception.ValidationException;
import com.dotran.oms.store.application.config.ProductImportLimits;
import com.dotran.oms.store.application.impex.product.ProductImportData;
import com.dotran.oms.store.application.impex.product.column.ProductImageColumn;
import com.dotran.oms.store.application.impex.product.column.ProductImportColumn;
import com.dotran.oms.store.application.impex.product.column.ProductSKUColumn;
import org.apache.poi.openxml4j.util.ZipSecureFile;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFRichTextString;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.http.Abortable;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProductExcelReader")
class ProductExcelReaderTest {

    @Mock private S3ObjectStorageReader s3ObjectStorageReader;
    @Mock private Abortable abortable;

    private long zipMaxEntrySize;
    private double zipMinInflateRatio;
    private ProductImportLimits limits;

    @BeforeEach
    void setUp() {
        zipMaxEntrySize = ZipSecureFile.getMaxEntrySize(); // the reader sets them JVM-wide
        zipMinInflateRatio = ZipSecureFile.getMinInflateRatio();
        limits = new ProductImportLimits();
    }

    @AfterEach
    void restoreZipSettings() throws IOException {
        ZipSecureFile.setMaxEntrySize(zipMaxEntrySize);
        ZipSecureFile.setMinInflateRatio(zipMinInflateRatio);
        try (var temp = Files.list(Path.of(System.getProperty("java.io.tmpdir")))) {
            assertThat(temp.map(f -> f.getFileName().toString()).filter(n -> n.startsWith("product-import-")))
                    .as("temp files are deleted on every path").isEmpty();
        }
    }

    private ProductImportData read(byte[] file) {
        when(s3ObjectStorageReader.read("bucket", "file.xlsx")).thenReturn(new ByteArrayInputStream(file));
        return new ProductExcelReader(s3ObjectStorageReader, limits).read("bucket", "file.xlsx");
    }

    private void assertRejected(byte[] file, String messagePart) {
        assertThatThrownBy(() -> read(file)).isInstanceOf(ValidationException.class).hasMessageContaining(messagePart);
    }

    // ---- workbook builders -------------------------------------------------------------------------------------

    /** A valid workbook with {@code products} products, one SKU and one image each; {@code extra} edits it before writing. */
    private static byte[] workbook(int products, Consumer<XSSFWorkbook> extra) {
        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet p = header(wb, "Product", columns(ProductImportColumn.values()));
            Sheet s = header(wb, "Product SKU", columns(ProductSKUColumn.values()));
            Sheet i = header(wb, "Product Image", columns(ProductImageColumn.values()));
            for (int n = 1; n <= products; n++) {
                row(p, n, "REF-" + n, "Product " + n, "Description", "https://img/" + n + ".jpg", UUID.randomUUID().toString());
                Row sku = row(s, n, "REF-" + n, "SKU-" + n, "Default");
                sku.createCell(ProductSKUColumn.PRICE.getIndex()).setCellValue(25000);
                sku.createCell(ProductSKUColumn.CURRENCY.getIndex()).setCellValue("VND");
                sku.createCell(ProductSKUColumn.WEIGHT.getIndex()).setCellValue(1.5);
                row(i, n, "REF-" + n, "https://img/" + n + "-1.jpg");
            }
            extra.accept(wb);
            wb.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static byte[] workbook(int products) {
        return workbook(products, wb -> { });
    }

    private static String[] columns(Enum<?>[] values) {
        String[] names = new String[values.length];
        for (int k = 0; k < values.length; k++) {
            Object v = values[k];
            names[k] = v instanceof ProductImportColumn c ? c.getColumnName()
                    : v instanceof ProductSKUColumn c ? c.getColumnName() : ((ProductImageColumn) v).getColumnName();
        }
        return names;
    }

    private static Sheet header(Workbook wb, String name, String... names) {
        Sheet sheet = wb.createSheet(name);
        row(sheet, 0, names);
        return sheet;
    }

    private static Row row(Sheet sheet, int index, String... values) {
        Row row = sheet.createRow(index);
        for (int k = 0; k < values.length; k++) {
            if (values[k] != null) {
                row.createCell(k).setCellValue(values[k]);
            }
        }
        return row;
    }

    /** Rewrites one zip entry of an xlsx (to build files POI itself would not write). */
    private static byte[] rewriteEntry(byte[] xlsx, String entry, java.util.function.UnaryOperator<String> change) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ZipInputStream in = new ZipInputStream(new ByteArrayInputStream(xlsx)); ZipOutputStream zip = new ZipOutputStream(out)) {
            for (ZipEntry e = in.getNextEntry(); e != null; e = in.getNextEntry()) {
                byte[] data = in.readAllBytes();
                if (e.getName().equals(entry)) {
                    data = change.apply(new String(data, StandardCharsets.UTF_8)).getBytes(StandardCharsets.UTF_8);
                }
                zip.putNextEntry(new ZipEntry(e.getName()));
                zip.write(data);
                zip.closeEntry();
            }
        }
        return out.toByteArray();
    }

    // ---- reading ---------------------------------------------------------------------------------------------------

    @Test
    @DisplayName("reads the three sheets: values trimmed, numbers and ids parsed, Brand Id ignored")
    void readsValidWorkbook() {
        ProductImportData data = read(workbook(2));

        assertThat(data.getProducts()).hasSize(2);
        assertThat(data.getProducts().get(0).getReference()).isEqualTo("REF-1");
        assertThat(data.getProducts().get(0).getCategoryId().getValue()).isNotNull();
        assertThat(data.getSkus()).hasSize(2);
        assertThat(data.getSkus().get(0).getSku().getValue()).isEqualTo("SKU-1");
        assertThat(data.getSkus().get(0).getPrice()).isEqualByComparingTo("25000");
        assertThat(data.getSkus().get(0).getWeight()).isEqualByComparingTo("1.5");
        assertThat(data.getImages()).hasSize(2);
    }

    @Test
    @DisplayName("fully blank rows (formatted, or with a value only in Brand Id) are skipped and not counted")
    void blankRows_skipped_notCounted() {
        limits.setMaxProducts(1);
        byte[] file = workbook(1, wb -> {
            Sheet p = wb.getSheet("Product");
            CellStyle bold = wb.createCellStyle();
            Row formatted = p.createRow(2);
            formatted.createCell(0).setCellStyle(bold);
            formatted.createCell(1).setCellValue("   ");
            p.createRow(3).createCell(ProductImportColumn.BRAND_ID.getIndex()).setCellValue("brand");
        });

        ProductImportData data = read(file);

        assertThat(data.getProducts()).hasSize(1);
    }

    @Test
    @DisplayName("formula cells give their cached result")
    void formula_cachedValue() {
        byte[] file = workbook(1, wb -> {
            Row sku = wb.getSheet("Product SKU").getRow(1);
            sku.getCell(ProductSKUColumn.PRICE.getIndex()).setCellFormula("20000+5000");
            wb.getCreationHelper().createFormulaEvaluator().evaluateAll();
        });

        assertThat(read(file).getSkus().get(0).getPrice()).isEqualByComparingTo("25000");
    }

    @Test
    @DisplayName("rich-text shared strings are joined; phonetic runs are skipped")
    void richText_andPhonetic() throws IOException {
        byte[] file = workbook(1, wb -> {
            XSSFRichTextString name = new XSSFRichTextString("Rich name");
            XSSFFont bold = wb.createFont();
            bold.setBold(true);
            name.applyFont(0, 4, bold);
            wb.getSheet("Product").getRow(1).getCell(ProductImportColumn.NAME.getIndex()).setCellValue(name);
        });
        byte[] withPhonetic = rewriteEntry(file, "xl/sharedStrings.xml",
                xml -> xml.replaceFirst("<t>REF-1</t>", "<t>REF-1</t><rPh sb=\"0\" eb=\"1\"><t>IGNORED</t></rPh>"));

        ProductImportData data = read(withPhonetic);

        assertThat(data.getProducts().get(0).getName()).isEqualTo("Rich name");
        assertThat(data.getProducts().get(0).getReference()).isEqualTo("REF-1");
    }

    @Test
    @DisplayName("cells without an r attribute are read by position")
    void cellsWithoutReference() throws IOException {
        byte[] file = rewriteEntry(workbook(1), "xl/worksheets/sheet2.xml", xml -> xml.replaceAll(" r=\"[A-Z]+[0-9]+\"", ""));

        ProductImportData data = read(file);

        assertThat(data.getSkus().get(0).getSku().getValue()).isEqualTo("SKU-1");
        assertThat(data.getSkus().get(0).getPrice()).isEqualByComparingTo("25000");
    }

    // ---- size ------------------------------------------------------------------------------------------------------

    @Test
    @DisplayName("size: exactly the cap passes, one byte less → 400")
    void size_atCap_passes_overCap_400() {
        byte[] file = workbook(1);
        limits.setMaxBytes(file.length);
        assertThat(read(file).getProducts()).hasSize(1);

        limits.setMaxBytes(file.length - 1);
        assertRejected(file, "larger than");
    }

    @Test
    @DisplayName("size: S3 content length over the cap → 400 and the download is aborted, nothing parsed")
    void size_contentLength_abort() {
        limits.setMaxBytes(1000);
        InputStream body = new ByteArrayInputStream(new byte[10]);
        ResponseInputStream<GetObjectResponse> response = new ResponseInputStream<>(
                GetObjectResponse.builder().contentLength(5000L).build(), AbortableInputStream.create(body, abortable));
        when(s3ObjectStorageReader.read("bucket", "file.xlsx")).thenReturn(response);

        assertThatThrownBy(() -> new ProductExcelReader(s3ObjectStorageReader, limits).read("bucket", "file.xlsx"))
                .isInstanceOf(ValidationException.class).hasMessageContaining("larger than");
        verify(abortable).abort();
    }

    @Test
    @DisplayName("size: no content length, more bytes than the cap → 400 and the download is aborted")
    void size_streamOverCap_abort() {
        limits.setMaxBytes(1000);
        ResponseInputStream<GetObjectResponse> response = new ResponseInputStream<>(
                GetObjectResponse.builder().build(), AbortableInputStream.create(new ByteArrayInputStream(new byte[5000]), abortable));
        when(s3ObjectStorageReader.read("bucket", "file.xlsx")).thenReturn(response);

        assertThatThrownBy(() -> new ProductExcelReader(s3ObjectStorageReader, limits).read("bucket", "file.xlsx"))
                .isInstanceOf(ValidationException.class).hasMessageContaining("larger than");
        verify(abortable).abort();
    }

    // ---- rows ------------------------------------------------------------------------------------------------------

    @Test
    @DisplayName("rows: each sheet at its cap passes, one more → 400 naming the sheet (header not counted)")
    void rows_capPerSheet() {
        limits.setMaxProducts(3);
        limits.setMaxSkus(3);
        limits.setMaxImages(3);
        assertThat(read(workbook(3)).getProducts()).hasSize(3);

        limits.setMaxProducts(2);
        assertRejected(workbook(3), "Product sheet has more than 2 rows");

        limits.setMaxProducts(3);
        limits.setMaxSkus(2);
        assertRejected(workbook(3), "Product SKU sheet has more than 2 rows");

        limits.setMaxSkus(3);
        limits.setMaxImages(2);
        assertRejected(workbook(3), "Product Image sheet has more than 2 rows");
    }

    @Test
    @DisplayName("rows: orphan rows (no matching product) count toward their sheet's cap")
    void rows_orphansCount() {
        limits.setMaxImages(1);
        byte[] file = workbook(1, wb -> row(wb.getSheet("Product Image"), 2, "REF-ORPHAN", "https://img/x.jpg"));

        assertRejected(file, "Product Image sheet has more than 1 rows");
    }

    @Test
    @DisplayName("rows: too many row elements, blank ones included → 400")
    void rows_elementsOverFactor() {
        limits.setMaxProducts(2);
        limits.setRowElementFactor(2); // at most 4 row elements in the Product sheet
        byte[] file = workbook(1, wb -> {
            Sheet p = wb.getSheet("Product");
            for (int r = 2; r <= 5; r++) {
                p.createRow(r).createCell(0).setCellStyle(wb.createCellStyle());
            }
        });

        assertRejected(file, "Product sheet has more than 4 rows including blank formatted rows");
    }

    // ---- text and styles budget --------------------------------------------------------------------------------

    @Test
    @DisplayName("text: more shared strings than allowed → 400")
    void text_sharedStringCount() {
        limits.setMaxSharedStrings(5);
        assertRejected(workbook(3), "distinct texts");
    }

    @Test
    @DisplayName("text: more characters in total than allowed → 400")
    void text_totalChars() {
        limits.setMaxTextChars(50);
        assertRejected(workbook(3), "characters in total");
    }

    @Test
    @DisplayName("text: a shared string longer than the cell cap → 400")
    void text_sharedStringTooLong() {
        limits.setMaxCellChars(20);
        byte[] file = workbook(1, wb -> wb.getSheet("Product").getRow(1)
                .getCell(ProductImportColumn.DESCRIPTION.getIndex()).setCellValue("d".repeat(21)));

        assertRejected(file, "a cell has more than 20 characters");
    }

    @Test
    @DisplayName("text: an inline-string cell longer than the cell cap → 400 (streamed workbooks write inline strings)")
    void text_inlineStringTooLong() throws IOException {
        limits.setMaxCellChars(20);
        byte[] file;
        try (SXSSFWorkbook wb = new SXSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet p = header(wb, "Product", columns(ProductImportColumn.values()));
            header(wb, "Product SKU", columns(ProductSKUColumn.values()));
            header(wb, "Product Image", columns(ProductImageColumn.values()));
            row(p, 1, "REF-1", "n".repeat(21));
            wb.write(out);
            wb.dispose();
            file = out.toByteArray();
        }

        assertRejected(file, "a cell has more than 20 characters");
    }

    @Test
    @DisplayName("text: formula text longer than the cell cap → 400")
    void text_formulaTooLong() {
        limits.setMaxCellChars(30);
        byte[] file = workbook(1, wb -> wb.getSheet("Product SKU").getRow(1)
                .getCell(ProductSKUColumn.PRICE.getIndex()).setCellFormula("1" + "+1".repeat(20)));

        assertRejected(file, "a cell has more than 30 characters");
    }

    @Test
    @DisplayName("styles: a styles part larger than allowed → 400")
    void styles_tooLarge() {
        limits.setMaxStylesBytes(100);
        assertRejected(workbook(1), "too much formatting");
    }

    // ---- layout --------------------------------------------------------------------------------------------------

    @Test
    @DisplayName("layout: a missing sheet → 400 naming it")
    void layout_missingSheet() {
        byte[] file = workbook(1, wb -> wb.removeSheetAt(wb.getSheetIndex("Product Image")));
        assertRejected(file, "no Product Image sheet");
    }

    @Test
    @DisplayName("layout: a wrong header → 400 naming sheet and column")
    void layout_badHeader() {
        byte[] file = workbook(1, wb -> wb.getSheet("Product SKU").getRow(0).getCell(3).setCellValue("Cost"));
        assertRejected(file, "Invalid header in sheet Product SKU, column 4: expected 'Price' but got 'Cost'");
    }

    @Test
    @DisplayName("layout: a header that is not on row 1 → 400")
    void layout_headerNotOnFirstRow() {
        byte[] file = workbook(1, wb -> {
            Sheet images = wb.getSheet("Product Image");
            images.shiftRows(0, images.getLastRowNum(), 1);
        });
        assertRejected(file, "Invalid header in sheet Product Image, column 1");
    }

    @Test
    @DisplayName("values: an invalid id or number → 400 naming sheet, row and column")
    void values_invalid() {
        byte[] badUuid = workbook(2, wb -> wb.getSheet("Product").getRow(2)
                .getCell(ProductImportColumn.CATEGORY.getIndex()).setCellValue("not-a-uuid"));
        assertRejected(badUuid, "Invalid id in sheet Product, row 3, column Category");

        byte[] badNumber = workbook(1, wb -> wb.getSheet("Product SKU").getRow(1)
                .getCell(ProductSKUColumn.PRICE.getIndex()).setCellValue("abc"));
        assertRejected(badNumber, "Invalid number in sheet Product SKU, row 2, column Price");
    }

    @Test
    @DisplayName("sheet names match case-insensitively (as before), and other sheets are never read")
    void sheetNames_caseInsensitive_otherSheetsNotRead() throws IOException {
        byte[] file = workbook(1, wb -> {
            wb.setSheetName(wb.getSheetIndex("Product Image"), "product image");
            wb.createSheet("Notes").createRow(0).createCell(0).setCellValue("ignored");
        });
        // break the Notes sheet: if it were parsed, the import would fail
        byte[] broken = rewriteEntry(file, "xl/worksheets/sheet4.xml", xml -> "<not-xml");

        assertThat(read(broken).getImages()).hasSize(1);
    }

    // ---- unreadable / infrastructure --------------------------------------------------------------------------

    @Test
    @DisplayName("not an Excel file → 422 Unable to read Excel file")
    void notExcel_422() {
        assertThatThrownBy(() -> read("this is not a zip".getBytes(StandardCharsets.UTF_8)))
                .isInstanceOf(BusinessException.class)
                .isNotInstanceOf(ValidationException.class)
                .hasMessage("Unable to read Excel file");
    }

    @Test
    @DisplayName("malformed or unsafe packages → 422: empty file, no zip directory, oversized entry, bad cell reference, bad shared-string index")
    void malformed_422() throws IOException {
        byte[] valid = workbook(1);
        assertUnreadable(new byte[0]);
        assertUnreadable(java.util.Arrays.copyOf(valid, valid.length - 22)); // end-of-central-directory record cut off
        assertUnreadable(rewriteEntry(valid, "xl/worksheets/sheet2.xml", xml -> xml.replaceFirst(" r=\"A2\"", " r=\"A9999999999\"")));
        assertUnreadable(rewriteEntry(valid, "xl/worksheets/sheet1.xml",
                xml -> xml.replaceFirst("(<c r=\"A2\"[^>]*t=\"s\"[^>]*><v>)\\d+(</v>)", "$199999$2")));

        limits.setMaxZipEntryBytes(500); // every sheet part is larger
        assertUnreadable(valid);
    }

    private void assertUnreadable(byte[] file) {
        assertThatThrownBy(() -> read(file))
                .isInstanceOf(BusinessException.class)
                .isNotInstanceOf(ValidationException.class)
                .hasMessage("Unable to read Excel file");
    }

    @Test
    @DisplayName("a broken download stream is an infrastructure error (500), not the client's file")
    void brokenStream_500() {
        InputStream broken = new InputStream() {
            @Override
            public int read() throws IOException {
                throw new IOException("connection reset");
            }

            @Override
            public int read(byte[] b, int off, int len) throws IOException {
                throw new IOException("connection reset");
            }
        };
        when(s3ObjectStorageReader.read("bucket", "file.xlsx")).thenReturn(broken);

        assertThatThrownBy(() -> new ProductExcelReader(s3ObjectStorageReader, limits).read("bucket", "file.xlsx"))
                .isInstanceOf(UncheckedIOException.class);
    }
}
