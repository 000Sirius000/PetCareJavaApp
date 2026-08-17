package com.example.petcare.util;

import android.content.Context;
import android.net.Uri;

import com.example.petcare.data.AppDatabase;
import com.example.petcare.data.entities.ActivitySession;
import com.example.petcare.data.entities.FeedingLog;
import com.example.petcare.data.entities.MedicationLog;
import com.example.petcare.data.entities.SymptomEntry;
import com.example.petcare.data.entities.Vaccination;
import com.example.petcare.data.entities.VetVisit;
import com.example.petcare.data.entities.WeightEntry;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public final class ExternalDataImporter {
    private static final int MAX_UNCOMPRESSED_BYTES = 64 * 1024 * 1024;

    private ExternalDataImporter() {}

    public static final class ParsedData {
        public final String fileName;
        public final int totalRows;
        public final int importableRows;
        public final int skippedRows;
        public final Map<String, Integer> skipReasons;
        private final List<ImportRow> rows;

        private ParsedData(String fileName, int totalRows, List<ImportRow> rows,
                           int skippedRows, Map<String, Integer> skipReasons) {
            this.fileName = fileName;
            this.totalRows = totalRows;
            this.rows = rows;
            this.importableRows = rows.size();
            this.skippedRows = skippedRows;
            this.skipReasons = new LinkedHashMap<>(skipReasons);
        }
    }

    public static final class ImportPreview {
        public final int totalRows;
        public final int importableRows;
        public final int skippedRows;
        public final int duplicateRows;
        public final Map<String, Integer> skipReasons;

        private ImportPreview(int totalRows, int importableRows, int skippedRows,
                              int duplicateRows, Map<String, Integer> skipReasons) {
            this.totalRows = totalRows;
            this.importableRows = importableRows;
            this.skippedRows = skippedRows;
            this.duplicateRows = duplicateRows;
            this.skipReasons = new LinkedHashMap<>(skipReasons);
        }
    }

    public static final class ImportResult {
        public final int totalRows;
        public final int importedRows;
        public final int skippedRows;
        public final int duplicateRows;
        public final Map<String, Integer> skipReasons;

        private ImportResult(int totalRows, int importedRows, int skippedRows,
                             int duplicateRows, Map<String, Integer> skipReasons) {
            this.totalRows = totalRows;
            this.importedRows = importedRows;
            this.skippedRows = skippedRows;
            this.duplicateRows = duplicateRows;
            this.skipReasons = new LinkedHashMap<>(skipReasons);
        }
    }

    public static final class ImportException extends Exception {
        public ImportException(String message) { super(message); }
        public ImportException(String message, Throwable cause) { super(message, cause); }
    }

    private enum Kind {
        ACTIVITY,
        MEDICATION_LOG,
        VET_VISIT,
        VACCINATION,
        SYMPTOM,
        WEIGHT,
        FEEDING
    }

    private static final class ImportRow {
        Kind kind;
        String sourceType;
        String title;
        String notes;
        long startMillis;
        long endMillis;
        double numericValue;
        String unit;
        String foodType;
    }

    public static ParsedData parse(Context context, Uri uri, String fileName) throws ImportException {
        if (uri == null) throw new ImportException("No file selected");
        try (InputStream input = context.getContentResolver().openInputStream(uri)) {
            if (input == null) throw new ImportException("The selected file cannot be opened");
            return parse(input, fileName);
        } catch (ImportException error) {
            throw error;
        } catch (Exception error) {
            throw new ImportException("Could not read XLSX: " + safeMessage(error), error);
        }
    }

    public static ParsedData parse(InputStream input, String fileName) throws ImportException {
        if (input == null) throw new ImportException("The selected file cannot be opened");
        try {
            Map<String, byte[]> entries = readWorkbookEntries(input);
            byte[] workbookXml = entries.get("xl/workbook.xml");
            byte[] relationshipsXml = entries.get("xl/_rels/workbook.xml.rels");
            if (workbookXml == null || relationshipsXml == null) {
                throw new ImportException("This file is not a supported XLSX workbook");
            }

            WorkbookInfo workbook = parseWorkbook(workbookXml);
            String relationshipId = workbook.sheetRelationships.get("All");
            if (relationshipId == null) throw new ImportException("Worksheet 'All' was not found");
            Map<String, String> relationships = parseRelationships(relationshipsXml);
            String target = relationships.get(relationshipId);
            if (target == null) throw new ImportException("Worksheet 'All' has no valid relationship");
            byte[] sheetXml = entries.get(normalizeWorksheetTarget(target));
            if (sheetXml == null) throw new ImportException("Worksheet 'All' data is missing");

            List<String> sharedStrings = parseSharedStrings(entries.get("xl/sharedStrings.xml"));
            return parseAllSheet(sheetXml, sharedStrings, workbook.date1904,
                    fileName == null || fileName.trim().isEmpty() ? "Selected workbook" : fileName.trim());
        } catch (ImportException error) {
            throw error;
        } catch (Exception error) {
            throw new ImportException("Could not read XLSX: " + safeMessage(error), error);
        }
    }

    public static ImportPreview preview(AppDatabase db, ParsedData data, long petId) {
        Set<String> existing = existingKeys(db, petId);
        int duplicates = 0;
        int importable = 0;
        for (ImportRow row : data.rows) {
            String key = stableKey(row);
            if (!existing.add(key)) duplicates++;
            else importable++;
        }
        return new ImportPreview(
                data.totalRows,
                importable,
                data.skippedRows,
                duplicates,
                data.skipReasons
        );
    }

    public static ImportResult importData(AppDatabase db, ParsedData data, long petId) {
        int[] counts = new int[2];
        db.runInTransaction(() -> {
            Set<String> existing = existingKeys(db, petId);
            for (ImportRow row : data.rows) {
                String key = stableKey(row);
                if (!existing.add(key)) {
                    counts[1]++;
                    continue;
                }
                insertRow(db, petId, row);
                counts[0]++;
            }
        });
        return new ImportResult(
                data.totalRows,
                counts[0],
                data.skippedRows,
                counts[1],
                data.skipReasons
        );
    }

    private static Map<String, byte[]> readWorkbookEntries(InputStream input) throws Exception {
        Map<String, byte[]> entries = new HashMap<>();
        int totalBytes = 0;
        try (ZipInputStream zip = new ZipInputStream(input)) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                String name = entry.getName().replace('\\', '/');
                boolean needed = "xl/workbook.xml".equals(name)
                        || "xl/_rels/workbook.xml.rels".equals(name)
                        || "xl/sharedStrings.xml".equals(name)
                        || name.startsWith("xl/worksheets/");
                if (!needed || entry.isDirectory()) continue;

                ByteArrayOutputStream output = new ByteArrayOutputStream();
                byte[] buffer = new byte[8192];
                int read;
                while ((read = zip.read(buffer)) != -1) {
                    totalBytes += read;
                    if (totalBytes > MAX_UNCOMPRESSED_BYTES) {
                        throw new ImportException("The workbook is too large to import safely");
                    }
                    output.write(buffer, 0, read);
                }
                entries.put(name, output.toByteArray());
            }
        }
        return entries;
    }

    private static final class WorkbookInfo {
        boolean date1904;
        final Map<String, String> sheetRelationships = new HashMap<>();
    }

    private static WorkbookInfo parseWorkbook(byte[] xml) throws Exception {
        WorkbookInfo info = new WorkbookInfo();
        XmlPullParser parser = parser(xml);
        int event;
        while ((event = parser.next()) != XmlPullParser.END_DOCUMENT) {
            if (event != XmlPullParser.START_TAG) continue;
            if ("workbookPr".equals(parser.getName())) {
                String value = attribute(parser, "date1904");
                info.date1904 = "1".equals(value) || "true".equalsIgnoreCase(value);
            } else if ("sheet".equals(parser.getName())) {
                String name = attribute(parser, "name");
                String relationshipId = attribute(parser, "id");
                if (!clean(name).isEmpty() && !clean(relationshipId).isEmpty()) {
                    info.sheetRelationships.put(name.trim(), relationshipId.trim());
                }
            }
        }
        return info;
    }

    private static Map<String, String> parseRelationships(byte[] xml) throws Exception {
        Map<String, String> result = new HashMap<>();
        XmlPullParser parser = parser(xml);
        int event;
        while ((event = parser.next()) != XmlPullParser.END_DOCUMENT) {
            if (event == XmlPullParser.START_TAG && "Relationship".equals(parser.getName())) {
                String id = attribute(parser, "Id");
                String target = attribute(parser, "Target");
                if (!clean(id).isEmpty() && !clean(target).isEmpty()) result.put(id, target);
            }
        }
        return result;
    }

    private static List<String> parseSharedStrings(byte[] xml) throws Exception {
        List<String> result = new ArrayList<>();
        if (xml == null) return result;
        XmlPullParser parser = parser(xml);
        StringBuilder current = null;
        int event;
        while ((event = parser.next()) != XmlPullParser.END_DOCUMENT) {
            if (event == XmlPullParser.START_TAG && "si".equals(parser.getName())) {
                current = new StringBuilder();
            } else if (event == XmlPullParser.START_TAG && "t".equals(parser.getName()) && current != null) {
                current.append(parser.nextText());
            } else if (event == XmlPullParser.END_TAG && "si".equals(parser.getName()) && current != null) {
                result.add(current.toString());
                current = null;
            }
        }
        return result;
    }

    private static ParsedData parseAllSheet(byte[] xml, List<String> sharedStrings,
                                            boolean date1904, String fileName) throws Exception {
        List<Map<Integer, String>> rows = readRows(xml, sharedStrings);
        if (rows.isEmpty()) throw new ImportException("Worksheet 'All' is empty");

        Map<Integer, String> headers = rows.get(0);
        int typeColumn = findHeader(headers, "Type");
        int titleColumn = findHeader(headers, "Title");
        int startColumn = findHeader(headers, "Event time iso");
        int endColumn = findHeader(headers, "Event end time iso");
        int notesColumn = findHeader(headers, "Notes");
        if (typeColumn < 0 || titleColumn < 0 || startColumn < 0 || endColumn < 0 || notesColumn < 0) {
            throw new ImportException("Worksheet 'All' must contain Type, Title, Event time iso, Event end time iso and Notes columns");
        }

        int weightColumn = findHeader(headers, "Weight value");
        int unitColumn = findHeader(headers, "Unit");
        int portionColumn = findHeader(headers, "Portion grams");
        int foodTypeColumn = findHeader(headers, "Food type");

        List<ImportRow> importable = new ArrayList<>();
        Map<String, Integer> reasons = new LinkedHashMap<>();
        int total = 0;
        int skipped = 0;
        for (int index = 1; index < rows.size(); index++) {
            Map<Integer, String> values = rows.get(index);
            String type = value(values, typeColumn);
            String title = value(values, titleColumn);
            String startRaw = value(values, startColumn);
            String endRaw = value(values, endColumn);
            String notes = value(values, notesColumn);
            if (type.isEmpty() && title.isEmpty() && startRaw.isEmpty() && endRaw.isEmpty() && notes.isEmpty()) continue;
            total++;

            long start = parseDate(startRaw, date1904);
            long end = parseDate(endRaw, date1904);
            ImportRow row = new ImportRow();
            row.sourceType = type;
            row.title = title;
            row.notes = notes;
            row.startMillis = start;
            row.endMillis = end;

            String normalizedType = type.trim().toLowerCase(Locale.ROOT);
            String skipReason = null;
            switch (normalizedType) {
                case "walk":
                    if (start <= 0L) skipReason = "Walk: invalid start time";
                    else if (end <= start) skipReason = "Walk: missing or invalid end time";
                    else row.kind = Kind.ACTIVITY;
                    break;
                case "medicine":
                case "antiparasitic":
                    if (start <= 0L) skipReason = type + ": invalid event time";
                    else row.kind = Kind.MEDICATION_LOG;
                    break;
                case "vet visit":
                    if (start <= 0L) skipReason = "Vet Visit: invalid event time";
                    else row.kind = Kind.VET_VISIT;
                    break;
                case "vaccination":
                    if (start <= 0L) skipReason = "Vaccination: invalid event time";
                    else row.kind = Kind.VACCINATION;
                    break;
                case "symptoms":
                    if (start <= 0L) skipReason = "Symptoms: invalid event time";
                    else row.kind = Kind.SYMPTOM;
                    break;
                case "weight":
                    row.numericValue = parseNumber(value(values, weightColumn));
                    row.unit = value(values, unitColumn);
                    if (start <= 0L) skipReason = "Weight: invalid event time";
                    else if (row.numericValue <= 0d) skipReason = "Weight: missing numeric Weight value";
                    else row.kind = Kind.WEIGHT;
                    break;
                case "food":
                    row.numericValue = parseNumber(value(values, portionColumn));
                    row.foodType = value(values, foodTypeColumn);
                    if (start <= 0L) skipReason = "Food: invalid event time";
                    else if (row.numericValue <= 0d || row.foodType.isEmpty()) {
                        skipReason = "Food: missing Portion grams or Food type";
                    } else row.kind = Kind.FEEDING;
                    break;
                case "sleep":
                    skipReason = "Sleep is not supported";
                    break;
                case "peed in place":
                case "pooped off spot":
                case "peed off spot":
                    skipReason = "Toilet events are not supported";
                    break;
                case "groomer":
                    skipReason = "Groomer is not supported";
                    break;
                case "other":
                    skipReason = "Other is not supported";
                    break;
                default:
                    skipReason = type.isEmpty() ? "Missing event type" : "Unsupported type: " + type;
                    break;
            }

            if (skipReason == null) importable.add(row);
            else {
                skipped++;
                reasons.put(skipReason, reasons.getOrDefault(skipReason, 0) + 1);
            }
        }
        return new ParsedData(fileName, total, importable, skipped, reasons);
    }

    private static List<Map<Integer, String>> readRows(byte[] xml, List<String> sharedStrings) throws Exception {
        List<Map<Integer, String>> rows = new ArrayList<>();
        XmlPullParser parser = parser(xml);
        Map<Integer, String> row = null;
        int cellColumn = -1;
        String cellType = null;
        String cellValue = "";
        int event;
        while ((event = parser.next()) != XmlPullParser.END_DOCUMENT) {
            if (event == XmlPullParser.START_TAG && "row".equals(parser.getName())) {
                row = new HashMap<>();
            } else if (event == XmlPullParser.START_TAG && "c".equals(parser.getName()) && row != null) {
                cellColumn = columnIndex(attribute(parser, "r"));
                cellType = attribute(parser, "t");
                cellValue = "";
            } else if (event == XmlPullParser.START_TAG && ("v".equals(parser.getName()) || "t".equals(parser.getName()))
                    && row != null && cellColumn >= 0) {
                cellValue = parser.nextText();
            } else if (event == XmlPullParser.END_TAG && "c".equals(parser.getName()) && row != null && cellColumn >= 0) {
                String resolved = cellValue == null ? "" : cellValue;
                if ("s".equals(cellType) && !resolved.isEmpty()) {
                    try {
                        int sharedIndex = Integer.parseInt(resolved.trim());
                        resolved = sharedIndex >= 0 && sharedIndex < sharedStrings.size()
                                ? sharedStrings.get(sharedIndex)
                                : "";
                    } catch (Exception ignored) {
                        resolved = "";
                    }
                }
                row.put(cellColumn, resolved);
                cellColumn = -1;
                cellType = null;
                cellValue = "";
            } else if (event == XmlPullParser.END_TAG && "row".equals(parser.getName()) && row != null) {
                rows.add(row);
                row = null;
            }
        }
        return rows;
    }

    private static Set<String> existingKeys(AppDatabase db, long petId) {
        Set<String> keys = new HashSet<>();
        for (ActivitySession item : db.activitySessionDao().getForPet(petId)) {
            keys.add(activityKey(item.activityType, item.sessionDateEpochMillis, item.durationMinutes, item.notes));
        }
        for (MedicationLog item : db.medicationLogDao().getForPet(petId)) {
            keys.add(medicationKey(item.administeredAt, item.medicationName, item.dosage));
        }
        for (VetVisit item : db.vetVisitDao().getForPet(petId)) {
            keys.add(vetKey(item.visitDateEpochMillis, item.reason, item.diagnosisNotes));
        }
        for (Vaccination item : db.vaccinationDao().getForPet(petId)) {
            keys.add(vaccinationKey(item.administeredAt, item.vaccineName, item.batchNumber));
        }
        for (SymptomEntry item : db.symptomEntryDao().getForPet(petId)) {
            keys.add(symptomKey(item.recordedAt, item.tagsCsv, item.notes));
        }
        for (WeightEntry item : db.weightEntryDao().getForPet(petId)) {
            keys.add(weightKey(item.measuredAt, item.weightValue, item.unit));
        }
        for (FeedingLog item : db.feedingLogDao().getForPet(petId)) {
            keys.add(feedingKey(item.completedAt, item.mealName,
                    FormatUtils.parseLeadingNumber(item.portion), item.foodType));
        }
        return keys;
    }

    private static String stableKey(ImportRow row) {
        switch (row.kind) {
            case ACTIVITY:
                return activityKey("Walk", row.startMillis, durationMinutes(row), activityNotes(row));
            case MEDICATION_LOG:
                return medicationKey(row.startMillis, medicationName(row), row.notes);
            case VET_VISIT:
                return vetKey(row.startMillis, fallback(row.title, "Vet visit"), row.notes);
            case VACCINATION:
                return vaccinationKey(row.startMillis, fallback(row.title, "Vaccination"), row.notes);
            case SYMPTOM:
                return symptomKey(row.startMillis, fallback(row.title, "Symptoms"), row.notes);
            case WEIGHT:
                return weightKey(row.startMillis, row.numericValue, fallback(row.unit, "kg"));
            case FEEDING:
                return feedingKey(row.startMillis, fallback(row.title, "Feeding"), row.numericValue, row.foodType);
            default:
                throw new IllegalStateException("Unsupported import row");
        }
    }

    private static void insertRow(AppDatabase db, long petId, ImportRow row) {
        switch (row.kind) {
            case ACTIVITY: {
                ActivitySession item = new ActivitySession();
                item.petId = petId;
                item.activityType = "Walk";
                item.durationMinutes = durationMinutes(row);
                item.distance = null;
                item.distanceUnit = "km";
                item.sessionDateEpochMillis = row.startMillis;
                item.notes = activityNotes(row);
                db.activitySessionDao().insert(item);
                return;
            }
            case MEDICATION_LOG: {
                MedicationLog item = new MedicationLog();
                item.petId = petId;
                item.medicationId = 0L;
                item.administeredAt = row.startMillis;
                item.sourceReminderAt = 0L;
                item.medicationName = medicationName(row);
                item.dosage = clean(row.notes);
                item.markedBy = "External import";
                item.missed = false;
                db.medicationLogDao().insert(item);
                return;
            }
            case VET_VISIT: {
                VetVisit item = new VetVisit();
                item.petId = petId;
                item.visitDateEpochMillis = row.startMillis;
                item.clinicName = "";
                item.vetName = "";
                item.reason = fallback(row.title, "Vet visit");
                item.diagnosisNotes = clean(row.notes);
                item.attachmentUri = "";
                db.vetVisitDao().insert(item);
                return;
            }
            case VACCINATION: {
                Vaccination item = new Vaccination();
                item.petId = petId;
                item.vaccineName = fallback(row.title, "Vaccination");
                item.administeredAt = row.startMillis;
                item.nextDueAt = null;
                item.batchNumber = clean(row.notes);
                db.vaccinationDao().insert(item);
                return;
            }
            case SYMPTOM: {
                SymptomEntry item = new SymptomEntry();
                item.petId = petId;
                item.recordedAt = row.startMillis;
                item.tagsCsv = fallback(row.title, "Symptoms");
                item.severity = "Unspecified";
                item.notes = clean(row.notes);
                item.linkedVetVisitId = null;
                db.symptomEntryDao().insert(item);
                return;
            }
            case WEIGHT: {
                WeightEntry item = new WeightEntry();
                item.petId = petId;
                item.measuredAt = row.startMillis;
                item.weightValue = row.numericValue;
                item.unit = fallback(row.unit, "kg");
                item.healthyMin = null;
                item.healthyMax = null;
                db.weightEntryDao().insert(item);
                return;
            }
            case FEEDING: {
                FeedingLog item = new FeedingLog();
                item.petId = petId;
                item.scheduleId = 0L;
                item.completedAt = row.startMillis;
                item.mealName = fallback(row.title, "Feeding");
                item.portion = FormatUtils.number(row.numericValue) + " g";
                item.foodType = row.foodType;
                db.feedingLogDao().insert(item);
            }
        }
    }

    private static int durationMinutes(ImportRow row) {
        return Math.max(1, (int) Math.round((row.endMillis - row.startMillis) / 60_000d));
    }

    private static String activityNotes(ImportRow row) {
        return FormatUtils.joinNonEmpty(" - ", row.title, row.notes);
    }

    private static String medicationName(ImportRow row) {
        return fallback(row.title, fallback(row.sourceType, "Medication"));
    }

    private static String activityKey(String type, long timestamp, int duration, String notes) {
        return "activity|" + timestamp + "|" + duration + "|" + normalized(type) + "|" + normalized(notes);
    }

    private static String medicationKey(long timestamp, String name, String dosage) {
        return "medication|" + timestamp + "|" + normalized(name) + "|" + normalized(dosage);
    }

    private static String vetKey(long timestamp, String reason, String notes) {
        return "vet|" + timestamp + "|" + normalized(reason) + "|" + normalized(notes);
    }

    private static String vaccinationKey(long timestamp, String name, String notes) {
        return "vaccination|" + timestamp + "|" + normalized(name) + "|" + normalized(notes);
    }

    private static String symptomKey(long timestamp, String tags, String notes) {
        return "symptom|" + timestamp + "|" + normalized(tags) + "|" + normalized(notes);
    }

    private static String weightKey(long timestamp, double value, String unit) {
        return "weight|" + timestamp + "|" + String.format(Locale.ROOT, "%.6f", value) + "|" + normalized(unit);
    }

    private static String feedingKey(long timestamp, String meal, double grams, String foodType) {
        return "feeding|" + timestamp + "|" + normalized(meal) + "|"
                + String.format(Locale.ROOT, "%.6f", grams) + "|" + normalized(foodType);
    }

    private static XmlPullParser parser(byte[] xml) throws Exception {
        XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
        factory.setNamespaceAware(true);
        XmlPullParser parser = factory.newPullParser();
        parser.setInput(new ByteArrayInputStream(xml), "UTF-8");
        return parser;
    }

    private static String attribute(XmlPullParser parser, String localName) {
        for (int index = 0; index < parser.getAttributeCount(); index++) {
            if (localName.equals(parser.getAttributeName(index))) return parser.getAttributeValue(index);
        }
        return null;
    }

    private static String normalizeWorksheetTarget(String target) {
        String clean = target.replace('\\', '/');
        while (clean.startsWith("../")) clean = clean.substring(3);
        if (clean.startsWith("/")) clean = clean.substring(1);
        if (!clean.startsWith("xl/")) clean = "xl/" + clean;
        return clean;
    }

    private static int columnIndex(String reference) {
        if (reference == null) return -1;
        int result = 0;
        int letters = 0;
        for (int index = 0; index < reference.length(); index++) {
            char value = Character.toUpperCase(reference.charAt(index));
            if (value < 'A' || value > 'Z') break;
            result = result * 26 + (value - 'A' + 1);
            letters++;
        }
        return letters == 0 ? -1 : result - 1;
    }

    private static int findHeader(Map<Integer, String> headers, String expected) {
        String normalizedExpected = normalized(expected);
        for (Map.Entry<Integer, String> entry : headers.entrySet()) {
            if (normalizedExpected.equals(normalized(entry.getValue()))) return entry.getKey();
        }
        return -1;
    }

    private static String value(Map<Integer, String> row, int column) {
        if (column < 0) return "";
        return clean(row.get(column));
    }

    private static long parseDate(String raw, boolean date1904) {
        String value = clean(raw);
        if (value.isEmpty()) return 0L;
        try {
            double numeric = Double.parseDouble(value.replace(',', '.'));
            if (numeric > 1_000_000_000_000d) return (long) numeric;
            if (numeric > 1_000_000_000d) return (long) (numeric * 1000d);
            if (numeric > 1d) return excelDate(numeric, date1904);
        } catch (Exception ignored) { }
        try { return Instant.parse(value).toEpochMilli(); }
        catch (Exception ignored) { }
        try { return OffsetDateTime.parse(value).toInstant().toEpochMilli(); }
        catch (Exception ignored) { }
        try { return ZonedDateTime.parse(value).toInstant().toEpochMilli(); }
        catch (Exception ignored) { }
        try { return LocalDateTime.parse(value).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(); }
        catch (Exception ignored) { }
        try { return LocalDate.parse(value).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(); }
        catch (Exception ignored) { }
        return 0L;
    }

    private static long excelDate(double serial, boolean date1904) {
        long wholeDays = (long) Math.floor(serial);
        double fraction = serial - wholeDays;
        long nanos = Math.round(fraction * 86_400_000_000_000L);
        if (nanos >= 86_400_000_000_000L) {
            wholeDays++;
            nanos -= 86_400_000_000_000L;
        }
        LocalDate epoch = date1904 ? LocalDate.of(1904, 1, 1) : LocalDate.of(1899, 12, 30);
        LocalTime time = LocalTime.ofNanoOfDay(Math.max(0L, nanos));
        return epoch.plusDays(wholeDays).atTime(time).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    private static double parseNumber(String raw) {
        String value = clean(raw).replace(',', '.');
        if (value.isEmpty()) return 0d;
        StringBuilder numeric = new StringBuilder();
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            if (Character.isDigit(character) || character == '.' || character == '-') numeric.append(character);
            else if (numeric.length() > 0) break;
        }
        try { return Double.parseDouble(numeric.toString()); }
        catch (Exception ignored) { return 0d; }
    }

    private static String normalized(String value) {
        return clean(value).toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private static String fallback(String value, String fallback) {
        String clean = clean(value);
        return clean.isEmpty() ? fallback : clean;
    }

    private static String safeMessage(Throwable error) {
        String message = error.getMessage();
        return message == null || message.trim().isEmpty() ? error.getClass().getSimpleName() : message;
    }
}
