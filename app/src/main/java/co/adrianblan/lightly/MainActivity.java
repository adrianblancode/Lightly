package co.adrianblan.lightly;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.SwitchCompat;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.google.gson.Gson;

import org.parceler.Parcels;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;

import butterknife.BindDrawable;
import butterknife.ButterKnife;
import butterknife.Bind;
import butterknife.OnCheckedChanged;
import butterknife.OnClick;
import co.adrianblan.lightly.data.LocationData;
import co.adrianblan.lightly.data.SunriseSunsetData;
import co.adrianblan.lightly.data.SunriseSunsetDataWrapper;
import co.adrianblan.lightly.helpers.Constants;
import co.adrianblan.lightly.helpers.PermissionHandler;
import co.adrianblan.lightly.network.DataRequestHandler;
import co.adrianblan.lightly.service.OverlayService;
import co.adrianblan.lightly.suncycle.SunCycle;
import co.adrianblan.lightly.suncycle.SunCycleColorHandler;
import co.adrianblan.lightly.suncycle.SunCycleColorWrapper;
import co.adrianblan.lightly.view.SunCycleView;
import retrofit.Call;
import retrofit.Callback;
import retrofit.Response;
import retrofit.Retrofit;

/**
 * The main class of the application. Handles all user interactions and services from the main screen.
 */
public class MainActivity extends AppCompatActivity {

    @Bind(R.id.switch_enabled)
    SwitchCompat switchEnabled;

    // Seekbars
    @Bind(R.id.seekbar_night_color)
    SeekBar seekBarNightColor;
    @Bind(R.id.night_color_circle)
    ImageView nightColorCircle;
    @Bind(R.id.night_color_value)
    TextView nightColorValue;

    @Bind(R.id.seekbar_night_brightness)
    SeekBar seekBarNightBrightness;
    @Bind(R.id.night_brightness_circle)
    ImageView nightBrightnessCircle;
    @Bind(R.id.night_brightness_value)
    TextView nightBrightnessValue;

    // Sun cycle
    @Bind(R.id.lightly_main)
    LinearLayout lightlyMainView;
    @Bind(R.id.sun_cycle_status)
    TextView sunCycleStatus;
    @Bind(R.id.sun_cycle)
    SunCycleView sunCycleView;
    @Bind(R.id.location_body)
    TextView locationBody;

    // Sun cycle drawables
    @BindDrawable(R.drawable.ic_brightness_medium_white_inverted_24dp)
    Drawable brightnessMediumInvertedDrawable;
    @BindDrawable(R.drawable.ic_brightness_high_white_24dp)
    Drawable brightnessHighDrawable;
    @BindDrawable(R.drawable.ic_brightness_medium_white_24dp)
    Drawable brightnessMediumDrawable;
    @BindDrawable(R.drawable.ic_brightness_low_white_24dp)
    Drawable brightnessLowDrawable;

    private static final int SEEKBAR_DAY_PROGRESS_DEFAULT_VALUE = 80;
    private static final int SEEKBAR_NIGHT_PROGRESS_DEFAULT_VALUE = 80;

    private Gson gson;
    private AlarmManager alarmManager;

    private boolean isOverlayServiceActive;
    private boolean hasDummyData;
    private LocationData locationData;
    private SunriseSunsetData sunriseSunsetData;
    private SunCycle sunCycle;
    private SunCycleColorHandler sunCycleColorHandler;
    private DataRequestHandler dataRequestHandler;
    private PermissionHandler permissionHandler;

    private Intent nonTemporaryOverlayIntent;
    private Intent temporaryOverlayIntent;
    private PendingIntent pendingOverlayIntent;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        gson = new Gson();
        alarmManager = (AlarmManager) this.getSystemService(Context.ALARM_SERVICE);

        // We request permissions to draw over the screen, if we don't have permissions
        permissionHandler = new PermissionHandler();

        // Request data from REST APIs
        dataRequestHandler = new DataRequestHandler();

        // Restore data from SharedPreferences
        SharedPreferences sharedPreferences = getPreferences(Context.MODE_PRIVATE);

