package com.example.videoeditor.utils;

import android.graphics.Bitmap;
import android.graphics.Canvas;

public class Utils {
    public static Bitmap createSquaredBitmap(Bitmap srcBmp) {
        boolean deuerro = false;
        try {
            int oi = srcBmp.getWidth();
        } catch (NullPointerException e) {
            deuerro = true;
        }
        Bitmap dstBmp;
        if (deuerro) {
            Bitmap.Config conf = Bitmap.Config.ARGB_4444;

            dstBmp = Bitmap.createBitmap(512, 512, conf);
        } else {
            int dim = Math.max(srcBmp.getWidth(), srcBmp.getHeight());

            Bitmap.Config conf = Bitmap.Config.ARGB_4444;

            dstBmp = Bitmap.createBitmap(10, 10, conf);

            dstBmp = Bitmap.createScaledBitmap(dstBmp, dim, dim, true);

            Canvas canvas = new Canvas();
            canvas.setBitmap(dstBmp);
            canvas.drawBitmap(srcBmp, (dim - srcBmp.getWidth()) / 2, (dim - srcBmp.getHeight()) / 2, null);
            dstBmp = Bitmap.createScaledBitmap(dstBmp, 512, 512, true);
        }
        return dstBmp;
    }
}
