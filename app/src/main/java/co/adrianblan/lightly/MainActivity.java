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
    private SunCycleData sunCycleData;
    private DataRequestHandler dataRequestHandler;
    private PermissionRequestHandler permissionRequestHandler;

    @Bind(R.id.switch_enabled)
    Switch switchEnabled;
    @Bind(R.id.seekbar_day)
    SeekBar seekBarDay;
    @Bind(R.id.seekbar_night)
    SeekBar seekBarNight;

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
        int progress = sharedPreferences.getInt("seekBarDayProgress", seekBarDayProgressDefaultValue);
        seekBarDay.setProgress(progress);

        progress = sharedPreferences.getInt("seekBarNightProgress", seekBarNightProgressDefaultValue);
        seekBarNight.setProgress(progress);

        SeekBar.OnSeekBarChangeListener seekBarListener = new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean userInitiated) {

                // If our day brightness is darker than night brightness, update
                if (seekBarDay.getProgress() < seekBarNight.getProgress() && userInitiated) {
                    seekBarDay.setProgress(progress);
                    seekBarNight.setProgress(progress);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        };

        seekBarDay.setOnSeekBarChangeListener(seekBarListener);
        seekBarNight.setOnSeekBarChangeListener(seekBarListener);

        // Populate with dummy data
        // TODO: serialize and store
        locationData = LocationData.getDummyLocationData();
        sunCycleData = SunCycleData.getDummySunCycleData();

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
     * request, attempts to also request SunCycleData.
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
     * Request the SunCycleData using the latitude and longitude of a location.
     */
    private void requestSunCycleData (String latitude, String longitude) {

        Call<SunCycleData> sunCycleDataCall = dataRequestHandler.getSunCycleDataCall(latitude,
                longitude);

        // Asynchronous callback for the request
        sunCycleDataCall.enqueue(new Callback<SunCycleData>() {

            @Override
            public void onResponse(Response<SunCycleData> response, Retrofit retrofit) {
                sunCycleData = response.body();
            }

            @Override
            public void onFailure(Throwable t) {
                System.err.println("Failed to get syn cycle data" + t.toString());
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
        editor.putInt("seekBarDayProgress", seekBarDay.getProgress());
        editor.putInt("seekBarNightProgress", seekBarNight.getProgress());

        editor.commit();
    }
}
