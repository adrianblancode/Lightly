package co.adrianblan.lightly;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;


public class MainActivity extends AppCompatActivity {

    private static final int OVERLAY_PERMISSION_REQUEST_CODE = 1;

    private boolean isOverlayServiceActive = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final FloatingActionButton serviceFab = (FloatingActionButton)  findViewById(R.id.serviceFab);
        serviceFab.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                if(hasDrawOverlayPermission()) {

                    if(!isOverlayServiceActive()) {
                        startOverlayService();

                        serviceFab.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.primary_material_dark)));
                        serviceFab.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.ic_pause_white_36dp));
                    } else {
                        stopOverlayService();

                        serviceFab.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.accent_material_dark)));
                        serviceFab.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.ic_play_arrow_white_36dp));
                    }
                }
            }
        });

        // We request permissions, if we don't have them
        if(!hasDrawOverlayPermission()) {
            requestDrawOverlayPermission();
        }
    }

    protected void startOverlayService() {
        Intent intent = new Intent(this, OverlayService.class);
        startService(intent);
        setOverlayServiceActive(true);
    }

    protected void stopOverlayService() {
        Intent intent = new Intent(this, OverlayService.class);
        stopService(intent);
        setOverlayServiceActive(false);
    }

    /**
     * Returns whether we have the permission to draw overlays.
     *
     * In Marshmallow or higher this has to be done programatically at runtime, however for earlier
     * versions they are accepted on install. Can only be false if on Marshmallow or higher.
     */
    public boolean hasDrawOverlayPermission() {
        if(android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return Settings.canDrawOverlays(getApplicationContext());
        } else {
            // If the version is lower than M and app is running, the permission is already granted.
            return true;
        }
    }

    /**
     * Requests permission for drawing an overlay.
     *
     * Will only run if we do not already have the permission, AND if we are running on
     * Marshmallow or higher.
     */
    public void requestDrawOverlayPermission() {
        if(android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(getApplicationContext())) {

                // Send an intent, requesting the permission
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, OVERLAY_PERMISSION_REQUEST_CODE);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == OVERLAY_PERMISSION_REQUEST_CODE) {
            if(android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

                /**
                 * The user has denied the permission request.
                 * Display an alert dialog informing them of their consequences.
                 */
                if (!Settings.canDrawOverlays(this)) {

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

    protected boolean isOverlayServiceActive() {
        return isOverlayServiceActive;
    }

    protected void setOverlayServiceActive(boolean overlayServiceActive) {
        isOverlayServiceActive = overlayServiceActive;
    }
}
