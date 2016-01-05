package co.adrianblan.lightly;

public class SunriseSunsetData {
    private String civil_twilight_begin;
    private String civil_twilight_end;

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
