package com.dotran.oms.store.infrastructure.excel;

import com.dotran.oms.store.common.constants.Constants;
import org.apache.poi.ss.usermodel.RichTextString;
import org.apache.poi.util.XMLHelper;
import org.apache.poi.xssf.model.SharedStrings;
import org.apache.poi.xssf.usermodel.XSSFRichTextString;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Shared strings of a workbook, read with a budget: at most {@code maxCount} strings, each and all of them within the
 * {@link TextBudget}. Text of an item is its plain runs joined; phonetic runs ({@code <rPh>}) are skipped, as the
 * previous XSSFWorkbook-based reader did.
 */
class BudgetedSharedStrings implements SharedStrings {

    private final List<String> strings = new ArrayList<>();

    BudgetedSharedStrings(InputStream sst, int maxCount, TextBudget budget)
            throws IOException, SAXException, ParserConfigurationException {
        if (sst == null) {
            return;
        }
        XMLReader reader = XMLHelper.newXMLReader();
        reader.setContentHandler(new DefaultHandler() {
            private StringBuilder item;
            private boolean inText;
            private int phoneticDepth;

            @Override
            public void startElement(String uri, String localName, String qName, Attributes attributes) {
                switch (localName) {
                    case "si" -> {
                        if (strings.size() >= maxCount) {
                            throw new ImportFileException(String.format(Constants.ERROR_MSG_IMPORT_TOO_MUCH_TEXT,
                                    "more than " + maxCount + " distinct texts"));
                        }
                        item = new StringBuilder();
                    }
                    case "rPh" -> phoneticDepth++;
                    case "t" -> inText = phoneticDepth == 0;
                    default -> { }
                }
            }

            @Override
            public void endElement(String uri, String localName, String qName) {
                switch (localName) {
                    case "si" -> {
                        strings.add(item.toString());
                        item = null;
                    }
                    case "rPh" -> phoneticDepth--;
                    case "t" -> inText = false;
                    default -> { }
                }
            }

            @Override
            public void characters(char[] ch, int start, int length) {
                if (inText && item != null) {
                    item.append(ch, start, length);
                    budget.add(length, item.length());
                }
            }
        });
        reader.parse(new InputSource(sst));
    }

    @Override
    public RichTextString getItemAt(int idx) {
        if (idx < 0 || idx >= strings.size()) {
            throw new IllegalArgumentException("Shared string index out of range: " + idx); // malformed file → 422
        }
        return new XSSFRichTextString(strings.get(idx));
    }

    @Override
    public int getCount() {
        return strings.size();
    }

    @Override
    public int getUniqueCount() {
        return strings.size();
    }
}
