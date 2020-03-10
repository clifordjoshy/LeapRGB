package com.example.leaprgb.snakeClasses;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;

import androidx.appcompat.widget.AppCompatTextView;
import androidx.core.view.GestureDetectorCompat;


public class SwipeView extends AppCompatTextView{

    private GestureDetectorCompat detector;
    public GestureClass referenceToGestureListener;

    public SwipeView(Context context) {
        super(context);
        referenceToGestureListener = new GestureClass();
        detector = new GestureDetectorCompat(getContext() , referenceToGestureListener);
    }

    public SwipeView(Context context, AttributeSet attrs) {
        super(context, attrs);
        referenceToGestureListener = new GestureClass();
        detector = new GestureDetectorCompat(getContext() , referenceToGestureListener);
    }

    public SwipeView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        referenceToGestureListener = new GestureClass();
        detector = new GestureDetectorCompat(getContext() , referenceToGestureListener);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return detector.onTouchEvent(event);
    }
}

