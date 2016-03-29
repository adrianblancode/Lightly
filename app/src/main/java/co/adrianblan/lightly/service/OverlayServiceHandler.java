package co.adrianblan.lightly.service;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import co.adrianblan.lightly.helpers.Constants;

/**
 * Handles lifecycle of the overlay service.
 */
public class OverlayServiceHandler {

    private Context context;
    private boolean isOverlayServiceActive;

    public OverlayServiceHandler(Context context) {
        this.context = context;
    }

    public void startService(Intent intent) {
        context.startService(intent);
    }

    public void stopService(Intent intent) {
        context.stopService(intent);
    }

    public Intent getNewOverlayService() {
        return new Intent(context, OverlayService.class);
    }

    public PendingIntent getPendingIntent(Intent intent) {
        return PendingIntent.getService(context, Constants.SERVICE_OVERLAY_REQUEST_CODE,
                intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    public boolean isOverlayServiceActive() {
        return isOverlayServiceActive;
    }

    public void setOverlayServiceActive(boolean overlayServiceActive) {
        isOverlayServiceActive = overlayServiceActive;
    }
}
