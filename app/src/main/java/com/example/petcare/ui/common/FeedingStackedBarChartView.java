package com.example.petcare.ui.common;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import com.example.petcare.data.entities.FeedingLog;
import com.example.petcare.util.FormatUtils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class FeedingStackedBarChartView extends View {
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint outlinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final int[] colors = {0xFF66BB6A, 0xFFFFCA28, 0xFF42A5F5, 0xFFAB47BC, 0xFFFF7043};
    private List<FeedingLog> logs = new ArrayList<>();
    private FilterRange range = FilterRange.WEEK;
    private List<Segment> segments = new ArrayList<>();
    private double total = 0d;

    public FeedingStackedBarChartView(Context context, AttributeSet attrs) {
        super(context, attrs);
        textPaint.setColor(Color.DKGRAY);
        textPaint.setTextSize(28f);
        outlinePaint.setColor(Color.GRAY);
        outlinePaint.setStyle(Paint.Style.STROKE);
        outlinePaint.setStrokeWidth(2f);
    }

    public void setData(List<FeedingLog> logs, FilterRange range) {
        this.logs = logs == null ? new ArrayList<>() : new ArrayList<>(logs);
        this.range = range == null ? FilterRange.WEEK : range;
        rebuild();
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float left = 50f;
        float right = getWidth() - 50f;
        float top = 90f;
        float bottom = getHeight() - 80f;

        if (segments.isEmpty() || total <= 0d) {
            canvas.drawText("No feeding log data yet", left, getHeight() / 2f, textPaint);
            return;
        }

        canvas.drawText(periodTitle() + " total: " + FormatUtils.number(total), left, 40f, textPaint);

        float width = right - left;
        float currentLeft = left;
        Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
        for (int i = 0; i < segments.size(); i++) {
            Segment segment = segments.get(i);
            float segmentWidth = (float) (width * (segment.value / total));
            segment.left = currentLeft;
            segment.top = top;
            segment.right = currentLeft + segmentWidth;
            segment.bottom = bottom;
            fill.setColor(colors[i % colors.length]);
            canvas.drawRect(segment.left, segment.top, segment.right, segment.bottom, fill);
            canvas.drawRect(segment.left, segment.top, segment.right, segment.bottom, outlinePaint);
            if (segmentWidth > 90f) {
                canvas.drawText(segment.label, segment.left + 8f, segment.top + 28f, textPaint);
                canvas.drawText(FormatUtils.number(segment.value), segment.left + 8f, segment.top + 60f, textPaint);
            }
            currentLeft += segmentWidth;
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() != MotionEvent.ACTION_UP) return true;
        float x = event.getX();
        float y = event.getY();
        for (Segment segment : segments) {
            if (x >= segment.left && x <= segment.right && y >= segment.top && y <= segment.bottom) {
                Toast toast = Toast.makeText(getContext(), segment.label + ": " + FormatUtils.number(segment.value), Toast.LENGTH_SHORT);
                toast.setGravity(Gravity.CENTER, 0, 0);
                toast.show();
                return true;
            }
        }
        return true;
    }

    private void rebuild() {
        total = 0d;
        segments = new ArrayList<>();
        long[] bounds = boundsForRange();
        Map<String, Double> grouped = new LinkedHashMap<>();
        for (FeedingLog log : logs) {
            if (log.completedAt < bounds[0] || log.completedAt > bounds[1]) continue;
            String meal = log.mealName == null || log.mealName.trim().isEmpty() ? "Other" : log.mealName.trim();
            double amount = FormatUtils.parseLeadingNumber(log.portion);
            if (amount <= 0d) amount = 1d;
            grouped.put(meal, grouped.getOrDefault(meal, 0d) + amount);
            total += amount;
        }
        for (Map.Entry<String, Double> item : grouped.entrySet()) {
            Segment segment = new Segment();
            segment.label = item.getKey();
            segment.value = item.getValue();
            segments.add(segment);
        }
    }

    private long[] boundsForRange() {
        Calendar now = Calendar.getInstance();
        Calendar start = Calendar.getInstance();
        Calendar end = Calendar.getInstance();
        if (range == FilterRange.YEAR) {
            start.clear();
            start.set(now.get(Calendar.YEAR), Calendar.JANUARY, 1, 0, 0, 0);
            end.clear();
            end.set(now.get(Calendar.YEAR), Calendar.DECEMBER, 31, 23, 59, 59);
        } else if (range == FilterRange.MONTH) {
            start.clear();
            start.set(now.get(Calendar.YEAR), now.get(Calendar.MONTH), 1, 0, 0, 0);
            end.clear();
            end.set(now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.getActualMaximum(Calendar.DAY_OF_MONTH), 23, 59, 59);
        } else {
            start.add(Calendar.DAY_OF_YEAR, -6);
            start.set(Calendar.HOUR_OF_DAY, 0);
            start.set(Calendar.MINUTE, 0);
            start.set(Calendar.SECOND, 0);
            start.set(Calendar.MILLISECOND, 0);
            end.set(Calendar.HOUR_OF_DAY, 23);
            end.set(Calendar.MINUTE, 59);
            end.set(Calendar.SECOND, 59);
        }
        return new long[]{start.getTimeInMillis(), end.getTimeInMillis()};
    }

    private String periodTitle() {
        switch (range) {
            case YEAR: return "Year";
            case MONTH: return "Month";
            default: return "Week";
        }
    }

    private static class Segment {
        String label;
        double value;
        float left;
        float top;
        float right;
        float bottom;
    }
}
