package com.dotran.example.store.infrastructure.excel;

import com.dotran.example.store.common.exception.ValidationException;
import com.dotran.example.store.common.utils.POIUtils;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ExcelValidator {

    public void validateHeader(Sheet sheet, List<String> expected) {
        Row header = sheet.getRow(0);

        if (header == null) {
            throw new ValidationException("Missing header in sheet: " + sheet.getSheetName());
        }

        for (int i = 0; i < expected.size(); i++) {
            String actual = POIUtils.getString(header, i);

            if (!expected.get(i).equals(actual)) {
                throw new ValidationException("Invalid header in sheet " +
                        sheet.getSheetName() +
                        ", column " +
                        (i + 1) +
                        ". Expected '" +
                        expected.get(i) +
                        "' but got '" +
                        actual +
                        "'"
                );
            }
        }
    }
}
