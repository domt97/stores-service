package com.dotran.example.store.common.utils;

import lombok.experimental.UtilityClass;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;

import java.math.BigDecimal;
import java.util.UUID;

@UtilityClass
public class POIUtils {

    private final DataFormatter formatter = new DataFormatter();

    public static String getString(Row row, int column) {
        Cell cell = row.getCell(column);

        if (cell == null) {
            return null;
        }

        String value = formatter.formatCellValue(cell);

        if (value == null) {
            return null;
        }

        value = value.trim();

        return value.isBlank() ? null : value;
    }

    public static UUID getValueAsUUID(Row row, int column) {
        String valueAsString = getString(row, column);
        return valueAsString == null ? null : UUID.fromString(valueAsString);
    }

    public static BigDecimal getValueAsBigDecimal(Row row, int column) {
        String valueAsString = getString(row, column);
        return valueAsString == null ? null : new BigDecimal(valueAsString);
    }
}
