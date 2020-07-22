package com.example.leaprgb.animationClasses;

import android.media.audiofx.Visualizer;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.example.leaprgb.LED;
import com.example.leaprgb.R;

public class VisualizerActivity extends AppCompatActivity {

    boolean isVisualizing = false;
    Visualizer vis;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_visualizer);
        vis = new Visualizer(0);

        vis.setDataCaptureListener(new Visualizer.OnDataCaptureListener() {
            @Override
            public void onWaveFormDataCapture(Visualizer visualizer, byte[] waveform, int samplingRate) {
            }

            @Override
            public void onFftDataCapture(Visualizer visualizer, byte[] fft, int samplingRate) {
                updateFft(fft);
            }
        }, 3 * Visualizer.getMaxCaptureRate()/5, false, true);
    }

    public void onVisualizerAction(View view) {
        isVisualizing = !isVisualizing;
        LED.sendMessage(isVisualizing ?"visualize":"stop");
        vis.setEnabled(isVisualizing);
        ((Button) view).setText(isVisualizing ?"STOP":"START");
    }

    void updateFft(byte[] fft){
        int n = fft.length / 2;
        float[] magnitudes = new float[n];
        StringBuilder vals = new StringBuilder();
        magnitudes[n-1] = (float)Math.abs(fft[1]);  // Nyquist
        for (int k = 1; k < n; k++) {
            int i = k * 2;
            magnitudes[k-1] = (float)Math.hypot(fft[i], fft[i + 1]) * 2f/n;
        }
        float[] float_bars = new float[16];
        float_bars[0] = magnitudes[0];
        float_bars[1] = magnitudes[1];
        float_bars[2] = magnitudes[2];
        float_bars[3] = magnitudes[3];
        float_bars[4] = magnitudes[4];
        float_bars[5] = avgFrequency(magnitudes, 5, 6);
        float_bars[6] = avgFrequency(magnitudes, 7, 9);
        float_bars[7] = avgFrequency(magnitudes, 10, 13);
        float_bars[8] = avgFrequency(magnitudes, 14, 19);
        float_bars[9] = avgFrequency(magnitudes, 20, 30);
        float_bars[10] = avgFrequency(magnitudes, 31, 50);
        float_bars[11] = avgFrequency(magnitudes, 51, 90);
        float_bars[12] = avgFrequency(magnitudes, 91, 170);
        float_bars[13] = avgFrequency(magnitudes, 171, 250);
        float_bars[14] = avgFrequency(magnitudes, 251, 330);
        float_bars[15] = avgFrequency(magnitudes, 331, 511);

        int[] int_bars = new int[16];
        for(int i = 0; i < 16; ++i){
            int_bars[i] = Math.round(float_bars[i]*15);
            if(int_bars[i]<16) ++int_bars[i];
        }

        for(int i = 0; i < 16; ++i)
            vals.append(int_bars[i]).append("/");

        vals.deleteCharAt(vals.length() - 1);
        LED.sendMessage(vals.toString());
    }

    float avgFrequency(float[] values, int start, int end){
        float sum = 0;
        for(int i = start; i<= end; ++i){
            sum += values[i];
        }
        return sum/(end - start + 1);
    }

    @Override
    protected void onDestroy() {
        if(isVisualizing)
            LED.sendMessage("stop");
        vis.setEnabled(false);
        LED.clear();
        LED.show();

        super.onDestroy();
    }
}

