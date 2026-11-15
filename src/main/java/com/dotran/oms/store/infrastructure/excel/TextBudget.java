package com.dotran.oms.store.infrastructure.excel;

import com.dotran.oms.store.common.constants.Constants;

/** Characters of text one import may read: per cell / shared string, and in total (BR-CAT-13). Not thread-safe. */
class TextBudget {

    private final long maxTotal;
    private final int maxPerCell;
    private long used;

    TextBudget(long maxTotal, int maxPerCell) {
        this.maxTotal = maxTotal;
        this.maxPerCell = maxPerCell;
    }

    /** Adds {@code chars} to the total and checks the size of the current cell (characters read so far). */
    void add(int chars, int cellCharsSoFar) {
        used += chars;
        if (cellCharsSoFar > maxPerCell) {
            throw new ImportFileException(String.format(Constants.ERROR_MSG_IMPORT_TOO_MUCH_TEXT,
                    "a cell has more than " + maxPerCell + " characters"));
        }
        if (used > maxTotal) {
            throw new ImportFileException(String.format(Constants.ERROR_MSG_IMPORT_TOO_MUCH_TEXT,
                    "more than " + maxTotal + " characters in total"));
        }
    }
}
