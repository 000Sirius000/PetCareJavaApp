package com.example.petcare.ui.common;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ActivityBarChartView extends View {
    public static class BarPoint {
        public final String label;
        public final int totalMinutes;
        public final int sessionCount;

        public BarPoint(String label, int totalMinutes, int sessionCount) {
            this.label = label;
            this.totalMinutes = totalMinutes;
            this.sessionCount = sessionCount;
        }
    }

    private final Paint axisPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint barPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint valuePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final List<BarPoint> points = new ArrayList<>();
    private final List<RectF> barBounds = new ArrayList<>();

    public ActivityBarChartView(Context context, AttributeSet attrs) {
        super(context, attrs);
        axisPaint.setColor(Color.GRAY);
        axisPaint.setStrokeWidth(3f);
        barPaint.setColor(0xFF5C6BC0);
        labelPaint.setColor(Color.DKGRAY);
        labelPaint.setTextSize(24f);
        valuePaint.setColor(Color.DKGRAY);
        valuePaint.setTextSize(26f);
    }

    public void setPoints(List<BarPoint> items) {
        points.clear();
        if (items != null) {
            points.addAll(items);
        }
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float left = 70f;
        float right = getWidth() - 24f;
        float top = 24f;
        float bottom = getHeight() - 56f;

        canvas.drawLine(left, bottom, right, bottom, axisPaint);
        canvas.drawLine(left, top, left, bottom, axisPaint);

        barBounds.clear();

        if (points.isEmpty()) {
            canvas.drawText("No walking data yet", left + 24f, bottom - 20f, labelPaint);
            return;
        }

        int max = 1;
        for (BarPoint point : points) {
            max = Math.max(max, point.totalMinutes);
        }

        float availableWidth = right - left;
        float gap = points.size() <= 12 ? 10f : 4f;
        float barWidth = Math.max(8f, (availableWidth - gap * (points.size() - 1)) / points.size());
        float chartHeight = bottom - top;

        for (int i = 0; i < points.size(); i++) {
            BarPoint point = points.get(i);
            float x = left + i * (barWidth + gap);
            float barHeight = (point.totalMinutes / (float) max) * chartHeight;
            RectF rect = new RectF(x, bottom - barHeight, x + barWidth, bottom);
            barBounds.add(rect);
            canvas.drawRoundRect(rect, 8f, 8f, barPaint);

            if (point.totalMinutes > 0) {
                canvas.drawText(String.valueOf(point.totalMinutes), x, rect.top - 8f, valuePaint);
            }

            if (points.size() <= 10 || i % Math.max(1, points.size() / 8) == 0 || i == points.size() - 1) {
                canvas.drawText(point.label, x, bottom + 28f, labelPaint);
            }
        }

        canvas.drawText("Minutes", 10f, top + 16f, valuePaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_UP) {
            for (int i = 0; i < barBounds.size(); i++) {
                RectF rect = barBounds.get(i);
                if (rect.contains(event.getX(), event.getY())) {
                    BarPoint point = points.get(i);
                    String message = String.format(Locale.getDefault(),
                            "%s • %d min • %d session%s",
                            point.label,
                            point.totalMinutes,
                            point.sessionCount,
                            point.sessionCount == 1 ? "" : "s");
                    Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
                    return true;
                }
            }
        }
        return true;
    }
}
