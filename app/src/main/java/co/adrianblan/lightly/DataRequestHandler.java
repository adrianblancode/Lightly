package co.adrianblan.lightly;

import com.squareup.okhttp.OkHttpClient;

import retrofit.Call;
import retrofit.GsonConverterFactory;
import retrofit.Retrofit;

/**
 * Class which handles requesting data from REST APIs
 */
public class DataRequestHandler {

    private static OkHttpClient okHttpClient = new OkHttpClient();

    /** Returns a Call for LocationData requested from http://ip-api.com/ using Retrofit */
    public Call<LocationData> getLocationDataCall() {

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(Constants.locationDataUrl)
                .addConverterFactory(GsonConverterFactory.create())
                .client(okHttpClient)
                .build();

        LocationDataApi service = retrofit.create(LocationDataApi.class);
        return service.fetchLocationData();
    }

    /** Returns a Call for LocationData requested from http://ip-api.com/ using Retrofit */
    public Call<SunCycleData> getSunCycleDataCall(String latitude, String longitude) {

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(Constants.sunCycleDataUrl)
                .addConverterFactory(GsonConverterFactory.create())
                .client(okHttpClient)
                .build();

        SunCycleDataApi service = retrofit.create(SunCycleDataApi.class);
        return service.fetchSunCycleData(latitude, longitude);
    }
}
