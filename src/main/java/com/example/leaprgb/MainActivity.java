package com.example.leaprgb;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import android.animation.Animator;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.ImageView;

import com.example.leaprgb.animationClasses.AnimationActivity;
import com.example.leaprgb.flooditClasses.FloodItActivity;
import com.example.leaprgb.manualconfigClasses.ManualConfigActivity;
import com.example.leaprgb.pongClasses.PongActivity;
import com.example.leaprgb.snakeClasses.SnakeActivity;
import com.example.leaprgb.tetrisClasses.TetrisActivity;

public class MainActivity extends AppCompatActivity {

    private String currentOption = "";
    private int currentPage = 0;

    //Views that remain between pages
    private ImageView hellotext, welcometext;
    private ImageView gopressed, gounpressed;
    private ImageView clicktext, ohwelltext;
    private ImageView indicator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        LED.initializeIP(this);
        currentPage = 1;
        /*
        Fade exittransition = new Fade();
        exittransition.setDuration(300);
        getWindow().setExitTransition(exittransition);

         */

        hellotext = findViewById(R.id.hellotext);
        welcometext = findViewById(R.id.welcometext);
        gounpressed = findViewById(R.id.gounpressed);
        clicktext = findViewById(R.id.clicktext);
        gopressed = findViewById(R.id.gopressed);
        indicator = findViewById(R.id.indicator);
        ohwelltext = findViewById(R.id.ohwelltext);


        clicktext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentPage = 2;
                ohwelltext.animate().alpha(1f).setDuration(400).setStartDelay(500);
                Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        doIntroTransition();
                    }
                }, 1800);
            }
        });
    }

    private void doIntroTransition() {
        int animationOffset = 0;     //delay 500

        //set exit animation
        ohwelltext.animate().alpha(0f).setDuration(500).setStartDelay(animationOffset);
        clicktext.animate().alpha(0f).setDuration(500).setStartDelay(animationOffset).setListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                clicktext.setVisibility(View.GONE);
                ohwelltext.setVisibility(View.GONE);
            }

            @Override
            public void onAnimationCancel(Animator animation) {
            }

            @Override
            public void onAnimationRepeat(Animator animation) {
            }
        });

        animationOffset += 1000; //500 animation + 500 delay

        hellotext.setVisibility(View.VISIBLE);
        welcometext.setVisibility(View.VISIBLE);
        gounpressed.setVisibility(View.VISIBLE);

        hellotext.setAlpha(0f);
        welcometext.setAlpha(0f);
        gounpressed.setAlpha(0f);

        hellotext.animate().alpha(1f).setDuration(500).setStartDelay(animationOffset);
        animationOffset += 1000;    //500 animation + 500 delay
        welcometext.animate().alpha(1f).setDuration(500).setStartDelay(animationOffset);
        animationOffset += 1000;    //500 + 500
        gounpressed.animate().alpha(1f).setDuration(500).setStartDelay(animationOffset);

        //onclicklistener declared below
    }

    private void doMenuTransition() {

        hellotext.animate().alpha(0f).setDuration(300).setStartDelay(0);
        gounpressed.animate().alpha(0f).setDuration(300).setStartDelay(0);
        welcometext.animate().alpha(0f).setDuration(300).setStartDelay(0).setListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                int animationOffset = 0;
                hellotext.setVisibility(View.GONE);
                welcometext.setVisibility(View.GONE);
                animationOffset += 700; //200 animation + 500 delay

                ImageView poisontext = findViewById(R.id.pickyourpoisontext);
                ImageView[] menuitems = {findViewById(R.id.snakebutton), findViewById(R.id.pongbutton),
                        findViewById(R.id.flooditbutton), findViewById(R.id.tetrisbutton),
                        findViewById(R.id.manualsetupbutton), findViewById(R.id.animationsbutton)};

                poisontext.setVisibility(View.VISIBLE);
                poisontext.setAlpha(0f);

                poisontext.animate().alpha(1f).setDuration(400).setStartDelay(animationOffset);
                animationOffset += 800;     //400 + 400

                for(int i = 0; i < 6; ++i){
                    menuitems[i].setVisibility(View.VISIBLE);
                    menuitems[i].setAlpha(0f);
                    menuitems[i].animate().alpha(1f).setDuration(400).setStartDelay(animationOffset);
                    animationOffset += 500;     //400 + 100
                }

                gounpressed.animate().alpha(1f).setDuration(400).setStartDelay(animationOffset);
            }

            @Override
            public void onAnimationCancel(Animator animation) {
            }

            @Override
            public void onAnimationRepeat(Animator animation) {
            }
        });
    }

    public void onOptionClicked(View view) {

        final int SNAKE_MARGINTOP = 30, PONG_MARGINTOP = 57, FLOODIT_MARGINTOP = 84, TETRIS_MARGINTOP = 111,
                MANUAL_MARGINTOP = 138, ANIMATIONS_MARGINTOP = 165;     //ALL IN DP
        int topConstraint= 0;

        ConstraintLayout.LayoutParams indicatorParams = (ConstraintLayout.LayoutParams) indicator.getLayoutParams();


        if("".equals(currentOption)) {
            indicator.setVisibility(View.VISIBLE);
        }


        switch(view.getId()){
            case R.id.snakebutton:
                currentOption = "snake";
                topConstraint = SNAKE_MARGINTOP;
                break;
            case R.id.pongbutton:
                currentOption = "pong";
                topConstraint = PONG_MARGINTOP;
                break;
            case R.id.flooditbutton:
                currentOption = "floodit";
                topConstraint = FLOODIT_MARGINTOP;
                break;
            case R.id.tetrisbutton:
                currentOption = "tetris";
                topConstraint = TETRIS_MARGINTOP;
                break;
            case R.id.manualsetupbutton:
                currentOption = "manualsetup";
                topConstraint = MANUAL_MARGINTOP;
                break;
            case R.id.animationsbutton:
                currentOption = "animations";
                topConstraint = ANIMATIONS_MARGINTOP;
                break;
        }

        //Convert dp to pixels (ConstraintSet not working.....)
        indicatorParams.topMargin = (int) (topConstraint *
                ((float)getApplicationContext().getResources().getDisplayMetrics().densityDpi/ DisplayMetrics.DENSITY_DEFAULT));
        indicator.setLayoutParams(indicatorParams);
    }

    public void onGoButtonPressed(View view) {
        gounpressed.setVisibility(View.INVISIBLE);
        gopressed.setVisibility(View.VISIBLE);
        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                gounpressed.setVisibility(View.VISIBLE);
                gopressed.setVisibility(View.INVISIBLE);
            }
        }, 200);

        if(currentPage == 2) {
            currentPage = 3;
            doMenuTransition();
        }

        else if(currentPage == 3){
            Intent intent = null;
            switch(currentOption) {
                case "snake":
                    intent = new Intent(this, SnakeActivity.class);
                    break;
                case "pong":
                    intent = new Intent(this, PongActivity.class);
                    break;
                case "floodit":
                    intent = new Intent(this, FloodItActivity.class);
                    break;
                case "tetris":
                    intent = new Intent(this, TetrisActivity.class);
                    break;
                case "manualsetup":
                    intent = new Intent(this, ManualConfigActivity.class);
                    break;
                case "animations":
                    intent = new Intent(this, AnimationActivity.class);
                    break;
                default:
                    return;
            }
            startActivity(intent);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(LED.isConnected(false)){
            LED.clear();
            LED.show();
            LED.endconnection();
        }
    }

}
