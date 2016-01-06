package co.adrianblan.lightly.network;

import co.adrianblan.lightly.data.LocationData;
import retrofit.Call;
import retrofit.http.GET;

public interface LocationDataApi {

    @GET("/json")
    Call<LocationData> fetchLocationData();
}
