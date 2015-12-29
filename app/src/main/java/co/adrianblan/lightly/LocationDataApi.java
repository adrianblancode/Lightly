package co.adrianblan.lightly;

import retrofit.Call;
import retrofit.http.GET;

public interface LocationDataApi {

    @GET("/json")
    Call<LocationData> fetchLocationData();
}
