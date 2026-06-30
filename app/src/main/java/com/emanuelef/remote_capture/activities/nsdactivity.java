package com.emanuelef.remote_capture.activities;
 
import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.content.Intent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.widget.Switch;
import android.os.Handler;
import android.app.NotificationManager;
import android.os.Build;
import android.app.NotificationChannel;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.RemoteInput;
import com.emanuelef.remote_capture.R;
import com.emanuelef.remote_capture.Utils;

public class nsdactivity extends Activity implements AdbNsdResolver.AdbServiceFoundListener {

    private AdbNsdResolver mAdbResolver;
    private TextView mStatusText,pairst,connst,resst,discost,logst,mrlog;
    private Button mDiscoveryButton,mconDiscoveryButton,mstopDiscobu;
    Switch swacti,swsec,swdisacc,swenacc,swcheckacc;
    private String storedconnect;
    String todo="";
    public static boolean ready=true;
    boolean refresh=false;
    //private NotificationHelper mNotificationHelper;
        
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Utils.setTheme(this);
      //  mNotificationHelper = new NotificationHelper(this);
        try{
            if(getActionBar().isShowing())
                getActionBar().hide();
        }catch(Exception e){}
        setContentView(R.layout.nsd_activity);
        try{
        //startForegroundService(new Intent(this,ser.class));
        }catch(Exception e){
            
        }
        mStatusText = (TextView) findViewById(R.id.status_text);
        pairst=findViewById(R.id.pairst);
        connst=findViewById(R.id.connst);
        resst=findViewById(R.id.resst);
        discost=findViewById(R.id.discost);
        logst=findViewById(R.id.logst);
        mrlog=findViewById(R.id.mrlog);
        
