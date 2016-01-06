package co.adrianblan.lightly;

import org.parceler.Parcel;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * A class which models the information for a sun cycle
 */
@Parcel
public class SunCycle {

    private float sunPositionHorizontal; // Position [0, 1] in x axis that the sun is at
    private float cycleOffsetHorizontal; // Position [0, 1] in x axis that the cycle should be offset
    private float twilightPositionVertical; // Position [0, 1] in y axis that the twilight is at

    private float sunrisePositionHorizontal; // Position [0, 1] in x axis that the sunrise is at
    private float sunsetPositionHorizontal; // Position [0, 1] in x axis that the sunset is set at

    public SunCycle() { /*Required empty bean constructor*/ }

    public SunCycle (Date current, SunriseSunsetData sunriseSunsetData) throws ParseException {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("hh:mm:ss aa", Locale.US);

        Date sunrise = simpleDateFormat.parse(sunriseSunsetData.getCivilTwilightBegin());
        Date sunset = simpleDateFormat.parse(sunriseSunsetData.getCivilTwilightEnd());

        initializeSunCycle(sunrise, sunset);
        updateSunPositionHorizontal(current);
    }

    public SunCycle (Date current, Date sunrise, Date sunset) {
        initializeSunCycle(sunrise, sunset);
        updateSunPositionHorizontal(current);
    }

    /**
     * Initializes a sun cycle, given the Dates of sunrise and sunset.
     * Sunset and sunrise are assumed to be during the same day.
     */
    private void initializeSunCycle(Date sunrise, Date sunset) {

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(sunrise);

        // Convert the dates to [0, 1] positions
        sunrisePositionHorizontal = getScaledTime(sunrise);
        sunsetPositionHorizontal = getScaledTime(sunset);

        // The position where the sun is at it's highest
        float solarNoonHorizontalPosition = sunrisePositionHorizontal + (sunsetPositionHorizontal - sunrisePositionHorizontal) / 2f;

        // The start of the cycle is a quarter earlier than the solar noon
        cycleOffsetHorizontal = ((solarNoonHorizontalPosition - 0.25f) + 1f) % 1f;

        // Calculate at what scaled height the twilight is at
        twilightPositionVertical = getVerticalPosition(sunrisePositionHorizontal);
    }

    /** Takes a position [0, 1] and returns the corresponding height [-1, 1] in the sun cycle */
    public float getVerticalPosition(float positionHorizontal) {
        return (float) Math.sin(positionHorizontal * Constants.tau - cycleOffsetHorizontal * Constants.tau);
    }

    /** Takes a position [0, 1] and returns the corresponding height [-1, 1] in the sun cycle */
    public static float getVerticalPosition(float positionHorizontal, float cycleOffsetHorizontal) {
        return (float) Math.sin(positionHorizontal * Constants.tau - cycleOffsetHorizontal * Constants.tau);
    }

    /** Calculates the position of the sun for the current time, given the initialized sun cycle */
    public void updateSunPositionHorizontal(Date current) {
        sunPositionHorizontal = getScaledTime(current);
    }

    /** Scales a Date [0, 1] according to how far it is in the current date */
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
        return ((radian + Constants.tau) % Constants.tau) / Constants.tau;
    }

    /** Takes a position [0, 1] and converts it to a string of the time (HH:MM) */
    public static String getTimeFromPosition(float position) {
        int hours = (int)(position * 24 / 1);

        int minutes = (int)((position * 24 % 1) * 60);

        // Make sure that minutes are always two digits
        if(minutes / 10 == 0) {
            return hours + ":0" + minutes;
        } else {
            return hours + ":" + minutes;
        }
    }

    /** Takes in a SynCycle object, and returns the text for the current status */
    public String getStatusText() {

        // If we are before the sunrise or after the sunset, we expect the sunrise
        if (getSunPositionHorizontal() < getSunrisePositionHorizontal() ||
                getSunPositionHorizontal() > getSunsetPositionHorizontal()) {

            int hoursUntilSunrise = (int) (((getSunrisePositionHorizontal() -
                    getSunPositionHorizontal() + 1.0f) % 1.0f) * 24f);

            return "Sunrise in " + StringUtils.getHumanizedHours(hoursUntilSunrise);
        } else {
            // Otherwise, we expect the sunset
            int hoursUntilSunset = (int) (((getSunsetPositionHorizontal() -
                    getSunPositionHorizontal() + 1.0f) % 1.0f) * 24f);

            return " Sunset in " + StringUtils.getHumanizedHours(hoursUntilSunset);
        }
    }

    public float getSunPositionHorizontal() {
        return sunPositionHorizontal;
    }

    public float getCycleOffsetHorizontal() {
        return cycleOffsetHorizontal;
    }

    public float getTwilightPositionVertical() {
        return twilightPositionVertical;
    }

    public float getSunrisePositionHorizontal() {
        return sunrisePositionHorizontal;
    }

    public float getSunsetPositionHorizontal() {
        return sunsetPositionHorizontal;
    }
}