package com.example.leaprgb.snakeClasses;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.Loader;

import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.transition.Slide;
import android.transition.Transition;
import android.transition.TransitionManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.example.leaprgb.LED;
import com.example.leaprgb.R;

import java.lang.ref.WeakReference;

public class SnakeActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Integer> {

    static final private int SNAKELOADERID=12;
    private Snake reference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_snake);
    }

    @NonNull
    @Override
    public Loader onCreateLoader(int id, @Nullable Bundle args) {
        reference = new Snake(this);
        return reference;
    }

    @Override
    public void onLoadFinished(@NonNull Loader loader, Integer finalScore) {
        AlertDialog.Builder scoreAlert= new AlertDialog.Builder(this);
        scoreAlert.setTitle("Game Over");
        scoreAlert.setMessage("Your score is " + finalScore);
        scoreAlert.setPositiveButton("NEW GAME", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                reference=null;     //In case the garbage collector can't access
                getSupportLoaderManager().restartLoader(SNAKELOADERID, null, SnakeActivity.this);
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
    public void onLoaderReset(@NonNull Loader loader) {
    }

    public void beginSnake(View view) {

        if(LED.isConnected(true)){
            RadioButton arrowButton = (RadioButton) findViewById(R.id.arrowChoice);
            RadioButton gestureButton = (RadioButton) findViewById(R.id.gestureChoice);
            RadioGroup choiceGroup = (RadioGroup) findViewById(R.id.radioGroup);
            Button beginButton = (Button) findViewById(R.id.beginSnake);
            TextView choiceTitle = (TextView) findViewById(R.id.textViewSnake);

            Transition transition = new Slide();

            transition.addTarget(choiceGroup);
            transition.addTarget(choiceTitle);
            transition.addTarget(beginButton);

            //enable the view needed
            if(arrowButton.isChecked()){
                ImageView uparrow = (ImageView) findViewById(R.id.uparrow);
                ImageView downarrow = (ImageView) findViewById(R.id.downarrow);
                ImageView leftarrow = (ImageView) findViewById(R.id.leftarrow);
                ImageView rightarrow = (ImageView) findViewById(R.id.rightarrow);

                transition.addTarget(uparrow);
                transition.addTarget(leftarrow);
                transition.addTarget(rightarrow);
                transition.addTarget(downarrow);

                TransitionManager.beginDelayedTransition((ConstraintLayout)findViewById(R.id.snake_root), transition);

                uparrow.setVisibility(View.VISIBLE);
                downarrow.setVisibility(View.VISIBLE);
                rightarrow.setVisibility(View.VISIBLE);
                leftarrow.setVisibility(View.VISIBLE);
            }

            else if(gestureButton.isChecked()){
                SwipeHandler myHandler = new SwipeHandler(this);
                SwipeView swipeView = (SwipeView) findViewById(R.id.swipeView);
                swipeView.referenceToGestureListener.referenceToHandler = myHandler;

                transition.addTarget(swipeView);

                TransitionManager.beginDelayedTransition((ConstraintLayout)findViewById(R.id.snake_root), transition);
                swipeView.setVisibility(View.VISIBLE);
            }

            //remove the choice option
            beginButton.setVisibility(View.GONE);
            choiceGroup.setVisibility(View.GONE);
            choiceTitle.setVisibility(View.GONE);

            getSupportLoaderManager().initLoader(SNAKELOADERID, null, this);
        }
        else
            Toast.makeText(this,"ESP8266 Not Found on Network",Toast.LENGTH_SHORT).show();
    }

    public void onArrowKeyPressed(View view) {
        switch(view.getId()) {
            case R.id.leftarrow:
                moveSnake("LEFT");
                break;
            case R.id.rightarrow:
                moveSnake("RIGHT");
                break;
            case R.id.uparrow:
                moveSnake("UP");
                break;
            case R.id.downarrow:
                moveSnake("DOWN");
                break;
            }
    }

    static class SwipeHandler extends Handler{
        private final WeakReference<SnakeActivity> referenceToActivity;

        SwipeHandler(SnakeActivity ref) {
            referenceToActivity = new WeakReference<>(ref);
        }

        @Override
        public void handleMessage(Message msg) {
            referenceToActivity.get().moveSnake((String) msg.obj);
        }
    }


    public void moveSnake(String dir){
        reference.moveSnakeHandle(dir);
        Log.i("mylog", "Moving "+dir);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(LED.isConnected(false))
           LED.clear();
    }
}
