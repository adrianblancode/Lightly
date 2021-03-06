package co.adrianblan.lightly.suncycle;

import org.parceler.Parcel;

/**
 * Class which handles the colors and information to be used for the overlay
 */
@Parcel
public class SunCycleColorHandler {

    private static final int TWILIGHT_TRANSITION_DURATION = 90; // Twilight transition duration in minutes [0, 1440[
    private static final float TWILIGHT_TRANSITION_DISTANCE =
            (TWILIGHT_TRANSITION_DURATION / 60f) / 24f; // Twilight transition distance in [0, 1]

    private static final SunCycleColorWrapper colorFilterBase = new SunCycleColorWrapper(0, 255, 130, 0);
    private static final SunCycleColorWrapper brightnessFilterBase = new SunCycleColorWrapper(0, 0, 0, 0);
    private static final int COLOR_FILTER_MAX_ALPHA = 180;
    private static final int COLOR_TEMPERATURE_MIN = 1800;
    private static final int COLOR_TEMPERATURE_MAX = 6000;

    // These intensities might be flipped due to SeekBar placement
    private int colorFilterIntensity;
    private int brightnessFilterIntensity;

    public SunCycleColorHandler() { /*Required empty bean constructor*/ }

    public SunCycleColorHandler(int colorFilterIntensity, int brightnessFilterIntensity) {
        this.colorFilterIntensity = colorFilterIntensity;
        this.brightnessFilterIntensity = brightnessFilterIntensity;
    }

    /** Gets the maximum possible prominent color */
    public int getOverlayColorMax() {
        return getOverlayColor().getColor();
    }

    /** Takes a SunCycle and gives the appropriate color in the cycle */
    public int getOverlayColor (SunCycle sunCycle) {
        float positionHorizontal = sunCycle.getSunPositionHorizontal();
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

        SunCycleColorWrapper colorFilterWrapper = getColorFilterWrapper();
        SunCycleColorWrapper brightnessFilterWrapper = getBrightnessFilterWrapper();

        return interpolate(colorFilterWrapper, brightnessFilterWrapper, 100 - colorFilterIntensity, 300 - 3 * brightnessFilterIntensity);
    }

    /** Returns a wrapper for the color of the color filter */
    public SunCycleColorWrapper getColorFilterWrapper() {
        SunCycleColorWrapper colorFilterWrapperBase = new SunCycleColorWrapper(colorFilterBase);
        SunCycleColorWrapper colorFilterWrapper = new SunCycleColorWrapper(colorFilterWrapperBase);
        float colorFilterIntensityScale = colorFilterIntensity / 100f;

        colorFilterWrapper.setAlpha(COLOR_FILTER_MAX_ALPHA -
                (int) (COLOR_FILTER_MAX_ALPHA * colorFilterIntensityScale));

        int green = colorFilterWrapperBase.getGreen() + (int) ((255 - colorFilterWrapperBase.getGreen())
                * colorFilterIntensityScale);

        colorFilterWrapper.setGreen(green);

        int blue = colorFilterWrapperBase.getBlue() + (int) ((255 - colorFilterWrapperBase.getBlue())
                * colorFilterIntensityScale);

        colorFilterWrapper.setBlue(blue);

        return colorFilterWrapper;
    }

    /** Gets the current color temperature of the color filter, rounded to nearest 100 */
    public int getColorTemperature() {
        int alpha = getColorFilterWrapper().getAlpha();

        // Get precise temperature
        int temperature = (int) (COLOR_TEMPERATURE_MIN + (COLOR_TEMPERATURE_MAX - COLOR_TEMPERATURE_MIN)
                * (1.0f - ((float) alpha / COLOR_FILTER_MAX_ALPHA)));

        // Round to nearest 100
        temperature = ((temperature + 50) / 100) * 100;

        return temperature;
    }

    /** Get the brightness percent, rounded to nearest 5 */
    public int getBrightnessPercent() {
        float brightness = (1.0f - (getBrightnessFilterWrapper().getAlpha() / 255f)) * 100f;

        // Round to nearest 5%
       return (int)((brightness + 2.5f) / 5) * 5;
    }

    /** Returns a wrapper for the color of the brightness filter */
    public SunCycleColorWrapper getBrightnessFilterWrapper() {
        SunCycleColorWrapper brightnessFilterWrapper = new SunCycleColorWrapper(brightnessFilterBase);
        brightnessFilterWrapper.setAlpha(200 - (int) 2.0 * brightnessFilterIntensity);

        return brightnessFilterWrapper;
    }
    
    /** Interpolate between two colors, the second one is scaled by a priority */
    public static int interpolateWithPriority(int backgroundColor, int priorityColor, int priorityScale) {
        SunCycleColorWrapper backgroundColorWrapper = new SunCycleColorWrapper(backgroundColor);
        SunCycleColorWrapper priorityColorWrapper = new SunCycleColorWrapper(priorityColor);

        SunCycleColorWrapper interpolated = interpolate(backgroundColorWrapper, priorityColorWrapper,
                255 - priorityColorWrapper.getAlpha(), priorityColorWrapper.getAlpha() * priorityScale);
        interpolated.setAlpha(255);
        return interpolated.getColor();
    }

    /** Magically interpolates two colors based on their intensities */
    private static SunCycleColorWrapper interpolate (SunCycleColorWrapper color1, SunCycleColorWrapper color2,
                                                     int colorIntensity, int brightnessIntensity) {

        float colorIntensityFraction = (float) colorIntensity / (float) (colorIntensity + brightnessIntensity);
        float brightnessIntensityFraction = 1.0f - colorIntensityFraction;

        int a = (int) Math.max((color1.getAlpha() + color1.getAlpha()) / 2.2, Math.max(color1.getAlpha(), color2.getAlpha()) * 0.9);
        int r = (int) (color1.getRed() * colorIntensityFraction + color2.getRed() * brightnessIntensityFraction);
        int g = (int) (color1.getGreen() * colorIntensityFraction + color2.getGreen() * brightnessIntensityFraction);
        int b = (int) (color1.getBlue() * colorIntensityFraction + color2.getBlue()* brightnessIntensityFraction);

        return new SunCycleColorWrapper(a, r, g, b);
    }

    public int getColorFilterIntensity() {
        return colorFilterIntensity;
    }

    public void setColorFilterIntensity(int colorFilterIntensity) {
        this.colorFilterIntensity = colorFilterIntensity;
    }

    public int getBrightnessFilterIntensity() {
        return brightnessFilterIntensity;
    }

    public void setBrightnessFilterIntensity(int brightnessFilterIntensity) {
        this.brightnessFilterIntensity = brightnessFilterIntensity;
    }
}