package com.example.petcare.ui.common;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;

import com.example.petcare.R;
import com.example.petcare.data.entities.WeightEntry;
import com.example.petcare.util.FormatUtils;
import com.example.petcare.util.ThemeUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class WeightChartView extends View {
    private static final float BASE_TEXT_SIZE = 26f;
    private final Paint axisPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pointPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint warningPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint cardPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint cardBorderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint areaPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pointOutlinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint valuePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF cardBounds = new RectF();
    private final Path trendPath = new Path();
    private final Path areaPath = new Path();
    private float gradientTop = Float.NaN;
    private float gradientBottom = Float.NaN;
    private int gradientAccent;
    private final List<PointInfo> points = new ArrayList<>();
    private List<WeightEntry> entries = new ArrayList<>();
    private ChartPeriod period = ChartPeriod.of(FilterRange.MONTH, System.currentTimeMillis());

    public WeightChartView(Context context, AttributeSet attrs) {
        super(context, attrs);
        axisPaint.setStrokeWidth(dp(0.75f));
        linePaint.setStrokeWidth(dp(1.6f));
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeCap(Paint.Cap.ROUND);
        linePaint.setStrokeJoin(Paint.Join.ROUND);
        cardBorderPaint.setStyle(Paint.Style.STROKE);
        cardBorderPaint.setStrokeWidth(dp(1f));
        labelPaint.setTextSize(BASE_TEXT_SIZE);
        valuePaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        setClickable(true);
    }

    public void setEntries(List<WeightEntry> items, ChartPeriod period) {
        entries = new ArrayList<>(items == null ? Collections.emptyList() : items);
        Collections.sort(entries, Comparator.comparingLong(item -> item.measuredAt));
        this.period = period == null
                ? ChartPeriod.of(FilterRange.MONTH, System.currentTimeMillis())
                : period;
        requestLayout();
        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = resolveSize(getSuggestedMinimumWidth(), widthMeasureSpec);
        labelPaint.setTextSize(BASE_TEXT_SIZE * (period.range == FilterRange.WEEK ? 2f : 1f));
        float lineHeight = labelPaint.descent() - labelPaint.ascent();
        float topSpace = Math.max(dp(24f), lineHeight + dp(8f));
        float dateSpace = Math.max(dp(24f), lineHeight * (period.range == FilterRange.WEEK ? 2f : 1f) + dp(14f));
        // Match the reference's compact card, allowing room for the larger weekly labels.
        int height = Math.round(Math.max(width / 2.46f, topSpace + dateSpace + dp(56f)));
        setMeasuredDimension(width, resolveSize(height, heightMeasureSpec));
    }

    @Override
    protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
        super.onSizeChanged(width, height, oldWidth, oldHeight);
        float inset = dp(0.5f);
        cardBounds.set(inset, inset, width - inset, height - inset);
        if (height > 0) {
            cardPaint.setShader(new LinearGradient(0f, 0f, 0f, height,
                    ContextCompat.getColor(getContext(), R.color.weight_chart_surface_start),
                    ContextCompat.getColor(getContext(), R.color.weight_chart_surface_end),
                    Shader.TileMode.CLAMP));
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int accent = ThemeUtils.getAccentColor(getContext());
        int secondary = ContextCompat.getColor(getContext(), R.color.pet_text_secondary);
        cardBorderPaint.setColor(ContextCompat.getColor(getContext(), R.color.pet_border));
        canvas.drawRoundRect(cardBounds, dp(22f), dp(22f), cardPaint);
        canvas.drawRoundRect(cardBounds, dp(22f), dp(22f), cardBorderPaint);
        axisPaint.setColor(ColorUtils.setAlphaComponent(secondary, 28));
        linePaint.setColor(accent);
        pointPaint.setColor(accent);
        pointOutlinePaint.setColor(ContextCompat.getColor(getContext(), R.color.weight_chart_surface_start));
        warningPaint.setColor(ContextCompat.getColor(getContext(), R.color.pet_warning));
        labelPaint.setColor(ColorUtils.setAlphaComponent(secondary, 155));
        float textSize = BASE_TEXT_SIZE * (period.range == FilterRange.WEEK ? 2f : 1f);
        labelPaint.setTextSize(textSize);
        valuePaint.setTextSize(textSize);
        points.clear();

        double min = Double.MAX_VALUE;
        double max = -Double.MAX_VALUE;
        for (WeightEntry entry : entries) {
            min = Math.min(min, entry.weightValue);
            max = Math.max(max, entry.weightValue);
        }
        if (entries.isEmpty()) {
            min = 0d;
            max = 1d;
        } else {
            // Small fluctuations should stay gentle instead of filling the entire card height.
            double padding = Math.max(0.5d, Math.max(max - min, Math.max(Math.abs(min), Math.abs(max)) * 0.15d));
            min = Math.max(0d, min - padding * 0.8d);
            max += padding * 0.2d;
        }

        Paint.FontMetrics metrics = labelPaint.getFontMetrics();
        float lineHeight = metrics.descent - metrics.ascent;
        float left = Math.max(dp(20f), getWidth() * 0.085f);
        float right = getWidth() - left;
        boolean splitWeekLabels = period.range == FilterRange.WEEK;
        float top = Math.max(dp(24f), lineHeight + dp(8f));
        float bottom = getHeight() - Math.max(dp(24f), lineHeight * (splitWeekLabels ? 2f : 1f) + dp(14f));
        if (right <= left || bottom <= top) return;

        canvas.drawLine(left, bottom, right, bottom, axisPaint);
        drawDateLabels(canvas, left, right, bottom, splitWeekLabels);

        if (entries.isEmpty()) {
            labelPaint.setColor(secondary);
            String rangeName = period.range == FilterRange.WEEK ? "week"
                    : period.range == FilterRange.YEAR ? "year" : "month";
            float center = (left + right) / 2f;
            String message = labelPaint.measureText("No weight data") <= right - left ? "No weight data" : "No data";
            boolean showRange = bottom - top >= lineHeight * 2f + 12f;
            float messageBaseline = (top + bottom) / 2f - (metrics.ascent + metrics.descent) / 2f
                    - (showRange ? lineHeight / 2f : 0f);
            drawCenteredText(canvas, message, center, messageBaseline, left, right, labelPaint);
            if (showRange) {
                drawCenteredText(canvas, "for this " + rangeName, center,
                        messageBaseline + lineHeight, left, right, labelPaint);
            }
            return;
        }

        float highestPoint = bottom;
        for (WeightEntry entry : entries) {
            PointInfo point = new PointInfo();
            point.entry = entry;
            point.x = xForTimestamp(entry.measuredAt, left, right);
            float normalized = (float) ((entry.weightValue - min) / (max - min));
            point.y = bottom - normalized * (bottom - top);
            point.outOfRange = (entry.healthyMin != null && entry.weightValue < entry.healthyMin)
                    || (entry.healthyMax != null && entry.weightValue > entry.healthyMax);
            point.label = formatWeight(entry.weightValue);
            points.add(point);
            highestPoint = Math.min(highestPoint, point.y);
        }
        drawTrend(canvas, bottom, highestPoint, accent);
        // Small outlined dots stay distinct against both the line and translucent fill.
        for (PointInfo point : points) {
            canvas.drawCircle(point.x, point.y, dp(3f), pointOutlinePaint);
            canvas.drawCircle(point.x, point.y, dp(2f), point.outOfRange ? warningPaint : pointPaint);
        }
        drawPointValues(canvas, left, right);
    }

    private void drawTrend(Canvas canvas, float bottom, float highestPoint, int accent) {
        if (points.size() < 2) return;
        PointInfo first = points.get(0);
        trendPath.reset();
        trendPath.moveTo(first.x, first.y);
        for (int index = 1; index < points.size(); index++) {
            PointInfo previous = points.get(index - 1);
            PointInfo current = points.get(index);
            float bend = (current.x - previous.x) * 0.35f;
            if (bend <= 0f) {
                trendPath.lineTo(current.x, current.y);
            } else {
                // Control points remain inside each pair's bounds: no artificial peaks.
                trendPath.cubicTo(previous.x + bend, previous.y,
                        current.x - bend, current.y, current.x, current.y);
            }
        }
        if (highestPoint != gradientTop || bottom != gradientBottom || accent != gradientAccent) {
            areaPaint.setShader(new LinearGradient(0f, highestPoint, 0f, bottom,
                    new int[]{ColorUtils.setAlphaComponent(accent, 88),
                            ColorUtils.setAlphaComponent(accent, 28), ColorUtils.setAlphaComponent(accent, 0)},
                    new float[]{0f, 0.6f, 1f}, Shader.TileMode.CLAMP));
            gradientTop = highestPoint;
            gradientBottom = bottom;
            gradientAccent = accent;
        }
        areaPath.set(trendPath);
        areaPath.lineTo(points.get(points.size() - 1).x, bottom);
        areaPath.lineTo(first.x, bottom);
        areaPath.close();
        canvas.drawPath(areaPath, areaPaint);
        canvas.drawPath(trendPath, linePaint);
    }

    private void drawPointValues(Canvas canvas, float left, float right) {
        float widestLabel = 0f;
        for (PointInfo point : points) {
            widestLabel = Math.max(widestLabel, valuePaint.measureText(point.label));
        }
        int interval = 1;
        if (period.range != FilterRange.WEEK && points.size() > 1) {
            float averageSpacing = (points.get(points.size() - 1).x - points.get(0).x) / (points.size() - 1);
            if (averageSpacing < widestLabel + 12f) {
                interval = averageSpacing * 2f >= widestLabel + 12f ? 2 : 3;
            }
        }
        Paint.FontMetrics metrics = valuePaint.getFontMetrics();
        List<RectF> drawnLabels = new ArrayList<>();
        // Prioritize the latest value. Dense periods show every second/third value;
        // the bounds check also handles irregularly spaced or same-day measurements.
        for (int index = points.size() - 1; index >= 0; index--) {
            if (index != points.size() - 1 && index % interval != 0) continue;
            PointInfo point = points.get(index);
            float width = valuePaint.measureText(point.label);
            float x = Math.max(dp(12f), Math.min(point.x - width / 2f, getWidth() - dp(12f) - width));
            float baseline = point.y - dp(7f);
            RectF bounds = new RectF(x - 6f, baseline + metrics.ascent - 4f,
                    x + width + 6f, baseline + metrics.descent + 4f);
            boolean overlaps = false;
            for (RectF drawn : drawnLabels) {
                if (RectF.intersects(drawn, bounds)) {
                    overlaps = true;
                    break;
                }
            }
            if (overlaps) continue;
            valuePaint.setColor(point.outOfRange ? warningPaint.getColor() : pointPaint.getColor());
            canvas.drawText(point.label, x, baseline, valuePaint);
            drawnLabels.add(bounds);
        }
    }

    private void drawDateLabels(Canvas canvas, float left, float right, float bottom, boolean splitWeekLabels) {
        Paint.FontMetrics metrics = labelPaint.getFontMetrics();
        float baseline = bottom + dp(5f) - metrics.ascent;
        float lineHeight = metrics.descent - metrics.ascent;
        float lastLabelRight = -Float.MAX_VALUE;
        for (int index = 0; index < period.buckets.size(); index++) {
            ChartPeriod.Bucket bucket = period.buckets.get(index);
            if (period.range == FilterRange.MONTH
                    && index != 0 && index != 9 && index != 19 && index != period.buckets.size() - 1) continue;
            long timestamp = period.range == FilterRange.MONTH ? bucket.startMillis
                    : bucket.startMillis + (bucket.endMillis - bucket.startMillis) / 2L;
            float x = xForTimestamp(timestamp, left, right);
            String firstLine = bucket.label;
            String secondLine = null;
            if (splitWeekLabels) {
                int separator = firstLine.lastIndexOf(' ');
                if (separator > 0) {
                    secondLine = firstLine.substring(separator + 1);
                    firstLine = firstLine.substring(0, separator);
                }
            }
            float width = labelPaint.measureText(firstLine);
            if (secondLine != null) width = Math.max(width, labelPaint.measureText(secondLine));
            float textLeft = Math.max(dp(12f), Math.min(x - width / 2f, getWidth() - dp(12f) - width));
            if (textLeft < lastLabelRight + 10f) continue;
            float center = textLeft + width / 2f;
            drawCenteredText(canvas, firstLine, center, baseline, dp(12f), getWidth() - dp(12f), labelPaint);
            if (secondLine != null) {
                drawCenteredText(canvas, secondLine, center, baseline + lineHeight, dp(12f), getWidth() - dp(12f), labelPaint);
            }
            lastLabelRight = textLeft + width;
        }
    }

    private float xForTimestamp(long timestamp, float left, float right) {
        long duration = Math.max(1L, period.endMillis - period.startMillis);
        float fraction = (float) ((timestamp - period.startMillis) / (double) duration);
        return left + (right - left) * Math.max(0f, Math.min(1f, fraction));
    }

    private void drawCenteredText(Canvas canvas, String text, float center, float baseline,
                                  float left, float right, Paint paint) {
        float x = Math.max(left, Math.min(center - paint.measureText(text) / 2f,
                right - paint.measureText(text)));
        canvas.drawText(text, x, baseline, paint);
    }

    private String formatWeight(double value) {
        return String.format(Locale.getDefault(), "%.1f", value);
    }

    private float dp(float value) {
        return value * getResources().getDisplayMetrics().density;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getActionMasked() != MotionEvent.ACTION_UP) return true;
        float nearestDistanceSquared = dp(18f) * dp(18f);
        PointInfo nearest = null;
        for (PointInfo point : points) {
            float dx = event.getX() - point.x;
            float dy = event.getY() - point.y;
            float distanceSquared = dx * dx + dy * dy;
            if (distanceSquared <= nearestDistanceSquared) {
                nearest = point;
                nearestDistanceSquared = distanceSquared;
            }
        }
        if (nearest != null) {
            WeightEntry entry = nearest.entry;
            String unit = entry.unit == null || entry.unit.trim().isEmpty() ? "kg" : entry.unit;
            Toast.makeText(getContext(), String.format(Locale.getDefault(), "%.1f %s · %s",
                    entry.weightValue, unit, FormatUtils.dateTime(entry.measuredAt)), Toast.LENGTH_SHORT).show();
        }
        return true;
    }

    @Override
    public boolean performClick() {
        super.performClick();
        return true;
    }

    private static class PointInfo {
        WeightEntry entry;
        String label;
        boolean outOfRange;
        float x;
        float y;
    }
}
