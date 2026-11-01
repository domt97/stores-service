package com.dotran.oms.store.infrastructure.excel;

import com.dotran.oms.store.common.constants.Constants;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.eventusermodel.XSSFSheetXMLHandler;
import org.apache.poi.xssf.usermodel.XSSFComment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Collects the rows of one import sheet while it is streamed. Row 1 must be the header (all {@code header} names, in
 * order); after it, only the first {@code parsedColumns} columns are read. A row counts when one of those columns has a
 * value (trimmed, blank → null, same rule as POIUtils.getString); fully blank rows are skipped and not counted.
 * Over {@code maxRows} counted rows, or {@code maxRowElements} row elements (blank ones included) → ImportFileException.
 */
class SheetRows implements XSSFSheetXMLHandler.SheetContentsHandler {

    private static final int MAX_HEADER_ECHO = 100;

    /** A counted data row: its 1-based row number in Excel and the values of the parsed columns. */
    record Row(int number, String[] values) {
        String get(int column) {
            return values[column];
        }
    }

    private final String sheetName;
    private final List<String> header;
    private final int parsedColumns;
    private final int maxRows;
    private final long maxRowElements;

    private final List<Row> rows = new ArrayList<>();
    private String[] headerValues;
    private String[] current;
    private int currentRow;
    private int nextColumn;
    private long rowElements;
    private boolean seen;
    private boolean headerChecked;

    SheetRows(String sheetName, List<String> header, int parsedColumns, int maxRows, int rowElementFactor) {
        this.sheetName = sheetName;
        this.header = header;
        this.parsedColumns = parsedColumns;
        this.maxRows = maxRows;
        this.maxRowElements = (long) maxRows * rowElementFactor;
    }

    String sheetName() {
        return sheetName;
    }

    boolean seen() {
        return seen;
    }

    void markSeen() {
        seen = true;
    }

    @Override
    public void startRow(int rowNum) {
        if (++rowElements > maxRowElements) {
            throw new ImportFileException(String.format(Constants.ERROR_MSG_IMPORT_TOO_MANY_ROW_ELEMENTS, sheetName, maxRowElements));
        }
        if (!headerChecked && rowNum != 0) {
            checkHeader(new String[header.size()]); // the first row is not row 1: no header
        }
        currentRow = rowNum;
        nextColumn = 0;
        current = new String[rowNum == 0 ? header.size() : parsedColumns];
    }

    @Override
    public void cell(String cellReference, String formattedValue, XSSFComment comment) {
        int column = cellReference != null ? new CellReference(cellReference).getCol() : nextColumn;
        nextColumn = column + 1;
        if (column >= 0 && column < current.length) {
            current[column] = normalise(formattedValue);
        }
    }

    @Override
    public void endRow(int rowNum) {
        if (rowNum == 0) {
            checkHeader(current);
            return;
        }
        if (Arrays.stream(current).anyMatch(value -> value != null)) {
            if (rows.size() >= maxRows) {
                throw tooManyRows();
            }
            rows.add(new Row(currentRow + 1, current));
        }
    }

    /** Rows read so far; checks the header of a sheet that had no rows at all. */
    List<Row> rows() {
        if (!headerChecked) {
            checkHeader(new String[header.size()]);
        }
        return rows;
    }

    private void checkHeader(String[] actual) {
        for (int i = 0; i < header.size(); i++) {
            if (!header.get(i).equals(actual[i])) {
                String got = actual[i] == null ? null
                        : actual[i].substring(0, Math.min(actual[i].length(), MAX_HEADER_ECHO));
                throw new ImportFileException(String.format(Constants.ERROR_MSG_IMPORT_BAD_HEADER,
                        sheetName, i + 1, header.get(i), got));
            }
        }
        headerChecked = true;
    }

    private ImportFileException tooManyRows() {
        return new ImportFileException(String.format(Constants.ERROR_MSG_IMPORT_TOO_MANY_ROWS, sheetName, maxRows));
    }

    private static String normalise(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isBlank() ? null : trimmed;
    }
}
