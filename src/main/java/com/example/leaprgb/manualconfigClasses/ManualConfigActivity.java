package com.example.leaprgb.manualconfigClasses;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.os.SystemClock;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.leaprgb.Coordinate;
import com.example.leaprgb.LED;
import com.example.leaprgb.R;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

import static java.lang.Character.isDigit;

public class ManualConfigActivity extends AppCompatActivity {

    EditText rowtext, columntext, hextext;
    ImageView hexbird;
    TextView pixelCount;

    class InputPoint{
        int row, column;
        String hexcode;
        InputPoint(int rowno, int colno, String hex) {
            row = rowno;
            column = colno;
            hexcode = hex;
        }
    }
    LinkedList<InputPoint> pointBank = new LinkedList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manual_config);
        rowtext = (EditText)findViewById(R.id.rowno);
        columntext = (EditText)findViewById(R.id.columnno);
        hextext = (EditText)findViewById(R.id.hexcode);
        hexbird = (ImageView) findViewById(R.id.hexbirdy);
        pixelCount = (TextView) findViewById(R.id.pixelCount);

        hextext.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                String mycolor = hextext.getText().toString();
                if(isValidColor(mycolor)) {
                    mycolor = "#".concat(mycolor);
                    int myColor = Color.parseColor(mycolor);
                    hexbird.setBackgroundColor(myColor);
                }
            }

        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.manual_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

        //case R.id.ipsetup:

        AlertDialog.Builder alertIPBuilder = new AlertDialog.Builder(this);
        alertIPBuilder.setTitle("IP Configuration");
        final EditText inputET = new EditText(this);
        String oldIP = LED.getIP();
        oldIP = oldIP.substring(5,oldIP.length() - 1);
        inputET.setText(oldIP);
        inputET.setInputType(InputType.TYPE_CLASS_PHONE);
        alertIPBuilder.setView(inputET);

        alertIPBuilder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                }
        });
        alertIPBuilder.setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
            }
        });

        //to prevent closure of alert dialog on button click.
        final AlertDialog alertIP = alertIPBuilder.create();

        alertIP.show();
        //Overriding the handler immediately after show
        alertIP.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String newIP = inputET.getText().toString();
                boolean isValidIP = true;
                if(newIP.isEmpty()){
                    inputET.setError("Field Empty");
                    return;
                }
                if(newIP.length() > 15)
                    isValidIP = false;
                else{
                    String[] separatedIP = newIP.split("\\.");  ///need escape sequence because . represents something in regex
                    if(separatedIP.length != 4)
                        isValidIP = false;
                    else {
                        for (String s : separatedIP) {
                            if (s.length() > 3) {
                                isValidIP = false;
                                break;
                            }
                        }
                    }
                }
                for(char c: newIP.toCharArray()){
                    if (!(isDigit(c) || c == '.')) {
                        isValidIP = false;
                        Log.i("mylog","test3");
                        break;
                    }
                }

                if(isValidIP){
                    LED.updateIP(newIP);
                    alertIP.dismiss();
                }
                else
                    inputET.setError("Invalid IP Address");
            }
        });

        return super.onOptionsItemSelected(item);
    }

    public void onGoButtonClick(View view) {
        boolean allGood = true;
        int rownumber = (!(rowtext.getText().toString().equals("")))? Integer.parseInt(rowtext.getText().toString()):0;
        int columnnumber =(!(columntext.getText().toString().equals("")))? Integer.parseInt(columntext.getText().toString()):0;
        String hexcode = (!(hextext.getText().toString().equals("")))? hextext.getText().toString():"x";
        if (!(rownumber>=1 && rownumber<= Coordinate.YMAX)) {
            rowtext.setError("Invalid Row Number");
            allGood = false;
        }
        if (!(columnnumber>=1 && columnnumber<=Coordinate.XMAX)){
            columntext.setError("Invalid Column Number");
            allGood = false;
        }
        if(!isValidColor(hexcode)){
            hextext.setError("Invalid Hex Code");
            allGood=false;
        }
        if(allGood){
            if(LED.isConnected(true)) {
                LED.setlight(rownumber, columnnumber, hexcode);

                InputPoint newpoint = new InputPoint(rownumber, columnnumber, hexcode);
                int indexToAdd = pointBank.size();
                for (InputPoint t : pointBank)
                    if (t.row == newpoint.row && t.column == newpoint.column) {
                        indexToAdd = pointBank.indexOf(t);
                        pointBank.remove(t);
                        break;
                    }
                pointBank.add(indexToAdd, newpoint);
                Log.i("mylog", hexcode +" set at ("+columnnumber+", "+rownumber+")");
                LED.show();
                String countString = "Affected Pixels: "+pointBank.size();
                pixelCount.setText(countString);
            }
            else
                Toast.makeText(this, "ESP8266 Not Found On Network", Toast.LENGTH_SHORT).show();
        }
    }

    public void onSaveButtonClick(View v){
        if(pointBank.size() == 0){
            Toast.makeText(this,"Nothing to Save", Toast.LENGTH_SHORT).show();
            return;
        }

        AlertDialog.Builder fileNameInput = new AlertDialog.Builder(this);
        fileNameInput.setTitle("Save As");
        final EditText inputET = new EditText(this);
        inputET.setHint("default.txt");
        fileNameInput.setView(inputET);
        fileNameInput.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String fileName = inputET.getText().toString();

                if("".equals(fileName))
                    fileName = "default.txt";
                else if(!fileName.endsWith(".txt"))
                    fileName = fileName + ".txt";

                Log.i("mylog","Saving "+fileName);

                File path = ManualConfigActivity.this.getFilesDir();

                File file = new File(path, "SavedConfigs.txt");
                if(!file.exists())
                    createHelloWorld();

                try{
                    FileOutputStream ostream = new FileOutputStream(file, true);
                    ostream.write((fileName+"\n").getBytes());
                    ostream.close();
                }catch(Exception e){
                    Log.i("mylog", "Writing to SavedConfigs.txt operation failed : "+e.getMessage());
                    return;
                }

                file = new File(path, fileName);

                try {
                    FileOutputStream ostream = new FileOutputStream(file);
                    for(InputPoint p : pointBank)
                        ostream.write((p.row + " " + p.column + " " + p.hexcode +"\n").getBytes());
                    ostream.close();
                }catch(Exception e){
                    Log.i("mylog","User File Writing Operation Failed : "+e.getMessage());
                    Toast.makeText(ManualConfigActivity.this,"File Operation Failed",Toast.LENGTH_SHORT).show();
                }
            }
        });
        fileNameInput.setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
            }
        });
        fileNameInput.show();
    }

    public void onOpenButtonPressed(View v){

        if(!LED.isConnected(true)){
            Toast.makeText(this, "ESP8266 Not Found On Network", Toast.LENGTH_SHORT).show();
            return;
        }

        onClearButtonPressed(null);

        File path = this.getFilesDir();

        //MENU OPERATIONS
        File file = new File(path, "SavedConfigs.txt");
        if(!file.exists())
            createHelloWorld();
        int length = (int) file.length();
        byte[] fileBytes = new byte[length];

        try{
            FileInputStream istream = new FileInputStream(file);
            istream.read(fileBytes);
            istream.close();
        }catch(Exception e){
            Log.i("mylog","Reading from SavedConfigs failed: " + e.getMessage());
            Toast.makeText(this,"Operation Failed",Toast.LENGTH_SHORT).show();
            return;
        }
        String fileData = new String(fileBytes);
        Set<String> fileSet = new HashSet<>(Arrays.asList(fileData.split("\n")));  //to avoid duplicates. hashset.

        Log.i("mylog",fileSet.toString());

        PopupMenu popup = new PopupMenu(this, v);
        for(String s : fileSet)
            popup.getMenu().add(s);
        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                File path = ManualConfigActivity.this.getFilesDir();
                String fileToOpen = item.getTitle().toString();
                //HANDLE THE SELECTED FILE
                File file = new File(path, fileToOpen);

                int length = (int) file.length();
                byte[] fileBytes = new byte[length];
                try{
                    FileInputStream istream = new FileInputStream(file);
                    istream.read(fileBytes);
                    istream.close();
                }catch(Exception e){
                    Log.i("mylog","Reading from File failed: " + e.getMessage());
                    Toast.makeText(ManualConfigActivity.this,"Open Operation Failed",Toast.LENGTH_SHORT).show();
                    return false;
                }
                String fileData = new String(fileBytes);
                String[] points = fileData.split("\n");
                InputPoint temp;
                for(String s : points){
                    String[] data = s.split(" ");
                    temp = new InputPoint(Integer.parseInt(data[0]), Integer.parseInt(data[1]), data[2]);
                    LED.setlight(temp.row, temp.column, temp.hexcode);
                    pointBank.add(temp);
                }
                SystemClock.sleep(20);
                LED.show();
                String countString = "Affected Pixels: " + pointBank.size();
                pixelCount.setText(countString);
                return true;
            }
        });
        popup.show();
    }

    void createHelloWorld(){

        Log.i("mylog","Hello World Creating");

        File path = this.getFilesDir();
        File file = new File(path, "SavedConfigs.txt");
        try {
            FileOutputStream stream = new FileOutputStream(file);
            stream.write("HelloWorld.txt\n".getBytes());
            stream.close();
        }catch(Exception e){
            Log.i("mylog","Failure in creating SavedConfigs in helloworldfn : "+e.getMessage());
        }

        file = new File(path, "HelloWorld.txt");
        try {
            FileOutputStream stream = new FileOutputStream(file);
            String[] data = getString(R.string.helloworldcoordinates).split("/");

            for(String s:data)
                stream.write((s + " 0000ff\n").getBytes());
        }catch(Exception e){
            Log.i("mylog","Failure in creating HelloWorld.txt : "+e.getMessage());
        }
    }

    public void onClearButtonPressed(View view) {
        if (LED.isConnected(true)) {
            LED.clear();
            LED.show();
            pixelCount.setText("Affected Pixels: 0");
            pointBank.clear();
        }
    }

    boolean isValidColor(String color){
        color = color.toUpperCase();
        if(color.length()!=6)
            return false;
        for(int i = 0; i < 6; ++i)
            if (!(isDigit(color.charAt(i)) || (color.charAt(i)>='A' && color.charAt(i)<='F')))
                return false;
        return true;
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
