package com.example.petcare.reminders;

import static org.junit.Assert.assertEquals;

import com.example.petcare.data.entities.Medication;

import org.junit.Test;

import java.time.LocalDate;
import java.time.ZoneId;

public class MedicationScheduleCalculatorTest {
    private final ZoneId zone = ZoneId.of("America/New_York");

    @Test
    public void disabledMedicationHasNoOccurrence() {
        Medication medication = medication(LocalDate.of(2024, 1, 1));
        medication.reminderEnabled = false;
        assertEquals(0L, MedicationScheduleCalculator.nextOccurrence(medication, at(2024, 1, 1, 0, 0), zone));
    }

    @Test
    public void onceDailyKeepsLocalTimeAcrossDst() {
        Medication medication = medication(LocalDate.of(2024, 3, 9));
        medication.frequencyType = "Once daily";
        medication.reminderMinuteOfDay1 = 9 * 60;
        long next = MedicationScheduleCalculator.nextOccurrence(medication, at(2024, 3, 9, 10, 0), zone);
        assertEquals(at(2024, 3, 10, 9, 0), next);
    }

    @Test
    public void twiceDailyUsesBothDistinctTimes() {
        Medication medication = medication(LocalDate.of(2024, 1, 1));
        medication.frequencyType = "Twice daily";
        medication.reminderMinuteOfDay1 = 9 * 60;
        medication.reminderMinuteOfDay2 = 21 * 60;
        assertEquals(at(2024, 1, 1, 21, 0),
                MedicationScheduleCalculator.nextOccurrence(medication, at(2024, 1, 1, 10, 0), zone));
        assertEquals(at(2024, 1, 2, 9, 0),
                MedicationScheduleCalculator.nextOccurrence(medication, at(2024, 1, 1, 22, 0), zone));
    }

    @Test
    public void everyNDaysIsAnchoredToStartDate() {
        Medication medication = medication(LocalDate.of(2024, 1, 1));
        medication.frequencyType = "Every N days";
        medication.frequencyIntervalDays = 3;
        assertEquals(at(2024, 1, 4, 9, 0),
                MedicationScheduleCalculator.nextOccurrence(medication, at(2024, 1, 2, 12, 0), zone));
    }

    @Test
    public void customUsesSelectedWeekdays() {
        Medication medication = medication(LocalDate.of(2024, 1, 1));
        medication.frequencyType = "Custom";
        medication.reminderWeekdayMask = (1 << 0) | (1 << 2);
        assertEquals(at(2024, 1, 3, 9, 0),
                MedicationScheduleCalculator.nextOccurrence(medication, at(2024, 1, 1, 10, 0), zone));
    }

    @Test
    public void endDateIncludesTheWholeSelectedDay() {
        Medication medication = medication(LocalDate.of(2024, 1, 1));
        medication.endDateEpochMillis = at(2024, 1, 2, 0, 0);
        assertEquals(at(2024, 1, 2, 9, 0),
                MedicationScheduleCalculator.nextOccurrence(medication, at(2024, 1, 1, 10, 0), zone));
        assertEquals(0L,
                MedicationScheduleCalculator.nextOccurrence(medication, at(2024, 1, 2, 10, 0), zone));
    }

    private Medication medication(LocalDate start) {
        Medication medication = new Medication();
        medication.reminderEnabled = true;
        medication.startDateEpochMillis = start.atStartOfDay(zone).toInstant().toEpochMilli();
        medication.frequencyType = "Once daily";
        medication.frequencyIntervalDays = 1;
        medication.reminderMinuteOfDay1 = 9 * 60;
        medication.reminderMinuteOfDay2 = 21 * 60;
        medication.reminderWeekdayMask = 127;
        return medication;
    }

    private long at(int year, int month, int day, int hour, int minute) {
        return LocalDate.of(year, month, day).atTime(hour, minute).atZone(zone).toInstant().toEpochMilli();
    }
}
