package com.example.leaprgb.snakeClasses;

import android.os.Message;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;

public class GestureClass implements GestureDetector.OnGestureListener {

    private static final int SWIPE_THRESHOLD = 100;
    private static final int SWIPE_VELOCITY_THRESHOLD = 100;
    SnakeActivity.SwipeHandler referenceToHandler;
    private String direction;

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        try {
            float diffY = e2.getY() - e1.getY();
            float diffX = e2.getX() - e1.getX();
            if (Math.abs(diffX) > Math.abs(diffY)) {
                if (Math.abs(diffX) > SWIPE_THRESHOLD && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                    if (diffX > 0)
                        direction = "RIGHT";
                    else
                        direction = "LEFT";
                }
            }
            else {
                if (Math.abs(diffY) > SWIPE_THRESHOLD && Math.abs(velocityY) > SWIPE_VELOCITY_THRESHOLD) {
                    if (diffY > 0)
                        direction = "DOWN";
                    else
                        direction = "UP";
                }
            }
            Message toActivity = new Message();
            toActivity.obj = direction;
            referenceToHandler.sendMessage(toActivity);

        } catch (Exception e) {
            Log.i("mylog",e.getMessage());
        }

        return true;
    }

    @Override
    public boolean onDown(MotionEvent e) {
        return true;
    }

    @Override
    public void onShowPress(MotionEvent e) {

    }

    @Override
    public boolean onSingleTapUp(MotionEvent e) {
        return true;
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        return true;
    }

    @Override
    public void onLongPress(MotionEvent e) {

    }


}

