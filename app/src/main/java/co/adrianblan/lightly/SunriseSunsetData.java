package co.adrianblan.lightly;

public class SunriseSunsetData {
    private String civil_twilight_begin;
    private String civil_twilight_end;

    /** Returns a SunriseSunsetData object that is mocked to a reasonable sunrise and sunset */
    public static SunriseSunsetData getDummySunriseSunsetData() {
        SunriseSunsetData dummySunriseSunsetData = new SunriseSunsetData();
        dummySunriseSunsetData.setCivilTwilightBegin("8:00:00 AM");
        dummySunriseSunsetData.setCivilTwilightBegin("5:00:00 PM");

        return dummySunriseSunsetData;
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
