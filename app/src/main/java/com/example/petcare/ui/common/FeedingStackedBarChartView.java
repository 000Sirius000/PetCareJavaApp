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
import com.example.petcare.data.entities.FeedingLog;
import com.example.petcare.util.FormatUtils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class FeedingStackedBarChartView extends View {
    private final Paint axisPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final List<BarInfo> bars = new ArrayList<>();

    private List<FeedingLog> logs = new ArrayList<>();
    private FilterRange range = FilterRange.WEEK;

    public FeedingStackedBarChartView(Context context, AttributeSet attrs) {
        super(context, attrs);
        axisPaint.setStrokeWidth(3f);
        textPaint.setTextSize(24f);
    }

    public void setData(List<FeedingLog> logs, FilterRange range) {
        this.logs = logs == null ? new ArrayList<>() : new ArrayList<>(logs);
        this.range = range == null ? FilterRange.WEEK : range;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        axisPaint.setColor(ContextCompat.getColor(getContext(), R.color.pet_border));
        textPaint.setColor(ContextCompat.getColor(getContext(), R.color.pet_text_secondary));

        float left = 76f;
        float right = getWidth() - 20f;
        float top = 42f;
        float bottom = getHeight() - 72f;

        canvas.drawLine(left, bottom, right, bottom, axisPaint);
        canvas.drawLine(left, top, left, bottom, axisPaint);
        canvas.drawText("g", 16f, top + 20f, textPaint);

        buildBars();
        if (bars.isEmpty()) {
            canvas.drawText("No feeding log data yet", left + 20f, bottom - 20f, textPaint);
            return;
        }

        double max = 0d;
        for (BarInfo bar : bars) {
            max = Math.max(max, bar.natural + bar.dry + bar.wet);
        }
        if (max <= 0d) max = 1d;

        float slotWidth = (right - left) / bars.size();
        float barWidth = Math.max(14f, slotWidth * 0.62f);

        for (int i = 0; i < bars.size(); i++) {
            BarInfo bar = bars.get(i);
            float x = left + i * slotWidth + (slotWidth - barWidth) / 2f;
            float currentBottom = bottom;

            currentBottom = drawSegment(canvas, x, barWidth, currentBottom, bottom, top, bar.natural, max, R.color.pet_primary);
            currentBottom = drawSegment(canvas, x, barWidth, currentBottom, bottom, top, bar.dry, max, R.color.pet_secondary);
            currentBottom = drawSegment(canvas, x, barWidth, currentBottom, bottom, top, bar.wet, max, R.color.pet_warning);

            bar.left = x;
            bar.right = x + barWidth;
            bar.top = currentBottom;
            bar.bottom = bottom;
            canvas.drawText(bar.label, x - 6f, bottom + 28f, textPaint);
        }

        float legendY = getHeight() - 18f;
        canvas.drawText("Natural", left, legendY, textPaint);
        canvas.drawText("Dry", left + 120f, legendY, textPaint);
        canvas.drawText("Wet", left + 200f, legendY, textPaint);
    }

    private float drawSegment(Canvas canvas, float x, float width, float currentBottom, float bottom, float top,
                              double value, double max, int colorRes) {
        if (value <= 0d) return currentBottom;

        float height = (float) ((value / max) * (bottom - top - 10f));
        float segmentTop = currentBottom - height;
        fillPaint.setColor(ContextCompat.getColor(getContext(), colorRes));
        canvas.drawRect(x, segmentTop, x + width, currentBottom, fillPaint);
        return segmentTop;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() != MotionEvent.ACTION_UP) return true;
        float x = event.getX();
        float y = event.getY();
        for (BarInfo bar : bars) {
            if (x >= bar.left && x <= bar.right && y >= bar.top && y <= bar.bottom) {
                double total = bar.natural + bar.dry + bar.wet;
                Toast.makeText(
                        getContext(),
                        bar.label + ": " + FormatUtils.number(total) + " g",
                        Toast.LENGTH_SHORT
                ).show();
                return true;
            }
        }
        return true;
    }

    private void buildBars() {
        bars.clear();

        Calendar now = Calendar.getInstance();
        if (range == FilterRange.YEAR) {
            int year = now.get(Calendar.YEAR);
            for (int month = 0; month < 12; month++) {
                Calendar start = Calendar.getInstance();
                start.clear();
                start.set(year, month, 1, 0, 0, 0);
                Calendar end = Calendar.getInstance();
                end.clear();
                end.set(year, month, start.getActualMaximum(Calendar.DAY_OF_MONTH), 23, 59, 59);

                BarInfo info = sumBetween(start.getTimeInMillis(), end.getTimeInMillis());
                info.label = new java.text.DateFormatSymbols(Locale.getDefault()).getShortMonths()[month];
                bars.add(info);
            }
            return;
        }

        if (range == FilterRange.MONTH) {
            int year = now.get(Calendar.YEAR);
            int month = now.get(Calendar.MONTH);
            int days = now.getActualMaximum(Calendar.DAY_OF_MONTH);
            for (int day = 1; day <= days; day++) {
                Calendar start = Calendar.getInstance();
                start.clear();
                start.set(year, month, day, 0, 0, 0);
                Calendar end = Calendar.getInstance();
                end.clear();
                end.set(year, month, day, 23, 59, 59);

                BarInfo info = sumBetween(start.getTimeInMillis(), end.getTimeInMillis());
                info.label = String.valueOf(day);
                bars.add(info);
            }
            return;
        }

        Calendar day = Calendar.getInstance();
        day.add(Calendar.DAY_OF_YEAR, -6);
        for (int i = 0; i < 7; i++) {
            Calendar start = (Calendar) day.clone();
            start.set(Calendar.HOUR_OF_DAY, 0);
            start.set(Calendar.MINUTE, 0);
            start.set(Calendar.SECOND, 0);
            start.set(Calendar.MILLISECOND, 0);

            Calendar end = (Calendar) start.clone();
            end.set(Calendar.HOUR_OF_DAY, 23);
            end.set(Calendar.MINUTE, 59);
            end.set(Calendar.SECOND, 59);

            BarInfo info = sumBetween(start.getTimeInMillis(), end.getTimeInMillis());
            info.label = FormatUtils.dayLabel(start.getTimeInMillis());
            bars.add(info);

            day.add(Calendar.DAY_OF_YEAR, 1);
        }
    }

    private BarInfo sumBetween(long start, long end) {
        BarInfo info = new BarInfo();
        for (FeedingLog log : logs) {
            if (log.completedAt < start || log.completedAt > end) continue;
            double grams = FormatUtils.parseLeadingNumber(log.portion);
            if (grams <= 0d) continue;

            String type = log.foodType == null ? "Dry food" : log.foodType.trim();
            if ("Natural".equalsIgnoreCase(type)) {
                info.natural += grams;
            } else if ("Wet food (canned)".equalsIgnoreCase(type)) {
                info.wet += grams;
            } else {
                info.dry += grams;
            }
        }
        return info;
    }

    private static class BarInfo {
        String label;
        double natural;
        double dry;
        double wet;
        float left;
        float top;
        float right;
        float bottom;
    }
}
