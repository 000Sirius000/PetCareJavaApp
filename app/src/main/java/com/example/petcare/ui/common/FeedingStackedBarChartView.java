package com.example.petcare.ui.common;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
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
import java.util.List;

public class FeedingStackedBarChartView extends View {
    private static final float DEFAULT_TEXT_SIZE = 22f;
    private static final int COLOR_NATURAL = 0xFF4CAF50;
    private static final int COLOR_DRY = 0xFFFFD600;
    private static final int COLOR_WET = 0xFF6EC6FF;

    private final Paint axisPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint tooltipPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint tooltipBorderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path roundedBarPath = new Path();
    private final List<BarInfo> bars = new ArrayList<>();

    private List<FeedingLog> logs = new ArrayList<>();
    private List<FeedingSchedule> schedules = new ArrayList<>();
    private ChartPeriod period = ChartPeriod.of(FilterRange.WEEK, System.currentTimeMillis());
    private int selectedBarIndex = -1;

    public FeedingStackedBarChartView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setClickable(true);
        axisPaint.setStrokeWidth(3f);
        textPaint.setTextSize(DEFAULT_TEXT_SIZE);
        tooltipPaint.setColor(0xFF1A1A1A);
        tooltipPaint.setStyle(Paint.Style.FILL);
        tooltipBorderPaint.setColor(0xFF3A3A3A);
        tooltipBorderPaint.setStyle(Paint.Style.STROKE);
        tooltipBorderPaint.setStrokeWidth(2f);
    }

    public void setData(List<FeedingLog> logs, List<FeedingSchedule> schedules, ChartPeriod period) {
        this.logs = logs == null ? new ArrayList<>() : new ArrayList<>(logs);
        this.schedules = schedules == null ? new ArrayList<>() : new ArrayList<>(schedules);
        this.period = period == null
                ? ChartPeriod.of(FilterRange.WEEK, System.currentTimeMillis())
                : period;
        this.selectedBarIndex = -1;
        invalidate();
    }

    public void setData(List<FeedingLog> logs, ChartPeriod period) {
        setData(logs, null, period);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        axisPaint.setColor(ContextCompat.getColor(getContext(), R.color.pet_border));
        textPaint.setColor(ContextCompat.getColor(getContext(), R.color.pet_text_secondary));
        boolean weekly = period.range == FilterRange.WEEK;
        textPaint.setTextSize(weekly ? DEFAULT_TEXT_SIZE * 2f : DEFAULT_TEXT_SIZE);
        Paint.FontMetrics metrics = textPaint.getFontMetrics();
        float lineHeight = metrics.descent - metrics.ascent;

        float left = weekly ? Math.max(72f, textPaint.measureText("kg") + 30f) : 72f;
        float right = getWidth() - 20f;
        float top = weekly ? Math.max(34f, 8f - metrics.ascent) : 34f;
        float bottom = getHeight() - (weekly ? 68f + 2f * lineHeight : 86f);

        canvas.drawLine(left, bottom, right, bottom, axisPaint);
        canvas.drawLine(left, top, left, bottom, axisPaint);
        canvas.drawText("kg", 18f, weekly ? top : top + 18f, textPaint);

        buildBars();
        if (bars.isEmpty()) {
            canvas.drawText("No feeding data yet", left + 20f, bottom - 20f, textPaint);
            textPaint.setTextSize(DEFAULT_TEXT_SIZE);
            drawLegend(canvas, left, getHeight() - 26f);
            return;
        }

        double max = 0d;
        for (BarInfo bar : bars) max = Math.max(max, bar.total());
        if (max <= 0d) max = 1d;

        float slotWidth = (right - left) / bars.size();
        float barWidth = Math.max(8f, slotWidth * 0.58f);
        int tickStride = weekly ? weeklyTickStride(slotWidth) : 1;
        float lastValueRight = Float.NEGATIVE_INFINITY;
        for (int i = 0; i < bars.size(); i++) {
            BarInfo bar = bars.get(i);
            float x = left + i * slotWidth + (slotWidth - barWidth) / 2f;
            float currentBottom = bottom;
            if (bar.total() <= 0d) {
                fillPaint.setColor(ContextCompat.getColor(getContext(), R.color.pet_border));
                canvas.drawRoundRect(x, bottom - 2f, x + barWidth, bottom, 1f, 1f, fillPaint);
                bar.top = bottom - 2f;
            } else {
                float totalHeight = (float) ((bar.total() / max) * (bottom - top - 12f));
                bar.top = bottom - totalHeight;
                float radius = Math.min(4f * getResources().getDisplayMetrics().density,
                        Math.min(barWidth * 0.2f, totalHeight / 2f));
                // Clip the whole stack once so adjacent food segments keep a seamless join.
                roundedBarPath.reset();
                roundedBarPath.addRoundRect(x, bar.top, x + barWidth, bottom,
                        radius, radius, Path.Direction.CW);
                canvas.save();
                canvas.clipPath(roundedBarPath);
                currentBottom = drawSegment(canvas, x, barWidth, currentBottom, bottom, top, bar.natural, max, COLOR_NATURAL);
                currentBottom = drawSegment(canvas, x, barWidth, currentBottom, bottom, top, bar.dry, max, COLOR_DRY);
                currentBottom = drawSegment(canvas, x, barWidth, currentBottom, bottom, top, bar.wet, max, COLOR_WET);
                canvas.restore();
                bar.top = currentBottom;
                lastValueRight = drawValueLabel(canvas, FormatUtils.kilogramsFromGrams(bar.total()),
                        x + barWidth / 2f, bar.top - 8f, lastValueRight, weekly);
            }
            bar.left = x; bar.right = x + barWidth; bar.bottom = bottom;
            if (shouldShowXAxisLabel(i, bar.label) && i % tickStride == 0) {
                drawXAxisLabel(canvas, bar.label, x + barWidth / 2f, bottom, weekly, metrics);
            }
        }

        // Keep the legend and tap details compact; only the weekly plot labels grow.
        textPaint.setTextSize(DEFAULT_TEXT_SIZE);
        drawLegend(canvas, left, getHeight() - 26f);
        if (selectedBarIndex >= 0 && selectedBarIndex < bars.size()) drawTooltip(canvas, bars.get(selectedBarIndex), selectedBarIndex, bars.size());
    }

    private int weeklyTickStride(float slotWidth) {
        float widest = 0f;
        for (BarInfo bar : bars) {
            int split = bar.label.lastIndexOf(' ');
            if (split > 0) {
                widest = Math.max(widest, textPaint.measureText(bar.label.substring(0, split)));
                widest = Math.max(widest, textPaint.measureText(bar.label.substring(split + 1)));
            } else {
                widest = Math.max(widest, textPaint.measureText(bar.label));
            }
        }
        return Math.max(1, (int) Math.ceil((widest + 8f) / Math.max(1f, slotWidth)));
    }

    private float drawValueLabel(Canvas canvas, String label, float centerX, float baseline,
                                 float previousRight, boolean weekly) {
        float textLeft = centeredTextLeft(label, centerX);
        if (weekly && textLeft < previousRight + 8f) return previousRight;
        canvas.drawText(label, textLeft, baseline, textPaint);
        return textLeft + textPaint.measureText(label);
    }

    private void drawXAxisLabel(Canvas canvas, String label, float centerX, float bottom,
                                boolean weekly, Paint.FontMetrics metrics) {
        int split = weekly ? label.lastIndexOf(' ') : -1;
        float baseline = weekly ? bottom + 10f - metrics.ascent : bottom + 28f;
        if (split > 0) {
            drawCenteredText(canvas, label.substring(0, split), centerX, baseline);
            drawCenteredText(canvas, label.substring(split + 1), centerX,
                    baseline + metrics.descent - metrics.ascent + 4f);
        } else {
            drawCenteredText(canvas, label, centerX, baseline);
        }
    }

    private void drawCenteredText(Canvas canvas, String label, float centerX, float baseline) {
        canvas.drawText(label, centeredTextLeft(label, centerX), baseline, textPaint);
    }

    private float centeredTextLeft(String label, float centerX) {
        float halfWidth = textPaint.measureText(label) / 2f;
        float safeCenter = Math.max(halfWidth + 4f, Math.min(getWidth() - halfWidth - 4f, centerX));
        return safeCenter - halfWidth;
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
        canvas.drawText("Total: " + FormatUtils.kilogramsFromGrams(bar.total()) + " kg", x + 12f, y + 104f, textPaint);
        textPaint.setColor(ContextCompat.getColor(getContext(), R.color.pet_text_secondary));
    }

    private void drawTooltipLine(Canvas canvas, float x, float y, int color, String label, double grams) {
        fillPaint.setColor(color);
        canvas.drawRect(x, y - 11f, x + 11f, y, fillPaint);
        canvas.drawText(label + ": " + (grams > 0d ? FormatUtils.kilogramsFromGrams(grams) + " kg" : "—"), x + 18f, y, textPaint);
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
        if (period.range != FilterRange.MONTH) return true;
        try {
            int day = Integer.parseInt(label);
            return day == 1 || day == 5 || day == 10 || day == 15 || day == 20 || day == 25 || day == 30;
        } catch (Exception ignored) {
            return index == 0 || index % 5 == 4;
        }
    }

    private void buildBars() {
        bars.clear();
        for (ChartPeriod.Bucket bucket : period.buckets) {
            BarInfo info = sumBetween(bucket.startMillis, bucket.endMillis);
            info.label = bucket.label;
            bars.add(info);
        }
    }

    private BarInfo sumBetween(long start, long end) {
        BarInfo info = new BarInfo();
        for (FeedingLog log : logs) {
            if (log.completedAt < start || log.completedAt > end) continue;
            add(info, log.foodType, FormatUtils.parseLeadingNumber(log.portion));
        }
        for (FeedingSchedule schedule : schedules) {
            long t = schedule.createdAtEpochMillis > 0L ? schedule.createdAtEpochMillis : period.endMillis;
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
