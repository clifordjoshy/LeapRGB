package com.example.leaprgb.tetrisClasses;

import android.content.Context;
import android.os.SystemClock;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.loader.content.AsyncTaskLoader;

import com.example.leaprgb.Coordinate;
import com.example.leaprgb.LED;

import java.util.LinkedList;
import java.util.Random;

public class Tetris extends AsyncTaskLoader<Integer> {
    private static final int DELAY=400, LOWERBOUND_WALL = 4, UPPERBOUND_WALL = 13, FIELDWIDTH = 8;
    private static final String colorWall = "ffffff", colorPositioned = "858483";
    private static final String[] colorPieces = {"922b8d", "ff0000", "c9740a", "f2ff00", "00ff00", "559ad4", "0000ff"};

    private LinkedList<boolean[]> field;
    private int score = 0;
    private boolean isRunningAlready = false, shouldDrop = false;
    private Random pieceGenerator;
    private Piece currentPiece;
    private String moveDirection = "", rotateSense = "";

    Tetris(Context context){
        super(context);

        pieceGenerator = new Random();
        field = new LinkedList<>();
        for(int i = 0;i < 15;++i)
            field.add(new boolean[FIELDWIDTH]); //false default
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
        int i;

        //Print the walls
        for(i = 1; i <= 15; ++i){
            LED.setlight(i, LOWERBOUND_WALL, colorWall);
            LED.setlight(i, UPPERBOUND_WALL, colorWall);
        }
          //process reqs
        LED.show();

        currentPiece = getNewPiece();

        boolean isGameOver = false;

        //MAIN GAME LOOP
        while(true) {
            for (Coordinate c : currentPiece.getUnits()) {
                if (c.y > 0) {
                    c.setoff();
                    SystemClock.sleep(10); //in case all reqs dont go through
                }
            }

            if(!"".equals(moveDirection)){
                currentPiece.move(moveDirection);
                moveDirection = "";
            }

            if(!"".equals(rotateSense)){
                currentPiece.rotate(rotateSense);
                for(Coordinate c : currentPiece.getUnits()) {
                    while ((c.x <= LOWERBOUND_WALL) || (c.x >= UPPERBOUND_WALL) || (c.y < 1) || (c.y > 15) || (field.get(c.y-1)[c.x-1-LOWERBOUND_WALL])){
                        if(c.x <= LOWERBOUND_WALL)
                            currentPiece.move("RIGHT");
                        else if(c.x >= UPPERBOUND_WALL)
                            currentPiece.move("LEFT");
                        else if(c.y < 1)
                            currentPiece.move("DOWN");
                        else if(c.y >15 || field.get(c.y-1)[c.x-1-LOWERBOUND_WALL])
                            currentPiece.move("UP");
                    }
                }
                rotateSense = "";
            }

            currentPiece.move("DOWN");

            if(shouldDrop) {
                while (!isPositioned(currentPiece))
                    currentPiece.move("DOWN");
                shouldDrop = false;
            }

            for(Coordinate c : currentPiece.getUnits()) {
                if (c.y > 0) {
                    c.setlight(colorPieces[currentPiece.getid()]);
                    SystemClock.sleep(20);
                }
            }

            SystemClock.sleep(DELAY);
            LED.show();

            if(isLoadInBackgroundCanceled())
                break;  //turns off the led immediately .

            if(isPositioned(currentPiece)){
                for(Coordinate c : currentPiece.getUnits()) {
                    //fix to current posn
                    if (c.y > 0) {
                        field.get(c.y - 1)[c.x - 1 - LOWERBOUND_WALL] = true;
                        c.setlight(colorPositioned);
                    }
                    else {
                        isGameOver = true;
                        break;
                    }
                }

                if(isGameOver)
                    break;

                //check for line clearance
                boolean lineCleared;
                //go through all possible rows in coordinates to check for possible finish
                for(Coordinate c : currentPiece.getUnits()) {
                    lineCleared = true;
                    for (int check=0; check < FIELDWIDTH; ++check) {
                        if (!field.get(c.y - 1)[check]) {
                            lineCleared = false;
                            break;
                        }
                    }

                    //the current line is filled
                    if(lineCleared){
                        score += FIELDWIDTH;
                        for(int r = 1; r <= c.y; ++r)
                            LED.clearrow(r, LOWERBOUND_WALL+1, UPPERBOUND_WALL-1);

                        //remove the row
                        field.remove(c.y - 1);
                        field.addFirst(new boolean[FIELDWIDTH]);

                        for(int r = c.y -1; r >0; --r)   //empty row at 0
                            for(int s = 0; s < FIELDWIDTH; ++s)
                                if(field.get(r)[s])
                                    LED.setlight(r+1, s+1+LOWERBOUND_WALL, colorPositioned);
                        LED.show();
                    }
                }

                currentPiece = getNewPiece();
            }
        }
        LED.clear();
        LED.show();
        return score;
    }

    private boolean isPositioned(Piece currentPiece) {
        for(Coordinate c : currentPiece.getUnits())
            if (c.y > 0)
                if (c.y == 15 || field.get(c.y)[c.x - 1 - LOWERBOUND_WALL])  //index is from 0
                    return true;

        return false;
    }


    private Piece getNewPiece(){
        switch(pieceGenerator.nextInt(7)){
            case 0:
                return new Piece0();
            case 1:
                return new Piece1();
            case 2:
                return new Piece2();
            case 3:
                return new Piece3();
            case 4:
                return new Piece4();
            case 5:
                return new Piece5();
            case 6:
                return new Piece6();
        }
    Log.i("mylog", "Piece generation failed");
        return null;
    }

    void movePiece(String direction){
        if(!"".equals(moveDirection))   //no changes while processing
            return;

        if("LEFT".equals(direction)) {
            boolean isMovableLeft = true;
            for (Coordinate c : currentPiece.getUnits()) {
                if (c.x == LOWERBOUND_WALL + 1 || c.y == 0 || field.get(c.y - 1)[c.x - 2 - LOWERBOUND_WALL]) {
                    isMovableLeft = false;
                    break;
                }
            }
            if (isMovableLeft)
                moveDirection = "LEFT";
        }

        if("RIGHT".equals(direction)) {
            boolean isMovableRight = true;
            for (Coordinate c : currentPiece.getUnits()) {
                if (c.x == UPPERBOUND_WALL - 1 || c.y == 0 || field.get(c.y - 1)[c.x - LOWERBOUND_WALL]) {
                    isMovableRight = false;
                    break;
                }
            }
            if (isMovableRight)
                moveDirection = "RIGHT";
        }
    }

    void dropPiece(){
        if(shouldDrop)
            return;
        shouldDrop = true;
    }

    void doRotate(String sense){
        if(!"".equals(rotateSense))   //no changes while processing
            return;
        rotateSense = sense;
    }
}
