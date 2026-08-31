package com.dotran.example.store.infrastructure.excel;

import com.dotran.example.store.application.impex.parser.StoreProductImportReader;
import com.dotran.example.store.application.impex.product.ProductImageImportRow;
import com.dotran.example.store.application.impex.product.ProductImportData;
import com.dotran.example.store.application.impex.product.ProductImportRow;
import com.dotran.example.store.application.impex.product.ProductSKUImportRow;
import com.dotran.example.store.application.impex.product.column.ProductImageColumn;
import com.dotran.example.store.application.impex.product.column.ProductImportColumn;
import com.dotran.example.store.application.impex.product.column.ProductSKUColumn;
import com.dotran.example.store.common.domain.valueobject.CategoryId;
import com.dotran.example.store.common.domain.valueobject.SKU;
import com.dotran.example.store.common.exception.ImportException;
import com.dotran.example.store.infrastructure.cloud.s3.S3ObjectStorageReader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static com.dotran.example.store.common.utils.POIUtils.getString;
import static com.dotran.example.store.common.utils.POIUtils.getValueAsBigDecimal;
import static com.dotran.example.store.common.utils.POIUtils.getValueAsUUID;

@Component
@Slf4j
@RequiredArgsConstructor
public class ProductExcelReader implements StoreProductImportReader {

    private final S3ObjectStorageReader s3ObjectStorageReader;
    private final ExcelValidator excelValidator;

    public ProductImportData read(String path, String fileName) {
        try (InputStream inputStream = s3ObjectStorageReader.read(path, fileName);
             XSSFWorkbook workbook = new XSSFWorkbook(inputStream)
        ) {
            excelValidator.validateHeader(workbook.getSheet("Product"),
                    Arrays.stream(ProductImportColumn.values())
                            .map(ProductImportColumn::getColumnName)
                            .collect(Collectors.toList())
            );
            excelValidator.validateHeader(workbook.getSheet("Product SKU"),
                    Arrays.stream(ProductSKUColumn.values())
                            .map(ProductSKUColumn::getColumnName)
                            .collect(Collectors.toList())
            );
            excelValidator.validateHeader(workbook.getSheet("Product Image"),
                    Arrays.stream(ProductImageColumn.values())
                            .map(ProductImageColumn::getColumnName)
                            .collect(Collectors.toList())
            );

            List<ProductImportRow> products = parseProducts(workbook);
            List<ProductSKUImportRow> skus = parseProductSKUs(workbook);
            List<ProductImageImportRow> images = parseProductImages(workbook);

            return ProductImportData.builder()
                    .products(products)
                    .skus(skus)
                    .images(images)
                    .build();

        } catch (IOException e) {
            log.error("Unable to read Excel file, error: {}", e.getMessage(), e);
            throw new ImportException("Unable to read Excel file");
        }
    }

    private List<ProductImportRow> parseProducts(Workbook workbook) {
        Sheet sheet = workbook.getSheet("Product");

        List<ProductImportRow> result = new ArrayList<>();

        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);

            if (row == null) {
                continue;
            }

            result.add(
                    ProductImportRow.builder()
                            .reference(getString(row, ProductImportColumn.REFERENCE.getIndex()))
                            .name(getString(row, ProductImportColumn.NAME.getIndex()))
                            .description(getString(row, ProductImportColumn.DESCRIPTION.getIndex()))
                            .thumbnailUrl(getString(row, ProductImportColumn.THUMBNAIL_URL.getIndex()))
                            .categoryId(CategoryId.of(getValueAsUUID(row, ProductImportColumn.CATEGORY.getIndex())))
                            .build()
            );
        }

        return result;
    }

    private List<ProductSKUImportRow> parseProductSKUs(Workbook workbook) {
        Sheet sheet = workbook.getSheet("Product SKU");

        List<ProductSKUImportRow> result = new ArrayList<>();

        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);

            if (row == null) {
                continue;
            }

            result.add(
                    ProductSKUImportRow.builder()
                            .productRef(getString(row, ProductSKUColumn.PRODUCT_REF.getIndex()))
                            .sku(SKU.of(getString(row, ProductSKUColumn.SKU.getIndex())))
                            .name(getString(row, ProductSKUColumn.NAME.getIndex()))
                            .price(getValueAsBigDecimal(row, ProductSKUColumn.PRICE.getIndex()))
                            .currency(getString(row, ProductSKUColumn.CURRENCY.getIndex()))
                            .weight(getValueAsBigDecimal(row, ProductSKUColumn.WEIGHT.getIndex()))
                            .length(getValueAsBigDecimal(row, ProductSKUColumn.LENGTH.getIndex()))
                            .width(getValueAsBigDecimal(row, ProductSKUColumn.WIDTH.getIndex()))
                            .height(getValueAsBigDecimal(row, ProductSKUColumn.HEIGHT.getIndex()))
                            .build()
            );
        }

        return result;
    }

    private List<ProductImageImportRow> parseProductImages(Workbook workbook) {
        Sheet sheet = workbook.getSheet("Product Image");

        List<ProductImageImportRow> result = new ArrayList<>();

        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);

            if (row == null) {
                continue;
            }

            result.add(
                    ProductImageImportRow.builder()
                            .productRef(getString(row, ProductImageColumn.PRODUCT_REF.getIndex()))
                            .imageUrl(getString(row, ProductImageColumn.URL.getIndex()))
                            .build()
            );
        }

        return result;
    }
}
