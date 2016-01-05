package co.adrianblan.lightly;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.support.v7.app.AlertDialog;

/**
 * Handles checking for permissions, and requesting permissions.
 */
public class PermissionHandler {

    public static final int OVERLAY_PERMISSION_REQUEST_CODE = 1;

    /**
     * Returns whether we have the permission to draw overlays.
     *
     * In Marshmallow or higher this has to be done programatically at runtime, however for earlier
     * versions they are accepted on install. Can only be false if on Marshmallow or higher.
     */
    public boolean hasDrawOverlayPermission(Context context) {
        if(android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return Settings.canDrawOverlays(context.getApplicationContext());
        } else {
            // If the version is lower than M and app is running, the permission is already granted.
            return true;
        }
    }

    /**
     * Returns an intent for requesting permission for drawing an overlay on the screen.
     *
     * Will only run if we do not already have the permission, AND if we are running on
     * Marshmallow or higher.
     */
    public Intent getDrawOverlayPermissionIntent(Context context) {
        if(android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(context)) {

                // Aan intent, requesting the permission
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + context.getPackageName()));
                return intent;
            }
        }

        return null;
    }

}
