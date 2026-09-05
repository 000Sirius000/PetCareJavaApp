package com.example.petcare.ui.common;

import org.junit.Test;

import static org.junit.Assert.*;

public class ChartSwipeGestureTest {
    private final ChartSwipeGesture gesture = new ChartSwipeGesture(8f, 48f);

    @Test public void leftAndRightNavigateOnePeriod() {
        gesture.start(100f, 100f);
        gesture.move(70f, 104f);
        assertEquals(1, gesture.finish(30f, 106f));
        gesture.start(100f, 100f);
        assertEquals(-1, gesture.finish(180f, 110f));
    }

    @Test public void tapJitterKeepsPointDetails() {
        gesture.start(100f, 100f);
        assertEquals(0, gesture.finish(104f, 103f));
        assertFalse(gesture.hasMoved());
    }

    @Test public void shortDragDoesNotNavigateOrShowDetails() {
        gesture.start(100f, 100f);
        assertEquals(0, gesture.finish(130f, 100f));
        assertTrue(gesture.hasMoved());
    }

    @Test public void verticalScrollStaysVerticalEvenIfFingerDriftsSideways() {
        gesture.start(100f, 100f);
        gesture.move(102f, 120f);
        assertTrue(gesture.allowsParentScroll());
        assertEquals(0, gesture.finish(280f, 150f));
    }

    @Test public void diagonalAndReturningDragsDoNotNavigate() {
        gesture.start(100f, 100f);
        assertEquals(0, gesture.finish(170f, 160f));
        assertTrue(gesture.hasMoved());
        assertTrue(gesture.allowsParentScroll());
        gesture.start(100f, 100f);
        gesture.move(180f, 100f);
        assertEquals(0, gesture.finish(103f, 100f));
        assertTrue(gesture.hasMoved());
    }

    @Test public void diagonalDragYieldsToVerticalScroll() {
        gesture.start(100f, 100f);
        gesture.move(180f, 170f);
        assertTrue(gesture.allowsParentScroll());
        assertEquals(0, gesture.finish(220f, 210f));
    }

    @Test public void cancellationOrMultipleFingersDoNotNavigate() {
        gesture.start(100f, 100f);
        gesture.move(160f, 100f);
        gesture.cancel();
        assertEquals(0, gesture.finish(220f, 100f));
        assertTrue(gesture.allowsParentScroll());
        assertTrue(gesture.hasMoved());
        gesture.start(100f, 100f);
        assertEquals(1, gesture.finish(20f, 100f));
    }
}
