package co.adrianblan.lightly.data;

import com.google.gson.annotations.SerializedName;

import org.parceler.Parcel;

/**
 * Stores the data of the name of the location, and the position.
 *
 * We are getting the data from ip-api.com and thus the serialized names have to conform to their API.
 */
@Parcel
public class LocationData {

    private String regionName; // Region name is roughly the largest nearby city
    private String country;

    @SerializedName("lat")
    private double latitude;
    @SerializedName("lon")
    private double longitude;

    public LocationData() { /*Required empty bean constructor*/ }

    public LocationData (String regionName, String country, double latitude, double longitude) {
        this.regionName = regionName;
        this.country = country;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    /** Returns a LocationData object that is mocked to central Stockholm */
    public static LocationData getDummyLocationData() {
        LocationData dummyLocationData = new LocationData();

        dummyLocationData.setRegionName("Unknown");
        dummyLocationData.setCountry("Unknown");
        dummyLocationData.setLatitude(59.32);
        dummyLocationData.setLongitude(18.07);

        return dummyLocationData;
    }

    public String getHumanizedLocation() {
        if(regionName.equals("Unknown") && country.equals("Unknown")) {
            return "Unknown location";
        } else {
            return regionName + ", " + country;
        }
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

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }
}
