package co.adrianblan.lightly;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;

import butterknife.ButterKnife;
import butterknife.Bind;
import butterknife.OnCheckedChanged;
import butterknife.OnClick;
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

    @Bind(R.id.sun_cycle_status)
    TextView sunCycleStatus;
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

        // Add sun drawables that the SunCycle will draw over the cycle
        ArrayList<Drawable> sunDrawables = new ArrayList<Drawable>();
        sunDrawables.add(ContextCompat.getDrawable(this, R.drawable.ic_brightness_medium_white_inverted_24dp));
        sunDrawables.add(ContextCompat.getDrawable(this, R.drawable.ic_brightness_high_white_24dp));
        sunDrawables.add(ContextCompat.getDrawable(this, R.drawable.ic_brightness_medium_white_24dp));
        sunDrawables.add(ContextCompat.getDrawable(this, R.drawable.ic_brightness_low_white_24dp));
        sunCycleView.setSunDrawables(sunDrawables);

        // Populate with dummy data
        // TODO: serialize and store
        locationData = LocationData.getDummyLocationData();
        sunriseSunsetData = SunriseSunsetData.getDummySunriseSunsetData();

        // Request data from REST APIs
        dataRequestHandler = new DataRequestHandler();
        requestLocationData();

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
     */
    private void requestLocationData() {

        Call<LocationData> locationDataCall = dataRequestHandler.getLocationDataCall();

        // Asynchronous callback for the request
        locationDataCall.enqueue(new Callback<LocationData>() {

            @Override
            public void onResponse(Response<LocationData> response, Retrofit retrofit) {
                locationData = response.body();
                locationBody.setText(locationData.getRegionName() + ", " + locationData.getCountry());

                System.err.println("Lat: " + locationData.getLat() + ", Lon:" + locationData.getLon());

                // As the request for location was successful, we now request for sun cycle data
                requestSunCycleData(locationData.getLat(), locationData.getLon());
            }

            @Override
            public void onFailure(Throwable t) {
                System.err.println("Failed to get location data" + t.toString());
                locationBody.setText(locationData.getRegionName() + ", " + locationData.getCountry());
            }
        });
    }

    /** Requests the SunriseSunsetData using the latitude and longitude of a location. */
    private void requestSunCycleData (Double latitude, Double longitude) {

        Call<SunriseSunsetDataWrapper> sunriseSunsetDataWrapperCall =
                dataRequestHandler.getSunriseSunsetDataCall(
                        Double.toString(latitude), Double.toString(longitude)
                );

        // Asynchronous callback for the request
        sunriseSunsetDataWrapperCall.enqueue(new Callback<SunriseSunsetDataWrapper>() {

            @Override
            public void onResponse(Response<SunriseSunsetDataWrapper> response, Retrofit retrofit) {
                SunriseSunsetDataWrapper sunriseSunsetDataWrapper = response.body();

                // TODO add null check
                if(sunriseSunsetDataWrapper != null) {
                    sunriseSunsetData = sunriseSunsetDataWrapper.getResults();

                    if (sunriseSunsetData.getCivilTwilightBegin() != null &&
                            sunriseSunsetData.getCivilTwilightEnd() != null) {
                        try {

                            // We create a SunCycle using the sunrise and sunset data
                            Date currentDate = new Date();
                            System.err.println("Date: " + currentDate.toString());
                            sunCycle = new SunCycle(currentDate, sunriseSunsetData);

                            System.err.println("twilightBegin: " + sunCycle.getSunrisePosition() +
                                    ", twilightEnd: " + sunCycle.getSunsetPosition());

                            sunCycleView.setCycleOffsetHorizontal(sunCycle.getCycleOffsetHorizontal());
                            sunCycleView.setSunPositionHorizontal(sunCycle.getSunPositionHorizontal());
                            sunCycleView.setTwilightPositionVertical(sunCycle.getTwilightPositionVertical());

                            sunCycleStatus.setText(getStatusTextFromSunCycle(sunCycle));

                            // Redraw the view
                            sunCycleView.invalidate();

                        } catch (ParseException e) {
                            e.printStackTrace();
                        }
                    } else {
                        System.err.println("Error: sunrise and sunset are null");
                    }
                } else {
                    System.err.println("Error: sunrise sunset request returned error code");
                }
            }

            @Override
            public void onFailure(Throwable t) {
                System.err.println("Error: Failed to get sunrise and sunset data" + t.toString());
            }
        });
    }

    /** Takes in a SynCycle object, and returns the text for the current status */
    private String getStatusTextFromSunCycle(SunCycle sunCycle) {

        // If we are before the sunrise or after the sunset, we expect the sunrise
        if (sunCycle.getSunPositionHorizontal() < sunCycle.getSunrisePosition() ||
                sunCycle.getSunPositionHorizontal() > sunCycle.getSunsetPosition()) {

            int hoursUntilSunrise = (int) (((sunCycle.getSunrisePosition() -
                    sunCycle.getSunPositionHorizontal() + 1.0f) % 1.0f) * 24f);

            return "Sunrise in " + getHumanizedHours(hoursUntilSunrise) + " (" +
                    SunCycle.getTimeFromPosition(sunCycle.getSunrisePosition()) + ")";
        } else {
            // Otherwise, we expect the sunset
            int hoursUntilSunset = (int) (((sunCycle.getSunsetPosition() -
                    sunCycle.getSunPositionHorizontal() + 1.0f) % 1.0f) * 24f);

            return " Sunset in " + getHumanizedHours(hoursUntilSunset) + " (" +
                    SunCycle.getTimeFromPosition(sunCycle.getSunsetPosition()) + ")";
        }
    }

    /** Takes an int, and returns a humanized String specifying the amount time */
    private String getHumanizedHours(int hours) {
        if(hours == 0) {
            return "Less than an hour";
        } else {
            return hours + " hours";
        }
    }

    /** When the user clicks the update location button, we refresh all location data */
    @OnClick(R.id.location_button)
    public void onClick() {
        requestLocationData();
    }

    /** When the user checks the enabled switch, we toggle the overlay */
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
