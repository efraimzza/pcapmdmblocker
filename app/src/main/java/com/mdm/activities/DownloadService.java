package com.mdm.activities;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import java.io.File;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.URL;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.cert.X509Certificate;
import com.emanuelef.remote_capture.activities.LogUtil;
import java.net.HttpURLConnection;
import java.net.URLConnection;
import android.content.pm.PackageInstaller;
import java.util.List;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.security.cert.CertificateException;
import java.util.Iterator;
import org.json.JSONObject;
import java.util.concurrent.ConcurrentHashMap;
import android.os.Looper;
import android.os.Handler;
//import java.net.HttpURLConnection;

public class DownloadService extends Service {

    public static final String ACTION_START_DOWNLOAD = "com.mdm.action.START_DOWNLOAD";
    public static final String ACTION_CANCEL_DOWNLOAD = "com.mdm.action.CANCEL_DOWNLOAD";
    public static final String EXTRA_URL = "extra_url";
    public static final String EXTRA_PKG = "extra_pkgname";
    public static final String EXTRA_TYPE = "extra_type";

    private static final String CHANNEL_ID = "download_channel";
    private static final int NOTIFICATION_ID = 1001;

    private final IBinder binder = new LocalBinder();
    private NotificationManager notificationManager;

    // משתני מצב חשופים עבור ה-Activity
    public boolean isDownloading = false;
    //public String currentFileName = "";
    public long totalBytes = 0;
    public long bytesDownloaded = 0;
    public long bytesPerSecond = 0;
    private long lastBytesCalculated = 0;
    private long lastTimeCalculated = 0;
    public String state="";

    private boolean isCanceled = false;
    private Thread downloadThread;
    DownloadService instanc=null;

