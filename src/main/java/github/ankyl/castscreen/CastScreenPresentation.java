package github.ankyl.castscreen;

import android.content.Context;
import android.os.Bundle;
import android.view.Display;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.google.android.gms.cast.CastPresentation;

/**
 * A {@link CastPresentation} is a {@link Dialog} used to show media on a remote display
 * CastScreenPresentation uses {@link ProjectionManager} to draw on the presentation surface
 */
public class CastScreenPresentation extends CastPresentation {
    private ProjectionManager mProjectionManager;

    public CastScreenPresentation(Context context, Display display, ProjectionManager projectionManager) {
        super(context, display);
        mProjectionManager = projectionManager;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.cast_screen_presentation);

        final SurfaceView mSurfaceView = (SurfaceView) findViewById(R.id.castScreenPresentationSurface);
        mSurfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
                if (width > 0 && height > 0)
                    mProjectionManager.drawOnSurfaceView(mSurfaceView);
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {

            }
        });
    }

}
