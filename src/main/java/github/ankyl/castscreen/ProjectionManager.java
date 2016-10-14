package github.ankyl.castscreen;

import android.content.Context;
import android.content.Intent;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.support.v7.media.MediaRouter;
import android.util.DisplayMetrics;
import android.view.Surface;
import android.view.SurfaceView;

/**
 * ProjectionManager encapsulate the call to {@link MediaProjection} to draw the user's screen
 * on a {@link VirtualDisplay}
 */
public class ProjectionManager {
    private static final String VIRTUAL_DISPLAY_NAME = "CastScreenVirtualDisplay";
    private int mDensity;
    private MediaProjection mProjection;
    private VirtualDisplay mDisplay;

    public ProjectionManager(Context context, DisplayMetrics metrics,
                             int permissionsResultCode, Intent permissionsData, final MediaRouter router) {
        mDensity = metrics.densityDpi;
        mProjection = ((MediaProjectionManager) context.getSystemService(Context.MEDIA_PROJECTION_SERVICE))
                .getMediaProjection(permissionsResultCode, permissionsData);
        if (mProjection == null) {
            throw new IllegalStateException("Null media projection manager - " + CastScreenActivity.PLEASE_REPORT_BUG);
        }
        // When the MediaProjection is stopped, deselect the active route to ensure casting stops
        mProjection.registerCallback(new MediaProjection.Callback() {
            @Override
            public void onStop() {
                router.selectRoute(router.getDefaultRoute());
            }
        }, null);
    }

    /**
     * @param view the SurfaceView on which to draw user's screen
     */
    public void drawOnSurfaceView(SurfaceView view) {
        if (mDisplay != null) mDisplay.release();

        Surface surface = view.getHolder().getSurface();
        mDisplay = mProjection.createVirtualDisplay(
            VIRTUAL_DISPLAY_NAME,
            view.getWidth(),
            view.getHeight(),
            mDensity,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY,
            surface,
            null,
            null
        );
    }

    public void release() {
        if (mDisplay != null) mDisplay.release();
        if (mProjection != null) {
            mProjection.stop();
            mProjection = null;
        }
    }

}