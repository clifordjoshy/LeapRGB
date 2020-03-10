package com.example.leaprgb.tetrisClasses;

import com.example.leaprgb.Coordinate;

public interface Piece {
    Coordinate[] getUnits();
    void move(String direction);
    int getid();
    void rotate(String sense);
}

class PieceFunctions{
    static void move(Coordinate[] units, String direction){
        for(Coordinate u : units){
            switch(direction){
                case "DOWN":
                    u.y += 1;
                    break;
                case "RIGHT":
                    u.x += 1;
                    break;
                case "LEFT":
                    u.x -= 1;
                    break;
                case "UP":
                    u.y -= 1;
                    break;
            }
        }
    }
}

//rotationalState gives the current rotational position. Incrementing it gives us the rotationalState for one clockwise rotation.

class Piece0 implements Piece{
    /*  [0][1][2]               rotationalState -> 1
           [3]
     */
    private Coordinate[] units = {new Coordinate(7,-1), new Coordinate(8, -1), new Coordinate(9, -1), new Coordinate(8,0) };
    private byte rotationalState = 1;
    @Override
    public void move(String direction) {
        PieceFunctions.move(units, direction);
    }

    @Override
    public Coordinate[] getUnits() {
        return units;
    }

    @Override
    public void rotate(String sense) {
        if("CLOCKWISE".equals(sense)){
            if(++rotationalState == 5) rotationalState =1;

            switch(rotationalState){
                case 1: //4 -> 1
                    units[0].x -= 1; units[0].y -= 1;
                    units[2].x += 1; units[2].y += 1;
                    units[3].x -= 1; units[3].y += 1;
                    break;
                case 2: //1 -> 2
                    units[0].x += 1; units[0].y -= 1;
                    units[2].x -= 1; units[2].y += 1;
                    units[3].x -= 1; units[3].y -= 1;
                    break;
                case 3: //2 -> 3
                    units[0].x += 1; units[0].y += 1;
                    units[2].x -= 1; units[2].y -= 1;
                    units[3].x += 1; units[3].y -= 1;
                    break;
                case 4: //3 -> 4
                    units[0].x -= 1; units[0].y += 1;
                    units[2].x += 1; units[2].y -= 1;
                    units[3].x += 1; units[3].y += 1;
                    break;
            }
        }

        else if("ANTICLOCKWISE".equals(sense)){
            if(--rotationalState == 0) rotationalState =4;

            switch(rotationalState){
                case 4: //1 -> 4
                    units[0].x += 1; units[0].y += 1;
                    units[2].x -= 1; units[2].y -= 1;
                    units[3].x += 1; units[3].y -= 1;
                    break;
                case 1: //2 -> 1
                    units[0].x -= 1; units[0].y += 1;
                    units[2].x += 1; units[2].y -= 1;
                    units[3].x += 1; units[3].y += 1;
                    break;
                case 2: //3 -> 2
                    units[0].x -= 1; units[0].y -= 1;
                    units[2].x += 1; units[2].y += 1;
                    units[3].x -= 1; units[3].y += 1;
                    break;
                case 3: //4 -> 3
                    units[0].x += 1; units[0].y -= 1;
                    units[2].x -= 1; units[2].y += 1;
                    units[3].x -= 1; units[3].y -= 1;
                    break;
            }
        }
    }

    @Override
    public int getid() {
        return 0;
    }
}

class Piece1 implements Piece{
    /*  [0][1]
           [2][3]
    */
    private Coordinate[] units = {new Coordinate(7, -1), new Coordinate(8, -1), new Coordinate(8,0), new Coordinate(9,0)};
    private byte rotationalState = 1;

    @Override
    public void move(String direction) {
        PieceFunctions.move(units, direction);
    }

    @Override
    public Coordinate[] getUnits() {
        return units;
    }

    @Override
    public void rotate(String sense) {
        if("CLOCKWISE".equals(sense)){
            if(++rotationalState == 5) rotationalState =1;

            switch(rotationalState){
                case 1: //4 -> 1
                    units[0].x -= 1; units[0].y -= 1;
                    units[2].x -= 1; units[2].y += 1;
                                     units[3].y += 2;
                    break;
                case 2: //1 -> 2
                    units[0].x += 1; units[0].y -= 1;
                    units[2].x -= 1; units[2].y -= 1;
                    units[3].x -= 2;
                    break;
                case 3: //2 -> 3
                    units[0].x += 1; units[0].y += 1;
                    units[2].x += 1; units[2].y -= 1;
                                     units[3].y -= 2;
                    break;
                case 4: //3 -> 4
                    units[0].x -= 1; units[0].y += 1;
                    units[2].x += 1; units[2].y += 1;
                    units[3].x += 2;
                    break;
            }
        }

        else if("ANTICLOCKWISE".equals(sense)){
            if(--rotationalState == 0) rotationalState =4;

            switch(rotationalState){
                case 4: //1 -> 4
                    units[0].x += 1; units[0].y += 1;
                    units[2].x += 1; units[2].y -= 1;
                                     units[3].y -= 2;
                    break;
                case 1: //2 -> 1
                    units[0].x -= 1; units[0].y += 1;
                    units[2].x += 1; units[2].y += 1;
                    units[3].x += 2;
                    break;
                case 2: //3 -> 2
                    units[0].x -= 1; units[0].y -= 1;
                    units[2].x -= 1; units[2].y += 1;
                                     units[3].y += 2;
                    break;
                case 3: //4 -> 3
                    units[0].x += 1; units[0].y -= 1;
                    units[2].x -= 1; units[2].y -= 1;
                    units[3].x -= 2;
                    break;
            }
        }
    }

