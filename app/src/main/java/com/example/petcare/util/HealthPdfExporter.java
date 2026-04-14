package com.example.petcare.util;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Environment;

import com.example.petcare.data.PetRepository;
import com.example.petcare.data.entities.Pet;
import com.example.petcare.data.entities.Vaccination;
import com.example.petcare.data.entities.VetVisit;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.List;

public class HealthPdfExporter {

    public static File exportPetHealthRecord(Context context, long petId) throws Exception {
        File dir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
        if (dir == null) {
            dir = context.getFilesDir();
        }
        File file = new File(dir, "pet_health_" + petId + ".pdf");
        try (FileOutputStream fos = new FileOutputStream(file)) {
            writePdf(context, petId, fos);
        }
        return file;
    }

    public static void exportPetHealthRecordToUri(Context context, long petId, Uri targetUri) throws Exception {
        try (OutputStream os = context.getContentResolver().openOutputStream(targetUri, "w")) {
            if (os == null) {
                throw new IllegalStateException("Unable to open export destination");
            }
            writePdf(context, petId, os);
        }
    }

    private static void writePdf(Context context, long petId, OutputStream outputStream) throws Exception {
        PetRepository repository = new PetRepository(context);
        Pet pet = repository.getPet(petId);
        if (pet == null) {
            throw new IllegalStateException("Pet not found");
        }
        List<VetVisit> visits = repository.getVetVisits(petId);
        List<Vaccination> vaccinations = repository.getVaccinations(petId);

        PdfDocument document = new PdfDocument();
        Paint titlePaint = new Paint();
        titlePaint.setTextSize(22f);
        titlePaint.setFakeBoldText(true);

        Paint sectionPaint = new Paint();
        sectionPaint.setTextSize(16f);
        sectionPaint.setFakeBoldText(true);

        Paint bodyPaint = new Paint();
        bodyPaint.setTextSize(11f);

        int pageNumber = 1;
        PdfDocument.Page page = document.startPage(new PdfDocument.PageInfo.Builder(595, 842, pageNumber).create());
        Canvas canvas = page.getCanvas();
        int y = 40;

        y = drawLine(canvas, "Pet health record", 40, y, titlePaint, 28);
        y = drawLine(canvas, "Pet: " + pet.name + " (" + pet.species + ")", 40, y, bodyPaint, 24);
        y = drawLine(canvas, "Breed: " + FormatUtils.nullable(pet.breed), 40, y, bodyPaint, 24);

        y = drawLine(canvas, "Vet visits", 40, y + 10, sectionPaint, 20);
        for (VetVisit visit : visits) {
            if (y > 760) {
                document.finishPage(page);
                pageNumber++;
                page = document.startPage(new PdfDocument.PageInfo.Builder(595, 842, pageNumber).create());
                canvas = page.getCanvas();
                y = 40;
            }
            y = drawWrappedText(canvas, "- " + FormatUtils.date(visit.visitDateEpochMillis) + " | " + visit.reason + " | " + visit.clinicName, 40, y, bodyPaint, 520);
            y = drawWrappedText(canvas, "  Vet: " + FormatUtils.nullable(visit.vetName), 50, y + 4, bodyPaint, 510);
            y = drawWrappedText(canvas, "  Notes: " + FormatUtils.nullable(visit.diagnosisNotes), 50, y + 4, bodyPaint, 510);
            y += 10;
        }

        y = drawLine(canvas, "Vaccinations", 40, y + 10, sectionPaint, 20);
        for (Vaccination vaccination : vaccinations) {
            if (y > 760) {
                document.finishPage(page);
                pageNumber++;
                page = document.startPage(new PdfDocument.PageInfo.Builder(595, 842, pageNumber).create());
                canvas = page.getCanvas();
                y = 40;
            }
            String due = vaccination.nextDueAt == null ? "No due date" : FormatUtils.date(vaccination.nextDueAt);
            y = drawWrappedText(canvas, "- " + vaccination.vaccineName + " | given " + FormatUtils.date(vaccination.administeredAt) + " | next due " + due, 40, y, bodyPaint, 520);
            y = drawWrappedText(canvas, "  Batch: " + FormatUtils.nullable(vaccination.batchNumber), 50, y + 4, bodyPaint, 510);
            y += 10;
        }

        document.finishPage(page);
        document.writeTo(outputStream);
        document.close();
    }

    private static int drawLine(Canvas canvas, String text, int x, int y, Paint paint, int lineGap) {
        canvas.drawText(text, x, y, paint);
        return y + lineGap;
    }

    private static int drawWrappedText(Canvas canvas, String text, int x, int y, Paint paint, int maxWidth) {
        String[] words = text.split(" ");
        StringBuilder line = new StringBuilder();
        for (String word : words) {
            String trial = line.length() == 0 ? word : line + " " + word;
            if (paint.measureText(trial) > maxWidth && line.length() > 0) {
                canvas.drawText(line.toString(), x, y, paint);
                y += 16;
                line = new StringBuilder(word);
            } else {
                line = new StringBuilder(trial);
            }
        }
        if (line.length() > 0) {
            canvas.drawText(line.toString(), x, y, paint);
            y += 16;
        }
        return y;
    }
}
