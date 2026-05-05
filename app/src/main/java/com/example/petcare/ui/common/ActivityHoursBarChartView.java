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
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class ActivityHoursBarChartView extends View {
    private final Paint axisPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint barPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint insideTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final List<BarInfo> bars = new ArrayList<>();

    private List<ActivitySession> sessions = new ArrayList<>();
    private FilterRange range = FilterRange.WEEK;

    public ActivityHoursBarChartView(Context context, AttributeSet attrs) {
        super(context, attrs);
        axisPaint.setStrokeWidth(3f);
        textPaint.setTextSize(24f);
        insideTextPaint.setTextSize(24f);
        insideTextPaint.setFakeBoldText(true);
    }

    public void setData(List<ActivitySession> sessions, FilterRange range) {
        this.sessions = sessions == null ? new ArrayList<>() : new ArrayList<>(sessions);
        this.range = range == null ? FilterRange.WEEK : range;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        axisPaint.setColor(ContextCompat.getColor(getContext(), R.color.pet_border));
        barPaint.setColor(ThemeUtils.getAccentColor(getContext()));
        textPaint.setColor(ContextCompat.getColor(getContext(), R.color.pet_text_secondary));
        insideTextPaint.setColor(ContextCompat.getColor(getContext(), R.color.black));

        float left = 85f;
        float right = getWidth() - 20f;
        float top = 30f;
        float bottom = getHeight() - 60f;

        canvas.drawLine(left, bottom, right, bottom, axisPaint);
        canvas.drawLine(left, top, left, bottom, axisPaint);
        canvas.drawText("Hours", 10f, top + 20f, textPaint);

        buildBars();
        if (bars.isEmpty()) {
            canvas.drawText("No activity data yet", left + 20f, bottom - 20f, textPaint);
            return;
        }

        double max = 0d;
        for (BarInfo bar : bars) max = Math.max(max, bar.hours);
        if (max < 1d) max = 1d;

        float slotWidth = (right - left) / bars.size();
        float barWidth = Math.max(16f, slotWidth * 0.65f);

        for (int i = 0; i < bars.size(); i++) {
            BarInfo bar = bars.get(i);
            float x = left + i * slotWidth + (slotWidth - barWidth) / 2f;
            float height = (float) ((bar.hours / max) * (bottom - top - 10f));
            float y = bottom - height;

            bar.left = x;
            bar.top = y;
            bar.right = x + barWidth;
            bar.bottom = bottom;

            canvas.drawRect(bar.left, bar.top, bar.right, bar.bottom, barPaint);

            String label = hoursText(bar.hours);
            if (height > 42f) {
                canvas.drawText(label, x + 6f, y + 28f, insideTextPaint);
            }
            canvas.drawText(label, x + 2f, y - 8f, textPaint);
            canvas.drawText(bar.label, x - 6f, bottom + 28f, textPaint);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() != MotionEvent.ACTION_UP) return true;
        float x = event.getX();
        float y = event.getY();
        for (BarInfo bar : bars) {
            if (x >= bar.left && x <= bar.right && y >= bar.top && y <= bar.bottom) {
                Toast.makeText(getContext(), bar.label + ": " + hoursText(bar.hours) + " h", Toast.LENGTH_SHORT).show();
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

                BarInfo info = new BarInfo();
                info.hours = sumHours(start.getTimeInMillis(), end.getTimeInMillis());
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

                BarInfo info = new BarInfo();
                info.hours = sumHours(start.getTimeInMillis(), end.getTimeInMillis());
                info.label = String.valueOf(day);
                bars.add(info);
            }
            return;
        }

        Calendar startDay = Calendar.getInstance();
        startDay.add(Calendar.DAY_OF_YEAR, -6);
        for (int i = 0; i < 7; i++) {
            Calendar start = (Calendar) startDay.clone();
            start.set(Calendar.HOUR_OF_DAY, 0);
            start.set(Calendar.MINUTE, 0);
            start.set(Calendar.SECOND, 0);
            start.set(Calendar.MILLISECOND, 0);

            Calendar end = (Calendar) start.clone();
            end.set(Calendar.HOUR_OF_DAY, 23);
            end.set(Calendar.MINUTE, 59);
            end.set(Calendar.SECOND, 59);

            BarInfo info = new BarInfo();
            info.hours = sumHours(start.getTimeInMillis(), end.getTimeInMillis());
            info.label = FormatUtils.dayLabel(start.getTimeInMillis());
            bars.add(info);

            startDay.add(Calendar.DAY_OF_YEAR, 1);
        }
    }

    private double sumHours(long start, long end) {
        double hours = 0d;
        for (ActivitySession item : sessions) {
            if (item.sessionDateEpochMillis >= start && item.sessionDateEpochMillis <= end) {
                hours += item.durationMinutes / 60d;
            }
        }
        return hours;
    }

    private String hoursText(double hours) {
        if (hours == 0d) return "0";
        return FormatUtils.number(hours);
    }

    private static class BarInfo {
        String label;
        double hours;
        float left;
        float top;
        float right;
        float bottom;
    }
}
