package co.adrianblan.lightly;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
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

    private boolean isOverlayServiceActive;
    private boolean hasDummyData;
    private LocationData locationData;
    private SunriseSunsetData sunriseSunsetData;
    private SunCycle sunCycle;
    private SunCycleColorHandler sunCycleColorHandler;
    private DataRequestHandler dataRequestHandler;

    private Intent overlayIntent;
    private PendingIntent pendingOverlayIntent;

    private int nightColorProgress;
    private int nightBrightnessProgress;
    private boolean isInitialized = false;

    public void initialize(Context context, OverlayServiceHandler overlayServiceHandler, AlarmManager alarmManager,
                           SharedPreferences sharedPreferences, PermissionHandler permissionHandler) {
        gson = new Gson();

        // We request permissions to draw over the screen, if we don't have permissions


        // Request data from REST APIs
        dataRequestHandler = new DataRequestHandler();

        // Restore data from SharedPreferences
        isOverlayServiceActive = sharedPreferences.getBoolean("isOverlayServiceActive", false);

        nightColorProgress = sharedPreferences.getInt("seekBarNightColorProgress", SEEKBAR_DAY_PROGRESS_DEFAULT_VALUE);
        nightBrightnessProgress = sharedPreferences.getInt("seekBarNightBrightnessProgress", SEEKBAR_NIGHT_PROGRESS_DEFAULT_VALUE);

        if(isViewAttached()) {
            getView().setSwitchEnabled(isOverlayServiceActive);

            // Update SeekBars
            getView().setNightColorProgress(nightColorProgress);
            getView().setNightBrightnessProgress(nightBrightnessProgress);
        }

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
            sunCycleColorHandler = new SunCycleColorHandler(nightColorProgress,
                    nightBrightnessProgress);
        }

        // Automatically request location data if we only have dummy data
        if(hasDummyData) {
            requestLocationData();
        }

        overlayIntent = overlayServiceHandler.getNewOverlayService();

        // If the service was active before, start it again
        if (isOverlayServiceActive) {
            startOverlayService(overlayServiceHandler, alarmManager, permissionHandler);
        }

        isInitialized = true;
    }

    public boolean isInitialized() {
        return isInitialized;
    }

    public void onStart() {
        if(isViewAttached()) {
            getView().updateSunCycleView(sunCycle, sunCycleColorHandler, locationData.getHumanizedLocation());
        }
    }

    public void switchEnabled(boolean isChecked, OverlayServiceHandler overlayServiceHandler, AlarmManager alarmManager,
                              PermissionHandler permissionHandler) {
        // Do stuff if we have permission, otherwise set switch to disabled

        if (permissionHandler.hasDrawOverlayPermission()) {

            if (isChecked) {
                startOverlayService(overlayServiceHandler, alarmManager, permissionHandler);
            } else {
                stopOverlayService(overlayServiceHandler, alarmManager);
            }
        } else {

            if(isViewAttached()) {
                getView().setSwitchEnabled(isChecked);
                getView().startActivity(permissionHandler.getDrawOverlayPermissionIntent(),
                        Constants.OVERLAY_PERMISSION_REQUEST_CODE);
            }
        }
    }

    public void nightColorChanged(int progress, OverlayServiceHandler overlayServiceHandler,
                                  PermissionHandler permissionHandler) {
        nightColorProgress = progress;
        sunCycleColorHandler.setColorFilterIntensity(progress);

        startOverlayServiceTemporary(overlayServiceHandler, permissionHandler);
        if(isViewAttached()) {
            getView().updateSunCycleView(sunCycle, sunCycleColorHandler, locationData.getHumanizedLocation());
        }
    }

    public void nightColorStartTouch(OverlayServiceHandler overlayServiceHandler, PermissionHandler permissionHandler) {
        startOverlayServiceTemporary(overlayServiceHandler, permissionHandler);
    }

    public void nightColorStopTouch(OverlayServiceHandler overlayServiceHandler, AlarmManager alarmManager,
                                    PermissionHandler permissionHandler) {
        restartOverlayService(overlayServiceHandler, alarmManager, permissionHandler);
    }

    public void nightBrightnessChanged(int progress, OverlayServiceHandler overlayServiceHandler,
                                       PermissionHandler permissionHandler) {
        nightBrightnessProgress = progress;
        sunCycleColorHandler.setBrightnessFilterIntensity(progress);

        startOverlayServiceTemporary(overlayServiceHandler, permissionHandler);
        if(isViewAttached()) {
            getView().updateSunCycleView(sunCycle, sunCycleColorHandler, locationData.getHumanizedLocation());
        }
    }

    public void nightBrightnessStartTouch(OverlayServiceHandler overlayServiceHandler, PermissionHandler permissionHandler) {
        startOverlayServiceTemporary(overlayServiceHandler, permissionHandler);
    }

    public void nightBrightnessStopTouch(OverlayServiceHandler overlayServiceHandler, AlarmManager alarmManager,
                                         PermissionHandler permissionHandler) {
        restartOverlayService(overlayServiceHandler, alarmManager, permissionHandler);
    }

    /** Restarts the overlay service if the active flag is set, otherwise stops the service */
    private void restartOverlayService(OverlayServiceHandler overlayServiceHandler, AlarmManager alarmManager,
                                       PermissionHandler permissionHandler) {
        if(isOverlayServiceActive) {
            startOverlayService(overlayServiceHandler, alarmManager, permissionHandler);
        } else {
            stopOverlayService(overlayServiceHandler, alarmManager);
        }
    }

    /** Starts the overlay service, if we have permission to do so. Also sends all required info */
    private void startOverlayService(OverlayServiceHandler overlayServiceHandler, AlarmManager alarmManager,
                                     PermissionHandler permissionHandler) {
        if(permissionHandler.hasDrawOverlayPermission()) {

            cancelPendingOverlayIntents(alarmManager);

            Bundle bundle = new Bundle();

            // We are sending these two objects every time the filter updates, which is bad
            // But it's only two parcelable POJOs, and premature optimization is the root of all evil
            bundle.putParcelable("sunCycle", Parcels.wrap(sunCycle));
            bundle.putParcelable("sunCycleColorHandler", Parcels.wrap(sunCycleColorHandler));

            overlayIntent.putExtras(bundle);
            overlayServiceHandler.startService(overlayIntent);
            overlayServiceHandler.setOverlayServiceActive(true);

            pendingOverlayIntent = overlayServiceHandler.getPendingIntent(overlayIntent);

            // Repeat the intent in 15 minutes, every 15 minutes
            // Overwrites previous alarms because they have the same ID
            alarmManager.setInexactRepeating(AlarmManager.ELAPSED_REALTIME,
                    AlarmManager.INTERVAL_FIFTEEN_MINUTES, AlarmManager.INTERVAL_FIFTEEN_MINUTES,
                    pendingOverlayIntent);
        }
    }

    /** Starts a temporary overlay service with a temporary color, without setting the active flag */
    private void startOverlayServiceTemporary(OverlayServiceHandler overlayServiceHandler, PermissionHandler permissionHandler) {
        if(permissionHandler.hasDrawOverlayPermission()) {

            Intent temporaryOverlayIntent = overlayServiceHandler.getNewOverlayService();
            Bundle bundle = new Bundle();

            // Sends the strongest color on the cycle
            bundle.putInt("filterColor", sunCycleColorHandler.getOverlayColorMax());
            temporaryOverlayIntent.putExtras(bundle);
            overlayServiceHandler.startService(temporaryOverlayIntent);
        }
    }

    /** Stops the overlay service */
    private void stopOverlayService(OverlayServiceHandler overlayServiceHandler, AlarmManager alarmManager) {
        cancelPendingOverlayIntents(alarmManager);

        if(overlayIntent != null) {
            overlayServiceHandler.stopService(overlayIntent);
        }

        isOverlayServiceActive = false;
    }

    /** If we have a pending overlay intent already, cancel it */
    private void cancelPendingOverlayIntents(AlarmManager alarmManager) {
        if(pendingOverlayIntent != null) {
            alarmManager.cancel(pendingOverlayIntent);
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
                            getView().showSnackBar("LocationUpdated");
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
                System.err.println("Error: Failed to request sunrise and sunset data.\n" + t.toString());


            }
        });
    }

    public void onActivityResult(PermissionHandler permissionHandler, int requestCode, int resultCode, Intent data) {
        if (requestCode == Constants.OVERLAY_PERMISSION_REQUEST_CODE) {
            if(android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

                /**
                 * The user has denied the permission request.
                 * Display an alert dialog informing them of their consequences.
                 */
                if (!permissionHandler.hasDrawOverlayPermission()) {
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

        editor.putBoolean("isOverlayServiceActive", isOverlayServiceActive);
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
