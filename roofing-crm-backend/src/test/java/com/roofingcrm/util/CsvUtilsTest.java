package com.roofingcrm.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CsvUtilsTest {

    @Test
    void neutralizesFormulaWithQuotesBeforeApplyingCsvEscaping() {
        assertEquals("\"'=HYPERLINK(\"\"x\"\")\"", CsvUtils.cell("=HYPERLINK(\"x\")"));
    }

    @Test
    void neutralizesEverySpreadsheetFormulaTrigger() {
        assertEquals("'+1", CsvUtils.cell("+1"));
        assertEquals("'-1", CsvUtils.cell("-1"));
        assertEquals("'@x", CsvUtils.cell("@x"));
        assertEquals("'\tx", CsvUtils.cell("\tx"));
        assertEquals("\"'\rx\"", CsvUtils.cell("\rx"));
    }

    @Test
    void leavesNormalNameUnchanged() {
        assertEquals("John Smith", CsvUtils.cell("John Smith"));
    }

    @Test
    void quotesCommaWithoutAddingApostrophe() {
        assertEquals("\"Smith, John\"", CsvUtils.cell("Smith, John"));
    }

    @Test
    void neutralizesNegativeNumberInTextCell() {
        assertEquals("'-123.45", CsvUtils.cell("-123.45"));
    }
}