    public class LocalBinder extends Binder {
        public DownloadService getService() {
            return instanc;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();

        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        createNotificationChannel();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }
    private static final ConcurrentHashMap<String, String[]> qitems = new ConcurrentHashMap<>();
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.getAction() != null && intent.getStringExtra(EXTRA_PKG) != null) {
            instanc=this;
            String action = intent.getAction();
            if (action.equals(ACTION_START_DOWNLOAD)) {
                LogUtil.logToFile("isdown="+isDownloading+"iscanc="+isCanceled);
                String murl = intent.getStringExtra(EXTRA_URL);
                String pkgname = intent.getStringExtra(EXTRA_PKG);
                String type = intent.getStringExtra(EXTRA_TYPE);
                if (!isDownloading) {
                    utils.clearDownloadDirectory(this);
                    try {
                        List<PackageInstaller.SessionInfo> lses = this.getPackageManager().getPackageInstaller().getAllSessions();
                        if (lses != null) {
                            for (PackageInstaller.SessionInfo pses : lses) {
                                if (pses != null && pses.getInstallerPackageName() != null) {
                                    if (pses.getInstallerPackageName().equals(this.getPackageName())) {
                                        this.getPackageManager().getPackageInstaller().abandonSession(pses.getSessionId());
                                    }
                                }
                            }
                        }
                    } catch (Exception e) {
                        LogUtil.logToFile("" + e);
                    }
                    executeDownload( murl, pkgname, type);
                }else{
                    //put to list queue
                    //key is pkgname, value is link & type
                    LogUtil.logToFile("a-q="+qitems.containsKey(pkgname)+"curpn="+pkgname.equals(packageName)+pkgname);
                    if(!qitems.containsKey(pkgname)&&!pkgname.equals(packageName)){
                        LogUtil.logToFile("b-q="+qitems.containsKey(pkgname)+"curpn="+pkgname.equals(packageName)+pkgname);
                        LogUtil.logToFile(pkgname);
                        qitems.put(pkgname,new String[]{murl,type});
                    }else{
                        LogUtil.logToFile("c-q="+qitems.containsKey(pkgname)+"curpn="+pkgname.equals(packageName)+pkgname);
                        LogUtil.logToFile("already have the same pkg key..."+pkgname);
                    }
                }
            } else if (action.equals(ACTION_CANCEL_DOWNLOAD)) {
                cancelDownload(intent.getStringExtra(EXTRA_PKG),false);
            }
            return START_STICKY;
        }
        return START_NOT_STICKY;
    }
    public static final boolean isinqueue(String pkgname){
        return qitems.containsKey(pkgname);
    }
    private static long lastBytes = 0;
    String finalFilename = "";
    String packageName="";
    String driveUrl="";
    Handler handler=new Handler(Looper.getMainLooper());
    private void executeDownload(final String murl,final String pkgname, final String type) {
        isDownloading = true;
        isCanceled = false;
        //currentFileName = "";
        finalFilename = "";
        driveUrl="";
        bytesDownloaded = 0;
        totalBytes = 0;
        packageName=pkgname;
        // התחלת ה-Foreground Service עם התראה ראשונית
        startForeground(NOTIFICATION_ID, buildNotification("מכין הורדה...", 0));
        state="מכין הורדה...";
        try{
            Runnable r = new Runnable() {
                @Override
                public void run() {
                    try{
                        /*Intent intent = new Intent(mactivity, DownloadService.class);
                         mactivity. bindService(intent,mactivity.serviceConnection, Context.BIND_AUTO_CREATE);
                         */
                        //dont bind beacause has can bind when ui is stopped & isnt have onstop to make binder null to stop the runnable when isnt in ui...
                        LogUtil.logToFile("connect from download service");//if isnt already connected
                        storeActivity.startUiUpdater();
                    }catch(Throwable t){LogUtil.logToFile(t);}
                }};
            handler.post(r);
        }catch(Throwable t){LogUtil.logToFile(t);}
        downloadThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    HttpsURLConnection conn = null;
                    HttpsURLConnection preCheck = null;
                    HttpURLConnection preCheckHttp = null;
                    if(type.equals("drive")){
                        try {
                            // // שלב 1: השגת דף האישור וחילוץ שם, UUID ואישור
                            //get file name & generate link
                            String initialUrl = "https://drive.google.com/uc?export=download&id=" + murl;
                            conn = (HttpsURLConnection) new URL(initialUrl).openConnection();
                            TrustManager[] trustAllCerts = new TrustManager[]{
                                new X509TrustManager() {
                                    public java.security.cert.X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
                                    @Override public void checkClientTrusted(X509Certificate[] certs, String authType) throws CertificateException {}
                                    @Override public void checkServerTrusted(X509Certificate[] certs, String authType) throws CertificateException {}
                                }
                            };
                            SSLContext sslContext = SSLContext.getInstance("TLS");
                            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
                            SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();

                            conn.setSSLSocketFactory(sslSocketFactory);
                            conn.connect();

                            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                            StringBuilder html = new StringBuilder();
                            String line;
                            while ((line = reader.readLine()) != null) {
                                html.append(line);
                            }
                            conn.disconnect();
                            conn=null;
                            String htmlStr = html.toString();
                            String confirm =utils.extractValue(htmlStr, "name=\"confirm\" value=\"", "\"");
                            String uuid = utils.extractValue(htmlStr, "name=\"uuid\" value=\"", "\"");
                            // // חילוץ שם הקובץ מה-HTML (נמצא בתוך ה-class "uc-name-size")
                            finalFilename = utils.extractValue(htmlStr, "class=\"uc-name-size\"><a href=\"/open?id=" + murl + "\">", "</a>");
                            //LogUtil.logToFile(finalFilename);
                            //LogUtil.logToFile(htmlStr);
                            if (finalFilename == null || finalFilename.isEmpty()) finalFilename = "downloaded_file.apk";
                            // // שלב 2: בדיקת קובץ זמני קיים
                            File tempFile = new File(getExternalFilesDir(""), finalFilename + ".tmp");
                            long existingSize = 0;
                            if (tempFile.exists()) {
                                existingSize = tempFile.length();
                            }
                            //link
                            driveUrl = "https://drive.usercontent.google.com/download?id=" + murl 
                                + "&export=download&confirm=" + confirm + "&uuid=" + uuid;
                            // downloadWithResume(fileId, fileName, url, existingSize);

                        } catch (Exception e) {
                            LogUtil.logToFile(e);
                            //showError(fileId, "שגיאה בחיבור הראשוני");
                            //continue qitems...
                            quconti();
                        } finally {
                            if (conn != null) conn.disconnect();

                            //stopForeground(true);
                            //isDownloading = false;
                            //stopSelf();

                        }
                    }else if(type.equals("normal")){
                        try {

                            URL url = new URL(murl);
                            URLConnection uc=url.openConnection();
                            /* if(uc instanceof HttpURLConnection){
                             preCheckHttp=(HttpURLConnection) uc;
                             preCheckHttp.setConnectTimeout(5000);
                             preCheckHttp.setRequestMethod("HEAD"); // // בקשת HEAD לא מורידה את הקובץ, רק headers //
                             preCheckHttp.connect();

                             finalFilename =utils.getFilenameFromHeaderHttp(preCheckHttp);
                             preCheckHttp.disconnect();
                             preCheckHttp=null;
                             }else if(uc instanceof HttpsURLConnection){*/
                            preCheck=(HttpsURLConnection) uc;


                            //preCheck = (HttpsURLConnection) url.openConnection();
                            // // כאן צריך להוסיף את ה-SSLContext שלך //
                            TrustManager[] trustAllCerts = new TrustManager[]{
                                new X509TrustManager() {
                                    public java.security.cert.X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
                                    @Override public void checkClientTrusted(X509Certificate[] certs, String authType) throws CertificateException {}
                                    @Override public void checkServerTrusted(X509Certificate[] certs, String authType) throws CertificateException {}
                                }
                            };
                            SSLContext sslContext = SSLContext.getInstance("TLS");
                            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
                            SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();

                            preCheck.setSSLSocketFactory(sslSocketFactory);
                            preCheck.setConnectTimeout(5000);
                            preCheck.setRequestMethod("HEAD"); // // בקשת HEAD לא מורידה את הקובץ, רק headers //
                            preCheck.connect();

                            finalFilename =utils.getFilenameFromHeader(preCheck);
                            preCheck.disconnect();
                            preCheck=null;
                            //}
                        } catch (Exception e) {
                            // // אם HEAD נכשל, נמשיך עם השם המקורי //

                            LogUtil.logToFile("head err="+e.toString());
                            //cancelDownload(fileurl);
                            LogUtil.logToFile(e);
                            /*isDownloading = false;
                             stopForeground(true);
                             stopSelf();*/
                        } finally {
                            if (preCheck != null) preCheck.disconnect();
                            if (preCheckHttp != null) preCheckHttp.disconnect();
                            //stopForeground(true);
                            //isDownloading = false;
                            //stopSelf();

                        }
                        LogUtil.logToFile(finalFilename);
                        if(finalFilename.equals("")){
                            try{
                                finalFilename= utils.getFileNameFromUrl(murl);
                            }catch(Exception e){
                                LogUtil.logToFile(e.toString());
                                if(finalFilename.equals("")){
                                    //continue qitems...
                                    quconti();
                                }
                            }
                        }
                        LogUtil.logToFile(finalFilename);
                    }


                    boolean downloadSuccess = false;
                    if(type.equals("gplay")){
                        try{
                            if(murl.equals("")) return;
                            JSONObject json = new JSONObject(murl);
                            Iterator<String> its=json.keys();
                            while(its.hasNext()){

                                finalFilename=its.next();
                                state="מוריד את "+finalFilename;
                                //final String filename=finalFilename;//for final require for prg dialog in runnable...

                                final String fileurl=json.getString(finalFilename);

                                LogUtil.logToFile(finalFilename);
                                downloadSuccess = false;
                                bytesDownloaded = 0;
                                totalBytes = 0;

                                /*if(finalFilename.equals("")){
                                 LogUtil.logToFile("head err="+finalFilename);
                                 cancelDownload(fileurl);
                                 return false;
                                 }*/

                                //finalFilename="Activity Launcher_2.1.6_APKPure.xapk";
                                /*
                                 //isnt secure - can replace the files with the same file name...
                                 //only with pwd...
                                 if(new File(context.getExternalFilesDir(""), finalFilename).exists()){
                                 oncon(context, context.getExternalFilesDir("")+"/"+ finalFilename);
                                 return true;
                                 }*/
                                // // נתיב הקובץ הזמני מבוסס על השם שחולץ //
                                new File(getExternalFilesDir("")+"/"+pkgname).mkdirs();
                                File tempFile = new File(getExternalFilesDir("")+"/"+pkgname, finalFilename + ".tmp");
                                //   while (retryCount < maxRetries && !downloadSuccess) {
                                if (isCanceled) break;
                                InputStream input = null;
                                RandomAccessFile output = null;
                                HttpsURLConnection connection = null;
                                try {
                                    //LogUtil.logToFile(driveUrl);
                                    URL url = new URL(fileurl);
                                    connection = (HttpsURLConnection) url.openConnection();
                                    // // הוספת SSLContext שוב לחיבור הראשי //
                                    TrustManager[] trustAllCerts = new TrustManager[]{
                                        new X509TrustManager() {
                                            public java.security.cert.X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
                                            @Override public void checkClientTrusted(X509Certificate[] certs, String authType) throws CertificateException {}
                                            @Override public void checkServerTrusted(X509Certificate[] certs, String authType) throws CertificateException {}
                                        }
                                    };
                                    SSLContext sslContext = SSLContext.getInstance("TLS");
                                    sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
                                    SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();

                                    connection.setSSLSocketFactory(sslSocketFactory);
                                    long existingFileSize = 0;
                                    //if (tempFile.exists()) {
                                    //existingFileSize = tempFile.length();
                                    // // בקשת המשך מהנקודה הקיימת //
                                    //connection.setRequestProperty("Range", "bytes=" + existingFileSize + "-");
                                    //}else{
                                    connection.setRequestProperty("Connection", "Close");
                                    //}
                                    connection.setConnectTimeout(30000);
                                    connection.setReadTimeout(60000);

                                    connection.connect();
                                    int responseCode = connection.getResponseCode();
                                    //LogUtil.logToFile("rc="+responseCode);
                                    if (
                                        responseCode == HttpsURLConnection.HTTP_OK 
                                    //|| responseCode == HttpsURLConnection.HTTP_PARTIAL
                                        ) {
                                        output = new RandomAccessFile(tempFile, "rw");

                                        //if (responseCode == HttpsURLConnection.HTTP_OK) {
                                        existingFileSize = 0;
                                        output.setLength(0); 
                                        //} else {
                                        //  output.seek(existingFileSize);
                                        //}

                                        // // עדכון הגודל הכולל לצורך הדיאלוג //
                                        totalBytes = (int) (connection.getContentLength() + existingFileSize);
                                        input = connection.getInputStream();

                                        byte[] data = new byte[8192];
                                        bytesDownloaded = existingFileSize;
                                        lastBytesCalculated = bytesDownloaded;
                                        lastTimeCalculated = System.currentTimeMillis();
                                        int count;

                                        long lastNotificationUpdate = 0;

                                        while ((count = input.read(data)) != -1) {
                                            if (isCanceled) break;

                                            output.write(data, 0, count);
                                            bytesDownloaded += count;

                                            long currentTime = System.currentTimeMillis();
                                            // חישוב מהירות ועדכון התראה פעם בשנייה
                                            if (currentTime - lastNotificationUpdate > 1000) {
                                                long bytesInLastSecond = bytesDownloaded - lastBytes;
                                                lastBytes = bytesDownloaded;
                                                //String speedStrr = formatFileSize(bytesInLastSecond) + "/s";
                                                String etaStr = "מחשב...";
                                                if (bytesInLastSecond > 0 && totalBytes > 0) {
                                                    long bytesRemaining = totalBytes - bytesDownloaded;
                                                    long secondsRemaining = bytesRemaining / bytesInLastSecond;

                                                    // // פורמט זמן קריא (דקות:שניות) //
                                                    if (secondsRemaining < 60) {
                                                        etaStr = secondsRemaining + " שניות";
                                                    } else {
                                                        etaStr = (secondsRemaining / 60) + " דקות ו-" + (secondsRemaining % 60) + " שניות";
                                                    }
                                                }

                                                calculateSpeed(currentTime);
                                                int progress = (totalBytes > 0) ? (int) ((bytesDownloaded * 100) / totalBytes) : -1;

                                                String speedStr = formatFileSize(bytesPerSecond) + "/s";
                                                notificationManager.notify(NOTIFICATION_ID, buildNotification("מוריד: " + progress + "% | " + speedStr + " | נותר: " + etaStr, progress));
                                                lastNotificationUpdate = currentTime;
                                            }
                                        }
                                        if (!isCanceled) {
                                            downloadSuccess = true;
                                            // // סיום מוצלח: שינוי שם לקובץ סופי ללא .tmp //
                                            tempFile.renameTo(new File(getExternalFilesDir("")+"/"+pkgname, finalFilename));

                                        }else{
                                            quconti();
                                        }

                                    }else{
                                        quconti();
                                    }
                                } catch (Exception e) {
                                    //  retryCount++;
                                    LogUtil.logToFile(" "+e.toString());
                                    // // הודעת לוג על ניסיון המשך //
                                    try {
                                        if (output != null) output.close();
                                        if (input != null) input.close();
                                        if (connection != null) connection.disconnect();
                                    } catch (Exception ignored) {}//before breaking...
                                    //continue qitems...
                                    //quconti();
                                    downloadSuccess=false;//instead of quconti - to make sure to qconti after break
                                    break; //break the "for"
                                } finally {
                                    try {
                                        if (output != null) output.close();
                                        if (input != null) input.close();
                                        if (connection != null) connection.disconnect();
                                    } catch (Exception ignored) {}
                                    notificationManager.cancel(NOTIFICATION_ID);
                                }
                                //    }

                            }
                            if (downloadSuccess) {
                                utils.oncon(DownloadService.this, getExternalFilesDir("") + "/"+pkgname,true);
                            }else{
                                quconti();
                            }

                        }catch(Throwable t){
                            LogUtil.logToFile(t);
                            //continue qitems...
                            quconti();
                        }finally{
                            notificationManager.cancel(NOTIFICATION_ID);
                        }
                    }else{
                        //final String filename=finalFilename;
                        state="מוריד את "+finalFilename;
                        InputStream input = null;
                        RandomAccessFile output = null;
                        HttpsURLConnection connection = null;
                        HttpURLConnection httpconnection = null;

                        try {
                            // הגדרת אמון SSL (כפי שהיה בקוד המקורי שלך)
                            TrustManager[] trustAllCerts = new TrustManager[]{
                                new X509TrustManager() {
                                    public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
                                    @Override public void checkClientTrusted(X509Certificate[] certs, String authType) {}
                                    @Override public void checkServerTrusted(X509Certificate[] certs, String authType) {}
                                }
                            };
                            SSLContext sslContext = SSLContext.getInstance("TLS");
                            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
                            SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();

                            URL url = new URL(type.equals("drive")?driveUrl:murl);
                            URLConnection u=url.openConnection();
                            if(u instanceof HttpURLConnection){
                                httpconnection=(HttpURLConnection) u;
                                httpconnection.setConnectTimeout(30000);
                                httpconnection.setReadTimeout(60000);
                            }else if(u instanceof HttpsURLConnection){
                                connection=(HttpsURLConnection) u;
                                //connection = (HttpsURLConnection) url.openConnection();
                                connection.setSSLSocketFactory(sslSocketFactory);
                                connection.setConnectTimeout(30000);
                                connection.setReadTimeout(60000);
                            }
                            File tempFile = new File(getExternalFilesDir(""), finalFilename + ".tmp");
                            long existingFileSize = 0;
                            // if (tempFile.exists()) {
                            // existingFileSize = tempFile.length();
                            // if(u instanceof HttpURLConnection){
                            //  httpconnection.setRequestProperty("Range", "bytes=" + existingFileSize + "-");
                            //}else if(u instanceof HttpsURLConnection){
                            // connection.setRequestProperty("Range", "bytes=" + existingFileSize + "-");
                            // }
                            //}else{
                            if(u instanceof HttpURLConnection){
                                httpconnection.setRequestProperty("Connection", "Close");
                            }else if(u instanceof HttpsURLConnection){
                                connection.setRequestProperty("Connection", "Close");
                            }

                            //}
                            LogUtil.logToFile("5");
                            int responseCode = 0;
                            if(u instanceof HttpURLConnection){
                                LogUtil.logToFile("1h");
                                httpconnection.connect();
                                LogUtil.logToFile("2h");
                                responseCode = httpconnection.getResponseCode();
                                LogUtil.logToFile("rc="+responseCode);
                                LogUtil.logToFile("3h");
                            }else if(u instanceof HttpsURLConnection){
                                LogUtil.logToFile("5s");
                                connection.connect();
                                LogUtil.logToFile("6");
                                responseCode = connection.getResponseCode();
                                LogUtil.logToFile("7"+responseCode);
                            }
                            if (responseCode == HttpsURLConnection.HTTP_OK
                            // || responseCode == HttpsURLConnection.HTTP_PARTIAL
                                ) {
                                output = new RandomAccessFile(tempFile, "rw");
                                // if (responseCode == HttpsURLConnection.HTTP_OK) {
                                LogUtil.logToFile("6");
                                existingFileSize = 0;
                                output.setLength(0);
                                //} else {
                                //  output.seek(existingFileSize);
                                // }
                                if(u instanceof HttpURLConnection){
                                    LogUtil.logToFile("1");
                                    totalBytes = httpconnection.getContentLength() + existingFileSize;
                                    LogUtil.logToFile("2");
                                    input = httpconnection.getInputStream();
                                }else if(u instanceof HttpsURLConnection){
                                    LogUtil.logToFile("1");
                                    totalBytes = connection.getContentLength() + existingFileSize;
                                    LogUtil.logToFile("2");
                                    input = connection.getInputStream();
                                }
                                LogUtil.logToFile("3");
                                byte[] data = new byte[8192];
                                bytesDownloaded = existingFileSize;
                                lastBytesCalculated = bytesDownloaded;
                                lastTimeCalculated = System.currentTimeMillis();
                                int count;
                                long lastNotificationUpdate = 0;
                                while ((count = input.read(data)) != -1) {
                                    if (isCanceled) break;
                                    output.write(data, 0, count);
                                    bytesDownloaded += count;
                                    long currentTime = System.currentTimeMillis();
                                    // חישוב מהירות ועדכון התראה פעם בשנייה
                                    if (currentTime - lastNotificationUpdate > 1000) {
                                        long bytesInLastSecond = bytesDownloaded - lastBytes;
                                        lastBytes = bytesDownloaded;
                                        //String speedStrr = formatFileSize(bytesInLastSecond) + "/s";
                                        String etaStr = "מחשב...";
                                        if (bytesInLastSecond > 0 && totalBytes > 0) {
                                            long bytesRemaining = totalBytes - bytesDownloaded;
                                            long secondsRemaining = bytesRemaining / bytesInLastSecond;

                                            if (secondsRemaining < 60) {
                                                etaStr = secondsRemaining + " שניות";
                                            } else {
                                                etaStr = (secondsRemaining / 60) + " דקות ו-" + (secondsRemaining % 60) + " שניות";
                                            }
                                        }
                                        calculateSpeed(currentTime);
                                        int progress = (totalBytes > 0) ? (int) ((bytesDownloaded * 100) / totalBytes) : -1;
                                        String speedStr = formatFileSize(bytesPerSecond) + "/s";
                                        notificationManager.notify(NOTIFICATION_ID, buildNotification("מוריד: " + progress + "% | " + speedStr + " | נותר: " + etaStr, progress));
                                        lastNotificationUpdate = currentTime;
                                    }
                                }
                                LogUtil.logToFile("4");
                                if (!isCanceled) {
                                    File finalFile = new File(getExternalFilesDir(""), finalFilename);
                                    tempFile.renameTo(finalFile);
                                    utils.oncon(DownloadService.this, finalFile.getAbsolutePath(), false);
                                    /*

                                     Intent launchIntent = new Intent(DownloadService.this, InvokeActivity.class);
                                     // FLAG_ACTIVITY_NEW_TASK חובה כי אנחנו מפעילים אקטיביטי מתוך שירות (Service Context)
                                     launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

                                     launchIntent.putExtra("start_installation", true);
                                     launchIntent.putExtra("apk_path", finalFile.getAbsolutePath());

                                     startActivity(launchIntent);
                                     */
                                }else{
                                    quconti();
                                }
                            }else{
                                quconti();
                            }
                        } catch (Exception e) {
                            LogUtil.logToFile(e);
                            //continue qitems...
                            quconti();


                        } finally {
                            try { if (output != null) output.close(); } catch (Exception ignored) {}
                            try { if (input != null) input.close(); } catch (Exception ignored) {}
                            if (connection != null) connection.disconnect();
                            if (httpconnection != null) httpconnection.disconnect();
                            //stopForeground(true);
                            //isDownloading = false;
                            //stopSelf();
                            notificationManager.cancel(NOTIFICATION_ID);
                        }
                    }
                }
            });
        downloadThread.start();
    }

    private void calculateSpeed(long currentTime) {
        long timePassed = currentTime - lastTimeCalculated;
        if (timePassed > 0) {
            long bytesPassed = bytesDownloaded - lastBytesCalculated;
            bytesPerSecond = (bytesPassed * 1000) / timePassed;
            lastBytesCalculated = bytesDownloaded;
            lastTimeCalculated = currentTime;
        }
    }

    public void quconti(){
        LogUtil.logToFile("quconti");
        //new
        notificationManager.cancel(NOTIFICATION_ID);
        //continue - get the next k v from qitems
        Object[] keys=qitems.keySet().toArray();
        if(keys.length>0){
            String pkgname=(String) keys[0];
            String murl=qitems.get(keys[0])[0];
            String type=qitems.get(keys[0])[1];
            qitems.remove(pkgname);
            utils.clearDownloadDirectory(this);
            executeDownload( murl, pkgname, type);

        }else{
            //old
            stopForeground(true);
            isDownloading = false;
            stopSelf();

            instanc=null;
        }

    }

    private void cancelDownload(String pkg,boolean destr) {

        if(destr){
            qitems.clear();
            isCanceled = true;
            if (downloadThread != null) {
                downloadThread.interrupt();
                //not safe but because destroy...
            }
            stopForeground(true);
            isDownloading = false;
            stopSelf();

            instanc=null;
        }else{
            //continue qitems...
            if(pkg.equals(packageName)){
                isCanceled = true;
                quconti();
            }else{
                qitems.remove(pkg);
            }
        }
    }

    private Notification buildNotification(String text, int progress) {
        Intent cancelIntent = new Intent(this, DownloadService.class);
        cancelIntent.setAction(ACTION_CANCEL_DOWNLOAD);
        cancelIntent.putExtra(EXTRA_PKG,packageName);

        int flags = (Build.VERSION.SDK_INT >= 23) ? PendingIntent.FLAG_IMMUTABLE : PendingIntent.FLAG_UPDATE_CURRENT;
        PendingIntent pCancel = PendingIntent.getService(this, 0, cancelIntent, flags);

        Notification.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder = new Notification.Builder(this, CHANNEL_ID);
        } else {
            builder = new Notification.Builder(this);
        }

        builder.setContentTitle(finalFilename)
            .setContentText(text)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setOngoing(true)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "ביטול", pCancel);

        if (progress >= 0) {
            builder.setProgress(100, progress, false);
        } else {
            builder.setProgress(0, 0, true); // Indeterminate במקרה שאין Content-Length
        }

        return builder.build();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID, "הורדות ברקע", NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("מציג התראות על התקדמות ההורדות ברקע");
            notificationManager.createNotificationChannel(channel);
        }
    }

    private String formatFileSize(long bytes) {
        if (bytes <= 0) return "0 B";
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp - 1) + "";
        return String.format("%.1f %sB", bytes / Math.pow(1024, exp), pre);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        instanc=null;
        cancelDownload("cancelAll",true);
    }
}

