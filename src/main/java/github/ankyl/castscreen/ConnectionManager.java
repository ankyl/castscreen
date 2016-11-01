package github.ankyl.castscreen;

import android.app.Presentation;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.media.MediaRouteSelector;
import android.support.v7.media.MediaRouter;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;

import com.google.android.gms.cast.ApplicationMetadata;
import com.google.android.gms.cast.Cast;
import com.google.android.gms.cast.CastDevice;
import com.google.android.gms.cast.CastMediaControlIntent;
import com.google.android.gms.cast.CastRemoteDisplay;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallbacks;
import com.google.android.gms.common.api.Status;

/**
 * ConnectionManager performs all the heavy lifting: connecting to the {@link GoogleApiClient},
 * starting the {@link ProjectionManager}, initiating the {@link CastRemoteDisplay} session, and
 * creating the {@link CastScreenPresentation}
 */
public class ConnectionManager {
    private static final String TAG = "ConnectionManager";

    private ProjectionManager mProjectionManager;
    private Presentation mPresentation;
    private GoogleApiClient mApiClient;
    private boolean mPresentationShowing = false;
    private CastScreenService mService;
    private MediaRouter mRouter;
    private StopCallback mStopCallback;
    private Context mAppContext;
    private String mAppId;
    private Handler mMainHandler;

    public ConnectionManager(Context context,
                              DisplayMetrics metrics,
                              int permissionsResultCode,
                              Intent permissionsData,
                              CastDevice device,
                              CastScreenService service,
                              MediaRouter router,
                              String appId) {
        mProjectionManager = new ProjectionManager(context, metrics, permissionsResultCode, permissionsData, router);
        mRouter = router;
        mStopCallback = new StopCallback();

        mService = service;
        mMainHandler = new Handler(service.getMainLooper());
        mAppContext = context;
        mAppId = appId;

        mApiClient = createApiClient(device);
    }

    /**
     * connect to the GoogleApiClient and start casting
     */
    public void connect() {
        MediaRouteSelector selector = new MediaRouteSelector.Builder().addControlCategory(
                CastMediaControlIntent.categoryForCast(mAppId)
        ).build();
        // Listen on MediaRouter so we can stop casting when route is unselected
        mRouter.addCallback(selector, mStopCallback, MediaRouter.CALLBACK_FLAG_REQUEST_DISCOVERY);
        mApiClient.connect();
    }

    /**
     * starts remote display when the GoogleApiClient is connected
     * @return a GoogleApiClient with Cast.API and CastRemoteDisplay.API attached
     */
    private GoogleApiClient createApiClient(CastDevice device) {
        // Cast API callbacks
        Cast.CastOptions.Builder castBuilder = new Cast.CastOptions.Builder(device, new Cast.Listener() {
            @Override
            public void onApplicationDisconnected(int statusCode) {
                Log.i(TAG, "Stop Casting because application disconnected");
                deselectRoute();
            }

            @Override
            public void onApplicationMetadataChanged(ApplicationMetadata metadata) {
                if (metadata != null && !(metadata.getApplicationId().equals(mAppId)) && mPresentationShowing) {
                    Log.i(TAG, "Stop Casting because another app started casting");
                    deselectRoute();
                }
            }
        });

        // Cast Remote Display API callbacks
        CastRemoteDisplay.CastRemoteDisplayOptions.Builder remoteDisplayBuilder = new
                CastRemoteDisplay.CastRemoteDisplayOptions.Builder(device, new CastRemoteDisplay.CastRemoteDisplaySessionCallbacks() {
            @Override
            public void onRemoteDisplayEnded(Status status) {
                Log.i(TAG, "Stop Casting because Remote Display session ended");
                deselectRoute();
            }
        });
        remoteDisplayBuilder.setConfigPreset(CastRemoteDisplay.CONFIGURATION_INTERACTIVE_REALTIME);

        // Google API callbacks
        GoogleApiClient.OnConnectionFailedListener apiFailListener = new GoogleApiClient.OnConnectionFailedListener() {
            @Override
            public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
                Log.i(TAG, "Stop Casting because GoogleApiClient connection failed");
                deselectRoute();
            }
        };
        GoogleApiClient.ConnectionCallbacks apiCallbacks = new GoogleApiClient.ConnectionCallbacks() {
            @Override
            public void onConnected(@Nullable Bundle bundle) {
                Log.i(TAG, "Connected to GoogleApiClient");
                connectToRemoteDisplayApi();
            }

            @Override
            public void onConnectionSuspended(int i) {
                Log.i(TAG, "Stop Casting because GoogleApiClient connection suspended");
                deselectRoute();
            }
        };

