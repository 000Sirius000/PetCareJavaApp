package com.example.petcare.ui.common;

import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewParent;

/** Adds period swipes while leaving stationary taps to the chart's detail handler. */
public final class ChartSwipeTouchListener implements View.OnTouchListener {
    private final ChartSwipeGesture gesture;
    private final Runnable previous;
    private final Runnable next;

    private ChartSwipeTouchListener(View view, Runnable previous, Runnable next) {
        int slop = ViewConfiguration.get(view.getContext()).getScaledTouchSlop();
        float density = view.getResources().getDisplayMetrics().density;
        gesture = new ChartSwipeGesture(slop, Math.max(slop * 3f, 48f * density));
        this.previous = previous;
        this.next = next;
    }

    public static void attach(View view, Runnable previous, Runnable next) {
        view.setOnTouchListener(new ChartSwipeTouchListener(view, previous, next));
    }

    @Override
    public boolean onTouch(View view, MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                gesture.start(event.getX(), event.getY());
                // Keep the first move so a ScrollView cannot steal a horizontal gesture.
                disallowParentIntercept(view, true);
                return false;
            case MotionEvent.ACTION_POINTER_DOWN:
                gesture.cancel();
                disallowParentIntercept(view, false);
                return true;
            case MotionEvent.ACTION_MOVE:
                gesture.move(event.getX(), event.getY());
                if (gesture.allowsParentScroll()) disallowParentIntercept(view, false);
                return gesture.hasMoved();
            case MotionEvent.ACTION_UP:
                int amount = gesture.finish(event.getX(), event.getY());
                disallowParentIntercept(view, false);
                if (amount < 0) previous.run();
                else if (amount > 0) next.run();
                if (!gesture.hasMoved()) {
                    view.performClick();
                    return false;
                }
                return true;
            case MotionEvent.ACTION_CANCEL:
                gesture.cancel();
                disallowParentIntercept(view, false);
                return false;
            default:
                return gesture.hasMoved();
        }
    }

    private static void disallowParentIntercept(View view, boolean disallow) {
        ViewParent parent = view.getParent();
        if (parent != null) parent.requestDisallowInterceptTouchEvent(disallow);
    }
}
