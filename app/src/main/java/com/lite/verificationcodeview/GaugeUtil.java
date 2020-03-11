package com.lite.verificationcodeview;

import android.content.res.Resources;

public class GaugeUtil {

    public static float dpToPx(float dp) {
        return Resources.getSystem().getDisplayMetrics().density * dp;
    }
}
