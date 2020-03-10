package com.example.leaprgb;

import androidx.annotation.Nullable;

import java.util.Random;

public class Coordinate {

    public static final int YMAX = 15, XMAX = 16;
    public int x, y;
    private static Random r = new Random();

    public Coordinate(int x, int y){
        this.x=x;
        this.y=y;
    }

    public static Coordinate getRandomCoordinate(){
        return new Coordinate(r.nextInt(XMAX)+1, r.nextInt(YMAX)+1);
    }

    public void setlight(String Hex){
        LED.setlight(y, x, Hex);
    }

    public void setoff(){
        LED.setoff(y, x);
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if(!(obj instanceof Coordinate))
            return false;
        Coordinate c = (Coordinate)obj;
        return (this.x == c.x && this.y == c.y);
    }
}

