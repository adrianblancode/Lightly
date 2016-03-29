package co.adrianblan.lightly;

import android.content.Intent;
import android.view.View;

import com.hannesdorfmann.mosby.mvp.MvpView;

import co.adrianblan.lightly.suncycle.SunCycle;
import co.adrianblan.lightly.suncycle.SunCycleColorHandler;

/**
 * Interface for a view used by MainActivity
 */
interface MainView extends MvpView {

    void setSwitchEnabled(boolean enabled);

    void setNightColorProgress(int progress);
    void setNightBrightnessProgress(int progress);

    void setLocationBody(String location);

    void updateSunCycleView(SunCycle sunCycle, SunCycleColorHandler sunCycleColorHandler, String location);

    void showSnackBar(String message);
    void showSnackBar(String message, String actionMessage, View.OnClickListener onClickListener);

    void showErrorDialog();

    void startActivity(Intent intent, int code);
}
