package co.adrianblan.lightly.suncycle;

import android.graphics.Color;

import org.parceler.Parcel;

/**
 * Class which wraps the color data for Color
 */
@Parcel
public class SunCycleColorWrapper {
    int alpha;
    int red;
    int green;
    int blue;

    public SunCycleColorWrapper() {
        this.alpha = 0;
        this.red = 0;
        this.blue = 0;
        this.green = 0;
    }

    public SunCycleColorWrapper(int alpha, int red, int green, int blue) {
        this.alpha = alpha;
        this.red = red;
        this.blue = blue;
        this.green = green;
    }

    public SunCycleColorWrapper(SunCycleColorWrapper color) {
        this.alpha = color.getAlpha();
        this.red = color.getRed();
        this.blue = color.getBlue();
        this.green = color.getGreen();
    }

    /** Converts the individual components to int with Color.argb() */
    public int getColor() {
        return Color.argb(alpha, red, green, blue);
    }

    public int getAlpha() {
        return alpha;
    }

    public void setAlpha(int alpha) {
        this.alpha = alpha;
    }

    public int getRed() {
        return red;
    }

    public void setRed(int red) {
        this.red = red;
    }

    public int getBlue() {
        return blue;
    }

    public void setBlue(int blue) {
        this.blue = blue;
    }

    public int getGreen() {
        return green;
    }

    public void setGreen(int green) {
        this.green = green;
    }
}