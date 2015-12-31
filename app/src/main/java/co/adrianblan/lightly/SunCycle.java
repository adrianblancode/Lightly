package co.adrianblan.lightly;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * A class which models the information for a sun cycle
 */
public class SunCycle {

    private static final double tau = Math.PI * 2.0;

    // Container for keeping coordinate positions
    private class Position {
        float x;
        float y;
    };

    private float scaledTimeOffset; // Position [0, 1] that the sun cycle should be offset
    private float twilightVerticalPosition; // Position [0, 1] that the twilight is set at
    private Position sunPosition;
    float sunriseHorizontalPosition;
    float sunsetHorizontalPosition;

    public SunCycle (Date current, SunriseSunsetData sunriseSunsetData) throws ParseException {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("hh:mm:ss aa", Locale.US);
        simpleDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

        Date sunrise = simpleDateFormat.parse(sunriseSunsetData.getSunrise());
        Date sunset = simpleDateFormat.parse(sunriseSunsetData.getSunset());

        initializeSunCycle(sunrise, sunset);
        updateSunPosition(current);
    }

    public SunCycle (Date current, Date sunrise, Date sunset) {
        initializeSunCycle(sunrise, sunset);
        updateSunPosition(current);
    }

    /**
     * Initializes a sun cycle, given the Dates of sunrise and sunset.
     * Sunset and sunrise are assumed to be during the same day.
     */
    private void initializeSunCycle(Date sunrise, Date sunset) {

        sunPosition = new Position();

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(sunrise);

        // Convert the dates to [0, 1] positions
        sunriseHorizontalPosition = getScaledTime(sunrise);
        sunsetHorizontalPosition = getScaledTime(sunset);

        // The position where the sun is at it's highest
        float solarNoonHorizontalPosition = sunriseHorizontalPosition +  (sunsetHorizontalPosition - sunriseHorizontalPosition) / 2f;

        // If the solar noon is at 0.25f, we count that as zero offset
        scaledTimeOffset = ((solarNoonHorizontalPosition - 0.25f) + 1f) % 1f;

        // Calculate at what scaled height the twilight is at
        twilightVerticalPosition = (float) getScaledRadian(
                Math.sin(sunriseHorizontalPosition * tau + scaledTimeOffset * tau));
    }

    /** Calculates the position of the sun for the current time, given the initialized sun cycle */
    public void updateSunPosition(Date current) {
        sunPosition.x = getScaledTime(current);
        sunPosition.y = (float) getScaledRadian(
                Math.sin(sunPosition.x * tau + scaledTimeOffset * tau));
    }

    /** Scales a date to modulo one day, and maps it [0, 1] */
    private float getScaledTime(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);

        // Scale according to hours and minutes
        float scaledTime = calendar.get(Calendar.HOUR_OF_DAY) / 24f;
        scaledTime += calendar.get(Calendar.MINUTE) / (60f * 24f);

        return scaledTime;
    }

    /** Takes an angle in radians, and converts it to an abs value with bounds [0, 1] */
    private double getScaledRadian(double radian) {
        return ((radian + tau) % tau) / tau;
    }

    public float getSunHorizontalPosition() {
        return sunPosition.x;
    }

    public float getOffset() {
        return scaledTimeOffset;
    }

    public float getTwilightVerticalPosition() {
        return twilightVerticalPosition;
    }

    public float getSunriseHorizontalPosition() {
        return sunriseHorizontalPosition;
    }

    public float getSunsetHorizontalPosition() {
        return sunsetHorizontalPosition;
    }
}