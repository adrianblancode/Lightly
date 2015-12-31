package co.adrianblan.lightly;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

import java.text.ParseException;
import java.util.Date;

import butterknife.ButterKnife;
import butterknife.Bind;
import butterknife.OnCheckedChanged;
import retrofit.Call;
import retrofit.Callback;
import retrofit.Response;
import retrofit.Retrofit;

public class MainActivity extends AppCompatActivity {

    private static final boolean isOverlayServiceActiveDefaultValue = true;
    private static final int seekBarDayProgressDefaultValue = 80;
    private static final int seekBarNightProgressDefaultValue = 20;

    private boolean isOverlayServiceActive;
    private LocationData locationData;
    private SunriseSunsetData sunriseSunsetData;
    private SunCycle sunCycle;
    private DataRequestHandler dataRequestHandler;
    private PermissionRequestHandler permissionRequestHandler;

    @Bind(R.id.switch_enabled)
    Switch switchEnabled;
    @Bind(R.id.seekbar_night_color)
    SeekBar seekBarNightColor;
    @Bind(R.id.seekbar_night_brightness)
    SeekBar seekBarNightBrightness;

    @Bind(R.id.sun_cycle)
    SunCycleView sunCycleView;

    @Bind(R.id.location_body)
    TextView locationBody;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        // Restore data from SharedPreferences
        SharedPreferences sharedPreferences = getPreferences(Context.MODE_PRIVATE);
        isOverlayServiceActive = sharedPreferences.getBoolean("isOverlayServiceActive",
                isOverlayServiceActiveDefaultValue);

        switchEnabled.setChecked(isOverlayServiceActive);

        // Update SeekBars
        int progress = sharedPreferences.getInt("seekBarNightColorProgress", seekBarDayProgressDefaultValue);
        seekBarNightColor.setProgress(progress);

        progress = sharedPreferences.getInt("seekBarNightBrightnessProgress", seekBarNightProgressDefaultValue);
        seekBarNightBrightness.setProgress(progress);

        // Populate with dummy data
        // TODO: serialize and store
        locationData = LocationData.getDummyLocationData();
        sunriseSunsetData = SunriseSunsetData.getDummySunriseSunsetData();

        // Request data from REST APIs
        dataRequestHandler = new DataRequestHandler();
        requestLocationData(dataRequestHandler);

        // We request permissions to draw over the screen, if we don't have permissions
        permissionRequestHandler = new PermissionRequestHandler();

        // TODO: fix Marshmallow permissions
        if (!permissionRequestHandler.hasDrawOverlayPermission(this)) {
            startActivityForResult(permissionRequestHandler.getDrawOverlayPermissionIntent(this),
                    permissionRequestHandler.OVERLAY_PERMISSION_REQUEST_CODE);
        }

        // If the service was active before, start it again
        if (isOverlayServiceActive) {
            startOverlayService();
        }
    }

    protected void startOverlayService() {
        Intent intent = new Intent(this, OverlayService.class);
        startService(intent);
        isOverlayServiceActive = true;
    }

    protected void stopOverlayService() {
        Intent intent = new Intent(this, OverlayService.class);
        stopService(intent);
        isOverlayServiceActive = false;
    }

    /**
     * Requests the LocationData of the user, and updates the view accordingly. On successful
     * request, attempts to also request SunriseSunsetData.
     * @param dataRequestHandler the locationDataHandler to use to request the location
     */
    private void requestLocationData(final DataRequestHandler dataRequestHandler) {

        Call<LocationData> locationDataCall = dataRequestHandler.getLocationDataCall();

        // Asynchronous callback for the request
        locationDataCall.enqueue(new Callback<LocationData>() {

            @Override
            public void onResponse(Response<LocationData> response, Retrofit retrofit) {
                locationData = response.body();
                locationBody.setText(locationData.getRegionName() + ", " + locationData.getCountry());
                System.err.println(locationData.getLat() + " " + locationData.getLon());

                // As the request for location was successful, we now request for sun cycle data
                requestSunCycleData(Double.toString(locationData.getLat()),
                        Double.toString(locationData.getLon()));
            }

            @Override
            public void onFailure(Throwable t) {
                System.err.println("Failed to get location data" + t.toString());
                locationBody.setText(locationData.getRegionName() + ", " + locationData.getCountry());
            }
        });
    }

    /**
     * Request the SunriseSunsetData using the latitude and longitude of a location.
     */
    private void requestSunCycleData (String latitude, String longitude) {

        Call<SunriseSunsetDataWrapper> sunriseSunsetDataWrapperCall = dataRequestHandler.getSunriseSunsetDataCall(latitude,
                longitude);

        // Asynchronous callback for the request
        sunriseSunsetDataWrapperCall.enqueue(new Callback<SunriseSunsetDataWrapper>() {

            @Override
            public void onResponse(Response<SunriseSunsetDataWrapper> response, Retrofit retrofit) {
                SunriseSunsetDataWrapper sunriseSunsetDataWrapper = response.body();

                // TODO add null check
                sunriseSunsetData = sunriseSunsetDataWrapper.getResults();

                if(sunriseSunsetData.getSunrise() != null &&  sunriseSunsetData.getSunset() != null) {
                    try {
                        Date currentDate = new Date(System.currentTimeMillis());
                        sunCycle = new SunCycle(currentDate, sunriseSunsetData);

                        // TODO fix this
                        sunCycleView.setPathOffset(sunCycle.getOffset());
                        sunCycleView.setSunOffset(sunCycle.getSunHorizontalPosition());
                        sunCycleView.setTwilightDividerPosition(sunCycle.getTwilightVerticalPosition());

                        sunCycleView.invalidate();
                        System.err.println("Successs!");

                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                } else {
                    System.err.println("Error: sunrise and sunset are null");
                }
            }

            @Override
            public void onFailure(Throwable t) {
                System.err.println("Error: Failed to get sunrise and sunset data" + t.toString());
            }
        });
    }

    @OnCheckedChanged(R.id.switch_enabled)
    public void onCheckedChanged(boolean isChecked) {
        if (isChecked) {
            startOverlayService();
        } else {
            stopOverlayService();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == PermissionRequestHandler.OVERLAY_PERMISSION_REQUEST_CODE) {
            if(android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

                /**
                 * The user has denied the permission request.
                 * Display an alert dialog informing them of their consequences.
                 */
                if (resultCode == RESULT_CANCELED) {

                    new AlertDialog.Builder(this)
                            .setTitle(R.string.permission_denied_title)
                            .setMessage(R.string.permission_denied_body)
                            .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                }
                            })
                            .show();
                }
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        SharedPreferences.Editor editor = getPreferences(MODE_PRIVATE).edit();

        editor.putBoolean("isOverlayServiceActive", isOverlayServiceActive);
        editor.putInt("seekBarNightColorProgress", seekBarNightColor.getProgress());
        editor.putInt("seekBarNightBrightnessProgress", seekBarNightBrightness.getProgress());

        editor.commit();
    }
}
