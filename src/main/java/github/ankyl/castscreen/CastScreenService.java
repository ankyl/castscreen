package github.ankyl.castscreen;

import android.app.Activity;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Binder;
import android.os.IBinder;
import android.support.v7.media.MediaRouter;
import android.util.DisplayMetrics;
import android.util.Log;

import com.google.android.gms.cast.CastDevice;

/**
 * CastScreenService is responsible for the {@link ConnectionManager} and {@link Notification}
 * displayed while casting
 */
public class CastScreenService extends Service {
    private static final String TAG = "CastScreenService";
    private static CastScreenService sCastScreenService;
    private ConnectionManager mConnectionManager;
    private Binder mBinder;
    private Context mAppContext;
    private ServiceConnection mServiceConnection;

    /**
     * attempt to bind to the service and initialize service if successful
     */
    public static void start(final Context context,
                             final String appId,
                             final DisplayMetrics metrics,
                             final int permissionsResultCode,
                             final Intent permissionsData,
                             final CastDevice device,
                             final MediaRouter router,
                             final Notification notification) {

        if (sCastScreenService != null) {
            Log.w(TAG, "Tried to start CastScreenService without stopping prior instance; will stop and return");
            router.selectRoute(router.getDefaultRoute());
            return;
        }

        Intent intent = new Intent(context, CastScreenService.class);
        context.startService(intent);
        context.bindService(intent, new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder binder) {
                CastScreenService service = ((CastScreenBinder) binder).get();
                service.initialize(context, appId, this, metrics, permissionsResultCode,
                        permissionsData, device, router, notification);
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                Log.d(TAG, "Service disconnected");
                unbind(context, this);
            }
        }, Context.BIND_IMPORTANT);
    }

    /**
     * connect to the cast device and start showing the notification controller
     */
    private void initialize(Context context,
                            String appId,
                            ServiceConnection connection,
                            DisplayMetrics metrics,
                            int permissionsResultCode,
                            Intent permissionsData,
                            CastDevice device,
                            MediaRouter router,
                            Notification notification) {
        mConnectionManager = new ConnectionManager(context, metrics, permissionsResultCode,
                permissionsData, device, this, router, appId);
        mConnectionManager.connect();
        mAppContext = context;
        mServiceConnection = connection;

        startForeground(com.google.android.gms.R.id.cast_notification_id, notification);
        sCastScreenService = this;
    }

    /**
     * @return a default notification that returns the user to {@param activity}
     */
    public static Notification makeNotification(Activity activity, CastDevice device) {
        Intent startActivity = new Intent(activity, activity.getClass());
        startActivity.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent notificationIntent = PendingIntent.getActivity(activity, 0, startActivity, 0);
        return new Notification.Builder(activity)
                .setContentTitle(activity.getResources().getString(R.string.casting_screen))
                .setContentText(activity.getResources().getString(R.string.connected_to, device.getFriendlyName()))
                .setContentIntent(notificationIntent)
                .setSmallIcon(R.drawable.cast_ic_notification_on)
                .setColor(0xFF000000)
                .build();
    }

    @Override
    public void onCreate() {
        mBinder = new CastScreenBinder();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    /**
     * called by MediaRouter.Callback.onRouteUnselected to stop the service if it is running
     */
    public static void stop() {
        if (sCastScreenService == null) {
            Log.i(TAG, "Tried to stop a dead service; ignoring request");
        } else {
            sCastScreenService.stopInstance();
        }
    }

    /**
     * disconnect from remote display and stop service
     */
    private void stopInstance() {
        stopForeground(true); // remove notification
        stopSelf();
        unbind(mAppContext, mServiceConnection);
        mConnectionManager.disconnect();
        sCastScreenService = null;
    }

    private static void unbind(Context context, ServiceConnection connection) {
        try {
            context.unbindService(connection);
        } catch (IllegalArgumentException e) {
            Log.d(TAG, "Called unbind, but service was already unbound");
        }
    }

    @Override
    public void onDestroy() {
        Log.i(TAG, "Cast service stopped");
    }

    private class CastScreenBinder extends Binder {
        public CastScreenService get() {
            return CastScreenService.this;
        }
    }

}
