package co.adrianblan.lightly;

import org.parceler.Parcel;

/**
 * Stores the data of the name of the location, and the position.
 */
@Parcel
public class LocationData {

    private String regionName; // Region name is roughly the largest nearby city
    private String country;

    private double lat;
    private double lon;

    public LocationData() { /*Required empty bean constructor*/ }

    public LocationData (String regionName, String country, double lat, double lon) {
        this.regionName = regionName;
        this.country = country;
        this.lat = lat;
        this.lon = lon;
    }

    /** Returns a LocationData object that is mocked to central Stockholm */
    public static LocationData getDummyLocationData() {
        LocationData dummyLocationData = new LocationData();

        dummyLocationData.setRegionName("Unknown");
        dummyLocationData.setRegionName("Unknown");
        dummyLocationData.setLat(59.32);
        dummyLocationData.setLon(18.07);

        return dummyLocationData;
    }

    /** Returns whether all the member variables in the object are not empty */
    public boolean isValid() {
        return (!regionName.isEmpty() && !country.isEmpty());
    }

    public String getRegionName() {
        return regionName;
    }

    public void setRegionName(String regionName) {
        this.regionName = regionName;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public double getLat() {
        return lat;
    }

    public void setLat(double lat) {
        this.lat = lat;
    }

    public double getLon() {
        return lon;
    }

    public void setLon(double lon) {
        this.lon = lon;
    }
}
