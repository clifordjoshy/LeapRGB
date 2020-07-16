package com.example.leaprgb.pongClasses;

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
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.leaprgb.LED;
import com.example.leaprgb.R;

public class PongActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Integer> {

    static final private int PONGLOADERID=23;
    private Pong reference;
    private SeekBar slider;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pong);
        slider = (SeekBar) findViewById(R.id.slider);
    }

    @NonNull
    @Override
    public Loader onCreateLoader(int id, @Nullable Bundle args) {
        reference = new Pong(this);
        return reference;
    }

    public void beginPong(View view){

        if(LED.isConnected(true)){
            RadioButton arrowButton = (RadioButton) findViewById(R.id.arrowChoice);
            RadioButton sliderButton = (RadioButton) findViewById(R.id.sliderChoice);
            RadioGroup choiceGroup = (RadioGroup) findViewById(R.id.radioGroup);
            Button beginButton = (Button) findViewById(R.id.beginPong);
            TextView choiceTitle = (TextView) findViewById(R.id.textViewPong);

            Transition transition = new Slide();
            transition.addTarget(choiceGroup);
            transition.addTarget(beginButton);

            //enable the view needed
            if(arrowButton.isChecked()){
                ImageView leftarrow = (ImageView) findViewById(R.id.leftarrow);
                ImageView rightarrow = (ImageView) findViewById(R.id.rightarrow);
                transition.addTarget(leftarrow);
                transition.addTarget(rightarrow);
                TransitionManager.beginDelayedTransition((ViewGroup) findViewById(R.id.pong_root), transition);

                rightarrow.setVisibility(View.VISIBLE);
                leftarrow.setVisibility(View.VISIBLE);
            }

            else if(sliderButton.isChecked()){
                transition.addTarget(slider);
                TransitionManager.beginDelayedTransition((ViewGroup) findViewById(R.id.pong_root), transition);

                slider.setVisibility(View.VISIBLE);
                slider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                        if(reference != null)  //restarted loader
                            reference.moveMyPaddleTo(progress+1);
                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {
                    }

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {
                    }
                });
            }

            //remove the choice option
            beginButton.setVisibility(View.GONE);
            choiceGroup.setVisibility(View.GONE);
            choiceTitle.setVisibility(View.GONE);

            getSupportLoaderManager().initLoader(PONGLOADERID, null, this);
        } else
            Toast.makeText(this,"ESP8266 Not Found on Network",Toast.LENGTH_SHORT).show();

    }

    public void onArrowKeyPressed(View view) {
        switch(view.getId()) {
            case R.id.leftarrow:
                reference.moveMyPaddle("LEFT");
                break;
            case R.id.rightarrow:
                reference.moveMyPaddle("RIGHT");
                break;
        }
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
                slider.setProgress(6);
                getSupportLoaderManager().restartLoader(PONGLOADERID, null, PongActivity.this);
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(LED.isConnected(false)) {
            LED.clear();
            LED.show();
        }

    }
}
