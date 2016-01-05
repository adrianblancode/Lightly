package co.adrianblan.lightly;

import org.parceler.Parcel;

/**
 * Stores the data of times for sunrise and sunset.
 *
 * All data is stored in the format 'hh:mm:ss aa' in UTC time.
 */
@Parcel
public class SunriseSunsetData {
    private String civil_twilight_begin;
    private String civil_twilight_end;

    public SunriseSunsetData() { /*Required empty bean constructor*/ }

    /** Returns a SunriseSunsetData object that is mocked to a reasonable sunrise and sunset */
    public static SunriseSunsetData getDummySunriseSunsetData() {
        SunriseSunsetData dummySunriseSunsetData = new SunriseSunsetData();
        dummySunriseSunsetData.setCivilTwilightBegin("08:00:00 AM");
        dummySunriseSunsetData.setCivilTwilightEnd("05:00:00 PM");

        return dummySunriseSunsetData;
    }

    /** Returns whether all the member variables in the object are not empty */
    public boolean isValid() {
        return (!civil_twilight_begin.isEmpty() && !civil_twilight_end.isEmpty());
    }

    public String getCivilTwilightBegin() {
        return civil_twilight_begin;
    }

    public void setCivilTwilightBegin(String civil_twilight_begin) {
        this.civil_twilight_begin = civil_twilight_begin;
    }

    public String getCivilTwilightEnd() {
        return civil_twilight_end;
    }

    public void setCivilTwilightEnd(String civil_twilight_end) {
        this.civil_twilight_end = civil_twilight_end;
    }
}