        isOverlayServiceActive = sharedPreferences.getBoolean("isOverlayServiceActive", false);
        switchEnabled.setChecked(isOverlayServiceActive);

        // Update SeekBars
        seekBarNightColor.setProgress(sharedPreferences.getInt("seekBarNightColorProgress", SEEKBAR_DAY_PROGRESS_DEFAULT_VALUE));
        seekBarNightBrightness.setProgress(sharedPreferences.getInt("seekBarNightBrightnessProgress", SEEKBAR_NIGHT_PROGRESS_DEFAULT_VALUE));

        hasDummyData = sharedPreferences.getBoolean("hasDummyData", true);

        // If we have stored previous data, retrieve it. Otherwise populate with dummy data.
        if(!hasDummyData) {
            String locationDataJson = sharedPreferences.getString("locationData", null);
            locationData = gson.fromJson(locationDataJson, LocationData.class);

            String sunriseSunsetDataJson = sharedPreferences.getString("sunriseSunsetData", null);
            sunriseSunsetData = gson.fromJson(sunriseSunsetDataJson, SunriseSunsetData.class);
        } else {
            locationData = LocationData.getDummyLocationData();
            sunriseSunsetData = SunriseSunsetData.getDummySunriseSunsetData();
        }

        try {
            // We create a SunCycle using the sunrise and sunset data
            Date currentDate = new Date();
            sunCycle = new SunCycle(currentDate, sunriseSunsetData);

        } catch (ParseException e) {
            e.printStackTrace();
        }

        // Update colors
        String sunCycleColorHandlerJson = sharedPreferences.getString("sunCycleColorHandler", null);

        if(sunCycleColorHandlerJson != null) {
            sunCycleColorHandler = gson.fromJson(sunCycleColorHandlerJson, SunCycleColorHandler.class);
        } else {
            System.err.println("No sun cycle color handler saved.");
            sunCycleColorHandler = new SunCycleColorHandler(seekBarNightColor.getProgress(),
                    seekBarNightBrightness.getProgress());
        }

