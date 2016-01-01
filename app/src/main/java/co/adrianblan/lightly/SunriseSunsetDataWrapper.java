package co.adrianblan.lightly;

import java.util.ArrayList;

/**
 * Class which wraps SunriseSunsetData objects for Retrofit
 */
public class SunriseSunsetDataWrapper {
    private SunriseSunsetData results;

    public SunriseSunsetData getResults() {
        return results;
    }

    public void setResults(SunriseSunsetData results) {
        this.results = results;
    }
}
