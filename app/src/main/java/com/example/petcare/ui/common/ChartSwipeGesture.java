package com.example.petcare.ui.common;

/** Direction locking keeps vertical scrolling and taps separate from period navigation. */
final class ChartSwipeGesture {
    private static final int UNDECIDED = 0;
    private static final int HORIZONTAL = 1;
    private static final int VERTICAL = 2;
    private final float touchSlop;
    private final float swipeDistance;
    private float startX;
    private float startY;
    private int direction;
    private boolean moved;
    private boolean cancelled;

    ChartSwipeGesture(float touchSlop, float swipeDistance) {
        this.touchSlop = touchSlop;
        this.swipeDistance = swipeDistance;
    }

    void start(float x, float y) {
        startX = x;
        startY = y;
        direction = UNDECIDED;
        moved = false;
        cancelled = false;
    }

    void move(float x, float y) {
        if (cancelled) return;
        float dx = Math.abs(x - startX);
        float dy = Math.abs(y - startY);
        moved |= dx > touchSlop || dy > touchSlop;
        if (direction != UNDECIDED) return;
        if (dy > touchSlop && dx <= dy * 1.5f) direction = VERTICAL;
        else if (dx > touchSlop && dx > dy * 1.5f) direction = HORIZONTAL;
    }

    int finish(float x, float y) {
        move(x, y);
        float dx = x - startX;
        float dy = y - startY;
        if (cancelled || direction != HORIZONTAL || Math.abs(dx) < swipeDistance
                || Math.abs(dx) <= Math.abs(dy) * 1.5f) return 0;
        return dx < 0f ? 1 : -1;
    }

    void cancel() {
        cancelled = true;
        moved = true;
    }

    boolean hasMoved() {
        return moved;
    }

    boolean allowsParentScroll() {
        return cancelled || direction == VERTICAL;
    }
}