        mDiscoveryButton = (Button) findViewById(R.id.start_discovery_button);
        mconDiscoveryButton=findViewById(R.id.start_conn_discovery_button);
        mstopDiscobu=findViewById(R.id.stop_discoverys);
        swacti=findViewById(R.id.swacti);
        swsec=findViewById(R.id.swsec);
        swdisacc=findViewById(R.id.swdisacc);
        swenacc=findViewById(R.id.swenacc);
        swcheckacc=findViewById(R.id.swcheckacc);
        mAdbResolver = new AdbNsdResolver(this, this);
        IntentFilter filter = new IntentFilter("com.ands.ACTION_START_CONNECT_DISCOVERY");
        registerReceiver(mConnectStartReceiver, filter);
        mDiscoveryButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // ללא lambda
                    mStatusText.setText("מחפש שירות חיבור עם קוד");
                    pairst.setText("מחפש");
                    connst.setText("מחפש");
                    mAdbResolver.startDiscovery();
                    mAdbResolver.startConnectDiscovery();
                    refresh=true;
                    logres="";
                    mstartlog();
                    //mNotificationHelper.sendSimpleNotification("adb", "מתחיל", 104);
                }
            });
        mconDiscoveryButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(mAdbResolver.pairactive){
                        mAdbResolver.stopDiscovery();
                    }
                    mStatusText.setText("מחפש שירות חיבור רגיל");
                    connst.setText("מחפש");
                    pairst.setText("");
                    mAdbResolver.startConnectDiscovery();
                    refresh=true;
                    logres="";
                    mstartlog();
                    //mNotificationHelper.sendSimpleNotification("adb", "מתחיל", 104);
                    
                }
            });
        mstopDiscobu.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mAdbResolver.stopDiscovery();
                    mAdbResolver.stopconDiscovery();
                    refresh=false;
                    mStatusText.setText("גילוי כבוי");
                    pairst.setText("");
                    connst.setText("");
                    discost.setText("גילוי כבוי");
                    logres="";
                }
            });
        mrlog.setOnClickListener(new View.OnClickListener(){

                @Override
                public void onClick(View p1) {
                    try{
                        logst.setText(logres);
                    }catch(Exception e){
                        LogUtil.logToFile(e.toString());
                    }
                }
            });
        mContext = this;
        // קבלת שירות ה-NotificationManager
        mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        // חובה ליצור ערוץ התראות ב-API 26 ומעלה
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel();
        }
    }
    public static String logres="";
    boolean last=false;
    Handler mhan=new Handler();
    Runnable r=null;
    void mstartlog(){
        
        if(r==null){
        r=new Runnable(){
                @Override
                public void run() {
                    if(last){
                        last=false;
                        mhan.removeCallbacks(r);
                        r=null;
                        return;
                    }
                    if(!refresh){
                        last=true;
                        mhan.postDelayed(this,5000);
                    }
                        try{
                        logst.setText(logres);
                        }catch(Exception e){
                            LogUtil.logToFile(e.toString());
                        }
                        mhan.postDelayed(this,1000);
                
                    
                }
            };
        boolean debug=true;
        if(debug)
        mhan.post(r);
        }
    }
        // 💡 מופע של BroadcastReceiver חדש
        private BroadcastReceiver mConnectStartReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if ("com.ands.ACTION_START_CONNECT_DISCOVERY".equals(intent.getAction())) {
                    if (intent.getBooleanExtra("PAIR_SUCCESS", false)) {
                        mStatusText.setText("צימוד הצליח! עובר לחיבור");
                        pairst.setText("הצליח!");
                        resst.setText("צימוד הצליח");
                        // 🛑 מפעיל את ה-CONNECT DISCOVERY
                        //mAdbResolver.startConnectDiscovery();
                        mAdbResolver.stopDiscovery();
                        discost.setText("גילוי חיבור עם קוד התאמה כבוי");
                        ready=true;
                        /*
                        if(storedconnect!=null){
                            todo=(swdisacc.isChecked()?"d":"")+(swenacc.isChecked()?"e":"")+(swacti.isChecked()?"a":"");
                            if(!todo.equals("")){
                            if(ready){
                                connst.setText("מריץ");
                                new myadb(MainActivity.this,storedconnect,"","connect",todo);
                                    ready=false;
                            }else
                                LogUtil.logToFile("isnt ready...");
                            }else{
                                mStatusText.setText("לא נבחר מה לעשות");
                            }
                        }else{
                            mStatusText.setText("תסגור ותפעיל שוב adb אלחוטי בהגדרות!");
                        }*/
                        mconnectrun();
                    }else if (intent.getBooleanExtra("dpm_SUCCESS", false)){
                        mStatusText.setText("mdm הצליח");
                        sendSimpleNotification("mdm","mdm הופעל בהצלחה",101);
                        mAdbResolver.stopDiscovery();
                        mAdbResolver.stopconDiscovery();
                        refresh=false;
                        discost.setText("גילוי כבוי");
                    }else{
                        String res= intent.getStringExtra("adb_res");
                        String txtres="";
                        if (res!=null){
                            ready=true;
                            if(res.equals("fail")){
                                mStatusText.setText("מנסה שוב...");
                                if(mAdbResolver.pairactive){
                                    mStatusText.setText("נסה שוב לפתוח חיבור עם קוד התאמה");
                                }else{
                                    mconnectrun();
                                }
                            }else{
                                if(res.contains("d")){
                                    txtres+="השבתת חשבונות ";
                                }
                                if(res.contains("a")){
                                    txtres+="הפעלת חסימה ";
                                }
                                if(res.contains("e")){
                                    txtres+="הפעלת חשבונות ";
                                }
                                if(res.contains("s")){
                                    txtres+="אישור נגישות ";
                                }
                                mStatusText.setText("הצליח");
                                resst.setText(txtres);
                                connst.setText("הצליח");
                                mAdbResolver.stopDiscovery();
                                mAdbResolver.stopconDiscovery();
                                refresh=false;
                                discost.setText("גילוי כבוי");
                            }
                            
                        }
                    }
                }
            }
        };

        

        @Override
        protected void onDestroy() {
            // 💡 ביטול הרשמה
            unregisterReceiver(mConnectStartReceiver);
            mAdbResolver.stopDiscovery();
            mAdbResolver.stopconDiscovery();
            refresh=false;
            super.onDestroy();
        }
        // ... (שאר המתודות)
    
    @Override
    protected void onPause() {
        // עצירת הדיסקברי כשיוצאים מהאפליקציה
        if (mAdbResolver != null) {
           // mAdbResolver.stopDiscovery();
        }
        super.onPause();
    }
   
