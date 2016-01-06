package co.adrianblan.lightly.network;

import co.adrianblan.lightly.data.SunriseSunsetDataWrapper;
import retrofit.Call;
import retrofit.http.GET;
import retrofit.http.Query;

public interface SunriseSunsetDataApi {

    @GET("/json")
    Call<SunriseSunsetDataWrapper> fetchSunriseSunsetData(@Query("lat") String latitude, @Query("lng") String longitude);
}
