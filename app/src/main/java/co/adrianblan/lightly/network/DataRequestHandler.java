package co.adrianblan.lightly.network;

import com.squareup.okhttp.OkHttpClient;

import co.adrianblan.lightly.helpers.Constants;
import co.adrianblan.lightly.data.LocationData;
import co.adrianblan.lightly.data.SunriseSunsetDataWrapper;
import retrofit.Call;
import retrofit.GsonConverterFactory;
import retrofit.Retrofit;

/**
 * Class which handles requesting data from REST APIs
 */
public class DataRequestHandler {

    private static OkHttpClient okHttpClient = new OkHttpClient();

    /** Returns a Call for LocationData requested from ip-api.com using Retrofit */
    public Call<LocationData> getLocationDataCall() {

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(Constants.locationDataUrl)
                .addConverterFactory(GsonConverterFactory.create())
                .client(okHttpClient)
                .build();

        LocationDataApi service = retrofit.create(LocationDataApi.class);
        return service.fetchLocationData();
    }

    /** Returns a Call for SunriseSunsetData requested from sunrise-sunset.org using Retrofit */
    public Call<SunriseSunsetDataWrapper> getSunriseSunsetDataCall(String latitude, String longitude) {

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(Constants.sunriseSunsetDataUrl)
                .addConverterFactory(GsonConverterFactory.create())
                .client(okHttpClient)
                .build();

        SunriseSunsetDataApi service = retrofit.create(SunriseSunsetDataApi.class);
        return service.fetchSunriseSunsetData(latitude, longitude);
    }
}
