package edu.dsu.mark.breakoutes;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.view.GestureDetectorCompat;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.WindowManager;

public class BreakoutESActivity extends Activity implements
        GestureDetector.OnGestureListener,
        GestureDetector.OnDoubleTapListener
{
    private GestureDetectorCompat mDetector;
    private BreakoutESSurfaceView mGLView;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        mGLView = new BreakoutESSurfaceView(this);
        setContentView(mGLView);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON, WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // Instantiate the gesture detector with the
        // application context and an implementation of
        // GestureDetector.OnGestureListener
        mDetector = new GestureDetectorCompat(this,this);
        // Set the gesture detector as the double tap
        // listener.
        mDetector.setOnDoubleTapListener(this);
    }

    @Override
    protected void onPause()
    {
        super.onPause();
        mGLView.onPause();
        mGLView.mRenderer.close();
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        mGLView.onResume();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event)
    {
        this.mDetector.onTouchEvent(event);

        //Asynchronous input, use threadsafe list
        try
        {
            mGLView.mRenderer.motEvents.put(event);
        } catch (InterruptedException e1)
        {
            e1.printStackTrace();
        }

        // Be sure to call the superclass implementation
        return super.onTouchEvent(event);
    }

    @Override
    public boolean onDown(MotionEvent event)
    {
        return true;
    }

    public void putRenderEvent(String sEvent)
    {
        try
        {
            mGLView.mRenderer.miscEvents.put(sEvent);
        } catch (InterruptedException e)
        {
            e.printStackTrace();
        }
    }

    @Override
    public boolean onFling(MotionEvent event1, MotionEvent event2, float velocityX, float velocityY)
    {
        if(velocityX > 4000 && Math.abs(velocityY) < 2000)
        {
            putRenderEvent(BreakoutESRenderer.SWIPE_RIGHT);
        }
        else if(velocityX < -4000 && Math.abs(velocityY) < 2000)
        {
            putRenderEvent(BreakoutESRenderer.SWIPE_LEFT);
        }
        else if(velocityY > 4000 && Math.abs(velocityX) < 2000)
        {
            putRenderEvent(BreakoutESRenderer.SWIPE_DOWN);
        }
        else if(velocityY < -4000 && Math.abs(velocityX) < 2000)
        {
            putRenderEvent(BreakoutESRenderer.SWIPE_UP);
        }
        return true;
    }

    @Override
    public void onLongPress(MotionEvent event)
    {
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY)
    {
        return true;
    }

    @Override
    public void onShowPress(MotionEvent event)
    {
    }

    @Override
    public boolean onSingleTapUp(MotionEvent event)
    {
        putRenderEvent(BreakoutESRenderer.TAP);
        return true;
    }

    @Override
    public boolean onDoubleTap(MotionEvent event)
    {
        return true;
    }

    @Override
    public boolean onDoubleTapEvent(MotionEvent event)
    {
        return true;
    }

    @Override
    public boolean onSingleTapConfirmed(MotionEvent event)
    {
        return true;
    }
}
