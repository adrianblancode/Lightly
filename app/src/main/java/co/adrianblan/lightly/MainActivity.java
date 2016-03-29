package co.adrianblan.lightly;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.os.Bundle;
import android.support.v7.widget.SwitchCompat;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.hannesdorfmann.mosby.mvp.MvpActivity;

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
import co.adrianblan.lightly.permission.PermissionHandler;
import co.adrianblan.lightly.service.OverlayService;
import co.adrianblan.lightly.service.OverlayServiceHandler;
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
public class MainActivity extends MvpActivity<MainView, MainPresenter> implements MainView {

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

    OverlayServiceHandler overlayServiceHandler;
    PermissionHandler permissionHandler;
    AlarmManager alarmManager;

    @NonNull
    @Override // Called internally by Mosby
    public MainPresenter createPresenter() {
        return new MainPresenter();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        overlayServiceHandler = new OverlayServiceHandler(this);
        permissionHandler = new PermissionHandler(this);
        alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

        // Add sun drawables that the SunCycle will draw over the cycle
        ArrayList<Drawable> sunDrawables = new ArrayList<>();
        sunDrawables.add(brightnessMediumInvertedDrawable);
        sunDrawables.add(brightnessHighDrawable);
        sunDrawables.add(brightnessMediumDrawable);
        sunDrawables.add(brightnessLowDrawable);
        sunCycleView.setSunDrawables(sunDrawables);

        if(!presenter.isInitialized()) {
            presenter.initialize(this, overlayServiceHandler, alarmManager,
                    PreferenceManager.getDefaultSharedPreferences(this), permissionHandler);
        }

        // Seekbar listener
        SeekBar.OnSeekBarChangeListener seekBarNightColorChangeListener = new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean b) {
                presenter.nightColorChanged(progress, overlayServiceHandler, permissionHandler);
            }

            @Override public void onStartTrackingTouch(SeekBar seekBar) {presenter.nightColorStartTouch(overlayServiceHandler, permissionHandler);}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {presenter.nightColorStopTouch(overlayServiceHandler, alarmManager, permissionHandler);}
        };

        SeekBar.OnSeekBarChangeListener seekBarNightBrightnessChangeListener = new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean b) {
                presenter.nightBrightnessChanged(progress, overlayServiceHandler, permissionHandler);
            }

            @Override public void onStartTrackingTouch(SeekBar seekBar) {presenter.nightBrightnessStartTouch(overlayServiceHandler, permissionHandler);}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {presenter.nightBrightnessStopTouch(overlayServiceHandler, alarmManager, permissionHandler);}
        };

        seekBarNightColor.setOnSeekBarChangeListener(seekBarNightColorChangeListener);
        seekBarNightBrightness.setOnSeekBarChangeListener(seekBarNightColorChangeListener);
    }

    @Override
    protected void onStart() {
        super.onStart();
        presenter.onStart();
    }

    @Override
    public void setSwitchEnabled(boolean enabled) {
        switchEnabled.setEnabled(enabled);
    }

    @Override
    public void setNightColorProgress(int progress) {
        seekBarNightColor.setProgress(progress);
    }

    @Override
    public void setNightBrightnessProgress(int progress) {
        seekBarNightBrightness.setProgress(progress);
    }

    @Override
    public void setLocationBody(String location) {
        locationBody.setText(location);
    }

    /** Takes a SunCycle, and updates the view according to the data inside */
    public void updateSunCycleView(SunCycle sunCycle, SunCycleColorHandler sunCycleColorHandler, String location) {

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

        locationBody.setText(location);

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

    /** When the user clicks the update location button, we refresh all location data */
    @OnClick(R.id.location_button)
    public void onClick() {
        presenter.requestLocationData();
    }

    /** When the user checks the enabled switch, we toggle the overlay */
    @OnCheckedChanged(R.id.switch_enabled)
    public void onCheckedChanged(boolean isChecked) {
        presenter.switchEnabled(isChecked, overlayServiceHandler, alarmManager, permissionHandler);
    }

    @Override
    public void startActivity(Intent intent, int code){
        startActivityForResult(permissionHandler.getDrawOverlayPermissionIntent(),
                Constants.OVERLAY_PERMISSION_REQUEST_CODE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        presenter.onActivityResult(permissionHandler, requestCode, resultCode, data);
    }

    @Override
    public void showSnackBar(String message) {
        Snackbar.make(lightlyMainView, message, Snackbar.LENGTH_SHORT).show();
    }

    @Override
    public void showSnackBar(String message, String actionMessage, View.OnClickListener onClickListener) {
        Snackbar.make(lightlyMainView, message, Snackbar.LENGTH_LONG)
                .setAction(actionMessage, onClickListener).show();
    }

    @Override
    public void showErrorDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.permission_denied_title)
                .setMessage(R.string.permission_denied_body)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                    }
                })
                .show();
    }

    @Override
    protected void onPause() {
        super.onPause();
        presenter.onPause(PreferenceManager.getDefaultSharedPreferences(this));
    }
}
