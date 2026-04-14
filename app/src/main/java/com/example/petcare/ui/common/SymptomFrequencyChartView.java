package com.example.petcare.ui.common;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import com.example.petcare.data.entities.SymptomEntry;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class SymptomFrequencyChartView extends View {
    private final Paint barPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private List<SymptomEntry> entries = new ArrayList<>();

    public SymptomFrequencyChartView(Context context, AttributeSet attrs) {
        super(context, attrs);
        barPaint.setColor(0xFF7E57C2);
        textPaint.setColor(Color.GRAY);
        textPaint.setTextSize(28f);
    }

    public void setEntries(List<SymptomEntry> entries) {
        this.entries = entries == null ? new ArrayList<>() : new ArrayList<>(entries);
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        Map<String, Integer> counts = new LinkedHashMap<>();
        for (SymptomEntry entry : entries) {
            if (entry.tagsCsv == null) continue;
            String[] tags = entry.tagsCsv.split(",");
            for (String raw : tags) {
                String tag = raw.trim();
                if (tag.isEmpty()) continue;
                counts.put(tag, counts.getOrDefault(tag, 0) + 1);
            }
        }

        if (counts.isEmpty()) {
            canvas.drawText("No symptom frequency data yet", 24, getHeight() / 2f, textPaint);
            return;
        }

        int max = 1;
        for (Integer value : counts.values()) {
            max = Math.max(max, value);
        }

        int index = 0;
        float left = 40;
        float top = 30;
        float barWidth = Math.max(40, (getWidth() - 80f) / counts.size() - 12f);
        float baseLine = getHeight() - 60;
        float chartHeight = getHeight() - 120f;

        for (Map.Entry<String, Integer> item : counts.entrySet()) {
            float x = left + index * (barWidth + 12f);
            float barHeight = (item.getValue() / (float) max) * chartHeight;
            canvas.drawRect(x, baseLine - barHeight, x + barWidth, baseLine, barPaint);
            canvas.drawText(String.valueOf(item.getValue()), x, baseLine - barHeight - 10, textPaint);
            canvas.drawText(trim(item.getKey()), x, baseLine + 28, textPaint);
            index++;
        }
    }

    private String trim(String value) {
        return value.length() > 10 ? value.substring(0, 10) + "…" : value;
    }
}
