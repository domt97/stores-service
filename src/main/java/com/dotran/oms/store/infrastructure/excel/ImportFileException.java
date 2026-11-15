package com.dotran.oms.store.infrastructure.excel;

/**
 * The file can't be read as an import file (a limit or the expected sheet layout is broken). Raised inside SAX callbacks
 * and turned into a 400 by {@link ProductExcelReader}; never wrapped into the 422 "unreadable" case.
 */
class ImportFileException extends RuntimeException {

    ImportFileException(String message) {
        super(message);
    }
}
