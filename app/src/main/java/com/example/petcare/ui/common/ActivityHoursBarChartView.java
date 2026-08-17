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

public class ActivityHoursBarChartView extends View {
    private final Paint axisPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint barPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final List<BarInfo> bars = new ArrayList<>();

    private List<ActivitySession> sessions = new ArrayList<>();
    private ChartPeriod period = ChartPeriod.of(FilterRange.WEEK, System.currentTimeMillis());

    public ActivityHoursBarChartView(Context context, AttributeSet attrs) {
        super(context, attrs);
        axisPaint.setStrokeWidth(3f);
        textPaint.setTextSize(22f);
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
        barPaint.setColor(ThemeUtils.getAccentColor(getContext()));
        textPaint.setColor(ContextCompat.getColor(getContext(), R.color.pet_text_secondary));

        float left = 76f;
        float right = getWidth() - 20f;
        float top = 34f;
        float bottom = getHeight() - 58f;

        canvas.drawLine(left, bottom, right, bottom, axisPaint);
        canvas.drawLine(left, top, left, bottom, axisPaint);
        canvas.drawText("h", 12f, top + 18f, textPaint);

        buildBars();
        if (bars.isEmpty()) {
            canvas.drawText("No activity data yet", left + 20f, bottom - 20f, textPaint);
            return;
        }

        double max = 0d;
        for (BarInfo bar : bars) max = Math.max(max, bar.minutes);
        if (max < 1d) max = 1d;

        float slotWidth = (right - left) / bars.size();
        float barWidth = Math.max(8f, slotWidth * 0.58f);

        for (int i = 0; i < bars.size(); i++) {
            BarInfo bar = bars.get(i);
            float x = left + i * slotWidth + (slotWidth - barWidth) / 2f;
            float height = bar.minutes <= 0d ? 2f : (float) ((bar.minutes / max) * (bottom - top - 12f));
            float y = bottom - height;

            bar.left = x;
            bar.top = y;
            bar.right = x + barWidth;
            bar.bottom = bottom;

            canvas.drawRect(bar.left, bar.top, bar.right, bar.bottom, barPaint);

            if (bar.minutes > 0d) {
                canvas.drawText(FormatUtils.number(bar.minutes / 60d), x + 2f, y - 8f, textPaint);
            }
            if (shouldShowXAxisLabel(i, bar.label)) {
                canvas.drawText(bar.label, x - 6f, bottom + 28f, textPaint);
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() != MotionEvent.ACTION_UP) return true;
        for (BarInfo bar : bars) {
            if (event.getX() >= bar.left && event.getX() <= bar.right && event.getY() >= bar.top && event.getY() <= bar.bottom) {
                Toast.makeText(getContext(), bar.label + ": " + FormatUtils.number(bar.minutes / 60d) + " h", Toast.LENGTH_SHORT).show();
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
            BarInfo info = new BarInfo();
            info.minutes = sumMinutes(bucket.startMillis, bucket.endMillis);
            info.label = bucket.label;
            bars.add(info);
        }
    }

    private double sumMinutes(long start, long end) {
        double minutes = 0d;
        for (ActivitySession item : sessions) {
            if (item.sessionDateEpochMillis >= start && item.sessionDateEpochMillis <= end) {
                minutes += Math.max(0, item.durationMinutes);
            }
        }
        return minutes;
    }

    private static class BarInfo {
        String label;
        double minutes;
        float left, top, right, bottom;
    }
}
