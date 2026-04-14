package com.example.petcare.ui.common;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import com.example.petcare.data.entities.WeightEntry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class WeightChartView extends View {
    private final Paint axisPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pointPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private List<WeightEntry> entries = new ArrayList<>();

    public WeightChartView(Context context, AttributeSet attrs) {
        super(context, attrs);
        axisPaint.setColor(Color.GRAY);
        axisPaint.setStrokeWidth(3f);
        linePaint.setColor(0xFF7E57C2);
        linePaint.setStrokeWidth(5f);
        pointPaint.setColor(0xFFFFB74D);
        labelPaint.setColor(Color.GRAY);
        labelPaint.setTextSize(28f);
    }

    public void setEntries(List<WeightEntry> items) {
        entries = new ArrayList<>(items);
        Collections.sort(entries, Comparator.comparingLong(item -> item.measuredAt));
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int left = 70;
        int right = getWidth() - 30;
        int top = 20;
        int bottom = getHeight() - 50;
        canvas.drawLine(left, bottom, right, bottom, axisPaint);
        canvas.drawLine(left, top, left, bottom, axisPaint);

        if (entries.isEmpty()) {
            canvas.drawText("No weight data yet", left + 20, bottom - 20, labelPaint);
            return;
        }

        double min = Double.MAX_VALUE;
        double max = Double.MIN_VALUE;
        for (WeightEntry entry : entries) {
            min = Math.min(min, entry.weightValue);
            max = Math.max(max, entry.weightValue);
        }
        if (max - min < 0.1) {
            max += 0.5;
            min -= 0.5;
        }

        int count = entries.size();
        float prevX = -1;
        float prevY = -1;
        for (int i = 0; i < count; i++) {
            WeightEntry entry = entries.get(i);
            float x = left + ((right - left) * (count == 1 ? 0.5f : (i / (float) (count - 1))));
            float normalized = (float) ((entry.weightValue - min) / (max - min));
            float y = bottom - normalized * (bottom - top);
            if (i > 0) {
                canvas.drawLine(prevX, prevY, x, y, linePaint);
            }
            canvas.drawCircle(x, y, 8f, pointPaint);
            prevX = x;
            prevY = y;
        }

        canvas.drawText(String.format(java.util.Locale.getDefault(), "%.1f", max), 10, top + 10, labelPaint);
        canvas.drawText(String.format(java.util.Locale.getDefault(), "%.1f", min), 10, bottom, labelPaint);
    }
}
