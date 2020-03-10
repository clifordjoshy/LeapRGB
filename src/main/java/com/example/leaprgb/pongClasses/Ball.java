package com.example.leaprgb.pongClasses;

import android.util.Log;

import com.example.leaprgb.Coordinate;

public class Ball implements Cloneable{
    Coordinate coordinate;
    private String directionX;        //LEFT OR RIGHT
    String directionY;        //UP OR DOWN
    private int angle;               //0,45,63 from vertical


    Ball(Coordinate c, String dirX, String dirY, int a) {
        coordinate = c;
        directionX = dirX;
        directionY = dirY;
        angle = a;
    }

    void reset(int coordinateX, int coordinateY, String dirX, String dirY, int a){
        coordinate.x = coordinateX;
        coordinate.y = coordinateY;
        directionX = dirX;
        directionY = dirY;
        angle = a;
    }

    private boolean isAtWall() {
        return (coordinate.x <=1 || coordinate.x >= Coordinate.XMAX);
    }

    boolean isGameOver(){
        return (coordinate.y == Coordinate.YMAX);
    }

    boolean hasScored(){
        return (coordinate.y == 1);
    }

    boolean isAtPaddle(){
        return (coordinate.y == 2 || coordinate.y == Coordinate.YMAX-1);
    }

    boolean hasHitPaddle(int paddleStartX) {
        for (int i = 0; i < 5; ++i)
            if (coordinate.x >= paddleStartX && coordinate.x <= paddleStartX+4)
        return true;
        return false;
    }

    void move() {

        //HORIZONTAL MOVEMENT
        if ("LEFT".equals(directionX)) {
            if (angle == 45)
                coordinate.x -= 1;
            else if (angle == 63)
                coordinate.x -= 2;
        } else if ("RIGHT".equals(directionX)) {
            if (angle == 45)
                coordinate.x += 1;
            else if (angle == 63)
                coordinate.x += 2;
        }

        //VERTICAL MOVEMENT
        if ("DOWN".equals(directionY))
            coordinate.y += 1;
        else if ("UP".equals(directionY))
            coordinate.y -= 1;

        if(isAtWall()) {
            reflectX();
        }

    }

    private void reflectX() {
        if ("LEFT".equals(directionX)) {
            directionX = "RIGHT";
            coordinate.x = 1;
        } else if ("RIGHT".equals(directionX)) {
            directionX = "LEFT";
            coordinate.x = Coordinate.XMAX;
        }
    }

    void reflectY() {
        if ("UP".equals(directionY)) {
            directionY = "DOWN";
        } else if ("DOWN".equals(directionY)) {
            directionY = "UP";
        }
    }

    void setAngle(int paddleStart) {
        if(coordinate.x == paddleStart || coordinate.x == paddleStart + 4)
            angle = 63;
        else if(coordinate.x == paddleStart + 1 || coordinate.x == paddleStart +3 )
            angle = 45;
        else if (coordinate.x == paddleStart + 2)
            angle = 0;
    }

    void seton(String Hex){
        coordinate.setlight(Hex);
    }

    void setoff(){
        coordinate.setoff();
    }

    @Override
    public Ball clone() {
        Ball clone = null;
        try{
            clone = (Ball)super.clone();  //shallow
            clone.coordinate = new Coordinate(coordinate.x,coordinate.y);
        }catch(CloneNotSupportedException e){
            Log.i("mylog", "Ball Cloning Error");
        }
    return clone;
    }
}

