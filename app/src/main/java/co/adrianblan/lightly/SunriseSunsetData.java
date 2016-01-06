package co.adrianblan.lightly;

import com.google.gson.annotations.SerializedName;

import org.parceler.Parcel;

/**
 * Stores the data of times for sunrise and sunset.
 *
 * All data is stored in the format 'hh:mm:ss aa' in UTC time. We are getting the data from
 * api.sunrise-sunset.org and thus the serialized names have to conform to their API.
 */
@Parcel
public class SunriseSunsetData {

    @SerializedName("civil_twilight_begin")
    private String civilTwilightBegin;

    @SerializedName("civil_twilight_end")
    private String civilTwilightEnd;

    public SunriseSunsetData() { /*Required empty bean constructor*/ }

    public SunriseSunsetData(String civilTwilightBegin, String civilTwilightEnd) {
        this.civilTwilightBegin = civilTwilightBegin;
        this.civilTwilightEnd = civilTwilightEnd;
    }

    /** Returns a SunriseSunsetData object that is mocked to a reasonable sunrise and sunset */
    public static SunriseSunsetData getDummySunriseSunsetData() {
        SunriseSunsetData dummySunriseSunsetData = new SunriseSunsetData();
        dummySunriseSunsetData.setCivilTwilightBegin("08:00:00 AM");
        dummySunriseSunsetData.setCivilTwilightEnd("05:00:00 PM");

        return dummySunriseSunsetData;
    }

    /** Returns whether all the member variables in the object are not empty */
    public boolean isValid() {
        return (!civilTwilightBegin.isEmpty() && !civilTwilightEnd.isEmpty());
    }

    public String getCivilTwilightBegin() {
        return civilTwilightBegin;
    }

    public void setCivilTwilightBegin(String civilTwilightBegin) {
        this.civilTwilightBegin = civilTwilightBegin;
    }

    public String getCivilTwilightEnd() {
        return civilTwilightEnd;
    }

    public void setCivilTwilightEnd(String civilTwilightEnd) {
        this.civilTwilightEnd = civilTwilightEnd;
    }
}
