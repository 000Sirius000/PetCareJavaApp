package com.example.petcare.ui.common;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.widget.EditText;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.Calendar;

public final class FormUiUtils {
    private FormUiUtils() {
    }

    public static void showDatePicker(Context context, long initialTime, EditText target, Runnable callback) {
        Calendar calendar = Calendar.getInstance();
        if (initialTime > 0) {
            calendar.setTimeInMillis(initialTime);
        }
        DatePickerDialog dialog = new DatePickerDialog(
                context,
                (view, year, month, dayOfMonth) -> {
                    Calendar c = Calendar.getInstance();
                    c.set(year, month, dayOfMonth, 9, 0, 0);
                    target.setTag(c.getTimeInMillis());
                    target.setText(String.format(java.util.Locale.getDefault(), "%04d-%02d-%02d", year, month + 1, dayOfMonth));
                    if (callback != null) callback.run();
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );
        dialog.show();
    }

    public static void showDateTimePicker(Context context, long initialTime, EditText target, Runnable callback) {
        Calendar calendar = Calendar.getInstance();
        if (initialTime > 0) {
            calendar.setTimeInMillis(initialTime);
        }
        new DatePickerDialog(
                context,
                (view, year, month, dayOfMonth) -> new TimePickerDialog(
                        context,
                        (timeView, hour, minute) -> {
                            Calendar c = Calendar.getInstance();
                            c.set(year, month, dayOfMonth, hour, minute, 0);
                            target.setTag(c.getTimeInMillis());
                            target.setText(String.format(java.util.Locale.getDefault(), "%04d-%02d-%02d %02d:%02d", year, month + 1, dayOfMonth, hour, minute));
                            if (callback != null) callback.run();
                        },
                        calendar.get(Calendar.HOUR_OF_DAY),
                        calendar.get(Calendar.MINUTE),
                        true
                ).show(),
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        ).show();
    }

    public static void showChoicesDialog(Context context, String title, String[] options, ChoiceListener listener) {
        new MaterialAlertDialogBuilder(context)
                .setTitle(title)
                .setItems(options, (dialog, which) -> listener.onChosen(which))
                .show();
    }

    public interface ChoiceListener {
        void onChosen(int index);
    }
}
