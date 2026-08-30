package com.dotran.example.store.infrastructure.excel;

import com.dotran.example.store.common.exception.ValidationException;
import com.dotran.example.store.common.utils.POIUtils;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
public class ExcelValidator {

    public void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ValidationException("Import file is empty");
        }

        String filename = file.getOriginalFilename();

        if (filename == null || !filename.toLowerCase().endsWith(".xlsx")) {
            throw new ValidationException("Only .xlsx files are supported");
        }
    }

    public void validateHeader(Sheet sheet, String... expected) {
        Row header = sheet.getRow(0);

        if (header == null) {
            throw new ValidationException("Missing header in sheet: " + sheet.getSheetName());
        }

        for (int i = 0; i < expected.length; i++) {
            String actual = POIUtils.getString(header, i);

            if (!expected[i].equals(actual)) {
                throw new ValidationException("Invalid header in sheet " +
                        sheet.getSheetName() +
                        ", column " +
                        (i + 1) +
                        ". Expected '" +
                        expected[i] +
                        "' but got '" +
                        actual +
                        "'"
                );
            }
        }
    }
}
