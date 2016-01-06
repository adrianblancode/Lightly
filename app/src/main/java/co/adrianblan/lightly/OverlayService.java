package co.adrianblan.lightly;

import android.app.Service;

import android.content.Context;
import android.content.Intent;

import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcelable;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;

import org.parceler.Parcels;

/**
 * A service which contains an overlay which dims the screen.
 */
public class OverlayService extends Service {

    private View overlayView;
    private int filterColor;
    private SunCycle sunCycle;
    private SunCycleColorHandler sunCycleColorHandler;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        if(intent != null && intent.getExtras() != null) {
            Bundle bundle = intent.getExtras();
            filterColor = bundle.getInt("filterColor");

            Parcelable sunCycleParcelable = bundle.getParcelable("sunriseCycle");
            sunCycle = Parcels.unwrap(sunCycleParcelable);

            Parcelable sunCycleColorHandleParcelable = bundle.getParcelable("sunriseSunsetData");
            sunCycleColorHandler = Parcels.unwrap(sunCycleColorHandleParcelable);
        }

        WindowManager windowManager = (WindowManager) getApplicationContext().getSystemService(Context.WINDOW_SERVICE);

        // If the overlay is null, we instansiate and add it to the WindowManager
        if(overlayView == null) {
            overlayView = new LinearLayout(this);

            // We set the overlay to be non-interactive
            overlayView.setFocusable(false);
            overlayView.setClickable(false);
            overlayView.setKeepScreenOn(false);
            overlayView.setLongClickable(false);
            overlayView.setFocusableInTouchMode(false);

            // Parameters for a fullscreen transparent overlay
            WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY,
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                    PixelFormat.TRANSPARENT);

            windowManager.addView(overlayView, layoutParams);
        }

        // Now that our view is added, we can simply change it's color
        overlayView.setBackgroundColor(filterColor);

        return START_STICKY;
    }

    /**
     * Takes the intensity as a parameter, and returns a color filter.
     *
     * @param intensity the intensity from 0 to 100
     * @return the background color which is applied as a filter to the screen
     */
    private int getBackgroundColor(int intensity) {
        int j = 255 - (int) Math.round(255D * Math.exp(4D * ((double) intensity / 100D) - 4D));
        return Color.argb(j, 255, 170, 84);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(overlayView != null){
            WindowManager windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
            windowManager.removeView(overlayView);
        }
    }
}