    @Override
    public int getid() {
        return 1;
    }
}

class Piece2 implements Piece{
    /*        [0]
        [1][2][3]
     */
    private Coordinate[] units = {new Coordinate(9,-1), new Coordinate(7,0), new Coordinate(8,0), new Coordinate(9,0)};
    private byte rotationalState = 1;

    @Override
    public void move(String direction) {
        PieceFunctions.move(units, direction);
    }

    @Override
    public Coordinate[] getUnits() {
        return units;
    }

    @Override
    public void rotate(String sense) {
        if ("CLOCKWISE".equals(sense)) {
            if (++rotationalState == 5) rotationalState = 1;

            switch (rotationalState) {
                case 1: //4 -> 1
                    units[0].x += 1; units[0].y -= 1;
                    units[1].x -= 2; units[1].y -= 2;
                    units[2].x -= 1; units[2].y -= 1;
                    break;
                case 2: //1 -> 2
                    units[0].x += 1; units[0].y += 1;
                    units[1].x += 2; units[1].y -= 2;
                    units[2].x += 1; units[2].y -= 1;
                    break;
                case 3: //2 -> 3
                    units[0].x -= 1; units[0].y += 1;
                    units[1].x += 2; units[1].y += 2;
                    units[2].x += 1; units[2].y += 1;
                    break;
                case 4: //3 -> 4
                    units[0].x -= 1; units[0].y -= 1;
                    units[1].x -= 2; units[1].y += 2;
                    units[2].x -= 1; units[2].y += 1;
                    break;
            }
        } else if ("ANTICLOCKWISE".equals(sense)) {
            if (--rotationalState == 0) rotationalState = 4;

            switch (rotationalState) {
                case 4: //1 -> 4
                    units[0].x -= 1;
                    units[0].y += 1;
                    units[1].x += 2;
                    units[1].y += 2;
                    units[2].x += 1;
                    units[2].y += 1;
                    break;
                case 1: //2 -> 1
                    units[0].x -= 1;
                    units[0].y -= 1;
                    units[1].x -= 2;
                    units[1].y += 2;
                    units[2].x -= 1;
                    units[2].y += 1;
                    break;
                case 2: //3 -> 2
                    units[0].x += 1;
                    units[0].y -= 1;
                    units[1].x -= 2;
                    units[1].y -= 2;
                    units[2].x -= 1;
                    units[2].y -= 1;
                    break;
                case 3: //4 -> 3
                    units[0].x += 1;
                    units[0].y += 1;
                    units[1].x += 2;
                    units[1].y -= 2;
                    units[2].x += 1;
                    units[2].y -= 1;
                    break;
            }
        }
    }

    @Override
    public int getid() {
        return 2;
    }
}

class Piece3 implements Piece{
    /*  [1][2]
        [3][4]
    */
    private Coordinate[] units = {new Coordinate(8,-1), new Coordinate(9,-1), new Coordinate(8,0), new Coordinate(9,0)};

    @Override
    public void move(String direction) {
        PieceFunctions.move(units, direction);
    }

    @Override
    public Coordinate[] getUnits() {
        return units;
    }

    @Override
    public void rotate(String sense) {
        //Symmetric. No need to rotate.
    }

    @Override
    public int getid() {
        return 3;
    }
}

class Piece4 implements Piece{
    /*     [0][1]
        [2][3]
     */
    private Coordinate[] units = {new Coordinate(8,-1), new Coordinate(9,-1), new Coordinate(7,0), new Coordinate(8,0)};
    private byte rotationalState = 1;

    @Override
    public void move(String direction) {
        PieceFunctions.move(units, direction);
    }

    @Override
    public Coordinate[] getUnits() {
        return units;
    }

