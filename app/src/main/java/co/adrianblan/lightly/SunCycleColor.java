package co.adrianblan.lightly;

import android.animation.ArgbEvaluator;
import android.graphics.Color;

/**
 * Class which handles the colors and information to be used for the overlay
 */
public class SunCycleColor {
    public static final int toColor = Color.argb(100, 255, 170, 84);
    public static final int fromColor = Color.argb(100, 255, 255, 255);

    public static int getOverlayColor (float positionVertical, float twilightPositionVertical) {
        return 0;
    }

    public static int getOverlayColor(int colorIntensity, int brightnessIntensity) {
        ArgbEvaluator a = new ArgbEvaluator();
        int color1 = Color.argb(200 - (int) 2.0 * colorIntensity, 255, 170, 84);
        int color2 = Color.argb(200 - (int) 2.00 * brightnessIntensity, 0, 0, 0);
        return interpolate(color1, color2, 100 - colorIntensity, 100 - brightnessIntensity);
    }

    private static int interpolate (int color1, int color2, int colorIntensity, int brightnessIntensity) {

        float colorIntensityFraction = (float) colorIntensity / (float) ((colorIntensity + brightnessIntensity) * 2);
        float brightnessIntensityFraction = 1.0f - colorIntensityFraction;

        int r = (int) (Color.red(color1) * colorIntensityFraction + Color.red(color2) * brightnessIntensityFraction);
        int g = (int) (Color.green(color1) * colorIntensityFraction + Color.green(color2) * brightnessIntensityFraction);
        int b = (int) (Color.blue(color1) * colorIntensityFraction + Color.blue(color2) * brightnessIntensityFraction);
        int a = (int) Math.max((Color.alpha(color1) + Color.alpha(color2)) / 2.2, Math.max(Color.alpha(color1), Color.alpha(color2)) * 0.9);

        return Color.argb(a, r, g, b);
    }
}