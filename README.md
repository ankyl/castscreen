# castscreen
CastScreen is an experimental library for emulating the Cast Screen feature of the Google Cast app, 
allowing you to cast your Android device's screen to a Chromecast device.  
*Minimum API level:* 21

##Usage:
0. Get an app id for a Remote Display Receiver from the [Google Cast developer console](https://cast.google.com/publish/). 
You may need to add your Cast dongle as a test device, and wait ~15 minutes for changes to propagate in Google's servers.

1. Extend `CastScreenActivity`:  
```java
public class MainActivity extends CastScreenActivity
```

2. In your `menu.xml` file, add the following item:
```xml
<item
    android:id="@+id/media_route_menu_item"
    android:title="Chromecast"
    app:actionProviderClass="github.ankyl.castscreen.CastScreenMediaRouteActionProvider"
    app:showAsAction="always" />
```

3. In your `Activity`'s `onCreateOptionsMenu`, call `super.prepareCastButton(castButtonMenuItem, YOUR_APP_ID)` like so:
```java
@Override
public boolean onCreateOptionsMenu(Menu menu) {
    super.onCreateOptionsMenu(menu);
    getMenuInflater().inflate(R.menu.menu_main, menu);
    MenuItem castButtonMenuItem = menu.findItem(R.id.media_route_menu_item);
    super.prepareCastButton(castButtonMenuItem, YOUR_APP_ID);
    return true;
}
```

4. Congratulations, you are done. The Cast button will appear in this activity, allowing users to cast their whole screen.
You will need to repeat steps 1-3 in any activity where you want the Cast button.

##How does it work?
Captures the user's screen using the [MediaProjection API](https://developer.android.com/reference/android/media/projection/MediaProjection.html) (ProjectionManager.java), then renders it to a Chromecast device using the [CastRemoteDisplay API](https://developers.google.com/cast/docs/remote) (ConnectionManager.java). 
The user's screen is drawn on a `SurfaceView` (`R.id.castScreenPresentationSurface`) in CastScreenPresentation.java. 
You can edit `R.layout.cast_screen_presentation` to add additional UI elements to the remote display (or even do post-processing effects on `castScreenPresentationSurface`).

##Known Issues
####Sometimes the receiver shows up black and my logcat repeatedly has the lines `W/GCastSource: video RTT is high (___ ms)`?
This is caused by a bug in the Remote display API. Please add a bug report [here](https://code.google.com/p/google-cast-sdk/issues/detail?id=957) and hopefully it will be fixed soon.

##Bugs or feature requests?
Please write it up on the issue tracker or submit a pull request. Thanks!
