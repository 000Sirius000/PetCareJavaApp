package com.example.petcare.util;

import com.example.petcare.data.entities.Pet;

import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeParseException;

public final class AgeUtils {
    private AgeUtils() {
    }

    public static LocalDate parseBirthDate(Pet pet) {
        if (pet == null || pet.birthInfo == null || pet.birthInfo.trim().isEmpty()) {
            return null;
        }
        try {
            return LocalDate.parse(pet.birthInfo.trim());
        } catch (DateTimeParseException ignored) {
            return null;
        }
    }

    public static String fullAge(Pet pet) {
        return fullAge(parseBirthDate(pet));
    }

    public static String fullAge(LocalDate birthDate) {
        if (birthDate == null) {
            return "Age unknown";
        }

        LocalDate today = LocalDate.now();
        if (birthDate.isAfter(today)) {
            return "Date of birth is in the future";
        }

        Period period = Period.between(birthDate, today);
        if (period.isZero()) {
            return "Born today";
        }

        if (period.getYears() == 0 && period.getMonths() == 0) {
            return period.getDays() + (period.getDays() == 1 ? " day old" : " days old");
        }

        if (period.getYears() == 0) {
            StringBuilder value = new StringBuilder();
            if (period.getMonths() > 0) {
                value.append(period.getMonths()).append(period.getMonths() == 1 ? " month" : " months");
            }
            if (period.getDays() > 0) {
                if (value.length() > 0) value.append(", ");
                value.append(period.getDays()).append(period.getDays() == 1 ? " day" : " days");
            }
            return value + " old";
        }

        StringBuilder value = new StringBuilder();
        value.append(period.getYears()).append(period.getYears() == 1 ? " year" : " years");
        if (period.getMonths() > 0) {
            value.append(", ").append(period.getMonths()).append(period.getMonths() == 1 ? " month" : " months");
        }
        if (period.getDays() > 0) {
            value.append(", ").append(period.getDays()).append(period.getDays() == 1 ? " day" : " days");
        }
        return value.toString();
    }

    public static String compactAge(Pet pet) {
        return compactAge(parseBirthDate(pet));
    }

    public static String compactAge(LocalDate birthDate) {
        if (birthDate == null) {
            return "Age unknown";
        }

        LocalDate today = LocalDate.now();
        if (birthDate.isAfter(today)) {
            return "Age unknown";
        }

        Period period = Period.between(birthDate, today);
        if (period.isZero()) {
            return "Born today";
        }

        StringBuilder value = new StringBuilder();
        if (period.getYears() > 0) {
            value.append(period.getYears()).append("y");
        }
        if (period.getMonths() > 0 || period.getYears() > 0) {
            if (value.length() > 0) value.append(", ");
            value.append(period.getMonths()).append("m");
        }
        if (period.getDays() > 0 || value.length() == 0) {
            if (value.length() > 0) value.append(", ");
            value.append(period.getDays()).append("d");
        }
        return value.toString();
    }

    public static String compactAgeWithBreed(Pet pet) {
        String age = compactAge(pet);
        String breed = pet == null || pet.breed == null || pet.breed.trim().isEmpty()
                ? safeSpecies(pet)
                : pet.breed.trim();
        return age + " — " + breed;
    }

    private static String safeSpecies(Pet pet) {
        if (pet == null || pet.species == null || pet.species.trim().isEmpty()) {
            return "Pet";
        }
        return pet.species.trim();
    }
}