        // Seekbar listener
        SeekBar.OnSeekBarChangeListener seekBarChangeListener = new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean b) {

                // Update color handler with new colors
                if(seekBar.equals(seekBarNightColor)) {
                    sunCycleColorHandler.setColorFilterIntensity(progress);
                } else if (seekBar.equals(seekBarNightBrightness)) {
                    sunCycleColorHandler.setBrightnessFilterIntensity(progress);
                }

                startOverlayServiceTemporary();
                updateSunCycleView(sunCycle);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                startOverlayServiceTemporary();
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                restartOverlayService();
            }
        };

        seekBarNightColor.setOnSeekBarChangeListener(seekBarChangeListener);
        seekBarNightBrightness.setOnSeekBarChangeListener(seekBarChangeListener);

        // Add sun drawables that the SunCycle will draw over the cycle
        ArrayList<Drawable> sunDrawables = new ArrayList<>();
        sunDrawables.add(brightnessMediumInvertedDrawable);
        sunDrawables.add(brightnessHighDrawable);
        sunDrawables.add(brightnessMediumDrawable);
        sunDrawables.add(brightnessLowDrawable);
        sunCycleView.setSunDrawables(sunDrawables);

        locationBody.setText(locationData.getRegionName() + ", " + locationData.getCountry());

        updateSunCycleView(sunCycle);

        // Automatically request location data if we only have dummy data
        if(hasDummyData) {
            requestLocationData();
        }

        nonTemporaryOverlayIntent = new Intent(this, OverlayService.class);
        temporaryOverlayIntent = new Intent(this, OverlayService.class);

        // If the service was active before, start it again
        if (isOverlayServiceActive) {
            startOverlayService();
        }
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
                LocationData locationDataTemp = response.body();

                // Check that our data was successfully fetched
                if(locationDataTemp.isValid()) {
                    locationData = locationDataTemp;
                    locationBody.setText(locationData.getRegionName() + ", " + locationData.getCountry());

                    // As the request for location was successful, we now request for sun cycle data
                    requestSunCycleData(locationData.getLatitude(), locationData.getLongitude());
                } else {
                    System.err.println("Error: Location data is null");
                }
            }

            @Override
            public void onFailure(Throwable t) {
                System.err.println("Failed to get location data" + t.toString());

                // Snackbar where user can retry fetching data
                Snackbar.make(lightlyMainView, "Oops! Unable to connect to server", Snackbar.LENGTH_LONG)
                        .setAction("Retry", new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                requestLocationData();
                            }
                        }).show();
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

                // Check that our data was successfully fetched
                if(sunriseSunsetDataWrapper != null && sunriseSunsetDataWrapper.getResults().isValid()) {
                    SunriseSunsetData sunriseSunsetDataTemp = sunriseSunsetDataWrapper.getResults();

                    try {
                        // We create a SunCycle using the sunrise and sunset data
                        Date currentDate = new Date();
                        sunCycle = new SunCycle(currentDate, sunriseSunsetDataTemp);

                        System.err.println("sunrise: " + sunriseSunsetData.getCivilTwilightBegin()
                                + ", sunset: " + sunriseSunsetData.getCivilTwilightEnd());

                        sunriseSunsetData = sunriseSunsetDataTemp;
                        hasDummyData = false;

                        updateSunCycleView(sunCycle);

                        // Snackbar that informs of the updated location
                        Snackbar.make(lightlyMainView, "Location updated", Snackbar.LENGTH_SHORT).show();

                    } catch (ParseException e) {
                        System.err.println("Error parsing sunrise and and sunset data in SunCycle");
                        e.printStackTrace();
                    }
                } else {
                    System.err.println("Error: sunrise sunset data is null");
                }
            }

            @Override
            public void onFailure(Throwable t) {
                System.err.println("Error: Failed to request sunrise and sunset data.\n" + t.toString());

                // Snackbar where user can retry fetching data
                Snackbar.make(lightlyMainView, "Oops! Unable to connect to server", Snackbar.LENGTH_LONG)
                        .setAction("Retry", new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                requestLocationData();
                            }
                        }).show();
            }
        });
    }

    /** Takes a SunCycle, and updates the view according to the data inside */
    private void updateSunCycleView(SunCycle sunCycle) {

        final float COLOR_EMPHASIS = 4f;
        final float BRIGHTNESS_EMPHASIS = 1.5f;

        // Exaggerate colors for emphasis
        SunCycleColorWrapper nightColor = sunCycleColorHandler.getColorFilterWrapper();
        nightColor.setAlpha(Math.min((int) (nightColor.getAlpha() * COLOR_EMPHASIS), 255));
        nightColorCircle.setColorFilter(nightColor.getColor());

        SunCycleColorWrapper nightBrightness = sunCycleColorHandler.getBrightnessFilterWrapper();
        nightBrightness.setAlpha(Math.min((int) (nightBrightness.getAlpha() * BRIGHTNESS_EMPHASIS), 255));
        nightBrightnessCircle.setColorFilter(nightBrightness.getColor());

        // Set seekbar value text
        nightColorValue.setText(sunCycleColorHandler.getColorTemperature() + "K");
        nightBrightnessValue.setText(sunCycleColorHandler.getBrightnessPercent() + "%");

        // Update sun position to current time
        sunCycle.updateSunPositionHorizontal(new Date());

        sunCycleView.setNightColor(sunCycleColorHandler.getOverlayColorMax());
        sunCycleView.setCycleOffsetHorizontal(sunCycle.getCycleOffsetHorizontal());
        sunCycleView.setSunPositionHorizontal(sunCycle.getSunPositionHorizontal());
        sunCycleView.setTwilightPositionVertical(sunCycle.getTwilightPositionVertical());

        sunCycleStatus.setText(sunCycle.getStatusText());

        // Redraw the view
        sunCycleView.invalidate();
    }

    /** Restarts the overlay service if the active flag is set, otherwise stops the service */
    private void restartOverlayService() {
        if(isOverlayServiceActive) {
            startOverlayService();
        } else {
            stopOverlayService();
        }
    }

    /** Starts the overlay service, if we have permission to do so. Also sends all required info */
    private void startOverlayService() {
        if(permissionHandler.hasDrawOverlayPermission(this)) {

            cancelPendingOverlayIntents();
            //stopOverlayService();

            Bundle bundle = new Bundle();

            // We are sending these two objects every time the filter updates, which is bad
            // But it's only two parcelable POJOs, and premature optimization is the root of all evil
            bundle.putParcelable("sunCycle", Parcels.wrap(sunCycle));
            bundle.putParcelable("sunCycleColorHandler", Parcels.wrap(sunCycleColorHandler));

            nonTemporaryOverlayIntent.putExtras(bundle);
            startService(nonTemporaryOverlayIntent);

            isOverlayServiceActive = true;

            pendingOverlayIntent = PendingIntent.getService(this, Constants.SERVICE_OVERLAY_REQUEST_CODE,
                    nonTemporaryOverlayIntent, PendingIntent.FLAG_UPDATE_CURRENT);

            // Repeat the intent in 15 minutes, every 15 minutes
            // Overwrites previous alarms because they have the same ID
            alarmManager.setInexactRepeating(AlarmManager.ELAPSED_REALTIME,
                    AlarmManager.INTERVAL_FIFTEEN_MINUTES, AlarmManager.INTERVAL_FIFTEEN_MINUTES,
                    pendingOverlayIntent);
        }
    }

    /** Starts a temporary overlay service with a temporary color, without setting the active flag */
    private void startOverlayServiceTemporary() {
        if(permissionHandler.hasDrawOverlayPermission(this)) {

            Intent temporaryOverlayIntent = new Intent(this, OverlayService.class);
            Bundle bundle = new Bundle();

            // Sends the strongest color on the cycle
            bundle.putInt("filterColor", sunCycleColorHandler.getOverlayColorMax());
            temporaryOverlayIntent.putExtras(bundle);
            startService(temporaryOverlayIntent);
        }
    }

    /** Stops the overlay service */
    private void stopOverlayService() {
        cancelPendingOverlayIntents();

        if(nonTemporaryOverlayIntent != null) {
            stopService(nonTemporaryOverlayIntent);
        }

        isOverlayServiceActive = false;
    }

    /** If we have a pending overlay intent already, cancel it */
    public void cancelPendingOverlayIntents() {
        if(pendingOverlayIntent != null) {
            alarmManager.cancel(pendingOverlayIntent);
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

        // Do stuff if we have permission, otherwise set switch to disabled
        if (permissionHandler.hasDrawOverlayPermission(this)) {

            if (isChecked) {
                startOverlayService();
            } else {
                stopOverlayService();
            }
        } else {
            startActivityForResult(permissionHandler.getDrawOverlayPermissionIntent(this),
                    Constants.OVERLAY_PERMISSION_REQUEST_CODE);
            switchEnabled.setChecked(false);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == Constants.OVERLAY_PERMISSION_REQUEST_CODE) {
            if(android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

                /**
                 * The user has denied the permission request.
                 * Display an alert dialog informing them of their consequences.
                 */
                if (!permissionHandler.hasDrawOverlayPermission(getApplicationContext())) {

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

        // Save our data when lifecycle is ending
        SharedPreferences.Editor editor = getPreferences(MODE_PRIVATE).edit();

        editor.putBoolean("isOverlayServiceActive", isOverlayServiceActive);
        editor.putBoolean("hasDummyData", hasDummyData);
        editor.putInt("seekBarNightColorProgress", seekBarNightColor.getProgress());
        editor.putInt("seekBarNightBrightnessProgress", seekBarNightBrightness.getProgress());

        // Store data with GSON
        editor.putString("locationData", gson.toJson(locationData));
        editor.putString("sunriseSunsetData", gson.toJson(sunriseSunsetData));
        editor.putString("sunCycleColorHandler", gson.toJson(sunCycleColorHandler));

        editor.apply();
    }
}
