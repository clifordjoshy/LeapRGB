package com.example.leaprgb.snakeClasses;

import android.content.Context;
import android.os.SystemClock;

import androidx.annotation.Nullable;
import androidx.loader.content.AsyncTaskLoader;

import com.example.leaprgb.Coordinate;
import com.example.leaprgb.LED;

import java.lang.Integer;

import java.util.LinkedList;

public class Snake extends  AsyncTaskLoader<Integer> {
    private static final int DELAY=400;
    private static final String colorBody="1aff1a", colorHead="006600", colorFood="009999", colorMix="003366";  //WITHOUT HASH

    private LinkedList<Body> body;
    private int score = 0;
    private boolean isRunningAlready = false;  //once initLoader(), unless restartloader(new constructor call) gets called we'll be on the same loader, only onStartLoading gets called(like in recents menu);
    private String updateDirection = "", snakeDirection = "LEFT";


    Snake(Context context){
        super(context);
        body = new LinkedList<>();
        Coordinate startCoordinate = new Coordinate(Coordinate.XMAX/2 + 3,Coordinate.YMAX/2);

        //First member of LinkedList is the tail. Last member is the head.
        Body initializer = new Body(startCoordinate);
        body.add(initializer);
        initializer = initializer.move("LEFT");
        body.add(initializer);
        initializer = initializer.move("LEFT");
        body.add(initializer);
        initializer = initializer.move("LEFT");
        body.add(initializer);
        initializer = initializer.move("LEFT");
        body.add(initializer);
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
        isRunningAlready = true;
        for (Body b : body) {
            SystemClock.sleep(300);
            b.setlight(colorBody);
            SystemClock.sleep(10);
            LED.show();
        }
        body.getLast().setlight(colorHead);
        SystemClock.sleep(20);  //request processing time
        LED.show();
        SystemClock.sleep(DELAY);

        LinkedList<Coordinate> foodList = new LinkedList<>();
        Coordinate foodPosition = getFood();
        boolean isDead = false, wasFoodEaten = false;

        //MAIN GAME LOOP
        while(true){

            if(!"".equals(updateDirection)) {
                snakeDirection = updateDirection;
                updateDirection = "";
            }

            //Move one unit in specified snakeDirection. We have colormix for food eaten.
            if(!wasFoodEaten)
                body.getLast().setlight(colorBody);
            else
                wasFoodEaten = false;

            body.add(body.getLast().move(snakeDirection));

            //Check if dead
            if(body.getLast().isBoundaryDead())
                isDead = true;

            for(int i = 0; i < body.size() - 1 ; ++i) {
                if (body.getLast().coordinate.equals(body.get(i).coordinate)) {
                    isDead = true;
                    break;
                }
            }

            if(isDead)
                break;

            //Check if food has been reached
            if(body.getLast().coordinate.equals(foodPosition)) {
                score++;
                foodPosition.setlight(colorMix);
                foodList.add(foodPosition);
                foodPosition = getFood();
                wasFoodEaten = true;
            }
            else
                body.getLast().setlight(colorHead);

            //Food travels to the end and becomes the body
            if (foodList.size()>0 && body.getFirst().coordinate.equals(foodList.getFirst())){
                body.getFirst().setlight(colorBody);
                foodList.remove();
            }
            else{
                body.getFirst().setoff();
                body.remove();
                }

            if(isLoadInBackgroundCanceled())
                break;  //turns off the led immediately .

            SystemClock.sleep(20);  //any other reqs get handled
            LED.show();
            SystemClock.sleep(DELAY);
        }

        SystemClock.sleep(50);  //req process time
        //LED cleared even if we move to recents
        LED.clear();
        return score;   //goes to deliverResult
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

    private Coordinate getFood(){
        Coordinate foodPosition = Coordinate.getRandomCoordinate();
        //Check if it is in a used coordinate
        while(true){
            boolean isUnique=true;
            for (Body b : body)
                if ((foodPosition.equals(b.coordinate)))
                    isUnique=false;
            if(isUnique)
                break;
            foodPosition=Coordinate.getRandomCoordinate();
        }
        //Print Food
        foodPosition.setlight(colorFood);
        return foodPosition;
    }

    void moveSnakeHandle(String dir){
        if("UP".equals(snakeDirection) || "DOWN".equals(snakeDirection))
            switch(dir) {
                case "LEFT":
                    updateDirection = "LEFT";
                    break;
                case "RIGHT":
                    updateDirection = "RIGHT";
                    break;
            }

        else if("LEFT".equals(snakeDirection) || "RIGHT".equals(snakeDirection))
            switch(dir){
                case "UP":
                    updateDirection = "UP";
                    break;
                case "DOWN":
                    updateDirection = "DOWN";
                    break;
            }
    }
}


