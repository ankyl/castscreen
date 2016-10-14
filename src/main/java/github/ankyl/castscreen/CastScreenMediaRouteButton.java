package github.ankyl.castscreen;

import android.app.Activity;

import android.content.Context;
import android.content.Intent;
import android.media.projection.MediaProjectionManager;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.MediaRouteButton;
import android.support.v7.media.MediaRouteSelector;
import android.support.v7.media.MediaRouter;
import android.util.AttributeSet;
import android.util.Log;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Similar to {@link MediaRouteButton}, except starts {@link CastScreenActivity} to get
 * permission to capture the user's screen
 */
public class CastScreenMediaRouteButton extends MediaRouteButton {
    private static final String TAG = "MediaRouteButton";
    public static final String CHOOSER_TAG =
            "android.support.v7.mediarouter:MediaRouteChooserDialogFragment";
    private static final String CONTROLLER_TAG =
            "android.support.v7.mediarouter:MediaRouteControllerDialogFragment";
    private MediaRouteSelector mSelector;

    public CastScreenMediaRouteButton(Context context) {
        this(context, null);
    }

    public CastScreenMediaRouteButton(Context context, AttributeSet attrs) {
        this(context, attrs, android.support.v7.mediarouter.R.attr.mediaRouteButtonStyle);
    }

    public CastScreenMediaRouteButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public boolean showDialog() {
        try {
            Activity currentActivity = getActivity();
            final FragmentManager fm = ((FragmentActivity) currentActivity).getSupportFragmentManager();
            if (!isAttachedToWindow()
                    || (fm.findFragmentByTag(CONTROLLER_TAG) != null)
                    || (fm.findFragmentByTag(CHOOSER_TAG) != null) ) {
                return false;
            }

            MediaRouter.RouteInfo route = getMediaRouter().getSelectedRoute();
            if (route.isDefault() || !route.matchesSelector(mSelector)) { // route chooser
                Intent intent = ((MediaProjectionManager)
                        currentActivity.getSystemService(Context.MEDIA_PROJECTION_SERVICE)).createScreenCaptureIntent();
                currentActivity.startActivityForResult(intent, CastScreenActivity.SCREEN_CAPTURE_REQUEST);

                return true;
            } else { // route controller
                return super.showDialog();
            }
        } catch (NoSuchFieldException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
            Log.e(TAG, "MediaRouteButton implementation changed - " + CastScreenActivity.PLEASE_REPORT_BUG);
        }
        return false;
    }

    @Override
    public void setRouteSelector(MediaRouteSelector selector) {
        super.setRouteSelector(selector);
        mSelector = selector;
    }

    private MediaRouter getMediaRouter() throws NoSuchFieldException, IllegalAccessException {
        Field routerField =  getClass().getSuperclass().getDeclaredField("mRouter");
        routerField.setAccessible(true);
        return (MediaRouter) routerField.get(this);
    }

    private Activity getActivity() throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        Method getActivityMethod = getClass().getSuperclass().getDeclaredMethod("getActivity");
        getActivityMethod.setAccessible(true);
        return (Activity) getActivityMethod.invoke(this);
    }

}