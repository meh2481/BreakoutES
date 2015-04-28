package edu.dsu.mark.breakoutes;

/**
 * Created by Mark on 3/31/2015.
 */
import android.content.Context;
import android.opengl.GLSurfaceView;

/**
 * A view container where OpenGL ES graphics can be drawn on screen.
 * This view can also be used to capture touch events, such as a user
 * interacting with drawn objects.
 */
public class BreakoutESSurfaceView extends GLSurfaceView {

    public final BreakoutESRenderer mRenderer;

    public BreakoutESSurfaceView(Context context) {
        super(context);

        // Create an OpenGL ES 2.0 context.
        setEGLContextClientVersion(2);

        // Set the Renderer for drawing on the GLSurfaceView
        mRenderer = new BreakoutESRenderer(context);
        setRenderer(mRenderer);
    }
}

