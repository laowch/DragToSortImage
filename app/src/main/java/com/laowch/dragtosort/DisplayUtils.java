package com.laowch.dragtosort;

import android.content.Context;

/**
 * Created by lao on 15/1/13.
 */
public class DisplayUtils {

    public static int getScreenWidth(final Context context) {
        return context.getResources().getDisplayMetrics().widthPixels;
    }

    public static int getScreenHeight(final Context context) {
        return context.getResources().getDisplayMetrics().heightPixels;
    }

    public static int pixelToDp(final Context context, final int pixels) {
        final float density = context.getResources().getDisplayMetrics().density;

        return (int) ((pixels / density) + 0.5);
    }

    public static int dpToPixel(final Context context, final int dp) {
        final float density = context.getResources().getDisplayMetrics().density;

        return (int) ((dp * density) + 0.5f);
    }
}
