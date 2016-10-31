package co.adrianblan.lightly;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AlertDialog;
import android.view.View;

import com.google.gson.Gson;
import com.hannesdorfmann.mosby.mvp.MvpBasePresenter;

import org.parceler.Parcels;

import java.text.ParseException;
import java.util.Date;

import co.adrianblan.lightly.data.LocationData;
import co.adrianblan.lightly.data.SunriseSunsetData;
import co.adrianblan.lightly.data.SunriseSunsetDataWrapper;
import co.adrianblan.lightly.helpers.Constants;
import co.adrianblan.lightly.permission.PermissionHandler;
import co.adrianblan.lightly.network.DataRequestHandler;
import co.adrianblan.lightly.service.OverlayServiceHandler;
import co.adrianblan.lightly.suncycle.SunCycle;
import co.adrianblan.lightly.suncycle.SunCycleColorHandler;
import retrofit.Call;
import retrofit.Callback;
import retrofit.Response;
import retrofit.Retrofit;

/**
 * Presenter for MainView
 */
public class MainPresenter extends MvpBasePresenter<MainView> {

    private static final int SEEKBAR_DAY_PROGRESS_DEFAULT_VALUE = 80;
    private static final int SEEKBAR_NIGHT_PROGRESS_DEFAULT_VALUE = 80;

    private Gson gson;

    private boolean hasDummyData;
    private LocationData locationData;
    private SunriseSunsetData sunriseSunsetData;
    private SunCycle sunCycle;
    private SunCycleColorHandler sunCycleColorHandler;
    private DataRequestHandler dataRequestHandler;

    private OverlayServiceHandler overlayServiceHandler;
    private PermissionHandler permissionHandler;

    private Intent overlayIntent;
    private PendingIntent pendingOverlayIntent;

    private int nightColorProgress;
    private int nightBrightnessProgress;
    private boolean isInitialized = false;

    public void initialize(Context context) {
        gson = new Gson();

        // We request permissions to draw over the screen, if we don't have permissions
        overlayServiceHandler = new OverlayServiceHandler();
        permissionHandler = new PermissionHandler();

        // Request data from REST APIs
        dataRequestHandler = new DataRequestHandler();

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);

        // Restore data from SharedPreferences
        loadFromSharedPreferences(sharedPreferences);

        if(isViewAttached()) {
            getView().setSwitchEnabled(overlayServiceHandler.isOverlayServiceActive());

            // Update SeekBars
            getView().setNightColorProgress(nightColorProgress);
            getView().setNightBrightnessProgress(nightBrightnessProgress);
        }

        try {
            // We create a SunCycle using the sunrise and sunset data
            Date currentDate = new Date();
            sunCycle = new SunCycle(currentDate, sunriseSunsetData);

        } catch (ParseException e) {
            e.printStackTrace();
        }

        // Automatically request location data if we only have dummy data
        if(hasDummyData) {
            requestLocationData();
        }

        overlayIntent = overlayServiceHandler.getNewOverlayService(context);

        // If the service was active before, start it again
        if (overlayServiceHandler.isOverlayServiceActive()) {
            startOverlayService(context);
        }