        return new GoogleApiClient.Builder(mAppContext, apiCallbacks, apiFailListener)
                .addApi(Cast.API, castBuilder.build())
                .addApi(CastRemoteDisplay.API, remoteDisplayBuilder.build())
                .build();
    }

    /**
     * connect to the remote display, and show the {@link CastScreenPresentation} if successful
     */
    private void connectToRemoteDisplayApi() {
        PendingResult<CastRemoteDisplay.CastRemoteDisplaySessionResult> result =
                CastRemoteDisplay.CastRemoteDisplayApi.startRemoteDisplay(mApiClient, mAppId);
        result.setResultCallback(new ResultCallbacks<CastRemoteDisplay.CastRemoteDisplaySessionResult>() {
            @Override
            public void onSuccess(@NonNull CastRemoteDisplay.CastRemoteDisplaySessionResult castRemoteDisplaySessionResult) {
                Display remoteDisplay = castRemoteDisplaySessionResult.getPresentationDisplay();
                mPresentation = new CastScreenPresentation(mService, remoteDisplay, mProjectionManager);
                mPresentation.show();
                mPresentationShowing = true;
                Log.d(TAG, "Created presentation");
            }

            @Override
            public void onFailure(@NonNull Status status) {
                Log.i(TAG, "Stop Casting because startRemoteDisplay failed");
                deselectRoute();
            }
        });
    }

    /**
     * disconnect and cleanup all resources
     */
    public void disconnect() {
        if (apiClientConnected()) {
            // Disconnect from remote display
            PendingResult<CastRemoteDisplay.CastRemoteDisplaySessionResult> result =
                    CastRemoteDisplay.CastRemoteDisplayApi.stopRemoteDisplay(mApiClient);
            result.setResultCallback(new ResultCallbacks<CastRemoteDisplay.CastRemoteDisplaySessionResult>() {
                @Override
                public void onSuccess(@NonNull CastRemoteDisplay.CastRemoteDisplaySessionResult castRemoteDisplaySessionResult) {
                    Log.i(TAG, "Success disconnecting from CastRemoteDisplayApi");
                }

                @Override
                public void onFailure(@NonNull Status status) {
                    Log.w(TAG, "Failed disconnecting from CastRemoteDisplayApi");
                }
            });

            // Disconnect from Google API
            mApiClient.disconnect();
        }

        // Stop listening for routes
        mRouter.removeCallback(mStopCallback);

        // Clean up MediaProjection resources
        if (mPresentation != null) mPresentation.dismiss();
        if (mProjectionManager != null) mProjectionManager.release();
    }

    private boolean apiClientConnected() {
        return (mApiClient != null && mApiClient.isConnected());
    }

    private void deselectRoute() {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            Log.i(TAG, "Deselecting route asynchronously");
            // because route selection must be done on the main thread
            mMainHandler.post(new Runnable() {
                @Override
                public void run() {
                    mRouter.selectRoute(mRouter.getDefaultRoute());
                }
            });
        } else {
            Log.i(TAG, "Deselecting route from main thread");
            mRouter.selectRoute(mRouter.getDefaultRoute());
        }
    }

    private class StopCallback extends MediaRouter.Callback {
        @Override
        public void onRouteUnselected(MediaRouter router, MediaRouter.RouteInfo route) {
            CastScreenService.stop();
        }
    }

}
