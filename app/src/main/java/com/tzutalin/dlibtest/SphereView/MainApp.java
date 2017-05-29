package com.tzutalin.dlibtest.SphereView;

import android.app.Activity;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

/**
 * Main android application file.  This starts everything else.
 *
 * @author Jim Cornmell
 * @since July 2013
 */
public class MainApp extends Activity {
    /** The OpenGL view. */
    private GLSurfaceView mGlSurfaceView;

    private MotionEvent.PointerCoords point0 = new MotionEvent.PointerCoords();
    private MotionEvent.PointerCoords point1 = new MotionEvent.PointerCoords();

    /**
     * Called when the activity is first created.
     * @param savedInstanceState The instance state.
     */
    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        this.mGlSurfaceView = new GLSurfaceView(this);
        this.mGlSurfaceView.setEGLConfigChooser(8, 8, 8, 8, 16, 0);
        final GlRenderer renderer = new GlRenderer(this);
        this.mGlSurfaceView.setRenderer(renderer);
        this.setContentView(this.mGlSurfaceView);

        // Handle touch events.
        this.mGlSurfaceView.setOnTouchListener(touchListener(renderer));

    }

    @NonNull
    private View.OnTouchListener touchListener(final GlRenderer renderer) {
        return new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getPointerCount() == 1) {
                    switch (event.getActionMasked()) {
                        case MotionEvent.ACTION_DOWN:
                            event.getPointerCoords(0, point0);
                            break;
                        case MotionEvent.ACTION_UP:
                        case MotionEvent.ACTION_MOVE:
                            event.getPointerCoords(0, point1);
                            renderer.setMoveDelta(point0, point1);
                            break;
                    }
                }

                return true;
            }
        };
    }

    /**
     * Remember to resume the glSurface.
     */
    @Override
    protected void onResume() {
        super.onResume();
        this.mGlSurfaceView.onResume();
    }

    /**
     * Also pause the glSurface.
     */
    @Override
    protected void onPause() {
        this.mGlSurfaceView.onPause();
        super.onPause();
    }
}
