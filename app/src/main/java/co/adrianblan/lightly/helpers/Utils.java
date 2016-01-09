package co.adrianblan.lightly.helpers;

import android.content.res.Resources;
import android.util.DisplayMetrics;

/**
 * Class which contain static String related utilities.
 */
public class Utils {

    /** Takes an int, and returns a humanized String specifying the amount time */
    public static String getHumanizedHours(int hours) {
        if(hours == 0) {
            return "less than an hour";
        } else if (hours == 1) {
            return hours + "hour";
        } else {
            return hours + " hours";
        }
    }

    /**
     * This method converts dp unit to equivalent pixels, depending on device density.
     *
     * @param dp A value in dp (density independent pixels) unit. Which we need to convert into pixels
     * @return A float value to represent px equivalent to dp depending on device density
     */
    public static float convertDpToPixels(float dp){
        DisplayMetrics metrics = Resources.getSystem().getDisplayMetrics();
        float px = dp * (metrics.densityDpi / 160f);
        return px;
    }

    /**
     * This method converts device specific pixels to density independent pixels.
     *
     * @param px A value in px (pixels) unit. Which we need to convert into dp
     * @return A float value to represent dp equivalent to px value
     */
    public static float convertPixelsToDp(float px){
        DisplayMetrics metrics = Resources.getSystem().getDisplayMetrics();
        float dp = px / (metrics.densityDpi / 160f);
        return dp;
    }
}
