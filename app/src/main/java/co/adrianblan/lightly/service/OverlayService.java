package co.adrianblan.lightly.service;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;

import android.content.Context;
import android.content.Intent;

import android.graphics.PixelFormat;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcelable;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;

import org.parceler.Parcels;

import co.adrianblan.lightly.MainActivity;
import co.adrianblan.lightly.R;
import co.adrianblan.lightly.helpers.Constants;
import co.adrianblan.lightly.suncycle.SunCycle;
import co.adrianblan.lightly.suncycle.SunCycleColorHandler;

/**
 * A service which contains an overlay which dims the screen.
 */
public class OverlayService extends Service {

    private View overlayView;
    private int filterColor;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        if(intent != null && intent.getExtras() != null) {
            Bundle bundle = intent.getExtras();
            filterColor = bundle.getInt("filterColor");
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

        // Intent for opening the app
        PendingIntent notifyPendingIntent = PendingIntent.getActivity(getApplicationContext(),
                Constants.ACTIVITY_MAIN_NOTIFICATION_REQUEST_CODE, new Intent(this, MainActivity.class), 0);

        // Persistent notification that is displayed on lowest priority whenever app is enabled
        Notification.Builder builder = new Notification.Builder(getApplicationContext());
        builder.setSmallIcon(R.mipmap.ic_launcher);
        builder.setContentTitle("Lightly");
        builder.setContentText("Running, press to open");
        builder.setContentIntent(notifyPendingIntent);
        builder.setOngoing(true);
        builder.setPriority(Notification.PRIORITY_MIN);
        Notification notification = builder.build();

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(0, notification);

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(overlayView != null){
            WindowManager windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
            windowManager.removeView(overlayView);
        }

        // Whenever the service is terminated, also destroy the notification
        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancelAll();
    }
}
