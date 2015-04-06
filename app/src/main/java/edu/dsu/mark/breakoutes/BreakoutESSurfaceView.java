package edu.dsu.mark.breakoutes;

/**
 * Created by Mark on 3/31/2015.
 */
import android.content.Context;
import android.opengl.GLSurfaceView;
import android.view.MotionEvent;

import static java.lang.Math.atan2;

/**
 * A view container where OpenGL ES graphics can be drawn on screen.
 * This view can also be used to capture touch events, such as a user
 * interacting with drawn objects.
 */
public class BreakoutESSurfaceView extends GLSurfaceView {

    private final BreakoutESRenderer mRenderer;

    public BreakoutESSurfaceView(Context context) {
        super(context);

        // Create an OpenGL ES 2.0 context.
        setEGLContextClientVersion(2);

        // Set the Renderer for drawing on the GLSurfaceView
        mRenderer = new BreakoutESRenderer();
        setRenderer(mRenderer);
        mRenderer.setContext(context);

        // Render the view only when there is a change in the drawing data
        //setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        // MotionEvent reports input details from the touch screen
        // and other input controls. In this case, you are only
        // interested in events where the touch position changed.

        float x = e.getX();
        float y = e.getY();

        switch (e.getAction()) {
            case MotionEvent.ACTION_MOVE:
                y = (float) (y - getHeight() / 2.0);
                x = (float) (x - getWidth() / 2.0);

                mRenderer.setAngle((float) (atan2(y, x)));
        }
        return true;
    }

    public void move(float x, float y, float z)
    {
        mRenderer.setCam(x, y, z);
    }

}

