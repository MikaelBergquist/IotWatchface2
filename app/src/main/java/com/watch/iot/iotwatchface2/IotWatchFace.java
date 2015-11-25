/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.watch.iot.iotwatchface2;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.text.format.Time;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.os.Vibrator;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.TimeZone;

/**
 * Analog watch face with a ticking second hand. In ambient mode, the second hand isn't shown. On
 * devices with low-bit ambient mode, the hands are drawn without anti-aliasing in ambient mode.
 */
public class

        IotWatchFace extends CanvasWatchFaceService  {
    /**
     * Update rate in milliseconds for interactive mode. We update once a second to advance the
     * second hand.
     */
    private static final long INTERACTIVE_UPDATE_RATE_MS = 10;
    private static final float PI = (float) Math.PI;


    /**
     * Handler message id for updating the time periodically in interactive mode.
     */
    private static final int MSG_UPDATE_TIME = 0;

    @Override
    public Engine onCreateEngine() {
        return new Engine();
    }

    private class Engine extends CanvasWatchFaceService.Engine implements SensorEventListener {
        Paint mBackgroundPaint,mHourHandPaint,mMinuteHandPaint,mSecondHandPaint,ctrlButtonPaint,mIconPaint;

        boolean mAmbient;
        Time mTime;
        //en lista för gadgets
        private ArrayList<Gadget> gadgetList;
        private Bitmap[] iconNoFocus, iconInFocus;
        //sätts till gadgetId när engadget  är i fokus klockan 12
        private int gadgetInFocus,lastGadgetInFocus;

        //Bitmaps för bakgrunden o ikonerna
        Bitmap mBackgroundNoFocusBitmap;
        Bitmap mBackgroundInFocusBitmap;
        Bitmap mBackgroundActiveBitmap;
        Bitmap mBackgroundScaledBitmap;
        Bitmap mIconLampBitmap;
        Bitmap mIconLampFocusBitmap;
        Bitmap mIconTvBitmap;
        Bitmap mIconTvFocusBitmap;


        private Vibrator v;


        final Handler mUpdateTimeHandler = new EngineHandler(this);

        final BroadcastReceiver mTimeZoneReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                mTime.clear(intent.getStringExtra("time-zone"));
                mTime.setToNow();
            }
        };
        boolean mRegisteredTimeZoneReceiver = false;

        /*
            Kod som egentligen skall flyttas till SensorUpdater
         */
        //Sensorvariabler
        private SensorManager mSensorManager;
        private Sensor mSensor;

        //Variabler för att komma ihåg sensordata
        private float[] rMat;
        private float[] orientation;

        //azimuth som skall uppdateras
        private Float azimuth;

        //gränser för när en gadget är i fokus
        private float focusTolerance = (float) PI/24;

        /**
         * Whether the display supports fewer bits for each color in ambient mode. When true, we
         * disable anti-aliasing in ambient mode.
         */
        boolean mLowBitAmbient;

        boolean tempClicked; //ta bort

        @Override
        public void onCreate(SurfaceHolder holder) {
            super.onCreate(holder);

            setWatchFaceStyle(new WatchFaceStyle.Builder(IotWatchFace.this)
                    .setCardPeekMode(WatchFaceStyle.PEEK_MODE_SHORT)
                    .setBackgroundVisibility(WatchFaceStyle.BACKGROUND_VISIBILITY_INTERRUPTIVE)
                    .setShowSystemUiTime(false)
                    .setAcceptsTapEvents(true)

                    .build());

            Resources resources = IotWatchFace.this.getResources();
            Drawable backgroundNoFocusDrawable = resources.getDrawable(R.drawable.background_no_focus, null);
            mBackgroundNoFocusBitmap = ((BitmapDrawable) backgroundNoFocusDrawable).getBitmap();
            Drawable backgroundInFocusDrawable = resources.getDrawable(R.drawable.background_in_focus, null);
            mBackgroundInFocusBitmap = ((BitmapDrawable) backgroundInFocusDrawable).getBitmap();
            mBackgroundActiveBitmap=mBackgroundNoFocusBitmap;



            //ikoner till gadgets
            iconInFocus = new Bitmap[4];
            iconNoFocus = new Bitmap[4];

            Drawable mIconLampDrawable = resources.getDrawable(R.drawable.small_lightbulb_white, null);
            mIconLampBitmap = ((BitmapDrawable) mIconLampDrawable).getBitmap();
            iconNoFocus[0]= mIconLampBitmap;

            Drawable mIconLampFocusDrawable = resources.getDrawable(R.drawable.small_lightbulb_yellow, null);
            mIconLampFocusBitmap = ((BitmapDrawable) mIconLampFocusDrawable).getBitmap();
            iconInFocus[0]= mIconLampFocusBitmap;

            Drawable mIconTvDrawable = resources.getDrawable(R.drawable.small_tv_white, null);
            mIconTvBitmap = ((BitmapDrawable) mIconTvDrawable).getBitmap();
            iconNoFocus[1]= mIconTvBitmap;

            Drawable mIconTvFocusDrawable = resources.getDrawable(R.drawable.small_tv_blue, null);
            mIconTvFocusBitmap = ((BitmapDrawable) mIconTvFocusDrawable).getBitmap();
            iconInFocus[1]= mIconTvFocusBitmap;



            mBackgroundPaint = new Paint();
            mBackgroundPaint.setColor(resources.getColor(R.color.analog_background));

            mHourHandPaint = new Paint();
            mHourHandPaint.setColor(resources.getColor(R.color.analog_hands));
            mHourHandPaint.setStrokeWidth(resources.getDimension(R.dimen.analog_hour_hand_stroke));
            mHourHandPaint.setAntiAlias(true);
            mHourHandPaint.setStrokeCap(Paint.Cap.ROUND);

            mMinuteHandPaint = new Paint();
            mMinuteHandPaint.setColor(resources.getColor(R.color.analog_hands));
            mMinuteHandPaint.setStrokeWidth(resources.getDimension(R.dimen.analog_minute_hand_stroke));
            mMinuteHandPaint.setAntiAlias(true);
            mMinuteHandPaint.setStrokeCap(Paint.Cap.ROUND);

            mSecondHandPaint = new Paint();
            mSecondHandPaint.setColor(resources.getColor(R.color.analog_hands));
            mSecondHandPaint.setStrokeWidth(resources.getDimension(R.dimen.analog_second_hand_stroke));
            mSecondHandPaint.setAntiAlias(true);
            mSecondHandPaint.setStrokeCap(Paint.Cap.ROUND);



           // paint till control-knappen
            ctrlButtonPaint = new Paint();
            ctrlButtonPaint.setColor(resources.getColor(R.color.controlButton));
            ctrlButtonPaint.setStrokeWidth(resources.getDimension(R.dimen.controlButtonStroke));
            ctrlButtonPaint.setAntiAlias(true);
            ctrlButtonPaint.setStyle(Paint.Style.STROKE);
            ctrlButtonPaint.setTextSize(20);
            ctrlButtonPaint.setTextAlign(Paint.Align.CENTER);

            mIconPaint= new Paint();
            mIconPaint.setFilterBitmap(true);
            mIconPaint.setAntiAlias(true);

            azimuth = new Float(0);

            mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
            mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);

            orientation = new float[3];
            rMat = new float[9];

            mTime = new Time();
            //en lista med några lampor (gadget(type,id,xpos,ypos))
            gadgetList=new ArrayList<Gadget>();
            gadgetList.add(new Gadget(0,1,0,3));
           gadgetList.add(new Gadget(1,2,3,3));
            gadgetList.add(new Gadget(0,3,3,0));
            gadgetList.add(new Gadget(0,4,-2,-3));
            v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);


        }

        @Override
        public void onDestroy() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            super.onDestroy();
        }

        @Override
        public void onPropertiesChanged(Bundle properties) {
            super.onPropertiesChanged(properties);
            mLowBitAmbient = properties.getBoolean(PROPERTY_LOW_BIT_AMBIENT, false);
        }

        @Override
        public void onTimeTick() {
            super.onTimeTick();
            invalidate();
        }

        @Override
        public void onAmbientModeChanged(boolean inAmbientMode) {
            super.onAmbientModeChanged(inAmbientMode);
            if (mAmbient != inAmbientMode) {
                mAmbient = inAmbientMode;
                if (mLowBitAmbient) {
                    mHourHandPaint.setAntiAlias(!inAmbientMode);
                    mMinuteHandPaint.setAntiAlias(!inAmbientMode);

                }
                invalidate();
            }

            // Whether the timer should be running depends on whether we're visible (as well as
            // whether we're in ambient mode), so we may need to start or stop the timer.
            updateTimer();
        }

        @Override
        public void onDraw(Canvas canvas, Rect bounds) {
            mTime.setToNow();

            int width = bounds.width();
            int height = bounds.height();

            // Draw the background.
//            canvas.drawRect(0, 0, canvas.getWidth(), canvas.getHeight(), mBackgroundPaint);
            setActiveBackground(); //sätter rätt bakgrund beroende på gadgetInFocus
            canvas.drawBitmap(mBackgroundActiveBitmap, 0, 0, null);


            // Find the center. Ignore the window insets so that, on round watches with a
            // "chin", the watch face is centered on the entire screen, not just the usable
            // portion.
            float centerX = width / 2f;
            float centerY = height / 2f;

            float secRot = mTime.second / 30f * (float) Math.PI;
            int minutes = mTime.minute;
            float minRot = minutes / 30f * (float) Math.PI;
            float hrRot = ((mTime.hour + (minutes / 60f)) / 6f) * (float) Math.PI;

            float secLength = centerX - 25;
            float minLength = centerX - 50;
            float hrLength = centerX - 90;


            //canvas.drawText("" + (int)(Math.toDegrees(azimuth)), centerX, centerY + 30, ctrlButtonPaint);
            //ritar upp control knapp
           //canvas.drawRoundRect(new RectF(centerX-64,centerY+45,centerX+64,centerY+105), 6, 6, ctrlButtonPaint);
           // canvas.drawText("Control", centerX, centerY + 65, ctrlButtonPaint);

            //ritar upp ikoner och sätter ev. gadgetInFocus=true
            boolean tempInFocus= false; //ändras till true om nåt objekt blir i fokus nedan
            for (Gadget g : gadgetList) {
                float[] coords = getDrawingCoords((float) g.angle, height);
                if (isGadgetInFocus(g)) {
                    tempInFocus = true;
                    canvas.drawBitmap(iconInFocus[g.type], coords[0], coords[1], mIconPaint); //
                } else {
                    canvas.drawBitmap(iconNoFocus[g.type], coords[0], coords[1], mIconPaint);
                }
            }
            if (tempInFocus==false) gadgetInFocus=-1; //gadgetId -1 är nullobjekt


            //ta bort
            if (tempClicked==true) {
                canvas.drawText("Button clicked", centerX, centerY + 30, ctrlButtonPaint);
            }
            else if (tempClicked==false) {

                canvas.drawText("Button not clicked", centerX, centerY + 30, ctrlButtonPaint);

            }


            if (!mAmbient) {

                float secX = (float) Math.sin(secRot) * secLength;
                float secY = (float) -Math.cos(secRot) * secLength;
                canvas.drawLine(centerX, centerY, centerX + secX, centerY + secY, mSecondHandPaint);
            }

            float minX = (float) Math.sin(minRot) * minLength;
            float minY = (float) -Math.cos(minRot) * minLength;
            canvas.drawLine(centerX, centerY, centerX + minX, centerY + minY, mMinuteHandPaint);

            float hrX = (float) Math.sin(hrRot) * hrLength;
            float hrY = (float) -Math.cos(hrRot) * hrLength;
            canvas.drawLine(centerX, centerY, centerX + hrX, centerY + hrY, mHourHandPaint);

        }


        @Override
        public void onSurfaceChanged(
                SurfaceHolder holder, int format, int width, int height) {
            if (mBackgroundScaledBitmap == null
                    || mBackgroundScaledBitmap.getWidth() != width
                    || mBackgroundScaledBitmap.getHeight() != height) {
                mBackgroundScaledBitmap = Bitmap.createScaledBitmap(mBackgroundActiveBitmap,
                        width, height, true /* filter */);
            }
            super.onSurfaceChanged(holder, format, width, height);
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);

            if (visible) {
                registerReceiver();

                // Update time zone in case it changed while we weren't visible.
                mTime.clear(TimeZone.getDefault().getID());
                mTime.setToNow();
            } else {
                unregisterReceiver();
            }

            // Whether the timer should be running depends on whether we're visible (as well as
            // whether we're in ambient mode), so we may need to start or stop the timer.
            updateTimer();
        }

        private void registerReceiver() {
            if (mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = true;
            IntentFilter filter = new IntentFilter(Intent.ACTION_TIMEZONE_CHANGED);
            IotWatchFace.this.registerReceiver(mTimeZoneReceiver, filter);

            //Registrerar sensor listener
            mSensorManager.registerListener(this, mSensor, SensorManager.SENSOR_DELAY_GAME);

        }

        private void unregisterReceiver() {
            if (!mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = false;
            IotWatchFace.this.unregisterReceiver(mTimeZoneReceiver);

            //Avregistrerar sensor lyssnaren
            mSensorManager.unregisterListener(this);
        }

        /**
         * Starts the {@link #mUpdateTimeHandler} timer if it should be running and isn't currently
         * or stops it if it shouldn't be running but currently is.
         */
        private void updateTimer() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            if (shouldTimerBeRunning()) {
                mUpdateTimeHandler.sendEmptyMessage(MSG_UPDATE_TIME);
            }
        }

        /**
         * Returns whether the {@link #mUpdateTimeHandler} timer should be running. The timer should
         * only run when we're visible and in interactive mode.
         */
        private boolean shouldTimerBeRunning() {
            return isVisible() && !isInAmbientMode();
        }

        /**
         * Handle updating the time periodically in interactive mode.
         */
        private void handleUpdateTimeMessage() {
            invalidate();
            if (shouldTimerBeRunning()) {
                long timeMs = System.currentTimeMillis();
                long delayMs = INTERACTIVE_UPDATE_RATE_MS
                        - (timeMs % INTERACTIVE_UPDATE_RATE_MS);
                mUpdateTimeHandler.sendEmptyMessageDelayed(MSG_UPDATE_TIME, delayMs);
            }
        }
        /*
            Metoder för sensordata
         */

        @Override
        public void onSensorChanged(SensorEvent sensorEvent) {
            if( sensorEvent.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR ){
//                 calculate th rotation matrix
                SensorManager.getRotationMatrixFromVector(rMat, sensorEvent.values);
//                 get the azimuth value (orientation[0]) in degree

                azimuth = (( SensorManager.getOrientation( rMat, orientation )[0] ) + 2*PI ) % (2*PI);
            }
        }

        @Override
        public void onAccuracyChanged(
                Sensor sensor, int i) {

        }

        /*@in en vinkel för var en gadget
          @ut x,y i en int-vektor
        */
        private float[] getDrawingCoords(float angle, int height) {
            angle=(azimuth-angle);
           // Log.d("differens", "diff "+ angle);
            float minlimit = 40;
            float maxlimit = height-40;
            float centre = height/2;
            float x = (centre * (float) Math.cos(angle+PI/2))*0.87f; //0.87f=marginal från kanten, -18 för centrera bitmap i koordinat
            float y = (centre * (float) Math.sin(angle+PI/2))*0.87f;//+PI/2 eftersom vi har 0 grader i norr
            x=x+centre; //vi vill ha positiva koordinater
            y=y+centre;
            if (x>maxlimit) x=maxlimit;
            if (y>maxlimit) y=maxlimit;
            if (y<minlimit) y=minlimit;
            if (x<minlimit) x=minlimit;
            return new float[] {x-16,height-y-16};
        }

        private void setActiveBackground() {
            if (gadgetInFocus==-1) {
                mBackgroundActiveBitmap=mBackgroundNoFocusBitmap;
            }
            else {
                mBackgroundActiveBitmap=mBackgroundInFocusBitmap;
            }
        }

        private boolean isGadgetInFocus(Gadget g) {
                   float diffAngle=(azimuth-(float)g.angle+2*PI)%(2*PI);//kolla om gadget är i fokus
            if (diffAngle<focusTolerance||diffAngle>(2*PI-focusTolerance)) {  //om i fokus: spara förra fokusobjektet
                //Log.d("gadgetinfocus", "azimuth= " + azimuth + " angle=" + g.angle+" diff="+(azimuth-g.angle+2*PI)%(2*PI));
                int tempId=g.id;
                lastGadgetInFocus=gadgetInFocus;
                if (lastGadgetInFocus!=tempId) {           //vibrera om fokusobjektet!=det förra
                    v.vibrate(30);
                }
                gadgetInFocus=tempId;
                return true;
            }
            return false;
        }
        @Override
        public void onTapCommand(
                @TapType int tapType, int x, int y, long eventTime) {

            if (x<=224 && x >= 96) {
                if (y>=205 && y<=265){
                    tempClicked=true;
                    Log.d("onTapCommand", "tappedButton= " + tempClicked );

                }
                else tempClicked=false;

            }
           else tempClicked=false;

            Log.d("onTapCommand", "tappedButton= " + tempClicked );
        }
    }



    private static class EngineHandler extends Handler {
        private final WeakReference<IotWatchFace.Engine> mWeakReference;

        public EngineHandler(IotWatchFace.Engine reference) {
            mWeakReference = new WeakReference<>(reference);
        }

        @Override
        public void handleMessage(Message msg) {
            IotWatchFace.Engine engine = mWeakReference.get();
            if (engine != null) {
                switch (msg.what) {
                    case MSG_UPDATE_TIME:
                        engine.handleUpdateTimeMessage();
                        break;
                }
            }
        }



    }
}
