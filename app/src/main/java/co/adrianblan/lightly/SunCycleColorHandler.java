package co.adrianblan.lightly;

import android.animation.ArgbEvaluator;
import android.graphics.Color;

import org.parceler.Parcel;

/**
 * Class which handles the colors and information to be used for the overlay
 */
@Parcel
public class SunCycleColorHandler {

    private static final int TWILIGHT_TRANSITION_DURATION = 90; // Twilight transition duration in minutes [0, 1440[
    private static final float TWILIGHT_TRANSITION_DISTANCE =
            (TWILIGHT_TRANSITION_DURATION / 60f) / 24f; // Twilight transition distance in [0, 1]

    private static final SunCycleColorWrapper colorFilterBase = new SunCycleColorWrapper(0, 255, 170, 84);
    private static final SunCycleColorWrapper brightnessFilterBase = new SunCycleColorWrapper(0, 0, 0, 0);

    // TODO change name?
    private int colorIntensity;
    private int brightnessIntensity;

    public SunCycleColorHandler() { /*Required empty bean constructor*/ }

    public SunCycleColorHandler(int colorIntensity, int brightnessIntensity) {
        this.colorIntensity = colorIntensity;
        this.brightnessIntensity = brightnessIntensity;
    }

    /** Gets the maximum possible prominent color */
    public int getOverlayColorMax() {
        return getOverlayColor().getColor();
    }

    /** Takes a position [0, 1], compares it to twilight in a SunCycle and gives the appropriate color */
    public int getOverlayColor (float positionHorizontal, SunCycle sunCycle) {
        float sunrise = sunCycle.getSunrisePositionHorizontal();
        float sunset = sunCycle.getSunsetPositionHorizontal();

        SunCycleColorWrapper sunCycleColorWrapper = getOverlayColor();

        // Scale the alpha of the color if we are under twilight
        if(positionHorizontal <= sunrise || positionHorizontal >= sunset) {
            float minVerticalDistanceFromTwilight = Math.min(Math.abs(sunrise - positionHorizontal), Math.abs(sunset - positionHorizontal));

            // Calculate alpha based on distance to twilight
            float colorAlphaScale = Math.min((minVerticalDistanceFromTwilight / TWILIGHT_TRANSITION_DISTANCE), 1.0f);

            sunCycleColorWrapper.setAlpha((int) (sunCycleColorWrapper.getAlpha() * colorAlphaScale));
        } else {
            sunCycleColorWrapper.setAlpha(0);
        }

        return sunCycleColorWrapper.getColor();
    }

    /** Returns the interpolated color of the temperature and the brightness based on their intensities */
    private SunCycleColorWrapper getOverlayColor() {

        SunCycleColorWrapper color1 = new SunCycleColorWrapper(colorFilterBase);
        color1.setAlpha(200 - (int) 2.0 * colorIntensity);

        SunCycleColorWrapper color2 = new SunCycleColorWrapper(brightnessFilterBase);
        color2.setAlpha(200 - (int) 2.00 * brightnessIntensity);

        return interpolate(color1, color2, 100 - colorIntensity, 100 - brightnessIntensity);
    }

    /** Magically interpolates two colors based on their intensities */
    private static SunCycleColorWrapper interpolate (SunCycleColorWrapper color1, SunCycleColorWrapper color2,
                                                     int colorIntensity, int brightnessIntensity) {

        float colorIntensityFraction = (float) colorIntensity / (float) ((colorIntensity + brightnessIntensity) * 2);
        float brightnessIntensityFraction = 1.0f - colorIntensityFraction;

        int a = (int) Math.max((color1.getAlpha() + color1.getAlpha()) / 2.2, Math.max(color1.getAlpha(), color2.getAlpha()) * 0.9);
        int r = (int) (color1.getRed() * colorIntensityFraction + color2.getRed() * brightnessIntensityFraction);
        int g = (int) (color1.getGreen() * colorIntensityFraction + color2.getGreen() * brightnessIntensityFraction);
        int b = (int) (color1.getBlue() * colorIntensityFraction + color2.getBlue()* brightnessIntensityFraction);

        return new SunCycleColorWrapper(a, r, g, b);
    }

    public int getColorIntensity() {
        return colorIntensity;
    }

    public void setColorIntensity(int colorIntensity) {
        this.colorIntensity = colorIntensity;
    }

    public int getBrightnessIntensity() {
        return brightnessIntensity;
    }

    public void setBrightnessIntensity(int brightnessIntensity) {
        this.brightnessIntensity = brightnessIntensity;
    }
}