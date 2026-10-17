package com.dotran.oms.store.infrastructure.excel;

import com.dotran.oms.core.cloud.s3.S3ObjectStorageReader;
import com.dotran.oms.core.exception.BusinessException;
import com.dotran.oms.core.exception.ImportValidationException;
import com.dotran.oms.core.exception.ValidationException;
import com.dotran.oms.core.io.excel.ExcelValidator;
import com.dotran.oms.store.application.impex.product.ProductImportData;
import com.dotran.oms.store.application.impex.product.column.ProductImageColumn;
import com.dotran.oms.store.application.impex.product.column.ProductImportColumn;
import com.dotran.oms.store.application.impex.product.column.ProductSKUColumn;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProductExcelReader")
class ProductExcelReaderTest {

    @Mock
    private S3ObjectStorageReader s3ObjectStorageReader;

    @Mock
    private ExcelValidator excelValidator;

    @InjectMocks
    private ProductExcelReader reader;

    @Test
    @DisplayName("read: throws BusinessException when InputStream is unreadable")
    void read_throwsBusinessException_whenIOExceptionOccurs() throws Exception {
        InputStream brokenStream = new InputStream() {
            @Override
            public int read() throws IOException {
                throw new IOException("Broken stream");
            }
        };
        when(s3ObjectStorageReader.read("bucket", "file.xlsx")).thenReturn(brokenStream);

        assertThatThrownBy(() -> reader.read("bucket", "file.xlsx"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Unable to read Excel file");
    }

    @Test
    @DisplayName("read: throws ValidationException when header validation fails")
    void read_throwsValidationException_whenImportValidationExceptionOccurs() throws Exception {
        byte[] workbookBytes = createMinimalWorkbook();
        when(s3ObjectStorageReader.read("bucket", "file.xlsx")).thenReturn(new ByteArrayInputStream(workbookBytes));
        doThrow(new ImportValidationException("Bad header")).when(excelValidator).validateHeader(any(), any());

        assertThatThrownBy(() -> reader.read("bucket", "file.xlsx"))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Import Validation failed");
    }

    @Test
    @DisplayName("read: returns ProductImportData with parsed rows when workbook is valid")
    void read_returnsProductImportData_forValidWorkbook() throws Exception {
        byte[] workbookBytes = createWorkbookWithData();
        when(s3ObjectStorageReader.read("bucket", "file.xlsx")).thenReturn(new ByteArrayInputStream(workbookBytes));

        ProductImportData result = reader.read("bucket", "file.xlsx");

        assertThat(result).isNotNull();
        assertThat(result.getProducts()).hasSize(1);
        assertThat(result.getSkus()).hasSize(1);
        assertThat(result.getImages()).hasSize(1);
    }

    private byte[] createMinimalWorkbook() throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            workbook.createSheet("Product");
            workbook.createSheet("Product SKU");
            workbook.createSheet("Product Image");
            workbook.write(bos);
            return bos.toByteArray();
        }
    }

    private byte[] createWorkbookWithData() throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream bos = new ByteArrayOutputStream()) {

            XSSFSheet productSheet = workbook.createSheet("Product");
            XSSFRow productHeader = productSheet.createRow(0);
            for (ProductImportColumn col : ProductImportColumn.values()) {
                productHeader.createCell(col.getIndex()).setCellValue(col.getColumnName());
            }
            XSSFRow productData = productSheet.createRow(1);
            productData.createCell(ProductImportColumn.REFERENCE.getIndex()).setCellValue("REF001");
            productData.createCell(ProductImportColumn.NAME.getIndex()).setCellValue("Test Product");
            productData.createCell(ProductImportColumn.DESCRIPTION.getIndex()).setCellValue("Description");
            productData.createCell(ProductImportColumn.THUMBNAIL_URL.getIndex()).setCellValue("http://img.com");
            productData.createCell(ProductImportColumn.CATEGORY.getIndex()).setCellValue(UUID.randomUUID().toString());
            productData.createCell(ProductImportColumn.BRAND_ID.getIndex()).setCellValue(UUID.randomUUID().toString());

            XSSFSheet skuSheet = workbook.createSheet("Product SKU");
            XSSFRow skuHeader = skuSheet.createRow(0);
            for (ProductSKUColumn col : ProductSKUColumn.values()) {
                skuHeader.createCell(col.getIndex()).setCellValue(col.getColumnName());
            }
            XSSFRow skuData = skuSheet.createRow(1);
            skuData.createCell(ProductSKUColumn.PRODUCT_REF.getIndex()).setCellValue("REF001");
            skuData.createCell(ProductSKUColumn.SKU.getIndex()).setCellValue("SKU001");
            skuData.createCell(ProductSKUColumn.NAME.getIndex()).setCellValue("SKU Name");
            skuData.createCell(ProductSKUColumn.PRICE.getIndex()).setCellValue(10.0);
            skuData.createCell(ProductSKUColumn.CURRENCY.getIndex()).setCellValue("USD");
            skuData.createCell(ProductSKUColumn.WEIGHT.getIndex()).setCellValue(1.0);
            skuData.createCell(ProductSKUColumn.LENGTH.getIndex()).setCellValue(2.0);
            skuData.createCell(ProductSKUColumn.WIDTH.getIndex()).setCellValue(3.0);
            skuData.createCell(ProductSKUColumn.HEIGHT.getIndex()).setCellValue(4.0);

            XSSFSheet imageSheet = workbook.createSheet("Product Image");
            XSSFRow imageHeader = imageSheet.createRow(0);
            for (ProductImageColumn col : ProductImageColumn.values()) {
                imageHeader.createCell(col.getIndex()).setCellValue(col.getColumnName());
            }
            XSSFRow imageData = imageSheet.createRow(1);
            imageData.createCell(ProductImageColumn.PRODUCT_REF.getIndex()).setCellValue("REF001");
            imageData.createCell(ProductImageColumn.URL.getIndex()).setCellValue("http://image.com/1.jpg");

            workbook.write(bos);
            return bos.toByteArray();
        }
    }
}
