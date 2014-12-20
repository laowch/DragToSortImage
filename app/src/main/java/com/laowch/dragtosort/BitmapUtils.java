package com.laowch.dragtosort;

import android.graphics.Bitmap;

/**
 * Created by lao on 14/12/20.
 */
public class BitmapUtils {

    public static Bitmap resizeBitmap(final Bitmap bitmap, final int requestedWidth, final int requestedHeight) {
        final int srcWidth = bitmap.getWidth();
        final int srcHeight = bitmap.getHeight();

        final float widthRatio = (float) srcWidth / (float) requestedWidth;
        final float heightRatio = (float) srcHeight / (float) requestedHeight;

        if (widthRatio < heightRatio) {
            final Bitmap scaleBitmap = Bitmap.createScaledBitmap(bitmap, requestedWidth, Math.round(srcHeight / widthRatio), true);

            return Bitmap.createBitmap(scaleBitmap, 0, Math.round((scaleBitmap.getHeight() - requestedHeight) / 2.0f), requestedWidth, requestedHeight);
        } else {
            final Bitmap scaleBitmap = Bitmap.createScaledBitmap(bitmap, Math.round(srcWidth / heightRatio), requestedHeight, true);

            return Bitmap.createBitmap(scaleBitmap, Math.round((scaleBitmap.getWidth() - requestedWidth) / 2.0f), 0, requestedWidth, requestedHeight);
        }
    }
}
