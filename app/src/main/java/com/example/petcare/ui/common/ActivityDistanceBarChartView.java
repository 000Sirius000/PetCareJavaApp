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
import com.example.petcare.data.entities.ActivitySession;
import com.example.petcare.util.FormatUtils;
import com.example.petcare.util.ThemeUtils;

import java.util.ArrayList;
import java.util.List;

public class ActivityDistanceBarChartView extends View {
    private static final float DEFAULT_TEXT_SIZE = 22f;
    private final Paint axisPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint barPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final List<BarInfo> bars = new ArrayList<>();

    private List<ActivitySession> sessions = new ArrayList<>();
    private ChartPeriod period = ChartPeriod.of(FilterRange.WEEK, System.currentTimeMillis());

    public ActivityDistanceBarChartView(Context context, AttributeSet attrs) {
        super(context, attrs);
        axisPaint.setStrokeWidth(3f);
        textPaint.setTextSize(DEFAULT_TEXT_SIZE);
        setClickable(true);
    }

    public void setData(List<ActivitySession> sessions, ChartPeriod period) {
        this.sessions = sessions == null ? new ArrayList<>() : new ArrayList<>(sessions);
        this.period = period == null
                ? ChartPeriod.of(FilterRange.WEEK, System.currentTimeMillis())
                : period;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        axisPaint.setColor(ContextCompat.getColor(getContext(), R.color.pet_border));
        barPaint.setColor(ThemeUtils.getActivityDistanceColor(getContext()));
        textPaint.setColor(ContextCompat.getColor(getContext(), R.color.pet_text_secondary));
        boolean weekly = period.range == FilterRange.WEEK;
        textPaint.setTextSize(weekly ? DEFAULT_TEXT_SIZE * 2f : DEFAULT_TEXT_SIZE);
        Paint.FontMetrics metrics = textPaint.getFontMetrics();
        float lineHeight = metrics.descent - metrics.ascent;

        float left = weekly ? Math.max(76f, textPaint.measureText("km") + 24f) : 76f;
        float right = getWidth() - 20f;
        float top = weekly ? Math.max(34f, 8f - metrics.ascent) : 34f;
        float bottom = getHeight() - (weekly ? 24f + 2f * lineHeight : 58f);

        canvas.drawLine(left, bottom, right, bottom, axisPaint);
        canvas.drawLine(left, top, left, bottom, axisPaint);
        canvas.drawText("km", 12f, weekly ? top : top + 18f, textPaint);

        buildBars();
        if (bars.isEmpty()) {
            canvas.drawText("No distance data yet", left + 20f, bottom - 20f, textPaint);
            return;
        }

        double max = 0d;
        for (BarInfo bar : bars) max = Math.max(max, bar.distanceKm);
        if (max < 1d) max = 1d;

        float slotWidth = (right - left) / bars.size();
        float barWidth = Math.max(8f, slotWidth * 0.58f);
        int tickStride = weekly ? weeklyTickStride(slotWidth) : 1;
        float lastValueRight = Float.NEGATIVE_INFINITY;

        for (int i = 0; i < bars.size(); i++) {
            BarInfo bar = bars.get(i);
            float x = left + i * slotWidth + (slotWidth - barWidth) / 2f;
            float height = bar.distanceKm <= 0d ? 2f : (float) ((bar.distanceKm / max) * (bottom - top - 12f));
            float y = bottom - height;
            bar.left = x; bar.top = y; bar.right = x + barWidth; bar.bottom = bottom;
            float radius = Math.min(4f * getResources().getDisplayMetrics().density,
                    Math.min(barWidth * 0.2f, height / 2f));
            canvas.drawRoundRect(bar.left, bar.top, bar.right, bar.bottom, radius, radius, barPaint);
            if (bar.distanceKm > 0d) {
                lastValueRight = drawValueLabel(canvas, FormatUtils.number(bar.distanceKm),
                        x + barWidth / 2f, y - 8f, lastValueRight, weekly);
            }
            if (shouldShowXAxisLabel(i, bar.label) && i % tickStride == 0) {
                drawXAxisLabel(canvas, bar.label, x + barWidth / 2f, bottom, weekly, metrics);
            }
        }
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

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() != MotionEvent.ACTION_UP) return true;
        for (BarInfo bar : bars) {
            if (event.getX() >= bar.left && event.getX() <= bar.right && event.getY() >= bar.top && event.getY() <= bar.bottom) {
                Toast.makeText(getContext(), bar.label + ": " + FormatUtils.number(bar.distanceKm) + " km · " + bar.sessionCount + " sessions", Toast.LENGTH_SHORT).show();
                return true;
            }
        }
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
            BarInfo info = sumDistance(bucket.startMillis, bucket.endMillis);
            info.label = bucket.label;
            bars.add(info);
        }
    }

    private BarInfo sumDistance(long start, long end) {
        BarInfo info = new BarInfo();
        for (ActivitySession item : sessions) {
            if (!supportsDistance(item.activityType) || item.distance == null) continue;
            if (item.sessionDateEpochMillis >= start && item.sessionDateEpochMillis <= end) {
                info.distanceKm += Math.max(0d, item.distance);
                info.sessionCount++;
            }
        }
        return info;
    }

    private boolean supportsDistance(String type) {
        return "walk".equalsIgnoreCase(type) || "run".equalsIgnoreCase(type);
    }

    private static class BarInfo {
        String label;
        double distanceKm;
        int sessionCount;
        float left, top, right, bottom;
    }
}
