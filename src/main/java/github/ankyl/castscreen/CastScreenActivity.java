package github.ankyl.castscreen;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.MediaRouteChooserDialogFragment;
import android.support.v7.app.MediaRouteDialogFactory;
import android.support.v7.media.MediaRouteSelector;
import android.support.v7.media.MediaRouter;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Toast;

import com.google.android.gms.cast.CastDevice;
import com.google.android.gms.cast.CastMediaControlIntent;

/**
 * To add screen casting to your application, extend CastScreenActivity and add the following to your menu
 * <pre>
 *     <item
 *     android:id="@+id/media_route_menu_item"
 *     android:title="Chromecast"
 *     app:actionProviderClass="github.ankyl.castscreen.CastScreenMediaRouteActionProvider"
 *     app:showAsAction="always" />
 * </pre>
 *
 * Then in onCreateOptionsMenu():
 * <pre>
 *     MenuItem castButton = menu.findItem(R.id.media_route_menu_item);
 *     super.prepareCastButton(castButton, YOUR_APP_ID);
 * </pre>
 *
 * Where YOUR_APP_ID is an id for a Remote Display Receiver application, obtainable at
 * https://cast.google.com/publish/
 */
public abstract class CastScreenActivity extends AppCompatActivity {
    private static final String TAG = "CastScreenActivity";
    public static final int SCREEN_CAPTURE_REQUEST = 9000;
    public static final String PLEASE_REPORT_BUG = "Please report this as a bug to github.ankyl.castscreen";

    private String mAppId;
    private MediaRouteSelector mSelector;
    private MediaRouterCallback mCallback;
    private MediaRouter mRouter;
    private CastScreenMediaRouteActionProvider mProvider;
    private int mPermissionsResultCode;
    private Intent mPermissionsData;

    /**
     * called after the user finishes the screen capture permissions activity
     * @param requestCode arbitrary int representing why the activity was started
     * @param resultCode int indicating success or failure
     * @param data an Intent representing screen capture permission
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode != SCREEN_CAPTURE_REQUEST) {
            // this request wasn't meant for us
        } else if (resultCode != AppCompatActivity.RESULT_OK) {
            Toast.makeText(this, "Screen casting won't work without capture permission", Toast.LENGTH_LONG).show();
        } else {
            mPermissionsResultCode = resultCode;
            mPermissionsData = data;

            if (mProvider != null && mProvider.getMediaRouteButton() != null) {
                MediaRouteDialogFactory factory = mProvider.getMediaRouteButton().getDialogFactory();
                MediaRouteChooserDialogFragment chooser = factory.onCreateChooserDialogFragment();
                chooser.setRouteSelector(mSelector);
                FragmentManager fm = getSupportFragmentManager();
                chooser.show(fm, CastScreenMediaRouteButton.CHOOSER_TAG);
            } else {
                Log.e(TAG, "Null action provider or route button - " + PLEASE_REPORT_BUG);
            }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        // This fixes an illegal state exception; c.f. http://stackoverflow.com/a/10261449/5495432
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mRouter != null && mCallback != null) {
            mRouter.removeCallback(mCallback);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mRouter != null && mCallback != null && mSelector != null) {
            mRouter.addCallback(mSelector, mCallback, MediaRouter.CALLBACK_FLAG_REQUEST_DISCOVERY);
        }
    }

    /**
     * sets up the Cast button and starts route discovery
     * @param castMenuItem the MenuItem holding a {@link CastScreenMediaRouteActionProvider}
     * @param appId id for a Remote Display Receiver application
     */
    protected void prepareCastButton(MenuItem castMenuItem, String appId) {
        mSelector = new MediaRouteSelector.Builder().addControlCategory(
                CastMediaControlIntent.categoryForCast(appId)
        ).build();

        mProvider = (CastScreenMediaRouteActionProvider)
                MenuItemCompat.getActionProvider(castMenuItem);
        mProvider.setRouteSelector(mSelector);
        mAppId = appId;

        mRouter = MediaRouter.getInstance(getApplicationContext());
        mCallback = new MediaRouterCallback();
        mRouter.addCallback(mSelector, mCallback, MediaRouter.CALLBACK_FLAG_REQUEST_DISCOVERY);
    }

    private class MediaRouterCallback extends MediaRouter.Callback {
        @Override
        public void onRouteSelected(MediaRouter router, MediaRouter.RouteInfo route) {
            // start casting when the user selects a media route
            CastDevice device = CastDevice.getFromBundle(route.getExtras());
            DisplayMetrics metrics = new DisplayMetrics();
            getWindowManager().getDefaultDisplay().getMetrics(metrics);

            CastScreenService.start(getApplicationContext(),
                mAppId,
                metrics,
                mPermissionsResultCode,
                mPermissionsData,
                device,
                mRouter,
                CastScreenService.makeNotification(CastScreenActivity.this, device)
            );
        }

        @Override
        public void onRouteUnselected(MediaRouter router, MediaRouter.RouteInfo route) {
            CastScreenService.stop();
        }
    }

}
