package com.dotran.oms.store.infrastructure.excel;

import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.xssf.eventusermodel.XSSFSheetXMLHandler;
import org.apache.poi.xssf.model.SharedStrings;
import org.apache.poi.xssf.model.Styles;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

/**
 * POI's streaming sheet handler, with every character it buffers (inline strings, values, formulas) counted against the
 * {@link TextBudget}: POI itself collects a cell's text in an unbounded buffer. Whitespace between elements is not
 * counted.
 */
class BudgetedSheetHandler extends XSSFSheetXMLHandler {

    private final TextBudget budget;
    private int cellChars;
    private int textDepth;

    BudgetedSheetHandler(Styles styles, SharedStrings strings, SheetContentsHandler rows, DataFormatter formatter,
                         TextBudget budget) {
        super(styles, null, strings, rows, formatter, false);
        this.budget = budget;
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
        if ("c".equals(localName)) {
            cellChars = 0;
        } else if (isText(localName)) {
            textDepth++;
        }
        super.startElement(uri, localName, qName, attributes);
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        if (isText(localName) && textDepth > 0) {
            textDepth--;
        }
        super.endElement(uri, localName, qName);
    }

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
        if (textDepth > 0) {
            cellChars += length;
            budget.add(length, cellChars);
        }
        super.characters(ch, start, length);
    }

    /** Elements whose text POI buffers: value, inline-string text, formula. */
    private static boolean isText(String localName) {
        return "v".equals(localName) || "t".equals(localName) || "f".equals(localName);
    }
}
