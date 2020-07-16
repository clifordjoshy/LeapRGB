package com.example.leaprgb.flooditClasses;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.DialogInterface;
import android.os.Bundle;
import android.os.SystemClock;
import android.transition.Slide;
import android.transition.Transition;
import android.transition.TransitionManager;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.example.leaprgb.Coordinate;
import com.example.leaprgb.LED;
import com.example.leaprgb.R;

import java.util.LinkedList;
import java.util.Random;

public class FloodItActivity extends AppCompatActivity {

    static final String RED = "ff0000", GREEN = "00ff00", BLUE = "0000ff", YELLOW = "ffff00", WHITE = "ffffff", VIOLET = "ff00ff";
    static final int DIMENSION = 15, TURNMAX = 28, COLORCOUNT =6;
    TextView turncheck;

    class Field{
        static final int INDEX_UP = 0, INDEX_RIGHT = 1, INDEX_DOWN = 2, INDEX_LEFT=3;
        String color;
        boolean isFlooded;
        boolean[] positionFlag = new boolean[4];
        boolean isCurrentColor() {return (color == currentcolor);}
    }

    Field[][] field;
    LinkedList<Coordinate> boundary;
    String currentcolor;
    int turnCounter;
    Random r;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_flood_it);
        turncheck = findViewById(R.id.floodinstruct);
    }

    public void beginFlood(View view) {
        if (LED.isConnected(true)) {
            Button redbn = findViewById(R.id.redbutton);
            Button bluebn = findViewById(R.id.bluebutton);
            Button greenbn = findViewById(R.id.greenbutton);
            Button yellowbn = findViewById(R.id.yellowbutton);
            Button whitebn = findViewById(R.id.whitebutton);
            Button violetbn = findViewById(R.id.violetbutton);
            Button beginbn = findViewById(R.id.beginflood);

            Transition transition = new Slide();

            transition.addTarget(redbn);
            transition.addTarget(bluebn);
            transition.addTarget(greenbn);
            transition.addTarget(yellowbn);
            transition.addTarget(whitebn);
            transition.addTarget(violetbn);
            transition.addTarget(beginbn);
            transition.addTarget(turncheck);

            TransitionManager.beginDelayedTransition((ViewGroup) findViewById(R.id.floodit_root), transition);

            redbn.setVisibility(View.VISIBLE);
            yellowbn.setVisibility(View.VISIBLE);
            bluebn.setVisibility(View.VISIBLE);
            greenbn.setVisibility(View.VISIBLE);
            whitebn.setVisibility(View.VISIBLE);
            violetbn.setVisibility(View.VISIBLE);
            beginbn.setVisibility(View.GONE);

            field = new Field[DIMENSION][DIMENSION];
            int i,j;
            for(i = 0; i < DIMENSION; ++i)
                for(j = 0; j < DIMENSION; ++j)
                    field[i][j] = new Field();

            startNewGame();


        }
        else
            Toast.makeText(this,"ESP8266 Not Found on Network",Toast.LENGTH_SHORT).show();
    }

    void startNewGame(){
        String settexttext = "Turn 0/"+TURNMAX;
        turncheck.setText(settexttext);
        turnCounter = 0;
        int i, j;
        r = new Random();
        for (i = 0; i < DIMENSION; ++i) {
            for (j = 0; j < DIMENSION; ++j) {
                field[i][j].color = getRandomColor();
                field[i][j].isFlooded = false;
                field[i][j].positionFlag[0] = false;
                field[i][j].positionFlag[1] = false;
                field[i][j].positionFlag[2] = false;
                field[i][j].positionFlag[3] = false;
            }
        }

        field[0][0].isFlooded = true;
        while(field[0][0].color == field[0][1].color || field[0][0].color == field[1][0].color)
            field[0][0].color = getRandomColor();       //since we set boundary as (0,0) only
        currentcolor = field[0][0].color;

        for (i = 0; i < DIMENSION; ++i) {
            for (j = 0; j < DIMENSION; ++j) {
                LED.setlight(i + 1, j + 1, field[i][j].color);
                LED.show();
                SystemClock.sleep(10);
            }
        }
    }

    public void onColorPressed(View view) {
        switch(view.getId()){
            case R.id.redbutton:
                gamePlay(RED);
                break;
            case R.id.greenbutton:
                gamePlay(GREEN);
                break;
            case R.id.yellowbutton:
                gamePlay(YELLOW);
                break;
            case R.id.whitebutton:
                gamePlay(WHITE);
                break;
            case R.id.bluebutton:
                gamePlay(BLUE);
                break;
            case R.id.violetbutton:
                gamePlay(VIOLET);
                break;
        }

        String settexttext = "Turn "+turnCounter+"/"+TURNMAX;
        turncheck.setText(settexttext);

        Log.i("mylog","Move finished");

        if(turnCounter == TURNMAX) {  //GAME OVER
            LED.clear();
            LED.show();
            int i, j;
            boolean hasWon = true;
            for(i = 0; i < DIMENSION; ++i)
                for(j =0; j < DIMENSION; ++j)
                    if(!field[i][j].isFlooded)
                        hasWon = false;

            final AlertDialog.Builder gameOverAlert = new AlertDialog.Builder(this);
            gameOverAlert.setTitle("Game Over");
            gameOverAlert.setMessage(hasWon?"Congrats! You beat the game in "+turnCounter+" moves." : "Better Luck Next Time");
            gameOverAlert.setPositiveButton("NEW GAME", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    startNewGame();
                }
            });
            gameOverAlert.setNegativeButton("QUIT", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    finish();
                }
            });
            gameOverAlert.setCancelable(false); //background press operation. false means cant leave by clicking outside
            gameOverAlert.show();
        }
    }

    private void gamePlay(String color) {
        if(color == currentcolor)
            return;

        turnCounter++;
        currentcolor = color;
        setAllToCurrent();

        int i,j;
        for(i = 0; i < DIMENSION; ++i){
            for(j = 0; j < DIMENSION; ++j){
                //filter out flooded points
                if(field[i][j].isFlooded)
                    continue;
                //filter out unfloodable points
                if(!(field[i][j].isCurrentColor()))
                    continue;

                if(isConnectedAt(i, j)!=10) {  //==10 means not connected. 10 is arbitrary.
                    floodAt(i, j, isConnectedAt(i, j));   //recursively floods all connected to the point, 10 is some random value
                    continue;
                }
                //a remaining point is current color, not connected, not flooded
                // So, flag nearbycheck
                if(!(field[i][j].positionFlag[Field.INDEX_DOWN]) && (i<DIMENSION-1) && field[i+1][j].isCurrentColor()){
                    field[i][j].positionFlag[Field.INDEX_DOWN] = true;
                    field[i+1][j].positionFlag[Field.INDEX_UP] = true;
                }
                if(!(field[i][j].positionFlag[Field.INDEX_UP]) && (i>0) && field[i-1][j].isCurrentColor()){
                    field[i][j].positionFlag[Field.INDEX_UP] = true;
                    field[i-1][j].positionFlag[Field.INDEX_DOWN] = true;
                }
                if(!(field[i][j].positionFlag[Field.INDEX_LEFT]) && (j>0) && field[i][j-1].isCurrentColor()){
                    field[i][j].positionFlag[Field.INDEX_LEFT] = true;
                    field[i][j-1].positionFlag[Field.INDEX_RIGHT] = true;
                }
                if(!(field[i][j].positionFlag[Field.INDEX_RIGHT]) && (j<DIMENSION-1) && field[i][j+1].isCurrentColor()){
                    field[i][j].positionFlag[Field.INDEX_RIGHT] = true;
                    field[i][j+1].positionFlag[Field.INDEX_LEFT] = true;
                }
            }
        }

    }

    private String getRandomColor() {
        switch(r.nextInt(COLORCOUNT)) {
            case 0:
                return YELLOW;
            case 1:
                return BLUE;
            case 2:
                return GREEN;
            case 3:
                return WHITE;
            case 4:
                return VIOLET;
            case 5:
                return RED;
        }
        Log.i("mylog", "Random Flood Color not generated");
        return null;
    }

    private int isConnectedAt(int i, int j){
        if(i<DIMENSION-1 && field[i+1][j].isFlooded)
            return Field.INDEX_UP;
        if(j<DIMENSION-1 && field[i][j+1].isFlooded)
            return Field.INDEX_LEFT;
        if(i>0 && field[i-1][j].isFlooded)
            return Field.INDEX_DOWN;
        if(j>0 && field[i][j-1].isFlooded)
            return Field.INDEX_RIGHT;
        return 10;
    }

    private void floodAt(int i, int j, int movedIndexDirection){
        if(field[i][j].isFlooded)
            return;
        field[i][j].isFlooded = true;
        Log.i("mylog","Flooded at "+i+" ,"+j);
        if(movedIndexDirection != Field.INDEX_DOWN && field[i][j].positionFlag[Field.INDEX_UP])
            floodAt(i-1, j, Field.INDEX_UP);
        if(movedIndexDirection != Field.INDEX_RIGHT && field[i][j].positionFlag[Field.INDEX_LEFT])
            floodAt(i, j-1, Field.INDEX_LEFT);
        if(movedIndexDirection != Field.INDEX_UP && field[i][j].positionFlag[Field.INDEX_DOWN])
            floodAt(i+1, j, Field.INDEX_DOWN);
        if(movedIndexDirection != Field.INDEX_LEFT && field[i][j].positionFlag[Field.INDEX_RIGHT])
            floodAt(i, j+1, Field.INDEX_RIGHT);
    }

    private void setAllToCurrent(){
        int i, j;
        for(i = 0; i < DIMENSION; ++i) {
            for (j = 0; j < DIMENSION; ++j) {
                if (field[i][j].isFlooded)
                    LED.setlight(i + 1, j + 1, currentcolor);
            }
        }
        LED.show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(LED.isConnected(false)) {
            LED.clear();
            LED.show();
        }
    }
}
