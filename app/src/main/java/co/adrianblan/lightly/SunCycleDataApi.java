package co.adrianblan.lightly;

import retrofit.Call;
import retrofit.http.GET;
import retrofit.http.Query;

public interface SunCycleDataApi {

    @GET("/json")
    Call<SunCycleData> fetchSunCycleData(@Query("lat") String latitude, @Query("lng") String longitude);
}
