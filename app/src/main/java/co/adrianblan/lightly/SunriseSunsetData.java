package co.adrianblan.lightly;

public class SunriseSunsetData {
    private String sunrise;
    private String sunset;

    /** Returns a SunriseSunsetData object that is mocked to a reasonable sunrise and sunset */
    public static SunriseSunsetData getDummySunriseSunsetData() {
        SunriseSunsetData dummySunriseSunsetData = new SunriseSunsetData();
        dummySunriseSunsetData.setSunrise("8:00:00 AM");
        dummySunriseSunsetData.setSunset("5:00:00 PM");

        return dummySunriseSunsetData;
    }

    public String getSunrise() {
        return sunrise;
    }

    public void setSunrise(String sunrise) {
        this.sunrise = sunrise;
    }

    public String getSunset() {
        return sunset;
    }

    public void setSunset(String sunset) {
        this.sunset = sunset;
    }
}
