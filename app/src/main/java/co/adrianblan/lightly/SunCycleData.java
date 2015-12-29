package co.adrianblan.lightly;

public class SunCycleData {
    private String sunrise;
    private String sunset;

    /** Returns a SunCycleData object that is mocked to a reasonable sun cycle */
    public static SunCycleData getDummySunCycleData() {
        SunCycleData dummySunCycleData = new SunCycleData();
        dummySunCycleData.setSunrise("8:00:00 AM");
        dummySunCycleData.setSunrise("5:00:00 PM");

        return dummySunCycleData;
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
