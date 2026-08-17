package com.example.petcare.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Assume;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ExternalDataImporterTest {
    @Test
    public void providedWorkbookHasExpectedImportCounts() throws Exception {
        String path = System.getenv("PETCARE_IMPORT_FIXTURE");
        File fixture = path == null ? null : new File(path);
        InputStream source = fixture != null && fixture.isFile() ? new FileInputStream(fixture) : null;
        Assume.assumeTrue(source != null);

        try (InputStream input = source) {
            ExternalDataImporter.ParsedData data = ExternalDataImporter.parse(input, "Data Lina.xlsx");
            assertEquals(768, data.totalRows);
            assertEquals(441, data.importableRows);
            assertEquals(327, data.skippedRows);
        }
    }

    @Test
    public void malformedFileIsRejected() throws Exception {
        try {
            ExternalDataImporter.parse(
                    new ByteArrayInputStream("not an xlsx".getBytes(StandardCharsets.UTF_8)),
                    "broken.xlsx"
            );
            fail("Malformed workbooks must be rejected");
        } catch (ExternalDataImporter.ImportException error) {
            assertTrue(error.getMessage().contains("supported XLSX"));
        }
    }

    @Test
    public void workbookWithoutAllSheetIsRejected() throws Exception {
        try {
            ExternalDataImporter.parse(workbook("Data", validWalkRow()), "missing-all.xlsx");
            fail("The All worksheet is required");
        } catch (ExternalDataImporter.ImportException error) {
            assertTrue(error.getMessage().contains("Worksheet 'All' was not found"));
        }
    }

    @Test
    public void walkWithoutEndTimeIsSkipped() throws Exception {
        String row = row(
                cell("A2", "Walk"),
                cell("B2", "Morning walk"),
                cell("C2", "2024-05-01T08:00:00Z"),
                cell("D2", ""),
                cell("E2", "Park")
        );
        ExternalDataImporter.ParsedData data = ExternalDataImporter.parse(workbook("All", row), "walk.xlsx");
        assertEquals(1, data.totalRows);
        assertEquals(0, data.importableRows);
        assertEquals(1, data.skippedRows);
        assertEquals(Integer.valueOf(1), data.skipReasons.get("Walk: missing or invalid end time"));
    }

    private InputStream workbook(String sheetName, String dataRow) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(bytes)) {
            add(zip, "xl/workbook.xml",
                    "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                            + "<workbook xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" "
                            + "xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\">"
                            + "<sheets><sheet name=\"" + sheetName + "\" sheetId=\"1\" r:id=\"rId1\"/>"
                            + "</sheets></workbook>");
            add(zip, "xl/_rels/workbook.xml.rels",
                    "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                            + "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">"
                            + "<Relationship Id=\"rId1\" Target=\"worksheets/sheet1.xml\" "
                            + "Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\"/>"
                            + "</Relationships>");
            add(zip, "xl/worksheets/sheet1.xml",
                    "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                            + "<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">"
                            + "<sheetData>" + headerRow() + dataRow + "</sheetData></worksheet>");
        }
        return new ByteArrayInputStream(bytes.toByteArray());
    }

    private String headerRow() {
        return row(
                cell("A1", "Type"),
                cell("B1", "Title"),
                cell("C1", "Event time iso"),
                cell("D1", "Event end time iso"),
                cell("E1", "Notes")
        );
    }

    private String validWalkRow() {
        return row(
                cell("A2", "Walk"),
                cell("B2", "Morning walk"),
                cell("C2", "2024-05-01T08:00:00Z"),
                cell("D2", "2024-05-01T08:30:00Z"),
                cell("E2", "Park")
        );
    }

    private String row(String... cells) {
        StringBuilder xml = new StringBuilder("<row>");
        for (String cell : cells) xml.append(cell);
        return xml.append("</row>").toString();
    }

    private String cell(String reference, String value) {
        return "<c r=\"" + reference + "\" t=\"inlineStr\"><is><t>" + value + "</t></is></c>";
    }

    private void add(ZipOutputStream zip, String name, String value) throws Exception {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(value.getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }
}
