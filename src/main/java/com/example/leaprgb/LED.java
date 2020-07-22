package com.example.leaprgb;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.SystemClock;
import android.util.Log;

import java.net.URI;


import tech.gusavila92.websocketclient.WebSocketClient;

public abstract class LED {
    private static String ESPAddress = "";
    private static boolean isConnected = false;
    static private WebSocketClient webSocketClient;
    static private String message = "";

    public static boolean isConnected(boolean doConnect){
        if(isConnected)
            return true;

        if(!doConnect)
            return false;

        URI uri;
        try{
            uri = new URI(ESPAddress);
        }
        catch (Exception e) {
            Log.i("mylog", "URL creation failed: "+e.getMessage());
            return false;
        }

        webSocketClient = new WebSocketClient(uri) {
            @Override
            public void onOpen() {
                Log.i("mylog","WebSocket Handshake Established");
                isConnected = true;
            }

            @Override
            public void onTextReceived(String message) {
            }

            @Override
            public void onBinaryReceived(byte[] data) {
            }

            @Override
            public void onPingReceived(byte[] data) {
            }

            @Override
            public void onPongReceived(byte[] data) {
            }

            @Override
            public void onException(Exception e) {
                Log.i("mylog","WebSocket Exception Received: "+e.getMessage());
            }

            @Override
            public void onCloseReceived() {
                isConnected = false;
                Log.i("mylog", "WebSocket Connection Closed");
            }
        };
        webSocketClient.setConnectTimeout(10000);
        webSocketClient.connect();
        SystemClock.sleep(500);
        return isConnected;
    }

    public static void setlight(int row, int column, String hex){
        String msgtoserver = "setlight/" + row + "/" + column + "/" + hex;
        message += msgtoserver + "&";
    }

    static void setoff(int row, int column){
        String msgtoserver = "setoff/" + row + "/" + column;
        message += msgtoserver + "&";
    }

    public static void show(){
        while(!"".equals(message)){
            if(message.length() > 300) {
                int split_ind;
                for (split_ind = 300; split_ind > 0; --split_ind) {
                    if (message.charAt(split_ind) == '&')
                        break;
                }
                sendMessage(message.substring(0, split_ind));
                message = message.substring(split_ind + 1);
            }
            else {
                sendMessage(message + "show");
                message = "";
            }
        }
    }

    public static void clear(){
        message += "clear&";
    }

    public static void clearrow(int row, int columnStart, int columnEnd){
        String msgtoserver = "clearrow/" + row + "/" + columnStart + "/" + columnEnd;
        message += msgtoserver + "&";
    }

    public static void sendMessage(String msg){
        webSocketClient.send(msg);
    }

    private static MainActivity referenceToMainActivity;
    public static void updateIP(String newIP){
        newIP = "ws://" + newIP + "/";
        SharedPreferences sharedPref = referenceToMainActivity.getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString("IPAddress", newIP);
        editor.apply();
        ESPAddress = newIP;

    }

    public static String getIP(){
        return ESPAddress;
    }

    static void initializeIP(MainActivity reference){
        SharedPreferences sharedPref = reference.getPreferences(Context.MODE_PRIVATE);
        ESPAddress = sharedPref.getString("IPAddress","ws://192.168.1.7/");
        referenceToMainActivity = reference;
    }

    static void endconnection(){
        webSocketClient.close();
    }
}
