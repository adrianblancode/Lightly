package co.adrianblan.lightly;

import retrofit.Call;
import retrofit.http.GET;
import retrofit.http.Query;

public interface SunriseSunsetDataApi {

    @GET("/json")
    Call<SunriseSunsetDataWrapper> fetchSunriseSunsetData(@Query("lat") String latitude, @Query("lng") String longitude);
}
