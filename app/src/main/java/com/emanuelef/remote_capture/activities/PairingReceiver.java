package com.emanuelef.remote_capture.activities;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.app.RemoteInput;
import android.util.Log;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.atomic.AtomicBoolean;
import java.io.DataOutputStream;
import java.io.File;
import android.app.NotificationManager;

public class PairingReceiver extends BroadcastReceiver {

    public static final String KEY_TEXT_REPLY = "key_pairing_pin";
    public static final String EXTRA_IP_PORT = "extra_ip_port";
    private static final String TAG = "PairingReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        String ipPort = intent.getStringExtra(EXTRA_IP_PORT);
        CharSequence reply = getMessageText(intent);
        setResultCode(0);
        if (reply != null && ipPort != null) {
            String pin = reply.toString().trim();
            LogUtil.logToFile( "PIN Received: " + pin + " for " + ipPort);
            //((NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE)).cancel(101);
            // הפעלת פקודת 'adb pair'
            //runAdbPairCommand(context, ipPort, pin);
            if(nsdactivity.ready){
            new myadb(context,ipPort,pin,"pair","");
            nsdactivity.ready=false;
            // עדכון המשתמש (באמצעות הודעה פשוטה)
            //NotificationHelper helper = new NotificationHelper(context);
            //helper.sendSimpleNotification("ADB Pairing", "מנסה זיווג עם קוד: " + pin, 102);
            }else
                LogUtil.logToFile("isnt ready...");
        } else {
            LogUtil.logToFile("Failed to get PIN or IP/Port from intent.");
        }
    }

    private CharSequence getMessageText(Intent intent) {
        android.os.Bundle remoteInput = RemoteInput.getResultsFromIntent(intent);
        if (remoteInput != null) {
            return remoteInput.getCharSequence(KEY_TEXT_REPLY);
        }
        return null;
    }
    /**
     * מריץ את פקודת 'adb pair' באמצעות הבינארי המקומי.
     */
     /*
    private void runAdbPairCommand(Context context, String ipPort, String pin) {
        // נתיב לבינארי ADB בתוך ספריית הליב המקומית
        String adbBinaryPath = context.getApplicationInfo().nativeLibraryDir + "/adb.so"; 

        try {
            String[] command = {
                adbBinaryPath, 
                "pair", 
                ipPort,
                pin
            };

            Process process = Runtime.getRuntime().exec(command);
            LogUtil.logToFile( "ADB pair command started: " + ipPort);
            final String output = readStream(process.getInputStream());
            // קריאת שגיאות (Error Output)
            final String error = readStream(process.getErrorStream());

            final int exitCode = process.waitFor();
            NotificationHelper helper = new NotificationHelper(context);
            helper.sendSimpleNotification("res", "ex=" +exitCode+"ou="+output+"er="+error , 103);
            
            // ביישום מלא, יש לקרוא את פלט ושגיאות התהליך כדי לדעת אם הזיווג הצליח.

        } catch (Exception e) {
            Log.e(TAG, "Error running adb pair command", e);
            NotificationHelper helper = new NotificationHelper(context);
            helper.sendSimpleNotification("שגיאת ADB", "הפעלת ADB נכשלה: " + e.getMessage(), 103);
        }
    }
    private String readStream(java.io.InputStream is) throws java.io.IOException {
        java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(is));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line).append("\n");
        }
        return sb.toString();
    }*/

            
                
       
   
}