// MainActivity.java

      
// ... (בתוך runAdbCommand ודא שאתה מריץ `adb connect` כפי שמופיע בקוד שלך)
        @Override
        public void onAdbServiceResolved(String ipAddress, int port,final String type) {
            final String connectionTarget = ipAddress + ":" + port;

            runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if(type.equals(AdbNsdResolver.SERVICE_TYPE_ADB_PAIRING)){
                            // ... (הלוגיקה הקיימת ל-PAIRING - מציג IP/Port ומפעיל התראת PIN)
                            //mNotificationHelper.sendSimpleNotification("גילוי adb", "גילוי", 104);
                            mStatusText.setText("נמצא שירות חיבור עם קוד התאמה, מפעיל התראה");
                            pairst.setText("מחכה לקוד התאמה בהתראה...");
                            sendPairingNotificationWithInput(connectionTarget);
                            // 🛑 עוצרים את גילוי ה-Pairing
                            //mAdbResolver.stopDiscovery();

                        }else if(type.equals(AdbNsdResolver.SERVICE_TYPE_ADB_CONNECTING)){
                            // 💡 לוגיקה חדשה ל-CONNECT לאחר שה-Pairing הצליח
                            mStatusText.setText("נמצא שירות חיבור");
                            connst.setText("שומר יעד");
                            //store the connect ip port to run after pairing successful
                            storedconnect=connectionTarget;
                            // 🛑 הפעלת פקודת ה-CONNECT
                            /*if(!mAdbResolver.pairactive){
                                todo=(swdisacc.isChecked()?"d":"")+(swenacc.isChecked()?"e":"")+(swacti.isChecked()?"a":"");
                                if(!todo.equals("")){
                                if(ready){
                                        connst.setText("מריץ");
                                    new myadb(MainActivity.this,connectionTarget,"","connect",todo);
                                    ready=false;
                                }else
                                    LogUtil.logToFile("isnt ready...");
                                }else{
                                    mStatusText.setText("לא נבחר מה לעשות");
                                }
                                
                            }*/
                            mconnectrun();
                            // אין צורך לעצור את ה-Connect Discovery כרגע.
                        }
                        
                        LogUtil.logToFile(connectionTarget);
                        /*
                        mStatusText.setText("סטטוס: שירות ADB נמצא ב- " + connectionTarget);
                        mNotificationHelper.sendNotification(
                            "ADB Connect Ready", 
                            "ה-IP והפורט נמצאו: " + connectionTarget
                        );
                        
                        // 1. הפעלת פקודת ה-CONNECT
                        runAdbCommand(connectionTarget); 

                        // 2. עדכון פקודת ה-ADB המוצגת
                        mAdbCommandText.setText("מנסה חיבור ל: " + connectionTarget);
                        */
                    }
                });
        }
        private void mconnectrun(){
            if(!mAdbResolver.pairactive){
                if(storedconnect!=null){
                    todo=(swdisacc.isChecked()?"d":"")+(swenacc.isChecked()?"e":"")+(swacti.isChecked()?"a":"")+(swsec.isChecked()?"s":"")+(swcheckacc.isChecked()?"c":"");
                    if(!todo.equals("")){
                        if(ready){
                            connst.setText("מריץ");
                            new myadb(nsdactivity.this,storedconnect,"","connect",todo);
                            ready=false;
                        }else
                            LogUtil.logToFile("isnt ready...");
                    }else{
                        mStatusText.setText("לא נבחר מה לעשות");
                    }
                }else{
                    mStatusText.setText("תסגור ותפעיל שוב adb אלחוטי בהגדרות!");
                }
            }
        }
        // ... (המשך onDiscoveryFailed כפי שהיה)

        /**
         * הפונקציה המעודכנת להרצת פקודת ADB מתוך הליב המקומי.
         * @param connectionTarget כתובת ה-IP והפורט (לדוגמה: 192.168.1.10:44445).
         */
         /*
        private void runAdbCommand(String connectionTarget) {

            // 💡 שלב 1: מציאת נתיב הבינארי ADB
            // הנתיב המדויק תלוי באופן שבו ה-libadb.so מופעל כבינארי.
            // אם זה דרך ה-JNI, אולי זה לא נתיב קובץ בינארי רגיל.

            // נניח שאתה משתמש בנתיב `getFilesDir()` או `nativeLibraryDir`
            String adbBinaryPath = getApplicationInfo().nativeLibraryDir + "/adb"; 
            // **או** אם ה-adb הועתק לספריית הקבצים:
            // String adbBinaryPath = getFilesDir().getAbsolutePath() + "/adb";

            // ודא שהקובץ קיים ויש לו הרשאות הרצה (chmod)

            try {
                // 💡 שלב 2: בניית הפקודה המלאה
                // אם אתה מריץ Pairing, שנה ל: adbBinaryPath, "pair", connectionTarget, "קוד זיווג"
                String[] command = {
                    adbBinaryPath, 
                    "connect", 
                    connectionTarget
                };
                //String[] command = {adbBinaryPath, "pair", connectionTarget, "123456"}; // 123456 הוא ה-PIN
                // יצירת התהליך
                Process process = Runtime.getRuntime().exec(command);

                // 💡 שלב 3: קריאת פלט התהליך (חשוב מאוד לדיבוג)

                // קריאת פלט רגיל (Standard Output)
                final String output = readStream(process.getInputStream());
                // קריאת שגיאות (Error Output)
                final String error = readStream(process.getErrorStream());

                final int exitCode = process.waitFor();

                // עדכון ממשק המשתמש עם תוצאות ההרצה
                runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mStatusText.setText(
                                "תוצאת ADB (קוד: " + exitCode + ")\n" +
                                "פלט: " + output.trim() + "\n" +
                                "שגיאה: " + error.trim()
                            );
                        }
                    });

            } catch (final Exception e) {
                runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mStatusText.setText("שגיאת הרצת ADB: " + e.getMessage());
                        }
                    });
            }
        }
*/
        /**
         * פונקציית עזר לקריאת פלט מתוך InputStream (ללא תלויות נוספות)
         */
         /*
        private String readStream(java.io.InputStream is) throws java.io.IOException {
            java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(is));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
            return sb.toString();
        }*/
    
    // 🎯 יישום ממשק ה-AdbServiceFoundListener



    @Override
    public void onDiscoveryFailed(final String message) {
        // עדכון ממשק המשתמש
        runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    discost.setText("סטטוס: גילוי נכשל: " + message);
                    //mNotificationHelper.sendSimpleNotification("שגיאת ADB", "גילוי נכשל.", 104);
                    LogUtil.logToFile(message);
                  /*  mStatusText.setText("סטטוס: גילוי נכשל: " + message);
                    mNotificationHelper.sendNotification(
                        "ADB Discovery Failed", 
                        "שגיאה: " + message
                    );*/
                }
            });
    }
    private static final String CHANNEL_ID = "adb_status_channel";
    private static final CharSequence CHANNEL_NAME = "ADB Status Notifications";
    private static final int NOTIFICATION_ID_PAIRING = 101;
    private Context mContext;
    private NotificationManager mNotificationManager;
    
    
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            int importance = NotificationManager.IMPORTANCE_MAX;

            // יצירת אובייקט NotificationChannel
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID, 
                CHANNEL_NAME, 
                importance
            );
            channel.setDescription("התראות עבור סטטוס חיבור ADB.");

            // רישום הערוץ
            mNotificationManager.createNotificationChannel(channel);
        }
    }
    public void sendSimpleNotification(String title, String text, int id) {
        // ... (הקוד של sendSimpleNotification נשאר זהה)
        Notification.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder = new Notification.Builder(mContext, CHANNEL_ID);
        } else {
            builder = new Notification.Builder(mContext);
        }

        builder.setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setAutoCancel(true); 

        mNotificationManager.notify(id, builder.build());
    }
    public void sendPairingNotificationWithInput(String ipPort) {

        // 1. הגדרת שדה הקלט (RemoteInput)
        RemoteInput remoteInput = new RemoteInput.Builder(PairingReceiver.KEY_TEXT_REPLY)
            .setLabel("הכנס קוד זיווג ADB (PIN)")
            .build();

        // 2. הגדרת ה-Intent שיופעל ב-PairingReceiver
        Intent replyIntent = new Intent(mContext, PairingReceiver.class);
        replyIntent.putExtra(PairingReceiver.EXTRA_IP_PORT, ipPort);

        // 3. הגדרת ה-PendingIntent
        PendingIntent replyPendingIntent = 
            PendingIntent.getBroadcast(
            mContext,
            0, // Request Code
            replyIntent,
            PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= 31 ? 33554432 : 0)
        );

        // 4. יצירת ה-Action (הכפתור + הקלט)
        Notification.Action action = new Notification.Action.Builder(
            android.R.drawable.ic_menu_send, 
            "שלח קוד",
            replyPendingIntent
        )
            .addRemoteInput(remoteInput) // הוספת שדה הקלט
            .build();

        // 5. בניית ההתראה
        Notification.Builder builder;
        // ... (בניית הבנאי כפי שמופיע בקוד המקורי)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder = new Notification.Builder(mContext, CHANNEL_ID);
        } else {
            builder = new Notification.Builder(mContext);
        }

        builder.setContentTitle("ADB Pairing נדרש")
            .setContentText("אנא הזן את קוד ה-PIN שמופיע על המסך השני:")
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setAutoCancel(true)
            .setPriority(Notification.PRIORITY_MAX)
            .addAction(action); // 🛑 הוספת האקשן עם שדה הקלט

        // 6. שליחה (send)
        mNotificationManager.notify(NOTIFICATION_ID_PAIRING, builder.build());
    }
}
