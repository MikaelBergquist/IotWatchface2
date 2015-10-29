package com.watch.iot.iotwatchface2;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.IBinder;
import android.support.annotation.Nullable;

/**
 * Created by Mikael on 2015-10-26.
 */
public class SensorUpdater extends Thread implements SensorEventListener {
    //Sensorvariabler
    private SensorManager mSensorManager;
    private Sensor mSensor;

    //Variabler för att komma ihåg sensordata
    private float[] rMat;
    private float[] orientation;

    //azimuth som skall uppdateras
    Float azimuth;

    //Context från activityn
    Context mContext;
public SensorUpdater (Float azimuth, Context mContext){
    this.mContext = mContext;
    this.azimuth = azimuth;

    //Initierar orientation matriser
    orientation = new float[3];
    rMat = new float[9];

    //Initierar sensor klasser

}

    public void run(){
        mSensorManager = (SensorManager) mContext.getSystemService(Context.SENSOR_SERVICE);
        mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        mSensorManager.registerListener(this, mSensor, SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager.unregisterListener(this);
        azimuth = (float) 7.0;
    }




    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        if( sensorEvent.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR ){
//                 calculate th rotation matrix
            SensorManager.getRotationMatrixFromVector(rMat, sensorEvent.values);
//                 get the azimuth value (orientation[0]) in degree

            azimuth = (float)( Math.toDegrees( SensorManager.getOrientation( rMat, orientation )[0] ) + 360 ) % 360;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }
}
