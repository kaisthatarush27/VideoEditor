package com.example.videoeditor.collageView;

import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;

public class MultiTouchListener implements OnTouchListener {
    private static final int INVALID_POINTER_ID = -1;
    private boolean isRotateEnabled = false;
    private boolean isScaleEnabled = true;
    private boolean isTranslateEnabled = true;
    private int mActivePointerId = INVALID_POINTER_ID;
    public static float mPrevX;
    public static float mPrevY;
    public static float mCurY;
    public static float mCurX;
    private ScaleGestureDetector mScaleGestureDetector = new ScaleGestureDetector(new ScaleGestureListener());
    private float maximumScale = 10.0f;
    private float minimumScale = 0.5f;

    private class TransformInfo {
        public float deltaAngle;
        public float deltaScale;
        public float deltaX;
        public float deltaY;
        public float maximumScale;
        public float minimumScale;
        public float pivotX;
        public float pivotY;

        private TransformInfo() {
        }
    }

    private class ScaleGestureListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        private float mPivotX;
        private float mPivotY;
        private Vector2D mPrevSpanVector;

        private ScaleGestureListener() {
            this.mPrevSpanVector = new Vector2D();
        }

        public boolean onScaleBegin(View view, ScaleGestureDetector detector) {
            this.mPivotX = detector.getFocusX();
            this.mPivotY = detector.getFocusY();
            this.mPrevSpanVector.set(detector.getCurrentSpanVector());
            return true;
        }

        public boolean onScale(View view, ScaleGestureDetector detector) {
            float angle;
            float f = 0.0f;
            TransformInfo info = new TransformInfo();
            info.deltaScale = MultiTouchListener.this.isScaleEnabled ? detector.getScaleFactor() : 1.0f;
            if (MultiTouchListener.this.isRotateEnabled) {
                angle = Vector2D.getAngle(this.mPrevSpanVector, detector.getCurrentSpanVector());
            } else {
                angle = 0.0f;
            }
            info.deltaAngle = angle;
            if (MultiTouchListener.this.isTranslateEnabled) {
                angle = detector.getFocusX() - this.mPivotX;
            } else {
                angle = 0.0f;
            }
            info.deltaX = angle;
            if (MultiTouchListener.this.isTranslateEnabled) {
                f = detector.getFocusY() - this.mPivotY;
            }
            info.deltaY = f;
            info.pivotX = this.mPivotX;
            info.pivotY = this.mPivotY;
            info.minimumScale = MultiTouchListener.this.minimumScale;
            info.maximumScale = MultiTouchListener.this.maximumScale;
            MultiTouchListener.move(view, info);
            return false;
        }
    }

    private static float adjustAngle(float degrees) {
        if (degrees > 180) {
            return degrees - 360.0f;
        }
        if (degrees < -180.0f) {
            return degrees + 360.0f;
        }
        return degrees;
    }

    private static void move(View view, TransformInfo info) {
        computeRenderOffset(view, info.pivotX, info.pivotY);
        adjustTranslation(view, info.deltaX, info.deltaY);
        float scale = Math.max(info.minimumScale, Math.min(info.maximumScale, view.getScaleX() * info.deltaScale));
        view.setScaleX(scale);
        view.setScaleY(scale);
        view.setRotation(adjustAngle(view.getRotation() + info.deltaAngle));
    }

    private static void adjustTranslation(View view, float deltaX, float deltaY) {
        float[] deltaVector = new float[]{deltaX, deltaY};
        view.getMatrix().mapVectors(deltaVector);
        view.setTranslationX(view.getTranslationX() + deltaVector[0]);
        view.setTranslationY(view.getTranslationY() + deltaVector[1]);
    }

    private static void computeRenderOffset(View view, float pivotX, float pivotY) {
        if (view.getPivotX() != pivotX || view.getPivotY() != pivotY) {
            float[] prevPoint = new float[]{0.0f, 0.0f};
            view.getMatrix().mapPoints(prevPoint);
            view.setPivotX(pivotX);
            view.setPivotY(pivotY);
            float[] currPoint = new float[]{0.0f, 0.0f};
            view.getMatrix().mapPoints(currPoint);
            float offsetY = currPoint[1] - prevPoint[1];
            view.setTranslationX(view.getTranslationX() - (currPoint[0] - prevPoint[0]));
            view.setTranslationY(view.getTranslationY() - offsetY);
        }
    }

    public boolean onTouch(View view, MotionEvent event) {
        int newPointerIndex = 0;
        this.mScaleGestureDetector.onTouchEvent(view, event);
        if (this.isTranslateEnabled) {
            int action = event.getAction();
            int pointerIndex;
            switch (event.getActionMasked() & action) {
                case MotionEvent.ACTION_DOWN:
                    this.mPrevX = event.getX();
                    this.mPrevY = event.getY();
                    this.mActivePointerId = event.getPointerId(0);
                    Log.d("eopa", "multi touch down mprevx" + mPrevX + " mprevy" + mPrevY);
                    break;
                case MotionEvent.ACTION_UP:
                    this.mCurX = event.getX();
                    this.mCurY = event.getY();
                    this.mActivePointerId = INVALID_POINTER_ID;
                    Log.d("eopa", "multi touch up mCurX" + mCurX + " mCurY" + mCurY);
                    view.performClick();
                    break;
                case MotionEvent.ACTION_MOVE:
                    pointerIndex = event.findPointerIndex(this.mActivePointerId);
                    if (pointerIndex != INVALID_POINTER_ID) {
                        float currX = event.getX(pointerIndex);
                        float currY = event.getY(pointerIndex);
                        if (!this.mScaleGestureDetector.isInProgress()) {
                            adjustTranslation(view, currX - this.mPrevX, currY - this.mPrevY);
                            break;
                        }
                    }
                    break;
                case MotionEvent.ACTION_CANCEL:
                    this.mActivePointerId = INVALID_POINTER_ID;
                    break;
                case MotionEvent.ACTION_POINTER_UP:
                    pointerIndex = (MotionEvent.ACTION_POINTER_INDEX_MASK & action) >> 8;
                    if (event.getPointerId(pointerIndex) == this.mActivePointerId) {
                        if (pointerIndex == 0) {
                            newPointerIndex = 1;
                        }
                        this.mPrevX = event.getX(newPointerIndex);
                        this.mPrevY = event.getY(newPointerIndex);
                        this.mActivePointerId = event.getPointerId(newPointerIndex);
                        Log.d("eopa", "multi touch pup mprevx" + mPrevX + " mprevy" + mPrevY);
                        break;
                    }
                    break;
                default:
                    break;
            }
        }
        return true;
    }
}
