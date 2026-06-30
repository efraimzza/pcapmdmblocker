package com.emanuelef.remote_capture.activities;

import android.content.Context;
import android.content.Intent;
import java.io.File;
import java.io.DataOutputStream;
import java.util.concurrent.atomic.AtomicBoolean;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class myadb { 
    private String pkgname,hompat,filesdir,menv,cmddpm;
    private CommandOutputListener commandListener;
    public myadb(final Context con, String ipPort, String pin,final String type,final String todo){
        pkgname=con.getPackageName();
        hompat=con.getDir("HOME",con. MODE_PRIVATE).getAbsolutePath();
        filesdir=con.getApplicationInfo().nativeLibraryDir;
        final String adb="libadb.so";
        menv="\nPATH=$PATH:"+filesdir+"\nTMPDIR="+hompat+"\nHOME="+hompat+"\nTERM=screen\nexport PATH\nexport TMPDIR\n";
        cmddpm="\n"+adb+" shell dpm set-device-owner com.emanuelef.remote_capture.debug/com.emanuelef.remote_capture.activities.admin & exit\nexit\n";
        //Toast.makeText(this,getDir("HOME", MODE_PRIVATE).getAbsolutePath(),1).show();
        con.getDir("HOME", con.MODE_PRIVATE).getAbsolutePath();//important to automaticly create
        commandListener = new CommandOutputListener() {
            @Override
            public void onOutputReceived(final String line) {
                LogUtil.logToFile("o: "+line);
                nsdactivity.logres+=line+"\n";
                if(line.toLowerCase().contains("success: device owner set to package")){
                    //new NotificationHelper(con).sendSimpleNotification("mdm","mdm הופעל בהצלחה",101);
                    Intent connectIntent = new Intent("com.ands.ACTION_START_CONNECT_DISCOVERY");
                    connectIntent.putExtra("dpm_SUCCESS", true);
                    con.sendBroadcast(connectIntent);
                }
                if(line.toLowerCase().contains("unauthorized")){
                    nsdactivity.logres+="\nשגיאה - עדיין אין אישור להרצת adb מהמכשיר\n";

                    err="unauthorized";
                }
                /*runOnUiThread(new Runnable() {
                 @Override
                 public void run() {
                 //Log.d(TAG, "(Real-time output): " + line);
                 if (outputTextView != null) {
                 outputTextView.append("o: " + line + "\n");
                 if(line.toLowerCase().contains("success: device owner set to package")){
                 outputTextView.setText("הפעלה הצליחה\n");
                 outputTextView.setTextColor(Color.parseColor("#FF00FF00"));
                 }
                 // גלילה אוטומטית לתחתית
                 if (outputTextView != null && outputScrollView != null) {
                 //outputTextView.append("Output: " + line + "\n");
                 // scrill down
                 outputScrollView.fullScroll(View.FOCUS_DOWN);
                 }
                 }
                 }
                 });*/
            }

            @Override
            public void onErrorReceived(final String line) {
                LogUtil.logToFile("e: "+line);
                nsdactivity.logres+=line+"\n";
                
                    if(line.toLowerCase().contains("account")){
                        nsdactivity.logres+="\nשגיאת חשבונות\n";
                        err="account";
                    }else if(line.toLowerCase().contains("is already set")){
                        nsdactivity.logres+="\nשגיאה - מכשיר כבר מוגדר\n";
                        err="already";
                    }else if(line.toLowerCase().contains("already several users")){
                        nsdactivity.logres+="\nשגיאה - למכשיר יש משתמשים מרובים\n";
                        err="users";
                    }else if(line.toLowerCase().contains("java.lang.IllegalStateException")){
                        nsdactivity.logres+="\nשגיאה - שגיאה לא מוכרת בינתיים\n";
                        err="Illegal";
                    }
                    
                
                /*runOnUiThread(new Runnable() {
                 @Override
                 public void run() {
                 //Log.e(TAG, "(Error output): " + line);
                 if (outputTextView != null) {
                 outputTextView.append("e: " + line + "\n");

                 if (outputTextView != null && outputScrollView != null) { // ודא ששניהם לא null
                 //outputTextView.append("Output: " + line + "\n");

                 outputScrollView.fullScroll(View.FOCUS_DOWN);
                 }
                 }
                 }
                 });*/
            }

            @Override
            public void onCommandFinished(final int exitCode, final String finalOutput, final String finalError) {
                LogUtil.logToFile("finish exit code-"+exitCode+"\n"+finalOutput);
                nsdactivity.logres+="finish exit code-"+exitCode+"\n";
                boolean end=false;
                
                if(err.equals("account")){
                    nsdactivity.logres+="\nשגיאה - יש עדיין חשבונות במכשיר\n";

                }else if(err.equals("already")){
                    nsdactivity.logres+="\nשגיאה - מכשיר כבר מוגדר\n";

                }else if(err.equals("users")){
                    nsdactivity.logres+="\nשגיאה - למכשיר יש משתמשים מרובים\n";

                }else if(err.equals("Illegal")){
                    nsdactivity.logres+="\nשגיאה - שגיאה לא מוכרת בינתיים\n";

                }else if(err.equals("unauthorized")){
                    nsdactivity.logres+="\nשגיאה - עדיין אין אישור להרצת adb מהמכשיר\n";
                }
               // if (exitCode == 0) {
                    
                    // הפעולה צריכה להתבצע על ה-UI thread של ה-MainActivity
                    // מכיוון ש-BroadcastReceiver לא יכול לגשת ל-MainActivity ישירות,
                    // הדרך הכי טובה היא לשלוח Intent או להשתמש ב-SharedPreferences.
                    // לשם פשטות התיקון, נשתמש בפתרון ישיר יותר:

                    // 🛑 אם ה-Pairing הצליח (exitCode 0), נתחיל גילוי לשירות ה-CONNECT.

                    // הערה: קוד יציאה 0 בדרך כלל מציין הצלחה, אך עם ADB שרץ בתוך שורת פקודה, ייתכן שתרצה לבדוק את finalOutput/finalError עבור מחרוזות ספציפיות ("Successfully paired").
                    if(type.equals("pair")){
                    if (finalOutput.contains("Successfully paired")) {
                        //NotificationHelper helper = new NotificationHelper(con);
                        //helper.sendSimpleNotification("ADB Pairing", "זיווג הצליח! מחפש שירות Connect...", 105);

                        // 💡 הפעלת גילוי שירות ה-CONNECT
                        // מכיוון שאין לנו גישה ישירה למופע של AdbNsdResolver ב-MainActivity, 
                        // נצטרך דרך אחרת להפעיל את זה.

                        // 1. ניתן לשלוח Broadcast ל-MainActivity
                        // 2. ניתן להפעיל את ה-Discovery ישירות כאן (אם מפעיל ה-NSD לא דורש Activity Context)

                        // לשם המשך הפתרון, נניח שה-MainActivity תהיה פעילה ותטפל בזה, או שפשוט נדליק את הגילוי כאן:

                        // **פתרון באמצעות Intent ל-MainActivity (הדרך הנכונה יותר):**
                        Intent connectIntent = new Intent("com.ands.ACTION_START_CONNECT_DISCOVERY");
                        connectIntent.putExtra("PAIR_SUCCESS", true);
                        con.sendBroadcast(connectIntent);
                        end=true;
                        // יש להוסיף BroadcastReceiver ב-MainActivity שיאזין ל-ACTION_START_CONNECT_DISCOVERY
                    } else {
                        //NotificationHelper helper = new NotificationHelper(con);
                        //helper.sendSimpleNotification("ADB Pairing", "זיווג נכשל (פלט): " + finalOutput + " (שגיאה): " + finalError, 105);
                    }
                    }else if(type.equals("connect")){
                        String disacc="";
                        String enacc="";
                        String acti="";
                        String sec="";
                        String res="";
                        if(todo.contains("d")){
                            disacc="new state: disabled-user";
                            if(finalOutput.toLowerCase().contains(disacc)){
                                //disacc suc
                                res+="d";
                            }
                        }
                        if(todo.contains("e")){
                            enacc="new state: enabled";
                            if(finalOutput.toLowerCase().contains(enacc)){
                                //enacc suc
                                res+="e";
                            }
                        }
                        if(todo.contains("a")){
                            acti="success: device owner set to package";
                            if(finalOutput.toLowerCase().contains(acti)){
                                //acti suc
                                res+="a";
                            }
                        }
                        if(todo.contains("s")){
                            sec="granted=true";
                            if(finalOutput.toLowerCase().contains(sec)){
                                //sec suc
                                res+="s";
                            }
                        }
                        //if(todo.contains("c")){
                            //only check no beed to do anymore
                        //}
                        
                        
                        
                        Intent connectIntent = new Intent("com.ands.ACTION_START_CONNECT_DISCOVERY");
                        connectIntent.putExtra("adb_res", res);
                        con.sendBroadcast(connectIntent);
                        end=true;
                        
                    }
                //}else{
                    if(!end){
                    Intent connectIntent = new Intent("com.ands.ACTION_START_CONNECT_DISCOVERY");
                    connectIntent.putExtra("adb_res", "fail");
                    con.sendBroadcast(connectIntent);
                    }
                //}
                /* runOnUiThread(new Runnable() {
                 @Override
                 public void run() {
                 //Log.d(TAG, "(Command finished). Exit Code: " + exitCode);
                 //Log.d(TAG, "(Final Output):\n" + finalOutput);
                 //Log.d(TAG, "(Final Error Output):\n" + finalError);

                 if (outputTextView != null) {
                 outputTextView.append("\n--- Command Finished ---\n");
                 outputTextView.append("Exit Code: " + exitCode + "\n");
                 //outputTextView.append("Final Output:\n" + finalOutput + "\n");
                 //outputTextView.append("Final Error:\n" + finalError + "\n");

                 if (outputTextView != null && outputScrollView != null) { // ודא ששניהם לא null
                 //outputTextView.append("Output: " + line + "\n");

                 outputScrollView.fullScroll(View.FOCUS_DOWN);
                 }
                 }

                 bupair.setEnabled(true);
                 bucon.setEnabled(true);
                 //buconmul.setEnabled(true);
                 buconmult.setEnabled(true);
                 budisacccon.setEnabled(true);
                 buenacccon.setEnabled(true);
                 budisaccmult.setEnabled(true);
                 buenaccmult.setEnabled(true);
                 budisacc.setEnabled(true);
                 buenacc.setEnabled(true);
                 buexecall.setEnabled(true);
                 }
                 });*/
            }
        };
        try {
            //InputStream adbb= getAssets().open("adb");
            //File fi=new File(getFilesDir()+"/adb");
            // fi=new File("/data/local/tmp"+"/adb");

            File fil=new File(con.getDataDir()+"/");
            File filb=new File(con.getFilesDir()+"/");
            File filc=new File(con.getFilesDir()+"/home/");
            filc.mkdir();

            //pcopyFile(adbb,fi);
            //fi.setExecutable(true,false);
            //fi.setWritable(true,false);
            //fi.setReadable(true,false);
            fil.setExecutable(true,false);
            fil.setWritable(true,false);
            fil.setReadable(true,false);
            filb.setExecutable(true,false);
            filb.setWritable(true,false);
            filb.setReadable(true,false);
            filc.setExecutable(true,false);
            filc.setWritable(true,false);
            filc.setReadable(true,false);
            /*try {
             Os.chmod(fi.getPath(), 777);
             } catch (ErrnoException e) {}*/
        } catch (Exception e) {
            LogUtil.logToFile(""+e);
        }

        /*commandEditText.setText("/system/bin/sh -"+menv+adb+" kill-server\n"+adb+" pair "+edtxip.getText().toString()+":"+edtxport.getText().toString()+"\n"+edtxpwd.getText().toString()+"\n");
         final String commandToExecute = commandEditText.getText().toString();
         if (commandToExecute.isEmpty()) {
         outputTextView.append("שגיאה: נא הכנס פקודה לביצוע.\n");
         bupair.setEnabled(true);
         return;
         }*/
        //
        String command="";
        if(type.equals("pair")){
        command="/system/bin/sh -"+menv+adb+" kill-server\necho \""+pin+"\" | "+adb+" pair "+ipPort+"\nexit\nexit\n";
        }else if(type.equals("connect")){
            String cmddpmnew="";
            String disacc="";
            String enacc="";
            String acti="";
            String sec="";
            String check="";
            if(todo.contains("d")){
                disacc="\nlibadb.so -s "+ipPort+" shell \"cmd package query-services -a android.accounts.AccountAuthenticator | grep packageName | cut -d '=' -f 2 | tr -d '\\r' | sort -u | sed 's/^/pm disable-user --user -0 /' | sh\" < /dev/null\n";
            }
            if(todo.contains("e")){
                enacc="\nlibadb.so -s "+ipPort+" shell \"pm list packages -d | cut -d ':' -f 2 | tr -d '\\r' | sed 's/^/pm enable /' | sh 2>/dev/null\" < /dev/null\n";
            }
            if(todo.contains("a")){
                acti="\nlibadb.so -s "+ipPort+" shell dpm set-device-owner com.emanuelef.remote_capture.debug/com.emanuelef.remote_capture.activities.admin</dev/null\n";
            }
            if(todo.contains("s")){
                sec="\nlibadb.so -s "+ipPort+" shell pm grant "+con.getPackageName()+" android.permission.WRITE_SECURE_SETTINGS < /dev/null\n"+
                "libadb.so -s "+ipPort+" shell \"dumpsys package "+con.getPackageName()+" | grep WRITE_SECURE_SETTINGS\" < /dev/null\n";
                
                //sec="\nlibadb.so -s "+ipPort+" shell dpm set-device-owner com.emanuelef.remote_capture.debug/com.emanuelef.remote_capture.activities.admin</dev/null\n";
            }
            if(todo.contains("c")){
                check="\nlibadb.so -s "+ipPort+" shell \"dumpsys account | grep Account | grep type | cut -d '=' -f 3 | cut -d '}' -f 1\" < /dev/null\n";
            }
            //if(pin.equals("false")){
             //   cmddpmnew="\nlibadb.so -s "+ipPort+" shell dpm set-device-owner com.emanuelef.remote_capture.debug/com.emanuelef.remote_capture.activities.admin</dev/null\nexit\nexit\n";
            //}else if(pin.equals("true")){
                
                
                
                //cmddpmnew="\nlibadb.so -s "+ipPort+" shell "+disacc+"libadb.so -s "+ipPort+" shell "+"dpm set-device-owner com.emanuelef.remote_capture.debug/com.emanuelef.remote_capture.activities.admin</dev/null\n"+"libadb.so -s "+ipPort+" shell "+enacc+"\nexit\nexit\n";
                cmddpmnew=disacc+"sleep 3"+acti+enacc+sec+check+"\nexit\nexit\n";
            //}
            //String exc="/system/bin/sh -"+menv+adb+" kill-server\nlibadb.so disconnect\nlibadb.so connect "+edtxip.getText().toString()+":"+edtxport.getText().toString()+"\nlibadb.so disconnect\nlibadb.so connect "+edtxip.getText().toString()+":"+edtxport.getText().toString()+cmddpm;
            //cmddpmnew="\nlibadb.so -s "+ipPort+" shell dpm set-device-owner com.emanuelef.remote_capture.debug/com.emanuelef.remote_capture.activities.admin</dev/null\nexit\nexit\n";
            String exc="/system/bin/sh -"+menv+adb+" kill-server\nlibadb.so disconnect\nlibadb.so connect "+ipPort+"\nsleep 5\nlibadb.so devices\n"+cmddpmnew;
            command=exc;
        }
        final String commandToExecute=command;
        new Thread(new Runnable() {
                @Override
                public void run() {
                    
                    executeRootCommandInternal(commandToExecute, commandListener);
                }
            }).start();
    }

    public interface CommandOutputListener {
        void onOutputReceived(String line);
        void onErrorReceived(String line);
        void onCommandFinished(int exitCode, String finalOutput, String finalError);
    }
    String err="";
    Process process = null;
    private void executeRootCommandInternal(final String command, final CommandOutputListener listener) {
        err="";
        DataOutputStream os = null;
        final StringBuilder finalOutput = new StringBuilder();
        final StringBuilder finalErrorOutput = new StringBuilder();
        int exitCode = -1;

        try {
            process = Runtime.getRuntime().exec("sh");
            os = new DataOutputStream(process.getOutputStream());

            os.writeBytes(command + "\n");
            os.writeBytes("exit\n");
            os.flush();

            final AtomicBoolean processFinished = new AtomicBoolean(false);

            // Thread לקריאת Standard Output
            Thread outputReaderThread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        BufferedReader reader = null;
                        try {
                            reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                            String line;
                            while ((line = reader.readLine()) != null || !processFinished.get()) {
                                if (line != null) {
                                    if (listener != null) {
                                        listener.onOutputReceived(line);
                                    }
                                    finalOutput.append(line).append("\n");
                                } else {
                                    try {
                                        Thread.sleep(50);
                                    } catch (InterruptedException e) {
                                        Thread.currentThread().interrupt();
                                        break;
                                    }
                                }
                            }
                        } catch (IOException e) {
                            if (listener != null) {
                                listener.onErrorReceived("Output Stream Read Error: " + e.getMessage());
                            }
                            finalErrorOutput.append("Output Stream Read Error: ").append(e.getMessage()).append("\n");
                        } finally {
                            if (reader != null) {
                                try {
                                    reader.close();
                                } catch (IOException e) { /* ignore */ }
                            }
                        }
                    }
                });

            // Thread לקריאת Standard Error
            Thread errorReaderThread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        BufferedReader reader = null;
                        try {
                            reader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
                            String line;
                            while ((line = reader.readLine()) != null || !processFinished.get()) {
                                if (line != null) {
                                    if (listener != null) {
                                        listener.onErrorReceived(line);
                                    }
                                    finalErrorOutput.append(line).append("\n");
                                } else {
                                    try {
                                        Thread.sleep(50);
                                    } catch (InterruptedException e) {
                                        Thread.currentThread().interrupt();
                                        break;
                                    }
                                }
                            }
                        } catch (IOException e) {
                            if (listener != null) {
                                listener.onErrorReceived("Error Stream Read Error: " + e.getMessage());
                            }
                            finalErrorOutput.append("Error Stream Read Error: ").append(e.getMessage()).append("\n");
                        } finally {
                            if (reader != null) {
                                try {
                                    reader.close();
                                } catch (IOException e) { /* ignore */ }
                            }
                        }
                    }
                });

            outputReaderThread.start();
            errorReaderThread.start();

            exitCode = process.waitFor();
            processFinished.set(true);

            outputReaderThread.join();
            errorReaderThread.join();

        } catch (Exception e) {
            if (listener != null) {
                listener.onErrorReceived("Execution Exception: " + e.getMessage());
            }
            finalErrorOutput.append("Execution Exception: ").append(e.getMessage()).append("\n");
        } finally {
            try {
                /*bupair.setEnabled(true);
                 bucon.setEnabled(true);
                 buconmul.setEnabled(true);
                 buconmult.setEnabled(true);
                 budisaccmult.setEnabled(true);
                 buenaccmult.setEnabled(true);
                 budisacc.setEnabled(true);
                 buenacc.setEnabled(true);
                 buexecall.setEnabled(true);*/
                if (os != null) os.close();
                if (process != null) process.destroy();
            } catch (Exception e) {
                if (listener != null) {
                    listener.onErrorReceived("Resource Close Error: " + e.getMessage());
                }
                finalErrorOutput.append("Resource Close Error: ").append(e.getMessage()).append("\n");
            }
            if (listener != null) {
                listener.onCommandFinished(exitCode, finalOutput.toString(), finalErrorOutput.toString());
            }
        }
    }}