        isInitialized = true;
    }

    /**
     * Loads the presenter variables from SharedPreferences
     */
    private void loadFromSharedPreferences(SharedPreferences sharedPreferences) {
        overlayServiceHandler.setOverlayServiceActive(sharedPreferences.getBoolean("isOverlayServiceActive", false));

        nightColorProgress = sharedPreferences.getInt("seekBarNightColorProgress", SEEKBAR_DAY_PROGRESS_DEFAULT_VALUE);
        nightBrightnessProgress = sharedPreferences.getInt("seekBarNightBrightnessProgress", SEEKBAR_NIGHT_PROGRESS_DEFAULT_VALUE);

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

        String sunCycleColorHandlerJson = sharedPreferences.getString("sunCycleColorHandler", null);

        // Update colors
        if(sunCycleColorHandlerJson != null) {
            sunCycleColorHandler = gson.fromJson(sunCycleColorHandlerJson, SunCycleColorHandler.class);
        } else {
            System.err.println("No sun cycle color handler saved.");
            sunCycleColorHandler = new SunCycleColorHandler(nightColorProgress,
                    nightBrightnessProgress);
        }
    }

    public boolean isInitialized() {
        return isInitialized;
    }

    public void onStart() {
        if(isViewAttached()) {
            getView().updateSunCycleView(sunCycle, sunCycleColorHandler, locationData.getHumanizedLocation());
        }
    }

    public void switchEnabled(Context context, boolean isChecked) {
        // Do stuff if we have permission, otherwise set switch to disabled
        if (permissionHandler.hasDrawOverlayPermission(context)) {

            if (isChecked) {
                startOverlayService(context);
            } else {
                stopOverlayService(context);
            }
        } else {

            if(isViewAttached()) {
                getView().setSwitchEnabled(isChecked);
                getView().startActivity(permissionHandler.getDrawOverlayPermissionIntent(context),
                        Constants.OVERLAY_PERMISSION_REQUEST_CODE);
            }
        }
    }

    public void nightColorChanged(Context context, int progress) {
        nightColorProgress = progress;
        sunCycleColorHandler.setColorFilterIntensity(progress);

        startOverlayServiceTemporary(context);
        if(isViewAttached()) {
            getView().updateSunCycleView(sunCycle, sunCycleColorHandler, locationData.getHumanizedLocation());
        }
    }

    public void nightColorStartTouch(Context context) {
        startOverlayServiceTemporary(context);
    }

    public void nightColorStopTouch(Context context) {
        restartOverlayService(context);
    }

    public void nightBrightnessChanged(Context context, int progress) {
        nightBrightnessProgress = progress;
        sunCycleColorHandler.setBrightnessFilterIntensity(progress);

        startOverlayServiceTemporary(context);
        if(isViewAttached()) {
            getView().updateSunCycleView(sunCycle, sunCycleColorHandler, locationData.getHumanizedLocation());
        }
    }

    public void nightBrightnessStartTouch(Context context) {
        startOverlayServiceTemporary(context);
    }

    public void nightBrightnessStopTouch(Context context) {
        restartOverlayService(context);
    }

    /** Restarts the overlay service if the active flag is set, otherwise stops the service */
    private void restartOverlayService(Context context) {
        if(overlayServiceHandler.isOverlayServiceActive()) {
            startOverlayService(context);
        } else {
            stopOverlayService(context);
        }
    }

    /** Starts the overlay service, if we have permission to do so. Also sends all required info */
    private void startOverlayService(Context context) {
        if(permissionHandler.hasDrawOverlayPermission(context)) {

            cancelPendingOverlayIntents(context);

            Bundle bundle = new Bundle();

            // We are sending these two objects every time the filter updates, which is bad
            // But it's only two parcelable POJOs, and premature optimization is the root of all evil
            bundle.putParcelable("sunCycle", Parcels.wrap(sunCycle));
            bundle.putParcelable("sunCycleColorHandler", Parcels.wrap(sunCycleColorHandler));

            overlayIntent.putExtras(bundle);
            overlayServiceHandler.startService(context, overlayIntent);
            overlayServiceHandler.setOverlayServiceActive(true);

            pendingOverlayIntent = overlayServiceHandler.getPendingIntent(context, overlayIntent);

            // Repeat the intent in 15 minutes, every 15 minutes
            // Overwrites previous alarms because they have the same ID
            ((AlarmManager) context.getSystemService(Context.ALARM_SERVICE))
                    .setInexactRepeating(AlarmManager.ELAPSED_REALTIME,
                    AlarmManager.INTERVAL_FIFTEEN_MINUTES, AlarmManager.INTERVAL_FIFTEEN_MINUTES,
                    pendingOverlayIntent);
        }
    }

    /** Starts a temporary overlay service with a temporary color, without setting the active flag */
    private void startOverlayServiceTemporary(Context context) {
        if(permissionHandler.hasDrawOverlayPermission(context)) {

            Intent temporaryOverlayIntent = overlayServiceHandler.getNewOverlayService(context);
            Bundle bundle = new Bundle();

            // Sends the strongest color on the cycle
            bundle.putInt("filterColor", sunCycleColorHandler.getOverlayColorMax());
            temporaryOverlayIntent.putExtras(bundle);
            overlayServiceHandler.startService(context, temporaryOverlayIntent);
        }
    }

    /** Stops the overlay service */
    private void stopOverlayService(Context context) {
        cancelPendingOverlayIntents(context);

        if(overlayIntent != null) {
            overlayServiceHandler.stopService(context, overlayIntent);
        }

        overlayServiceHandler.setOverlayServiceActive(false);
    }

    /** If we have a pending overlay intent already, cancel it */
    private void cancelPendingOverlayIntents(Context context) {
        if(pendingOverlayIntent != null) {
            ((AlarmManager) context.getSystemService(Context.ALARM_SERVICE))
                    .cancel(pendingOverlayIntent);
        }
    }

    /**
     * Requests the LocationData of the user, and updates the view accordingly. On successful
     * request, attempts to also request SunriseSunsetData.
     */
    public void requestLocationData() {

        Call<LocationData> locationDataCall = dataRequestHandler.getLocationDataCall();

        // Asynchronous callback for the request
        locationDataCall.enqueue(new Callback<LocationData>() {

            @Override
            public void onResponse(Response<LocationData> response, Retrofit retrofit) {
                LocationData locationDataTemp = response.body();

                // Check that our data was successfully fetched
                if(locationDataTemp.isValid()) {
                    locationData = locationDataTemp;
                    if(isViewAttached()) {
                        getView().setLocationBody(locationData.getRegionName() + ", " + locationData.getCountry());
                    }

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
                if(isViewAttached()) {
                    getView().showSnackBar("Oops! Unable to connect to server", "Retry", new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            requestLocationData();
                        }
                    });
                }
            }
        });
    }

    /** Requests the SunriseSunsetData using the latitude and longitude of a location. */
    public void requestSunCycleData (Double latitude, final Double longitude) {

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

                        sunriseSunsetData = sunriseSunsetDataTemp;
                        hasDummyData = false;

                        if(isViewAttached()) {
                            getView().updateSunCycleView(sunCycle, sunCycleColorHandler, locationData.getHumanizedLocation());
                        }

                        if(isViewAttached()) {
                            getView().showSnackBar("Location Updated");
                        }

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
                System.err.println("Error: Failed to request sunrise and sunset data.\n" + t.toString());}
        });
    }

    public void onActivityResult(Context context, int requestCode, int resultCode, Intent data) {
        if (requestCode == Constants.OVERLAY_PERMISSION_REQUEST_CODE) {
            if(android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

                /**
                 * The user has denied the permission request.
                 * Display an alert dialog informing them of their consequences.
                 */
                if (!permissionHandler.hasDrawOverlayPermission(context)) {
                    if(isViewAttached()) {
                        getView().showErrorDialog();
                    }
                }
            }
        }
    }

    public void onPause(SharedPreferences sharedPreferences) {

        // Save our data when lifecycle is ending
        SharedPreferences.Editor editor = sharedPreferences.edit();

        editor.putBoolean("isOverlayServiceActive", overlayServiceHandler.isOverlayServiceActive());
        editor.putBoolean("hasDummyData", hasDummyData);
        editor.putInt("seekBarNightColorProgress", nightColorProgress);
        editor.putInt("seekBarNightBrightnessProgress", nightBrightnessProgress);

        // Store data with GSON
        editor.putString("locationData", gson.toJson(locationData));
        editor.putString("sunriseSunsetData", gson.toJson(sunriseSunsetData));
        editor.putString("sunCycleColorHandler", gson.toJson(sunCycleColorHandler));

        editor.apply();
    }
}
