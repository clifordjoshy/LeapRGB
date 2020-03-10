package com.example.leaprgb.animationClasses;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.example.leaprgb.LED;
import com.example.leaprgb.R;

public class AnimationActivity extends AppCompatActivity {

    boolean isAnimationRunning = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_animation);
    }

    public void onAnimationClick(View v) {
        if (LED.isConnected(true)) {
            if (isAnimationRunning) {
                LED.sendMessage("stop");
                isAnimationRunning = false;
                return;
            }
            isAnimationRunning = true;
            Toast.makeText(this, "Click Again To Stop", Toast.LENGTH_SHORT).show();
            switch (v.getId()) {
                case R.id.animation1button:
                    LED.sendMessage("animation1");
                    break;
                case R.id.animation2button:
                    LED.sendMessage("animation2");
                    break;
            }
        }
        else
            Toast.makeText(this, "ESP8266 Not Found on Network",Toast.LENGTH_SHORT).show();

    }
}
