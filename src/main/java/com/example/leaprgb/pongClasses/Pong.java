package com.example.leaprgb.pongClasses;

import android.content.Context;
import android.os.SystemClock;

import androidx.annotation.Nullable;
import androidx.loader.content.AsyncTaskLoader;

import com.example.leaprgb.Coordinate;
import com.example.leaprgb.LED;

import java.util.Random;

public class Pong extends AsyncTaskLoader<Integer> {

    private static final int DELAY = 80, HANDICAP = 3;
    private static final String colorPaddle = "e6005c", colorBall = "ffff00";  //WITHOUT HASH

    private Ball ball;
    private int myPaddleX = 6, myPaddleXUpdate = myPaddleX, compPaddleX = 6, compPaddleDestination = 0;
    private int score = 0;
    private boolean isRunningAlready = false, paddleUpdate =false;  //once initLoader(), unless restartloader(new constructor call) gets called we'll be on the same loader, only onStartLoading gets called(like in recents menu);


    Pong(Context context){
        super(context);
        ball = new Ball(new Coordinate(Coordinate.XMAX/2 , Coordinate.YMAX/2 ), "LEFT", "DOWN", 0);
    }

    @Override
    protected void onStartLoading() {
        if(isRunningAlready)
            deliverResult(score);       //quit straight to onLoadFinished();
        else
            forceLoad();
    }

    @Nullable
    @Override
    public Integer loadInBackground() {
        int turnCounter = 0;
        isRunningAlready = true;
        ball.seton(colorBall);
        Coordinate temp = new Coordinate(myPaddleX, Coordinate.YMAX);
        for(int i = 0; i < 5; ++i){
            temp.setlight(colorPaddle);
            temp.y = 1;
            temp.setlight(colorPaddle);
            temp.y = Coordinate.YMAX;
            temp.x += 1;
        }

        //MAIN GAME LOOP
        while(true){
            LED.show();
            SystemClock.sleep(DELAY);
            turnCounter++;
            ball.setoff();
            //Ball movement
            ball.move();

            if(ball.isAtPaddle()){
                if ("DOWN".equals(ball.directionY) && ball.hasHitPaddle(myPaddleX)) {
                    ball.reflectY();
                    ball.setAngle(myPaddleX);
                    //Calculating ball destination
                    Ball calcBall = ball.clone();
                    calcBall.move();    //to move from myPaddle
                    while(!calcBall.isAtPaddle())
                        calcBall.move();
                    compPaddleDestination = calcBall.coordinate.x - new Random().nextInt(5);
                }

                else if("UP".equals(ball.directionY) && ball.hasHitPaddle(compPaddleX)) {
                    ball.reflectY();
                    ball.setAngle(compPaddleX);
                }
            }

            if(ball.isGameOver())
                break;

            if(ball.hasScored()){
                score++;
                int newangle = 0;
                switch (turnCounter%3){
                    case 0:
                        newangle = 0;
                        break;
                    case 1:
                        newangle = 45;
                        break;
                    case 2:
                        newangle = 63;
                        break;
                }
                ball.reset(Coordinate.XMAX/2, Coordinate.YMAX/2, "LEFT", "DOWN", newangle);
            }

            ball.seton(colorBall);

            //Paddle Movement
            if(compPaddleDestination != 0) {
                if (compPaddleX != compPaddleDestination) {
                    if (turnCounter % HANDICAP != 0) {   //(HANDICAP-1/HANDICAP) turns
                        temp.y = 1;
                        if (compPaddleDestination > compPaddleX) {
                            if(compPaddleX <= Coordinate.XMAX-4) {
                                temp.x = compPaddleX;
                                temp.setoff();
                                compPaddleX++;
                                temp.x = compPaddleX + 4;
                                temp.setlight(colorPaddle);
                            }
                        } else {
                            if(compPaddleX >= 1) {
                                temp.x = compPaddleX + 4;
                                temp.setoff();
                                compPaddleX--;
                                temp.x = compPaddleX;
                                temp.setlight(colorPaddle);
                            }
                        }
                    }
                } else
                    compPaddleDestination = 0;
            }

            if(paddleUpdate) {
                temp.y = Coordinate.YMAX;
                while (myPaddleXUpdate != myPaddleX) {  //in case multiple movements have been made
                    if (myPaddleXUpdate > myPaddleX) {
                        temp.x = myPaddleX;
                        temp.setoff();
                        myPaddleX++;
                        temp.x = myPaddleX + 4;
                        temp.setlight(colorPaddle);
                        } else {
                        temp.x = myPaddleX + 4;
                        temp.setoff();
                        myPaddleX--;
                        temp.x = myPaddleX;
                        temp.setlight(colorPaddle);
                        }
                }
                paddleUpdate = false;
            }

            if(isLoadInBackgroundCanceled())
                break;  //turns off the led immediately .
        }

        SystemClock.sleep(50);
        LED.clear();
        LED.show();
        return score;
    }

    void moveMyPaddle(String direction){
        paddleUpdate=true;
        switch(direction){
            case "LEFT":
                if(myPaddleXUpdate > 1)
                    myPaddleXUpdate -= 1;
                break;
            case "RIGHT":
                if(myPaddleXUpdate < Coordinate.YMAX - 4)
                    myPaddleXUpdate += 1;
        }
    }

    void moveMyPaddleTo(int newpos){
        paddleUpdate =true;
        myPaddleXUpdate = newpos;
    }

    @Override
    public void deliverResult(@Nullable Integer data) {
        //goes straight to onStopLoading() and onLoadFinished() in the activity. So we call this if we skip to menu.
        super.deliverResult(data);
    }

    @Override
    protected void onStopLoading() {
        cancelLoad();  //only gets queued up until loadinbackground() is done
    }

    @Override
    protected void onReset() {  //when everything is done with the loader and everything delivered,loader will be reset
        super.onReset();
        // Ensure the loader is stopped just in case it isnt
        onStopLoading();
    }
}