    @Override
    public void rotate(String sense) {
        if("CLOCKWISE".equals(sense)){
            if(++rotationalState == 5) rotationalState =1;

            switch(rotationalState){
                case 1: //4 -> 1
                    units[0].x += 1; units[0].y -= 1;
                    units[1].x += 2;
                    units[2].x -= 1; units[2].y -= 1;
                    break;
                case 2: //1 -> 2
                    units[0].x += 1; units[0].y += 1;
                                     units[1].y += 2;
                    units[2].x += 1; units[2].y -= 1;
                    break;
                case 3: //2 -> 3
                    units[0].x -= 1; units[0].y += 1;
                    units[1].x -= 2;
                    units[2].x += 1; units[2].y += 1;
                    break;
                case 4: //3 -> 4
                    units[0].x -= 1; units[0].y -= 1;
                                     units[1].y -= 2;
                    units[2].x -= 1; units[2].y += 1;
                    break;
            }
        }

        else if("ANTICLOCKWISE".equals(sense)){
            if(--rotationalState == 0) rotationalState =4;

            switch(rotationalState){
                case 4: //1 -> 4
                    units[0].x -= 1; units[0].y += 1;
                    units[1].x -= 2;
                    units[2].x += 1; units[2].y += 1;
                    break;
                case 1: //2 -> 1
                    units[0].x -= 1; units[0].y -= 1;
                                     units[1].y -= 2;
                    units[2].x -= 1; units[2].y += 1;
                    break;
                case 2: //3 -> 2
                    units[0].x += 1; units[0].y -= 1;
                    units[1].x += 2;
                    units[2].x -= 1; units[2].y -= 1;
                    break;
                case 3: //4 -> 3
                    units[0].x += 1; units[0].y += 1;
                                     units[1].y += 2;
                    units[2].x += 1; units[2].y -= 1;
                    break;
            }
        }
    }

    @Override
    public int getid() {
        return 4;
    }
}

class Piece5 implements Piece{
    /*  [0][1][2][3]
     */
    private Coordinate[] units = {new Coordinate(7,0), new Coordinate(8,0), new Coordinate(9,0), new Coordinate(10,0)};
    private byte rotationalState = 1;
    @Override
    public void move(String direction) {
        PieceFunctions.move(units, direction);
    }

    @Override
    public Coordinate[] getUnits() {
        return units;
    }

    @Override
    public void rotate(String sense) {
        //only two rotational states. Sense does not matter.
        if(rotationalState == 1)
            rotationalState = 2;
        else if (rotationalState == 2)
            rotationalState=1;

        switch(rotationalState){
            case 2://1 -> 2
                units[0].x += 2; units[0].y -= 2;
                units[1].x += 1; units[1].y -= 1;
                units[3].x -= 1; units[3].y += 1;
                break;

            case 1://2 -> 1
                units[0].x -= 2; units[0].y += 2;
                units[1].x -= 1; units[1].y += 1;
                units[3].x += 1; units[3].y -= 1;
                break;
        }
    }

    @Override
    public int getid() {
        return 5;
    }
}

class Piece6 implements Piece{
    /*  [0]
        [1][2][3]
     */
    private Coordinate[] units = {new Coordinate(7,-1), new Coordinate(7,0), new Coordinate(8,0), new Coordinate(9,0)};
    private byte rotationalState = 1;

    @Override
    public void move(String direction) {
        PieceFunctions.move(units, direction);
    }

    @Override
    public Coordinate[] getUnits() {
        return units;
    }

    @Override
    public void rotate(String sense) {
        if ("CLOCKWISE".equals(sense)) {
            if (++rotationalState == 5) rotationalState = 1;

            switch (rotationalState) {
                case 1: //4 -> 1
                    units[0].x += 1; units[0].y -= 1;
                    units[2].x += 1; units[2].y += 1;
                    units[3].x += 2; units[3].y += 2;
                    break;
                case 2: //1 -> 2
                    units[0].x += 1; units[0].y += 1;
                    units[2].x -= 1; units[2].y += 1;
                    units[3].x -= 2; units[3].y += 2;
                    break;
                case 3: //2 -> 3
                    units[0].x -= 1; units[0].y += 1;
                    units[2].x -= 1; units[2].y -= 1;
                    units[3].x -= 2; units[3].y -= 2;
                    break;
                case 4: //3 -> 4
                    units[0].x -= 1; units[0].y -= 1;
                    units[2].x += 1; units[2].y -= 1;
                    units[3].x += 2; units[3].y -= 2;
                    break;
            }
        } else if ("ANTICLOCKWISE".equals(sense)) {
            if (--rotationalState == 0) rotationalState = 4;

            switch (rotationalState) {
                case 4: //1 -> 4
                    units[0].x -= 1; units[0].y += 1;
                    units[2].x -= 1; units[2].y -= 1;
                    units[3].x -= 2; units[3].y -= 2;
                    break;
                case 1: //2 -> 1
                    units[0].x -= 1; units[0].y -= 1;
                    units[2].x += 1; units[2].y -= 1;
                    units[3].x += 2; units[3].y -= 2;
                    break;
                case 2: //3 -> 2
                    units[0].x += 1; units[0].y -= 1;
                    units[2].x += 1; units[2].y += 1;
                    units[3].x += 2; units[3].y += 2;
                    break;
                case 3: //4 -> 3
                    units[0].x += 1; units[0].y += 1;
                    units[2].x -= 1; units[2].y += 1;
                    units[3].x -= 2; units[3].y += 2;
                    break;
            }
        }
    }

    @Override
    public int getid() {
        return 6;
    }
}

