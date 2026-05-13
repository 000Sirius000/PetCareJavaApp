package com.example.petcare.ui.common;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.core.content.ContextCompat;

import com.example.petcare.R;
import com.example.petcare.data.entities.FeedingLog;
import com.example.petcare.data.entities.FeedingSchedule;
import com.example.petcare.util.FormatUtils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class FeedingStackedBarChartView extends View {
    private static final int COLOR_NATURAL = 0xFF4CAF50;
    private static final int COLOR_DRY = 0xFFFFD600;
    private static final int COLOR_WET = 0xFF6EC6FF;

    private final Paint axisPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint tooltipPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint tooltipBorderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final List<BarInfo> bars = new ArrayList<>();

    private List<FeedingLog> logs = new ArrayList<>();
    private List<FeedingSchedule> schedules = new ArrayList<>();
    private FilterRange range = FilterRange.WEEK;
    private int selectedBarIndex = -1;

    public FeedingStackedBarChartView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setClickable(true);
        axisPaint.setStrokeWidth(3f);
        textPaint.setTextSize(22f);
        tooltipPaint.setColor(0xFF1A1A1A);
        tooltipPaint.setStyle(Paint.Style.FILL);
        tooltipBorderPaint.setColor(0xFF3A3A3A);
        tooltipBorderPaint.setStyle(Paint.Style.STROKE);
        tooltipBorderPaint.setStrokeWidth(2f);
    }

    public void setData(List<FeedingLog> logs, List<FeedingSchedule> schedules, FilterRange range) {
        this.logs = logs == null ? new ArrayList<>() : new ArrayList<>(logs);
        this.schedules = schedules == null ? new ArrayList<>() : new ArrayList<>(schedules);
        this.range = range == null ? FilterRange.WEEK : range;
        this.selectedBarIndex = -1;
        invalidate();
    }

    public void setData(List<FeedingLog> logs, FilterRange range) {
        setData(logs, null, range);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        axisPaint.setColor(ContextCompat.getColor(getContext(), R.color.pet_border));
        textPaint.setColor(ContextCompat.getColor(getContext(), R.color.pet_text_secondary));

        float left = 72f;
        float right = getWidth() - 20f;
        float top = 34f;
        float bottom = getHeight() - 86f;

        canvas.drawLine(left, bottom, right, bottom, axisPaint);
        canvas.drawLine(left, top, left, bottom, axisPaint);
        canvas.drawText("g", 18f, top + 18f, textPaint);

        buildBars();
        if (bars.isEmpty()) {
            canvas.drawText("No feeding data yet", left + 20f, bottom - 20f, textPaint);
            drawLegend(canvas, left, getHeight() - 26f);
            return;
        }

        double max = 0d;
        for (BarInfo bar : bars) max = Math.max(max, bar.total());
        if (max <= 0d) max = 1d;

        float slotWidth = (right - left) / bars.size();
        float barWidth = Math.max(8f, slotWidth * 0.58f);
        for (int i = 0; i < bars.size(); i++) {
            BarInfo bar = bars.get(i);
            float x = left + i * slotWidth + (slotWidth - barWidth) / 2f;
            float currentBottom = bottom;
            if (bar.total() <= 0d) {
                fillPaint.setColor(ContextCompat.getColor(getContext(), R.color.pet_border));
                canvas.drawRect(x, bottom - 2f, x + barWidth, bottom, fillPaint);
                bar.top = bottom - 2f;
            } else {
                currentBottom = drawSegment(canvas, x, barWidth, currentBottom, bottom, top, bar.natural, max, COLOR_NATURAL);
                currentBottom = drawSegment(canvas, x, barWidth, currentBottom, bottom, top, bar.dry, max, COLOR_DRY);
                currentBottom = drawSegment(canvas, x, barWidth, currentBottom, bottom, top, bar.wet, max, COLOR_WET);
                bar.top = currentBottom;
                canvas.drawText(FormatUtils.number(bar.total()), x + 2f, bar.top - 8f, textPaint);
            }
            bar.left = x; bar.right = x + barWidth; bar.bottom = bottom;
            if (shouldShowXAxisLabel(i, bar.label)) canvas.drawText(bar.label, x - 8f, bottom + 28f, textPaint);
        }

        drawLegend(canvas, left, getHeight() - 26f);
        if (selectedBarIndex >= 0 && selectedBarIndex < bars.size()) drawTooltip(canvas, bars.get(selectedBarIndex), selectedBarIndex, bars.size());
    }

    private float drawSegment(Canvas canvas, float x, float width, float currentBottom, float bottom, float top, double value, double max, int color) {
        if (value <= 0d) return currentBottom;
        float height = (float) ((value / max) * (bottom - top - 12f));
        float segmentTop = currentBottom - height;
        fillPaint.setColor(color);
        canvas.drawRect(x, segmentTop, x + width, currentBottom, fillPaint);
        return segmentTop;
    }

    private void drawLegend(Canvas canvas, float left, float y) {
        drawLegendItem(canvas, left, y, COLOR_NATURAL, "Natural");
        drawLegendItem(canvas, left + 108f, y, COLOR_DRY, "Dry food");
        drawLegendItem(canvas, left + 224f, y, COLOR_WET, "Wet food");
    }

    private void drawLegendItem(Canvas canvas, float x, float y, int color, String label) {
        fillPaint.setColor(color);
        canvas.drawRect(x, y - 12f, x + 14f, y + 2f, fillPaint);
        canvas.drawText(label, x + 20f, y + 2f, textPaint);
    }

    private void drawTooltip(Canvas canvas, BarInfo bar, int index, int count) {
        float width = 178f;
        float height = 108f;
        float x = index >= count / 2f ? Math.max(8f, bar.right - width) : Math.min(getWidth() - width - 8f, bar.left);
        float y = Math.max(8f, bar.top - height - 12f);
        RectF box = new RectF(x, y, x + width, y + height);
        canvas.drawRoundRect(box, 14f, 14f, tooltipPaint);
        canvas.drawRoundRect(box, 14f, 14f, tooltipBorderPaint);

        textPaint.setColor(ContextCompat.getColor(getContext(), R.color.pet_text_primary));
        canvas.drawText(bar.label, x + 12f, y + 22f, textPaint);
        textPaint.setColor(ContextCompat.getColor(getContext(), R.color.pet_text_secondary));
        drawTooltipLine(canvas, x + 12f, y + 45f, COLOR_NATURAL, "Natural", bar.natural);
        drawTooltipLine(canvas, x + 12f, y + 66f, COLOR_DRY, "Dry food", bar.dry);
        drawTooltipLine(canvas, x + 12f, y + 87f, COLOR_WET, "Wet food", bar.wet);
        textPaint.setColor(ContextCompat.getColor(getContext(), R.color.pet_text_primary));
        canvas.drawText("Total: " + FormatUtils.number(bar.total()) + " g", x + 12f, y + 104f, textPaint);
        textPaint.setColor(ContextCompat.getColor(getContext(), R.color.pet_text_secondary));
    }

    private void drawTooltipLine(Canvas canvas, float x, float y, int color, String label, double grams) {
        fillPaint.setColor(color);
        canvas.drawRect(x, y - 11f, x + 11f, y, fillPaint);
        canvas.drawText(label + ": " + (grams > 0d ? FormatUtils.number(grams) + " g" : "—"), x + 18f, y, textPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() != MotionEvent.ACTION_UP) return true;
        for (int i = 0; i < bars.size(); i++) {
            BarInfo bar = bars.get(i);
            if (event.getX() >= bar.left && event.getX() <= bar.right && event.getY() >= bar.top && event.getY() <= bar.bottom) {
                selectedBarIndex = selectedBarIndex == i ? -1 : i;
                invalidate();
                return true;
            }
        }
        selectedBarIndex = -1;
        invalidate();
        return true;
    }

    private boolean shouldShowXAxisLabel(int index, String label) {
        if (range != FilterRange.MONTH) return true;
        try {
            int day = Integer.parseInt(label);
            return day == 1 || day == 5 || day == 10 || day == 15 || day == 20 || day == 25 || day == 30;
        } catch (Exception ignored) {
            return index == 0 || index % 5 == 4;
        }
    }

    private void buildBars() {
        bars.clear();
        Calendar now = Calendar.getInstance();
        if (range == FilterRange.YEAR) {
            int year = now.get(Calendar.YEAR);
            for (int month = 0; month < 12; month++) {
                Calendar start = Calendar.getInstance(); start.clear(); start.set(year, month, 1, 0, 0, 0);
                Calendar end = Calendar.getInstance(); end.clear(); end.set(year, month, start.getActualMaximum(Calendar.DAY_OF_MONTH), 23, 59, 59);
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
                Calendar start = Calendar.getInstance(); start.clear(); start.set(year, month, day, 0, 0, 0);
                Calendar end = Calendar.getInstance(); end.clear(); end.set(year, month, day, 23, 59, 59);
                BarInfo info = sumBetween(start.getTimeInMillis(), end.getTimeInMillis());
                info.label = String.valueOf(day);
                bars.add(info);
            }
            return;
        }
        Calendar day = Calendar.getInstance();
        day.add(Calendar.DAY_OF_YEAR, -6);
        for (int i = 0; i < 7; i++) {
            Calendar start = (Calendar) day.clone(); start.set(Calendar.HOUR_OF_DAY, 0); start.set(Calendar.MINUTE, 0); start.set(Calendar.SECOND, 0); start.set(Calendar.MILLISECOND, 0);
            Calendar end = (Calendar) start.clone(); end.set(Calendar.HOUR_OF_DAY, 23); end.set(Calendar.MINUTE, 59); end.set(Calendar.SECOND, 59);
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
            add(info, log.foodType, FormatUtils.parseLeadingNumber(log.portion));
        }
        for (FeedingSchedule schedule : schedules) {
            long t = schedule.createdAtEpochMillis > 0L ? schedule.createdAtEpochMillis : System.currentTimeMillis();
            if (t < start || t > end) continue;
            add(info, schedule.foodType, FormatUtils.parseLeadingNumber(schedule.portion));
        }
        return info;
    }

    private void add(BarInfo info, String type, double grams) {
        if (grams <= 0d) return;
        String normalized = type == null ? "Dry food" : type.trim();
        if ("Natural".equalsIgnoreCase(normalized)) info.natural += grams;
        else if ("Wet food (canned)".equalsIgnoreCase(normalized) || "Wet food".equalsIgnoreCase(normalized)) info.wet += grams;
        else info.dry += grams;
    }

    private static class BarInfo {
        String label;
        double natural, dry, wet;
        float left, top, right, bottom;
        double total() { return natural + dry + wet; }
    }
}
