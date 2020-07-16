package com.example.leaprgb.tetrisClasses;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.Loader;

import android.content.DialogInterface;
import android.os.Bundle;
import android.transition.Slide;
import android.transition.Transition;
import android.transition.TransitionManager;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.example.leaprgb.LED;
import com.example.leaprgb.R;

public class TetrisActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Integer>{

    static final private int TETRISLOADERID=34;
    private Tetris reference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tetris);
    }

    @NonNull
    @Override
    public Loader<Integer> onCreateLoader(int id, @Nullable Bundle args) {
        reference = new Tetris(this);
        return reference;
    }

    @Override
    public void onLoadFinished(@NonNull Loader<Integer> loader, Integer finalScore) {
        AlertDialog.Builder scoreAlert= new AlertDialog.Builder(this);
        scoreAlert.setTitle("Game Over");
        scoreAlert.setMessage("Your score is " + finalScore);
        scoreAlert.setPositiveButton("NEW GAME", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                reference=null;     //In case the garbage collector can't access
                getSupportLoaderManager().restartLoader(TETRISLOADERID, null, TetrisActivity.this);
            }
        });
        scoreAlert.setNegativeButton("QUIT", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                finish();
            }
        });
        scoreAlert.setCancelable(false); //background press operation. false means cant leave by clicking outside
        scoreAlert.show();
    }

    @Override
    public void onLoaderReset(@NonNull Loader<Integer> loader) {

    }


    public void beginTetris(View view) {
        if(LED.isConnected(true)){
            ImageView leftarrow = findViewById(R.id.leftarrow);
            ImageView rightarrow = findViewById(R.id.rightarrow);
            ImageView rotateanti = findViewById(R.id.rotateanti);
            ImageView rotateclock = findViewById(R.id.rotateclock);
            ImageView dropbutton = findViewById(R.id.dropbutton);
            Button beginbutton = findViewById(R.id.beginTetris);

            Transition transition = new Slide();
            transition.addTarget(leftarrow);
            transition.addTarget(rightarrow);
            transition.addTarget(rotateanti);
            transition.addTarget(rotateclock);
            transition.addTarget(dropbutton);
            transition.addTarget(beginbutton);

            TransitionManager.beginDelayedTransition((ViewGroup) findViewById(R.id.tetris_root), transition);

            leftarrow.setVisibility(View.VISIBLE);
            rightarrow.setVisibility(View.VISIBLE);
            rotateanti.setVisibility(View.VISIBLE);
            rotateclock.setVisibility(View.VISIBLE);
            dropbutton.setVisibility(View.VISIBLE);
            beginbutton.setVisibility(View.GONE);

            getSupportLoaderManager().initLoader(TETRISLOADERID, null, this);
        }
        else
            Toast.makeText(this,"ESP8266 Not Found on Network",Toast.LENGTH_SHORT).show();
    }

    public void onRotatePressed(View view) {
        switch(view.getId()){
            case R.id.rotateanti:
                reference.doRotate("ANTICLOCKWISE");
                break;
            case R.id.rotateclock:
                reference.doRotate("CLOCKWISE");
                break;
        }
    }

    public void onArrowKeyPressed(View view) {
        switch(view.getId()){
            case R.id.leftarrow:
                reference.movePiece("LEFT");
                break;
            case R.id.rightarrow:
                reference.movePiece("RIGHT");
                break;
        }
    }

    public void onDropPressed(View view) {
        reference.dropPiece();
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
