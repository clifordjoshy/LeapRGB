package com.example.leaprgb.snakeClasses;

import android.util.Log;

import com.example.leaprgb.Coordinate;

class Body {
    Coordinate coordinate;

    Body(Coordinate coordinate){
        this.coordinate = coordinate;
    }

    Body move(String direction){

        int newx = coordinate.x, newy = coordinate.y;

        switch(direction){
            case "LEFT":
                newx = coordinate.x - 1;
                break;
            case "RIGHT":
                newx = coordinate.x + 1;
                break;
            case "UP":
                newy = coordinate.y - 1;
                break;
            case "DOWN":
                newy = coordinate.y + 1;
                break;
            default:
                Log.i("mylog","Invalid Coordinate Shift Direction");
        }

        return new Body(new Coordinate(newx, newy));
    }

    void setlight(String Hex){
        coordinate.setlight(Hex);
    }

    void setoff(){
        coordinate.setoff();
    }

    boolean isBoundaryDead(){
        return (coordinate.x == 0 || coordinate.x == Coordinate.XMAX + 1 || coordinate.y == 0 || coordinate.y == Coordinate.YMAX + 1 );
    }
}

