package com.example.videoeditor.stickerView;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import com.example.videoeditor.R;

public class StickerView extends View {
    private Bitmap deleteBitmap;
    private Bitmap mBitmap;
    private Rect dst_delete;
    private int deleteBitmapWidth;
    private int deleteBitmapHeight;
    private Paint localPaint1;
    private Paint localPaint2;
    private Paint localPaint3;
    private Paint localPaint4;
    private int mScreenwidth;
    private int mScreenheight;
    private static final float BITMAP_SCALE = 0.7f;
    private OperationListener operationListener;
    private final Matrix matrix = new Matrix();
    private boolean isInSide;

    public float lastX, lastY;

    private boolean isInEdit = true;

    public StickerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public StickerView(Context context) {
        super(context);
        init();
    }

    public StickerView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        dst_delete = new Rect();
        localPaint1 = new Paint();
        localPaint1.setColor(getResources().getColor(R.color.red_e73a3d));
        localPaint1.setAntiAlias(true);
        localPaint1.setDither(true);
        localPaint1.setStyle(Paint.Style.STROKE);
        localPaint1.setStrokeWidth(2.0f);

        localPaint2 = new Paint();
        localPaint2.setColor(getResources().getColor(R.color.c2));
        localPaint2.setAntiAlias(true);
        localPaint2.setDither(true);
        localPaint2.setStyle(Paint.Style.STROKE);
        localPaint2.setStrokeWidth(2.0f);

        localPaint3 = new Paint();
        localPaint3.setColor(getResources().getColor(R.color.c4));
        localPaint3.setAntiAlias(true);
        localPaint3.setDither(true);
        localPaint3.setStyle(Paint.Style.STROKE);
        localPaint3.setStrokeWidth(2.0f);

        localPaint4 = new Paint();
        localPaint4.setColor(getResources().getColor(com.vanniktech.emoji.R.color.emoji_gray20));
        localPaint4.setAntiAlias(true);
        localPaint4.setDither(true);
        localPaint4.setStyle(Paint.Style.STROKE);
        localPaint4.setStrokeWidth(2.0f);
        DisplayMetrics dm = getResources().getDisplayMetrics();
        mScreenwidth = dm.widthPixels;
        mScreenheight = dm.heightPixels;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (mBitmap != null) {
            float[] arrayOfFloat = new float[9];
            matrix.getValues(arrayOfFloat);
            Log.d("eopa", "onDraw: matrix" + matrix);
// Recalculate the values of f1 to f8 with the modified matrix
            float f1 = 0.0F * arrayOfFloat[0] + 0.0F * arrayOfFloat[1] + arrayOfFloat[2] - deleteBitmapWidth;
            float f2 = 0.0F * arrayOfFloat[3] + 0.0F * arrayOfFloat[4] + arrayOfFloat[5] - deleteBitmapWidth / 2f;
            float f3 = arrayOfFloat[0] * this.mBitmap.getWidth() + 0.0F * arrayOfFloat[1] + arrayOfFloat[2] + deleteBitmapWidth;
            float f4 = arrayOfFloat[3] * this.mBitmap.getWidth() + 0.0F * arrayOfFloat[4] + arrayOfFloat[5] - deleteBitmapWidth / 2f;
            float f5 = 0.0F * arrayOfFloat[0] + arrayOfFloat[1] * this.mBitmap.getHeight() + arrayOfFloat[2] - deleteBitmapWidth;
            float f6 = 0.0F * arrayOfFloat[3] + arrayOfFloat[4] * this.mBitmap.getHeight() + arrayOfFloat[5];
            float f7 = arrayOfFloat[0] * this.mBitmap.getWidth() + arrayOfFloat[1] * this.mBitmap.getHeight() + arrayOfFloat[2] + deleteBitmapWidth;
            float f8 = arrayOfFloat[3] * this.mBitmap.getWidth() + arrayOfFloat[4] * this.mBitmap.getHeight() + arrayOfFloat[5];
            Log.d("eopa", "onDraw:f1 " + f1);
            Log.d("eopa", "onDraw:f2 " + f2);
            Log.d("eopa", "onDraw:f3 " + f3);
            Log.d("eopa", "onDraw:f4 " + f4);
            Log.d("eopa", "onDraw:f5 " + f5);
            Log.d("eopa", "onDraw:f6 " + f6);
            Log.d("eopa", "onDraw:f7 " + f7);
            Log.d("eopa", "onDraw:f8 " + f8);
//
            canvas.save();
            canvas.drawBitmap(mBitmap, matrix, null);
//
            dst_delete.left = (int) (f3 - deleteBitmapWidth / 2);
            dst_delete.right = (int) (f3 + deleteBitmapWidth / 2);
            dst_delete.top = (int) (f4 - deleteBitmapHeight / 2);
            dst_delete.bottom = (int) (f4 + deleteBitmapHeight / 2);
//
            if (isInEdit) {
                canvas.drawLine(f1, f2, f3, f4, localPaint1);
                canvas.drawLine(f3, f4, f7, f8, localPaint2);
                canvas.drawLine(f5, f6, f7, f8, localPaint3);
                canvas.drawLine(f5, f6, f1, f2, localPaint4);

//                canvas.drawLine(f1, f2, f3 - 545, f4, localPaint1);
//                canvas.drawLine(f3 - 545, f4, f7 - 545, f8, localPaint2);
//                canvas.drawLine(f5, f6, f7 - 545, f8, localPaint3);
//                canvas.drawLine(f5, f6, f1, f2, localPaint4);

                canvas.drawBitmap(deleteBitmap, null, dst_delete, null);
            }
            canvas.restore();


        }

