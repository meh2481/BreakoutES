package edu.dsu.mark.openglesenvironment;

import android.app.Activity;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.opengl.GLSurfaceView;
import android.os.Bundle;

public class OpenGLES20Activity extends Activity implements SensorEventListener {

    private MyGLSurfaceView mGLView;
    private SensorManager senSensorManager;
    private Sensor senAccelerometer;
    private float lastX, lastY, lastZ;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mGLView = new MyGLSurfaceView(this);
        setContentView(mGLView);

        senSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        senAccelerometer = senSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        senSensorManager.registerListener(this, senAccelerometer , SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    protected void onPause() {
        super.onPause();
        senSensorManager.unregisterListener(this);
        mGLView.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        senSensorManager.registerListener(this, senAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        mGLView.onResume();
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
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
    public void onAccuracyChanged(Sensor sensor, int i) {

    }
}
