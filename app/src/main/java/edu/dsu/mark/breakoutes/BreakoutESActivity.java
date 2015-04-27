package edu.dsu.mark.breakoutes;

import android.app.Activity;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.v4.view.GestureDetectorCompat;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.WindowManager;

public class BreakoutESActivity extends Activity implements
        //SensorEventListener,
        GestureDetector.OnGestureListener,
        GestureDetector.OnDoubleTapListener
{
    private GestureDetectorCompat mDetector;
    private BreakoutESSurfaceView mGLView;
    private SensorManager senSensorManager;
    private Sensor senAccelerometer;
    private float lastX, lastY, lastZ;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        mGLView = new BreakoutESSurfaceView(this);
        setContentView(mGLView);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON, WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        //mWL = ((PowerManager)getSystemService ( Context.POWER_SERVICE )).newWakeLock(PowerManager.FULL_WAKE_LOCK, "WakeLock");
        //mWL.acquire();

        //senSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        //senAccelerometer = senSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        //senSensorManager.registerListener(this, senAccelerometer , SensorManager.SENSOR_DELAY_NORMAL);

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
        //senSensorManager.unregisterListener(this);
        mGLView.onPause();
        //TODO Pause physics sim
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        //senSensorManager.registerListener(this, senAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        mGLView.onResume();
        //TODO Resume physics sim
    }

    /*@Override
    public void onSensorChanged(SensorEvent sensorEvent)
    {
        Sensor mySensor = sensorEvent.sensor;

        if (mySensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            float x = (sensorEvent.values[0] + lastX) / 2.0f;
            float y = (sensorEvent.values[1] + lastY) / 2.0f;
            float z = (sensorEvent.values[2] + lastZ) / 2.0f;
            mGLView.move(x, y, z);
            lastX = sensorEvent.values[0];
            lastY = sensorEvent.values[1];
            lastZ = sensorEvent.values[2];
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i)
    {

    }*/

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

    @Override
    public boolean onFling(MotionEvent event1, MotionEvent event2,
                           float velocityX, float velocityY) {
        //Log.e("TOUCHTEST", "onFling: " + event1.toString()+event2.toString());
        //Log.e("FLINGTHING", velocityX + ", " + velocityY);
        if(velocityX > 4000 && Math.abs(velocityY) < 4000)
        {
            //Log.e("FLINGTHING", "SWIPELR");
            try
            {
                mGLView.mRenderer.miscEvents.put(BreakoutESRenderer.SWIPE_RIGHT);
            } catch (InterruptedException e)
            {
                e.printStackTrace();
            }
        }
        else if(velocityX < -4000 && Math.abs(velocityY) < 4000)
        {
            //Log.e("FLINGTHING", "SWIPERL");
            try
            {
                mGLView.mRenderer.miscEvents.put(BreakoutESRenderer.SWIPE_LEFT);
            } catch (InterruptedException e)
            {
                e.printStackTrace();
            }
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
    public boolean onSingleTapUp(MotionEvent event) {
        //Log.e("TOUCHTEST", "onSingleTapUp: " + event.toString());
        return true;
    }

    @Override
    public boolean onDoubleTap(MotionEvent event)
    {
        //Log.e("TOUCHTEST", "onDoubleTap: " + event.toString());
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
