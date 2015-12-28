package co.adrianblan.lightly;

import android.app.Service;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.IBinder;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;

/**
 * A service which contains an overlay which dims the screen.
 */
public class OverlayService extends Service {

    private View overlayView;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        overlayView = new LinearLayout(this);

        // We set the overlay to be non-interactive
        overlayView.setFocusable(false);
        overlayView.setClickable(false);
        overlayView.setKeepScreenOn(false);
        overlayView.setLongClickable(false);
        overlayView.setFocusableInTouchMode(false);

        WindowManager windowManager = (WindowManager) getApplicationContext().getSystemService(Context.WINDOW_SERVICE);

        // Parameters for a fullscreen transparent overlay
        WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY,
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSPARENT);

        overlayView.setBackgroundColor(getBackgroundColor(85));
        windowManager.addView(overlayView, layoutParams);
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