        // mBitmap contains the bitmap which is drawn on the canvas.
    }

    private float getBitmapLeft() {
        return 200;
    }

    private float getBitmapTop() {
        return (getHeight() - mBitmap.getHeight()) / 2f;
    }

    public void setBitmap(Bitmap bitmap) {
        matrix.reset();
        mBitmap = bitmap;
        deleteBitmap = BitmapFactory.decodeResource(getResources(), R.mipmap.icon_delete);
        int w = mBitmap.getWidth();
        int h = mBitmap.getHeight();
        matrix.postTranslate(mScreenwidth / 2, (mScreenwidth) / 2 - h / 2);
        deleteBitmapWidth = (int) (deleteBitmap.getWidth() * BITMAP_SCALE);
        deleteBitmapHeight = (int) (deleteBitmap.getHeight() * BITMAP_SCALE);
        invalidate();
    }

    public Bitmap getBitmap() {
        return mBitmap;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getActionMasked();

        boolean handled = true;
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                if (isInButton(event, dst_delete)) {
                    if (operationListener != null) {
                        operationListener.onDeleteClick();
                    }
                } else if (isInBitmap(event)) {
                    isInSide = true;
                    lastX = event.getX(0);
                    lastY = event.getY(0);
                } else {
                    handled = false;
                }
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                break;
            case MotionEvent.ACTION_MOVE:

                if (isInSide) {
                    float x = event.getX(0);
                    float y = event.getY(0);
                    matrix.postTranslate(x - lastX, y - lastY);
                    lastX = x;
                    lastY = y;
                    invalidate();
                }
                break;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                isInSide = false;
                break;

        }
        if (handled && operationListener != null) {
            operationListener.onEdit(this);
        }
        return handled;
    }

    private boolean isInBitmap(MotionEvent event) {
        float[] arrayOfFloat1 = new float[9];
        this.matrix.getValues(arrayOfFloat1);
        float f1 = 0.0F * arrayOfFloat1[0] + 0.0F * arrayOfFloat1[1] + arrayOfFloat1[2];
        float f2 = 0.0F * arrayOfFloat1[3] + 0.0F * arrayOfFloat1[4] + arrayOfFloat1[5];
        float f3 = arrayOfFloat1[0] * this.mBitmap.getWidth() + 0.0F * arrayOfFloat1[1] + arrayOfFloat1[2];
        float f4 = arrayOfFloat1[3] * this.mBitmap.getWidth() + 0.0F * arrayOfFloat1[4] + arrayOfFloat1[5];
        float f5 = 0.0F * arrayOfFloat1[0] + arrayOfFloat1[1] * this.mBitmap.getHeight() + arrayOfFloat1[2];
        float f6 = 0.0F * arrayOfFloat1[3] + arrayOfFloat1[4] * this.mBitmap.getHeight() + arrayOfFloat1[5];
        float f7 = arrayOfFloat1[0] * this.mBitmap.getWidth() + arrayOfFloat1[1] * this.mBitmap.getHeight() + arrayOfFloat1[2];
        float f8 = arrayOfFloat1[3] * this.mBitmap.getWidth() + arrayOfFloat1[4] * this.mBitmap.getHeight() + arrayOfFloat1[5];

        float[] arrayOfFloat2 = new float[4];
        float[] arrayOfFloat3 = new float[4];
        arrayOfFloat2[0] = f1;//左上的x
        arrayOfFloat2[1] = f3;//右上的x
        arrayOfFloat2[2] = f7;//右下的x
        arrayOfFloat2[3] = f5;//左下的x
        arrayOfFloat3[0] = f2;//左上的y
        arrayOfFloat3[1] = f4;//右上的y
        arrayOfFloat3[2] = f8;//右下的y
        arrayOfFloat3[3] = f6;//左下的y
        return pointInRect(arrayOfFloat2, arrayOfFloat3, event.getX(0), event.getY(0));
    }

    private boolean pointInRect(float[] xRange, float[] yRange, float x, float y) {

        double a1 = Math.hypot(xRange[0] - xRange[1], yRange[0] - yRange[1]);
        double a2 = Math.hypot(xRange[1] - xRange[2], yRange[1] - yRange[2]);
        double a3 = Math.hypot(xRange[3] - xRange[2], yRange[3] - yRange[2]);
        double a4 = Math.hypot(xRange[0] - xRange[3], yRange[0] - yRange[3]);

        double b1 = Math.hypot(x - xRange[0], y - yRange[0]);
        double b2 = Math.hypot(x - xRange[1], y - yRange[1]);
        double b3 = Math.hypot(x - xRange[2], y - yRange[2]);
        double b4 = Math.hypot(x - xRange[3], y - yRange[3]);

        double u1 = (a1 + b1 + b2) / 2;
        double u2 = (a2 + b2 + b3) / 2;
        double u3 = (a3 + b3 + b4) / 2;
        double u4 = (a4 + b4 + b1) / 2;

        double s = a1 * a2;
        double ss = Math.sqrt(u1 * (u1 - a1) * (u1 - b1) * (u1 - b2))
                + Math.sqrt(u2 * (u2 - a2) * (u2 - b2) * (u2 - b3))
                + Math.sqrt(u3 * (u3 - a3) * (u3 - b3) * (u3 - b4))
                + Math.sqrt(u4 * (u4 - a4) * (u4 - b4) * (u4 - b1));
        return Math.abs(s - ss) < 0.5;
    }

    private boolean isInButton(MotionEvent event, Rect rect) {
        int left = rect.left;
        int right = rect.right;
        int top = rect.top;
        int bottom = rect.bottom;
        return event.getX(0) >= left && event.getX(0) <= right && event.getY(0) >= top && event.getY(0) <= bottom;
    }

    public interface OperationListener {
        void onDeleteClick();

        void onEdit(StickerView stickerView);

        void onTop(StickerView stickerView);
    }

    public void setOperationListener(OperationListener operationListener) {
        this.operationListener = operationListener;
    }

    public void setInEdit(boolean isInEdit) {
        this.isInEdit = isInEdit;
        invalidate();
    }
}
