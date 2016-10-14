package github.ankyl.castscreen;

import android.content.Context;
import android.support.v7.app.MediaRouteActionProvider;

/**
 * Subclasses {@link MediaRouteActionProvider} to use {@link CastScreenMediaRouteButton}
 */
public class CastScreenMediaRouteActionProvider extends MediaRouteActionProvider {

    public CastScreenMediaRouteActionProvider(Context context) {
        super(context);
    }

    @Override
    public CastScreenMediaRouteButton onCreateMediaRouteButton() {
        return new CastScreenMediaRouteButton(getContext());
    }
}
