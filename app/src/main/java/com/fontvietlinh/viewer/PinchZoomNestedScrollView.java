package com.fontvietlinh.viewer;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.widget.NestedScrollView;

/** Pinch zoom on the preview page while preserving normal one-finger scrolling. */
public final class PinchZoomNestedScrollView extends NestedScrollView {
    public interface OnZoomListener { void onZoom(float scaleFactor); }
    private OnZoomListener zoomListener;
    private final ScaleGestureDetector scaleDetector;
    private boolean pinching;

    public PinchZoomNestedScrollView(@NonNull Context context) { this(context, null); }
    public PinchZoomNestedScrollView(@NonNull Context context, @Nullable AttributeSet attrs) { this(context, attrs, 0); }
    public PinchZoomNestedScrollView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        scaleDetector = new ScaleGestureDetector(context, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override public boolean onScale(ScaleGestureDetector detector) {
                float factor = detector.getScaleFactor();
                if (factor > 0f && !Float.isNaN(factor) && !Float.isInfinite(factor) && zoomListener != null) zoomListener.onZoom(factor);
                return true;
            }
        });
    }

    public void setZoomListener(OnZoomListener listener) { zoomListener = listener; }

    @Override public boolean dispatchTouchEvent(MotionEvent event) {
        int action = event.getActionMasked();
        boolean starting = !pinching && action == MotionEvent.ACTION_POINTER_DOWN && event.getPointerCount() >= 2;
        if (starting) {
            MotionEvent cancel = MotionEvent.obtain(event);
            cancel.setAction(MotionEvent.ACTION_CANCEL);
            super.dispatchTouchEvent(cancel);
            cancel.recycle();
            pinching = true;
        }
        scaleDetector.onTouchEvent(event);
        if (pinching) {
            if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) pinching = false;
            return true;
        }
        return super.dispatchTouchEvent(event);
    }
}
