package com.example.petcare.ui.common;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import com.example.petcare.R;
import com.example.petcare.data.entities.WeightEntry;
import com.example.petcare.util.FormatUtils;
import com.example.petcare.util.ThemeUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class WeightChartView extends View {
    private final Paint axisPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pointPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint warningPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint tickPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final List<PointInfo> points = new ArrayList<>();
    private List<WeightEntry> entries = new ArrayList<>();

    public WeightChartView(Context context, AttributeSet attrs) {
        super(context, attrs);
        axisPaint.setStrokeWidth(3f);
        linePaint.setStrokeWidth(5f);
        labelPaint.setTextSize(26f);
        tickPaint.setStrokeWidth(2f);
    }

    public void setEntries(List<WeightEntry> items) {
        entries = new ArrayList<>(items == null ? new ArrayList<>() : items);
        Collections.sort(entries, Comparator.comparingLong(item -> item.measuredAt));
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        axisPaint.setColor(ContextCompat.getColor(getContext(), R.color.pet_border));
        linePaint.setColor(ThemeUtils.getAccentColor(getContext()));
        pointPaint.setColor(ThemeUtils.getAccentColor(getContext()));
        warningPaint.setColor(ContextCompat.getColor(getContext(), R.color.pet_warning));
        labelPaint.setColor(ContextCompat.getColor(getContext(), R.color.pet_text_secondary));
        tickPaint.setColor(ContextCompat.getColor(getContext(), R.color.pet_border));

        int left = 90;
        int right = getWidth() - 20;
        int top = 30;
        int bottom = getHeight() - 70;
        canvas.drawLine(left, bottom, right, bottom, axisPaint);
        canvas.drawLine(left, top, left, bottom, axisPaint);

        points.clear();

        if (entries.isEmpty()) {
            canvas.drawText("No weight data yet", left + 20, bottom - 20, labelPaint);
            return;
        }

        double min = Double.MAX_VALUE;
        double max = -Double.MAX_VALUE;
        for (WeightEntry entry : entries) {
            min = Math.min(min, entry.weightValue);
            max = Math.max(max, entry.weightValue);
            if (entry.healthyMin != null) min = Math.min(min, entry.healthyMin);
            if (entry.healthyMax != null) max = Math.max(max, entry.healthyMax);
        }
        if (max - min < 0.1) {
            max += 0.5;
            min -= 0.5;
        }

        float prevX = -1f;
        float prevY = -1f;
        int count = entries.size();
        for (int i = 0; i < count; i++) {
            WeightEntry entry = entries.get(i);
            float x = left + ((right - left) * (count == 1 ? 0.5f : (i / (float) (count - 1))));
            float normalized = (float) ((entry.weightValue - min) / (max - min));
            float y = bottom - normalized * (bottom - top);

            if (i > 0) canvas.drawLine(prevX, prevY, x, y, linePaint);

            boolean outOfRange = (entry.healthyMin != null && entry.weightValue < entry.healthyMin)
                    || (entry.healthyMax != null && entry.weightValue > entry.healthyMax);
            canvas.drawCircle(x, y, 10f, outOfRange ? warningPaint : pointPaint);

            PointInfo info = new PointInfo();
            info.entry = entry;
            info.x = x;
            info.y = y;
            points.add(info);

            prevX = x;
            prevY = y;
        }

        canvas.drawText(String.format(java.util.Locale.getDefault(), "%.1f", max), 10, top + 10, labelPaint);
        canvas.drawText(String.format(java.util.Locale.getDefault(), "%.1f", min), 10, bottom, labelPaint);

        int[] labelIndexes = labelIndexes(count);
        for (int index : labelIndexes) {
            if (index < 0 || index >= count) continue;
            WeightEntry entry = entries.get(index);
            float x = left + ((right - left) * (count == 1 ? 0.5f : (index / (float) (count - 1))));
            canvas.drawLine(x, bottom, x, bottom + 10, tickPaint);
            String label = FormatUtils.shortDate(entry.measuredAt);
            canvas.drawText(label, x - 28, bottom + 34, labelPaint);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() != MotionEvent.ACTION_UP) return true;

        float x = event.getX();
        float y = event.getY();
        for (PointInfo point : points) {
            float dx = x - point.x;
            float dy = y - point.y;
            if ((dx * dx + dy * dy) <= 900f) {
                WeightEntry entry = point.entry;
                String unit = entry.unit == null || entry.unit.trim().isEmpty() ? "kg" : entry.unit;
                Toast.makeText(
                        getContext(),
                        String.format(java.util.Locale.getDefault(), "%.1f %s · %s", entry.weightValue, unit, FormatUtils.dateTime(entry.measuredAt)),
                        Toast.LENGTH_SHORT
                ).show();
                return true;
            }
        }
        return true;
    }

    private int[] labelIndexes(int count) {
        if (count <= 1) return new int[]{0};
        if (count == 2) return new int[]{0, 1};
        if (count == 3) return new int[]{0, 1, 2};
        return new int[]{0, count / 3, (count * 2) / 3, count - 1};
    }

    private static class PointInfo {
        WeightEntry entry;
        float x;
        float y;
    }
}
