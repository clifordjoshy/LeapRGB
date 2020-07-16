package com.example.leaprgb.animationClasses;

import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.leaprgb.LED;
import com.example.leaprgb.R;

import static java.lang.Character.isDigit;

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
            Toast.makeText(this, "Click Again To Stop", Toast.LENGTH_SHORT).show();
            switch (v.getId()) {
                case R.id.animation1button:
                    LED.sendMessage("animation1");
                    isAnimationRunning = true;
                    break;
                case R.id.animation2button:
                    LED.sendMessage("animation2");
                    isAnimationRunning = true;
                    break;
                case R.id.renderbutton:
                    AlertDialog.Builder d = new AlertDialog.Builder(this);
                    final EditText e = new EditText(this);
                    LinearLayout l = new LinearLayout(this);
                    l.setOrientation(LinearLayout.VERTICAL);
                    final EditText t = new EditText(this);
                    t.setHint("ffffff");
                    t.addTextChangedListener(new TextWatcher() {
                        @Override
                        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                        }

                        @Override
                        public void onTextChanged(CharSequence s, int start, int before, int count) {

                        }

                        @Override
                        public void afterTextChanged(Editable s) {
                            String mycolor = t.getText().toString();
                            if(isValidColor(mycolor)) {
                                mycolor = "#".concat(mycolor);
                                int myColor = Color.parseColor(mycolor);
                                t.setBackgroundColor(myColor);
                            }
                        }
                    });
                    e.setHint("Hello World");
                    l.addView(e);
                    l.addView(t);
                    d.setView(l);
                    d.setTitle("Enter Your Text and Color");
                    d.setPositiveButton("RENDER", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            String entered = e.getText().toString();
                            if ("".equals(entered))
                                entered = "Hello World";
                            String mycolor = t.getText().toString();
                            if(!isValidColor(mycolor))
                                mycolor = "ffffff";
                            LED.sendMessage(("render/"+entered+"/"+mycolor));
                            isAnimationRunning = true;
                        }
                    });
                    d.setNegativeButton("CANCEL", null);
                    d.show();
            }
        }
        else
            Toast.makeText(this, "ESP8266 Not Found on Network",Toast.LENGTH_SHORT).show();

    }

    boolean isValidColor(String color){
        color = color.toUpperCase();
        if(color.length()!=6)
            return false;
        for(int i = 0; i < 6; ++i)
            if (!(isDigit(color.charAt(i)) || (color.charAt(i)>='A' && color.charAt(i)<='F')))
                return false;
        return true;
    }
}
