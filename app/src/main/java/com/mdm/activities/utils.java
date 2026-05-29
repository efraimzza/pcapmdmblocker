package com.mdm.activities;
import android.app.ProgressDialog;
import android.os.Handler;
import android.app.Activity;
import android.os.Looper;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ConcurrentHashMap;
import android.content.Context;
import javax.net.ssl.HttpsURLConnection;
import java.util.concurrent.Executors;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.cert.X509Certificate;
import java.security.cert.CertificateException;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import java.net.URL;
import java.io.FileOutputStream;
import java.io.File;
import android.content.pm.PackageInstaller;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.FileInputStream;
import android.content.Intent;
import android.app.PendingIntent;
import android.widget.Toast;
import java.util.List;
import android.content.DialogInterface;
import java.security.MessageDigest;
import android.content.pm.PackageManager;
import android.app.AlertDialog;
import java.io.RandomAccessFile;
import android.content.pm.Signature;
import java.util.ArrayList;
import java.util.Collections;
import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.model.FileHeader;
import android.content.pm.PackageInfo;
import android.os.Build;
import android.content.pm.SigningInfo;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipEntry;
import java.io.IOException;
import java.util.Comparator;
import com.emanuelef.remote_capture.R;
import java.net.URLDecoder;
import android.net.Uri;
import com.emanuelef.remote_capture.activities.LogUtil;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.BufferedInputStream;
import java.net.HttpURLConnection;
import org.json.JSONObject;
import java.util.Iterator;

public class utils {
    
    public static ProgressDialog progressDialog;
    private static Handler handler=new Handler(Looper.getMainLooper());
    private static Runnable updateProgressRunnable;
    private static boolean isDownloadCanceled = false;
    public static void startDownloadnewGplay(Activity mactivity,final String pkgname,String jstr){
        String uri = jstr;
        try {
            List<PackageInstaller.SessionInfo> lses= mactivity. getPackageManager().getPackageInstaller().getAllSessions();
            if (lses != null) {
                for (PackageInstaller.SessionInfo pses:lses) {
                    if (pses != null&&pses.getInstallerPackageName()!=null) {
                        try {
                            if (pses.getInstallerPackageName().equals(mactivity. getPackageName())) {
                                mactivity. getPackageManager().getPackageInstaller().abandonSession(pses.getSessionId());
                            }
                        } catch (Exception e) {
                            LogUtil.logToFile(""+e);
                            Toast.makeText(mactivity. getApplicationContext(), "" + e, 0).show();
                        }
                    }
                }
            }
        } catch (Exception e) {
            LogUtil.logToFile(""+e);
            Toast.makeText(mactivity. getApplicationContext(), "" + e, 0).show();
        }
        // הגדר כותרת ותיאור עבור ההתראה

        //new File(mactivity. getExternalFilesDir("")+"/updatebeta.apk").delete();
        //String destinationFile = mactivity. getExternalFilesDir("")+"/updatebeta.apk";

        // אפס את דגל הביטול לפני התחלת הורדה חדשה
        isDownloadCanceled = false;

        startDownloadGplay(mactivity,pkgname,jstr,new Runnable(){
                @Override
                public void run() {
                }
            }, new Runnable(){

                @Override
                public void run() {
                }
            });
        // הכנס את ההורדה לתור וקבל את מזהה ההורדה

        
    }
    public static String getRedirectLocation(String urlString) {
        HttpURLConnection connection = null;
        try {
            URL url = new URL(urlString);
            connection = (HttpURLConnection) url.openConnection();

            // 1. חובה: כיבוי מעקב אוטומטי אחר הפניות
            connection.setInstanceFollowRedirects(false); 

            connection.setRequestMethod("GET");
            connection.connect();

            int status = connection.getResponseCode();

            // 2. בדיקה אם הסטטוס הוא הפניה (301, 302, 303, 307, 308)
            if (status == HttpURLConnection.HTTP_MOVED_PERM || 
                status == HttpURLConnection.HTTP_MOVED_TEMP ||
                status == 303 || status == 307 || status == 308) {

                // 3. חילוץ הקישור הישיר מכותרת Location
                String newLocation = connection.getHeaderField("Location");
                if (newLocation != null && !newLocation.isEmpty()) {
                    return newLocation;
                }
            }
        } catch (Exception e) {
            // כשל בחיבור
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
        return "";
    }
    public static void startDownloadnew(Activity mactivity,String mlink,boolean isDrive) {
        String uri = mlink;

        try {
            List<PackageInstaller.SessionInfo> lses= mactivity. getPackageManager().getPackageInstaller().getAllSessions();
            if (lses != null) {
                for (PackageInstaller.SessionInfo pses:lses) {
                    if (pses != null&&pses.getInstallerPackageName()!=null) {
                        try {
                            if (pses.getInstallerPackageName().equals(mactivity. getPackageName())) {
                               mactivity. getPackageManager().getPackageInstaller().abandonSession(pses.getSessionId());
                            }
                        } catch (Exception e) {
                            LogUtil.logToFile(""+e);
                            Toast.makeText(mactivity. getApplicationContext(), "" + e, 0).show();
                        }
                    }
                }
            }
        } catch (Exception e) {
            LogUtil.logToFile(""+e);
            Toast.makeText(mactivity. getApplicationContext(), "" + e, 0).show();
        }
        // הגדר כותרת ותיאור עבור ההתראה

        //new File(mactivity. getExternalFilesDir("")+"/updatebeta.apk").delete();
        //String destinationFile = mactivity. getExternalFilesDir("")+"/updatebeta.apk";

        // אפס את דגל הביטול לפני התחלת הורדה חדשה
        isDownloadCanceled = false;

        startDownload(mactivity, uri,isDrive, new Runnable(){
                @Override
                public void run() {
                }
            }, new Runnable(){

                @Override
                public void run() {
                }
            });
        // הכנס את ההורדה לתור וקבל את מזהה ההורדה

        showProgressDialognew(mactivity,uri);
    }
    
    private static long lastBytes = 0;

    @Deprecated
    private static void showProgressDialognew(final Activity mactivity, final String fileurl) {
        lastBytes = 0; 

        progressDialog = new ProgressDialog(mactivity);
        progressDialog.setTitle("הורדת קובץ");
        progressDialog.setMessage("מתחיל הורדה...");
        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progressDialog.setCancelable(false);

        progressDialog.setButton(DialogInterface.BUTTON_NEGATIVE, "ביטול", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    isDownloadCanceled = true; //is already in the cancelDownload method//but because the gplay links jstr...
                    //cancelDownload(fileurl);
                    handler.removeCallbacks(updateProgressRunnable);
                    dialog.dismiss();
                    Toast.makeText(mactivity.getApplicationContext(), "ההורדה בוטלה.", Toast.LENGTH_SHORT).show();
                }
            });

        progressDialog.show();

        updateProgressRunnable = new Runnable() {
            @Override
            public void run() {
                if (isDownloadCanceled) {
                    handler.removeCallbacks(this);
                    if(progressDialog.isShowing())
                    progressDialog.dismiss();
                    Toast.makeText(mactivity.getApplicationContext(), "ההורדה בוטלה.", Toast.LENGTH_SHORT).show();
                    return;
                }

                long bytesDownloaded = total;
                long totalBytes = fileLength;

                // // חישוב מהירות לשנייה //
                long bytesInLastSecond = bytesDownloaded - lastBytes;
                lastBytes = bytesDownloaded;
                String speedStr = formatFileSize(bytesInLastSecond) + "/s";

                // // חישוב זמן נותר (ETA) //
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

                if (totalBytes > 0) {
                    int progress = (int) ((bytesDownloaded * 100) / totalBytes);
                    progressDialog.setProgress(progress);

                    String downloadedStr = formatFileSize(bytesDownloaded);
                    String totalStr = formatFileSize(totalBytes);

                    // // הצגת כל הנתונים: אחוזים, נפח, מהירות וזמן נותר //
                    progressDialog.setMessage("הורדה: " + progress + "% (" + downloadedStr + " / " + totalStr + ")\n" +
                                              "מהירות: " + speedStr + " | נותר: " + etaStr);
                } else {
                    progressDialog.setMessage("הורדה: " + formatFileSize(bytesDownloaded) + "\nמהירות: " + speedStr);
                }

                if (totalBytes > 0 && bytesDownloaded >= totalBytes) {
                    handler.removeCallbacks(this);
                    progressDialog.dismiss();
                    //oncon(mactivity, mactivity.getExternalFilesDir("") + "/updatebeta.apk");
                } else {
                    handler.postDelayed(this, 1000);
                }
            }
        };
        handler.post(updateProgressRunnable);
    }

// // פונקציית העזר להמרת גדלים (ללא שינוי) //
    private static String formatFileSize(long bytes) {
        if (bytes <= 0) return "0 B";
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp - 1) + "";
        return String.format("%.1f %sB", bytes / Math.pow(1024, exp), pre);
    }
    private static final ExecutorService executor = Executors.newCachedThreadPool();
    private static final ConcurrentHashMap<String, HttpsURLConnection> connections = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, Boolean> canceledDownloads = new ConcurrentHashMap<>();
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    public static void startDownload(final Activity context, final String fileurl,final boolean isDrive, final Runnable runonsuc, final Runnable runonfail) {
        /*if (connections.containsKey(fileurl)) {
            LogUtil.logToFile("Download already in progress for: " + fileurl);
            return;
        }*/

        canceledDownloads.put(fileurl, false);

        executor.submit(new Runnable() {
                // @Override
                public void run() {
                    boolean success = manualDownload(context, fileurl,isDrive);

                    canceledDownloads.remove(fileurl);

                    final Runnable resultRunner = success ? runonsuc : runonfail;
                    mainHandler.post(resultRunner);
                    
                }
            });
    }
    public static void startDownloadGplay(final Activity context, final String pkgname,final String jstr, final Runnable runonsuc, final Runnable runonfail) {
        /*if (connections.containsKey(fileurl)) {
         LogUtil.logToFile("Download already in progress for: " + fileurl);
         return;
         }*/

        //canceledDownloads.put(jstr, false);

        executor.submit(new Runnable() {
                // @Override
                public void run() {
                    boolean success = manualDownloadGplay(context, pkgname,jstr);

                    //canceledDownloads.remove(jstr);

                    final Runnable resultRunner = success ? runonsuc : runonfail;
                    mainHandler.post(resultRunner);

                }
            });
    }
    static int fileLength=0;
    static int total = 0;
    /*private static boolean manualDownload(final Context context, final String fileurl,  String filename) {
        InputStream input = null;
        OutputStream output = null;
        HttpsURLConnection connection = null;
        boolean downloadSuccess = false;

        try {
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
            URL url = new URL(fileurl);
            connection = (HttpsURLConnection) url.openConnection();
            connection.setSSLSocketFactory(sslSocketFactory);
            connection.setRequestProperty("Connection", "Close");
            connection.setConnectTimeout(30000);
            connection.setReadTimeout(60000);
            connections.put(fileurl, connection);
            connection.connect();

            if (connection.getResponseCode() != HttpsURLConnection.HTTP_OK) {
                final int responseCode = connection.getResponseCode();
                mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            LogUtil.logToFile("Server error: " + responseCode);
                        }
                    });
                return false;
            }
            final String serverFilename = getFilenameFromHeader(connection, filename);
            // שימוש בשם הקובץ הסופי לצורך הלוגים או שמירה
            filename =context.getExternalFilesDir("")+ "/"+serverFilename;
            LogUtil.logToFile(filename);
            fileLength = connection.getContentLength();
            input = connection.getInputStream();
            output = new FileOutputStream(filename + ".tmp");
            byte[] data = new byte[4096];
            total = 0;
            int count;

            while ((count = input.read(data)) != -1) {
                if (canceledDownloads.getOrDefault(fileurl, false)) {
                    mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                LogUtil.logToFile("Download canceled for: " + fileurl);
                            }
                        });
                    break;
                }
                total += count;
                output.write(data, 0, count);
            }
            output.flush();

            if (!canceledDownloads.getOrDefault(fileurl, false)) {
                downloadSuccess = true;
                File tempFile = new File(filename + ".tmp");
                if (tempFile.exists()) {
                    tempFile.renameTo(new File(filename));
                }
                mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            LogUtil.logToFile("Download succeeded for: " + fileurl);
                        }
                    });
            } else {
                File file = new File(filename + ".tmp");
                if (file.exists()) {
                    file.delete();
                }
            }
        } catch (Exception e) {
            final String errorMessage = "Download error for " + fileurl + ": " + e.getMessage();
            mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        LogUtil.logToFile(errorMessage);
                    }
                });
        } finally {
            try {
                if (output != null) output.close();
                if (input != null) input.close();
                if (connection != null) connection.disconnect();
            } catch (Exception ignored) { }
            connections.remove(fileurl);
        }
        return downloadSuccess;
    }
    private static boolean manualDownload(final Context context, final String fileurl, String filename) {
        int retryCount = 0;
        int maxRetries = 5; // // הגדלתי את הניסיונות כי עכשיו זה ממשיך מאותה נקודה //
        boolean downloadSuccess = false;

        // // נתיב הקובץ הזמני //
        File tempFile = new File(context.getFilesDir(), filename + ".tmp");

        while (retryCount < maxRetries && !downloadSuccess) {
            if (canceledDownloads.getOrDefault(fileurl, false)) break;

            InputStream input = null;
            RandomAccessFile output = null; // // שימוש ב-RandomAccessFile כדי לכתוב לסוף הקובץ //
            HttpsURLConnection connection = null;

            try {
                URL url = new URL(fileurl);
                connection = (HttpsURLConnection) url.openConnection();

                // // --- לוגיקת ה-RESUME --- //
                long existingFileSize = 0;
                if (tempFile.exists()) {
                    existingFileSize = tempFile.length();
                    // // בקשה מהשרת להמשיך מהנקודה שהפסקנו //
                    connection.setRequestProperty("Range", "bytes=" + existingFileSize + "-");
                }

                // // הגדרות SSL ו-Timeout (כפי שהיה קודם) //
                // sslContext.init... connection.setSSLSocketFactory...
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
                connection.setRequestProperty("Connection", "Close");
                connection.setConnectTimeout(30000);
                connection.setReadTimeout(60000);
                connections.put(fileurl, connection);
                
                connection.connect();

                int responseCode = connection.getResponseCode();
                // // HTTP 206 אומר שהשרת הסכים להביא רק חלק מהקובץ (Partial Content) //
                // // HTTP 200 אומר שהשרת מתחיל מהתחלה (לא תומך ב-Resume) //
                if (responseCode == HttpsURLConnection.HTTP_OK || responseCode == HttpsURLConnection.HTTP_PARTIAL) {

                    if (responseCode == HttpsURLConnection.HTTP_OK) {
                        // // אם השרת החזיר 200, הוא מתחיל מהתחלה, אז נאפס את המונה שלנו //
                        existingFileSize = 0;
                        output = new RandomAccessFile(tempFile, "rw");
                        output.setLength(0); 
                    } else {
                        // // אם השרת החזיר 206, נפתח את הקובץ ונזוז לסופו //
                        output = new RandomAccessFile(tempFile, "rw");
                        output.seek(existingFileSize);
                    }

                    // // עדכון גודל הקובץ הכולל (השרת מחזיר רק את מה שנשאר, אז נוסיף את מה שכבר יש) //
                    fileLength = (int) (connection.getContentLength() + existingFileSize);
                    input = connection.getInputStream();

                    byte[] data = new byte[8192]; // // הגדלתי באפר ל-8KB לביצועים טובים יותר //
                    total = (int) existingFileSize;
                    int count;

                    while ((count = input.read(data)) != -1) {
                        if (canceledDownloads.getOrDefault(fileurl, false)) break;

                        output.write(data, 0, count);
                        total += count;
                    }

                    if (!canceledDownloads.getOrDefault(fileurl, false)) {
                        downloadSuccess = true;
                        // // העברת הקובץ לשם הסופי בסיום מוצלח //
                        tempFile.renameTo(new File(context.getFilesDir(), filename));
                    }
                }
            } catch (final Exception e) {
                retryCount++;
                final int currentRetry = retryCount;
                mainHandler.post(new Runnable() {
                        @Override public void run() {
                            LogUtil.logToFile("שגיאה. מנסה להמשיך מנקודת העצירה... ניסיון " + currentRetry+e.toString());
                        }
                    });
                try { Thread.sleep(3000); } catch (InterruptedException ignored) {}
            } finally {
                try {
                    if (output != null) output.close();
                    if (input != null) input.close();
                    if (connection != null) connection.disconnect();
                } catch (Exception ignored) {}
            }
        }
        cancelDownload(fileurl);
        return downloadSuccess;
    }*/
    private static boolean manualDownload(final Activity context, final String fileurl,final boolean isDrive) {
        int retryCount = 0;
        int maxRetries = 5;
        boolean downloadSuccess = false;
        String finalFilename = ""; // // השם הסופי שישמש אותנו //
        String driveUrl="";
        if(isDrive){
            try {
                // // שלב 1: השגת דף האישור וחילוץ שם, UUID ואישור
                //get file name & generate link
                String initialUrl = "https://drive.google.com/uc?export=download&id=" + fileurl;
                HttpsURLConnection conn = (HttpsURLConnection) new URL(initialUrl).openConnection();
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

                String htmlStr = html.toString();
                String confirm = extractValue(htmlStr, "name=\"confirm\" value=\"", "\"");
                String uuid = extractValue(htmlStr, "name=\"uuid\" value=\"", "\"");
                // // חילוץ שם הקובץ מה-HTML (נמצא בתוך ה-class "uc-name-size")
                finalFilename = extractValue(htmlStr, "class=\"uc-name-size\"><a href=\"/open?id=" + fileurl + "\">", "</a>");
                //LogUtil.logToFile(finalFilename);
                //LogUtil.logToFile(htmlStr);
                if (finalFilename == null || finalFilename.isEmpty()) finalFilename = "downloaded_file.apk";
                // // שלב 2: בדיקת קובץ זמני קיים
                File tempFile = new File(context.getExternalFilesDir(""), finalFilename + ".tmp");
                long existingSize = 0;
                if (tempFile.exists()) {
                    existingSize = tempFile.length();
                }
                //link
                driveUrl = "https://drive.usercontent.google.com/download?id=" + fileurl 
                    + "&export=download&confirm=" + confirm + "&uuid=" + uuid;
                // downloadWithResume(fileId, fileName, url, existingSize);

            } catch (Exception e) {
                LogUtil.logToFile(e.toString());
                //showError(fileId, "שגיאה בחיבור הראשוני");
            }
        }else{
        try {
            // // שלב א': חיבור ראשוני רק כדי לחלץ את שם הקובץ האמיתי מהשרת //
            URL url = new URL(fileurl);
            HttpsURLConnection preCheck = (HttpsURLConnection) url.openConnection();
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

            finalFilename = getFilenameFromHeader(preCheck);
            preCheck.disconnect();
        } catch (Exception e) {
            // // אם HEAD נכשל, נמשיך עם השם המקורי //
            
            LogUtil.logToFile("head err="+e.toString());
            //cancelDownload(fileurl);
            return false;
        }
        LogUtil.logToFile(finalFilename);
            if(finalFilename.equals("")){
                try{
                    finalFilename= getFileNameFromUrl(fileurl);
                }catch(Exception e){
                    LogUtil.logToFile(e.toString());
                }
            }
        }
        LogUtil.logToFile(finalFilename);
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
        File tempFile = new File(context.getExternalFilesDir(""), finalFilename + ".tmp");

     //   while (retryCount < maxRetries && !downloadSuccess) {
            if (canceledDownloads.getOrDefault(fileurl, false)||isDownloadCanceled) return false;

            InputStream input = null;
            RandomAccessFile output = null;
            HttpsURLConnection connection = null;

            try {
                //LogUtil.logToFile(driveUrl);
                URL url = new URL(isDrive?driveUrl:fileurl);
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
                if (tempFile.exists()) {
                    existingFileSize = tempFile.length();
                    // // בקשת המשך מהנקודה הקיימת //
                    connection.setRequestProperty("Range", "bytes=" + existingFileSize + "-");
                }else{
                connection.setRequestProperty("Connection", "Close");
                }
                connection.setConnectTimeout(30000);
                connection.setReadTimeout(60000);
                connections.put(fileurl, connection);
                connection.connect();
                int responseCode = connection.getResponseCode();
                //LogUtil.logToFile("rc="+responseCode);
                if (responseCode == HttpsURLConnection.HTTP_OK || responseCode == HttpsURLConnection.HTTP_PARTIAL) {

                    output = new RandomAccessFile(tempFile, "rw");

                    if (responseCode == HttpsURLConnection.HTTP_OK) {
                        existingFileSize = 0;
                        output.setLength(0); 
                    } else {
                        output.seek(existingFileSize);
                    }

                    // // עדכון הגודל הכולל לצורך הדיאלוג //
                    fileLength = (int) (connection.getContentLength() + existingFileSize);
                    input = connection.getInputStream();

                    byte[] data = new byte[8192];
                    total = (int) existingFileSize;
                    int count;

                    while ((count = input.read(data)) != -1) {
                        if (canceledDownloads.getOrDefault(fileurl, false)) break;
                        output.write(data, 0, count);
                        total += count;
                    }

                    if (!canceledDownloads.getOrDefault(fileurl, false)) {
                        downloadSuccess = true;
                        // // סיום מוצלח: שינוי שם לקובץ סופי ללא .tmp //
                        tempFile.renameTo(new File(context.getExternalFilesDir(""), finalFilename));
                    }
                }
            } catch (Exception e) {
                retryCount++;
                LogUtil.logToFile(retryCount+" "+e.toString());
                // // הודעת לוג על ניסיון המשך //
                try { Thread.sleep(3000); } catch (InterruptedException ignored) {}
            } finally {
                try {
                    if (output != null) output.close();
                    if (input != null) input.close();
                    if (connection != null) connection.disconnect();
                } catch (Exception ignored) {}
            }
    //    }
        cancelDownload(fileurl);
        if(downloadSuccess){
            
                oncon(context, context.getExternalFilesDir("") + "/"+finalFilename,false);
           
        }
        return downloadSuccess;
    }
    private static boolean manualDownloadGplay(final Activity context,final String pkgname, final String jstr) {
        int retryCount = 0;
        int maxRetries = 5;
        boolean downloadSuccess = false;
        String finalFilename = ""; // // השם הסופי שישמש אותנו //
        fileLength=0;
        total=0;
        try{
        if(jstr.equals("")) return false;
            JSONObject json = new JSONObject(jstr);
            Iterator<String> its=json.keys();
            while(its.hasNext()){
                finalFilename=its.next();
                final String fileurl=json.getString(finalFilename);
                
            LogUtil.logToFile(finalFilename);
               
                handler.post(new Runnable() {
                        @Override
                        public void run() {
                            showProgressDialognew(context, fileurl);
                        }
                    });
                
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
                new File(context.getExternalFilesDir("")+"/"+pkgname).mkdirs();
            File tempFile = new File(context.getExternalFilesDir("")+"/"+pkgname, finalFilename + ".tmp");
            //   while (retryCount < maxRetries && !downloadSuccess) {
            if (canceledDownloads.getOrDefault(fileurl, false)||isDownloadCanceled) return false;
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
                if (tempFile.exists()) {
                    existingFileSize = tempFile.length();
                    // // בקשת המשך מהנקודה הקיימת //
                    connection.setRequestProperty("Range", "bytes=" + existingFileSize + "-");
                }else{
                    connection.setRequestProperty("Connection", "Close");
                }
                connection.setConnectTimeout(30000);
                connection.setReadTimeout(60000);
                connections.put(fileurl, connection);
                connection.connect();
                int responseCode = connection.getResponseCode();
                //LogUtil.logToFile("rc="+responseCode);
                if (responseCode == HttpsURLConnection.HTTP_OK || responseCode == HttpsURLConnection.HTTP_PARTIAL) {

                    output = new RandomAccessFile(tempFile, "rw");

                    if (responseCode == HttpsURLConnection.HTTP_OK) {
                        existingFileSize = 0;
                        output.setLength(0); 
                    } else {
                        output.seek(existingFileSize);
                    }

                    // // עדכון הגודל הכולל לצורך הדיאלוג //
                    fileLength = (int) (connection.getContentLength() + existingFileSize);
                    input = connection.getInputStream();

                    byte[] data = new byte[8192];
                    total = (int) existingFileSize;
                    int count;

                    while ((count = input.read(data)) != -1) {
                        if (canceledDownloads.getOrDefault(fileurl, false)||isDownloadCanceled) break;
                        output.write(data, 0, count);
                        total += count;
                    }

                    if (!canceledDownloads.getOrDefault(fileurl, false)&&!isDownloadCanceled) {
                        downloadSuccess = true;
                        // // סיום מוצלח: שינוי שם לקובץ סופי ללא .tmp //
                        tempFile.renameTo(new File(context.getExternalFilesDir("")+"/"+pkgname, finalFilename));
                    }
                }
            } catch (Exception e) {
                retryCount++;
                LogUtil.logToFile(retryCount+" "+e.toString());
                // // הודעת לוג על ניסיון המשך //
                try { Thread.sleep(3000); } catch (InterruptedException ignored) {}
                try {
                    if (output != null) output.close();
                    if (input != null) input.close();
                    if (connection != null) connection.disconnect();
                } catch (Exception ignored) {}//before breaking...
                break; //break the "for"
            } finally {
                try {
                    if (output != null) output.close();
                    if (input != null) input.close();
                    if (connection != null) connection.disconnect();
                } catch (Exception ignored) {}
            }
            //    }
            
            
        }
            its=json.keys();
            while(its.hasNext()){
                finalFilename=its.next();
                String fileurl=json.getString(finalFilename);
            cancelDownload(fileurl);
            }
            if(downloadSuccess){
                oncon(context, context.getExternalFilesDir("") + "/"+pkgname,true);
            }
        }catch(Throwable t){LogUtil.logToFile(t);}
        return downloadSuccess;
    }
    public static void cancelDownload(String fileurl) {
        LogUtil.logToFile("canceld "+fileurl);
        isDownloadCanceled = true;
        if (canceledDownloads.containsKey(fileurl)) {
            canceledDownloads.put(fileurl, true);
        }
        HttpsURLConnection connection = connections.get(fileurl);
        if (connection != null) {
            connection.disconnect();
        }
        for(Object con:connections.keySet().toArray()){
            connections.get((String)con).disconnect();
            connections.remove((String)con);
        }
    }
    private static String getFilenameFromHeader(HttpsURLConnection connection) {
        String contentDisposition = connection.getHeaderField("Content-Disposition");
        String result = "";

        if (contentDisposition != null && !contentDisposition.isEmpty()) {
            try {
                // חיפוש filename="filename.ext" בתוך ה-Header
                int index = contentDisposition.toLowerCase().indexOf("filename=");
                if (index != -1) {
                    result = contentDisposition.substring(index + 9);
                    // הסרת גרשיים אם קיימים
                    if (result.startsWith("\"")) {
                        result = result.substring(1, result.lastIndexOf("\""));
                    }
                }
            } catch (Exception e) {
                // במקרה של שגיאה בחיתוך, נחזור לשם ברירת המחדל
                result = "";
            }
        }
        return result;
    }
    public static String getFileNameFromUrl(String urlString) {
        // // נסיון לחלץ את שם הקובץ מה-Parameter 'filename' שנמצא בתוך 'response-content-disposition'
        try {
            Uri uri = Uri.parse(urlString);
            String contentDisposition = uri.getQueryParameter("response-content-disposition");

            if (contentDisposition != null && contentDisposition.contains("filename=")) {
                // // חיתוך המחרוזת כדי להגיע לערך שאחרי ה-filename=
                int index = contentDisposition.indexOf("filename=");
                String fileName = contentDisposition.substring(index + 9);

                // // ניקוי גרשיים אם קיימים
                fileName = fileName.replace("\"", "");

                // // פענוח תווים מיוחדים (כמו רווחים שמוצגים כ-%20)
                return URLDecoder.decode(fileName, "UTF-8");
            }

            // // במידה והפרמטר לא קיים, ניקח את החלק האחרון של ה-Path ב-URL
            String lastPathSegment = uri.getLastPathSegment();
            if (lastPathSegment != null) {
                return lastPathSegment;
            }

        } catch (Exception e) {
            // // טיפול בשגיאות במידה וה-URL לא תקין
            e.printStackTrace();
        }

        return "";
    }
    static void oncon(final Activity mcontext,String path,final boolean isFolder){
        if(!path.equals("")){
            utils. deleteTempDir(new File(mcontext.getFilesDir().toString() + "/cach"),true);
            mfilepath=path;
            path="";
            new Thread(){public void run() {
                    //prgmsg(mcontext,"מתחיל.",false);
                    
                    //no need to check if installed...
                    
                    handler.post(new Runnable(){@Override
                            public void run() {
                                if(progressDialog!=null&& progressDialog.isShowing()){
                                    progressDialog.dismiss();
                                    handler.removeCallbacks(updateProgressRunnable);
                                }
                    progressDialog = new ProgressDialog(mcontext);
                    progressDialog.getWindow().setBackgroundDrawableResource(R.drawable.rounded_button_background);
                    progressDialog.setMessage("מתחיל");
                    progressDialog.show();
                            }
                        });
                    startInstallSession(mcontext, new File(mfilepath), isFolder);

                }}.start();
        }
    }
    static String mfilepath="";
    static String befsha1="";
    static String aftsha1="";

    static String getsha1(final Context mcontext,String mfilename) {
        String res="";
        try {
            MessageDigest md= MessageDigest.getInstance("SHA-1");
            FileInputStream fis = new FileInputStream(new File(mfilename));
            int len;
            byte[] buffer = new byte[1024];
            while ((len = fis.read(buffer)) > 0) {
                md.update(buffer, 0, len);
            }
            byte[] bArr=md.digest();
            char[] aa = new char[]{'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};
            char[] cArr = new char[bArr.length * 2];
            for (int i = 0; i < bArr.length; i++) {
                cArr[i * 2] = aa[(bArr[i] & 255) >>> 4];
                cArr[(i * 2) + 1] = aa[bArr[i] & 15];
            }
            res = new String(cArr);
            //LogUtil.logToFile(new String(cArr));
        } catch (Exception e) {
            LogUtil.logToFile(e.toString());
            Toast.makeText(mcontext, "" + e, 1).show();
        }
        return res;
    }
    private static boolean isAppInstalled(final Context mcontext,String packageName) {
        try {
            mcontext.getPackageManager().getPackageInfo(packageName, 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    
    static ProgressDialog progressDialo=null;
    @Deprecated
    static void prgmsg(final Context context,final String msg,final boolean end){
        //context.getMainLooper().prepare();
        new Handler(context.getMainLooper()).post(new Runnable(){
                @Deprecated
                @Override
                public void run() {
                    if(progressDialo!=null){
                        if(progressDialo.isShowing()){
                            progressDialo.dismiss();
                        }
                    }
                    progressDialo = new ProgressDialog(context);
                    progressDialo.getWindow().setBackgroundDrawableResource(R.drawable.rounded_button_background);
                    progressDialo.setMessage("מתחיל.");
                    //progressDialog.setCancelable(false);
                    try{
                        progressDialo.show();
                        progressDialo.setMessage(msg);
                    }catch(Exception e){
                        LogUtil.logToFile(e.toString());
                    }
                    if(end){
                        new Handler().postDelayed(new Runnable(){
                                @Deprecated
                                @Override
                                public void run() {
                                    if(progressDialo!=null){
                                        if(progressDialo.isShowing()){
                                            progressDialo.dismiss();
                                        }
                                    }
                                }
                            }, 5000);
                    }
                }});


        //context.getMainLooper().loop();
    }
    public static final String ACTION_INSTALL_COMPLETE = "com.mdm.activities.ACTION_INSTALL_COMPLETE";
    public static final String EXTRA_PACKAGE_NAME = "packageName";
    public static final String EXTRA_INSTALL_STATUS = "installStatus";
    static String mainPackageName = null;
    static String reserr="";
    static Context mcontext;
    @Deprecated
    public static void startInstallSession(Context context, File sourceFile, boolean isFolder) {
        reserr="";
        mcontext=context;
        if (sourceFile == null || !sourceFile.exists()) {
            //Toast.makeText(context, "קובץ התקנה לא נמצא או לא חוקי.", Toast.LENGTH_LONG).show();
            //utils.progressDialog.setMessage("not faund or not possible" + sourceFile.getName());
            //dismissprogress(context);
            utils.prgmsg(context,"לא נמצא או לא אפשרי" + sourceFile.getName(),true);
            LogUtil.logToFile("not faund or not possible");
            return;
        }
        //LogUtil.logToFile("in 1");
        PackageManager pm = context.getPackageManager();
        PackageInstaller packageInstaller = pm.getPackageInstaller();
        PackageInstaller.Session session = null;

        List<File> apksToInstall = new ArrayList<File>();

        Signature[] mainApkSignatures = null;

        try {
            if(!isFolder){
            if (sourceFile.getName().toLowerCase().endsWith(".zip") ||
                sourceFile.getName().toLowerCase().endsWith(".apks") ||
                sourceFile.getName().toLowerCase().endsWith(".xapk") ||
                sourceFile.getName().toLowerCase().endsWith(".apkm")) {
                //LogUtil.logToFile("in 1");
                File tempDir = createTempDir(context);
                apksToInstall = extractApksFromZip(sourceFile, tempDir);
                //LogUtil.logToFile("in 2");
                if (apksToInstall.isEmpty()) {
                    //utils.progressDialog.setMessage("לא נמצאו קבצי APK בארכיון ה-ZIP." + sourceFile.getName());
                    //dismissprogress(context);
                    utils.prgmsg(context,"לא נמצאו קבצי APK בארכיון ה-ZIP." + sourceFile.getName(),true);
                    //Toast.makeText(context, "לא נמצאו קבצי APK בארכיון ה-ZIP.", Toast.LENGTH_LONG).show();
                    return;
                }

                File baseApk = findBaseApk(context, apksToInstall);
                if (baseApk != null) {
                    try {
                        mainPackageName = getApkPackageName(context, baseApk.getAbsolutePath());
                        mainApkSignatures = getApkSignature(context, baseApk.getAbsolutePath());
                        if (mainPackageName != null && mainApkSignatures != null) {
                            //LogUtil.logToFile("detected main pn");
                        }
                    } catch (Exception e) {
                        LogUtil.logToFile("e "+e);
                        for (File apk : apksToInstall) {
                            LogUtil.logToFile("for");
                            if (apk.getName().contains(".apk")) {
                                try {
                                    mainPackageName = getApkPackageName(context, apk.getAbsolutePath());
                                    mainApkSignatures = getApkSignature(context, apk.getAbsolutePath());
                                    if (mainPackageName != null && mainApkSignatures != null) {
                                        //LogUtil.logToFile("detected main pn");
                                        break;//breaking the for loop
                                    }
                                } catch (Exception ee) {}
                            }
                        }
                    }
                    if (mainPackageName == null) {
                        //utils.progressDialog.setMessage("לא ניתן לזהות את ה-APK הבסיסי בארכיון." + sourceFile.getName());
                        //dismissprogress(context);
                        //utils.prgmsg(context,"לא ניתן לזהות את ה-APK הבסיסי בארכיון." + sourceFile.getName(),true);
                        //return;
                        //LogUtil.logToFile("retrying detect pn");
                        for (File apk : apksToInstall) {
                            LogUtil.logToFile("for");
                            if (apk.getName().contains(".apk")) {
                                try {
                                    mainPackageName = getApkPackageName(context, apk.getAbsolutePath());
                                    mainApkSignatures = getApkSignature(context, apk.getAbsolutePath());
                                    if (mainPackageName != null && mainApkSignatures != null) {
                                        //LogUtil.logToFile("detected main pn - "+mainPackageName);
                                        break;
                                    }
                                } catch (Exception e) {
                                    LogUtil.logToFile(e.toString());
                                }
                            }
                        }
                    }
                } else {
                    for (File apk : apksToInstall) {
                        LogUtil.logToFile("for");
                        if (apk.getName().contains(".apk")) {
                            try {
                                mainPackageName = getApkPackageName(context, apk.getAbsolutePath());
                                mainApkSignatures = getApkSignature(context, apk.getAbsolutePath());
                                if (mainPackageName != null && mainApkSignatures != null) {
                                    //LogUtil.logToFile("detected main pn");
                                    break;
                                }
                            } catch (Exception e) {
                                LogUtil.logToFile(e.toString());
                            }
                        }
                    }
                    if (mainPackageName == null) {
                        //utils.progressDialog.setMessage("לא ניתן לזהות את ה-APK הבסיסי בארכיון." + sourceFile.getName());
                        //dismissprogress(context);
                        utils.prgmsg(context,"לא ניתן לזהות את ה-APK הבסיסי בארכיון." + sourceFile.getName(),true);
                        return;
                    }
                }
            } else if (sourceFile.getName().toLowerCase().endsWith(".apk")) {
                apksToInstall.add(sourceFile);
                //LogUtil.logToFile("in 1");
                //LogUtil.logToFile(sourceFile.getAbsolutePath());
                mainPackageName = getApkPackageName(context, sourceFile.getAbsolutePath());
                //LogUtil.logToFile(mainPackageName);
                mainApkSignatures = getApkSignature(context, sourceFile.getAbsolutePath());
                //LogUtil.logToFile("in 2");
            } else {
                //utils.progressDialog.setMessage("פורמט קובץ לא נתמך: " + sourceFile.getName());
                //dismissprogress(context);
                utils.prgmsg(context,"פורמט קובץ לא נתמך: " + sourceFile.getName(),true);
                //Toast.makeText(context, "פורמט קובץ לא נתמך: " + sourceFile.getName(), Toast.LENGTH_LONG).show();
                return;
            }
            }else{
                for(File f:sourceFile.listFiles()){
                    apksToInstall.add(f);
                }
                
                //LogUtil.logToFile("in 1");
                //LogUtil.logToFile(sourceFile.getAbsolutePath());
                mainPackageName = getApkPackageName(context, new File(sourceFile,"base.apk").getAbsolutePath());
                //LogUtil.logToFile(mainPackageName);
                mainApkSignatures = getApkSignature(context, new File(sourceFile,"base.apk").getAbsolutePath());
                //LogUtil.logToFile("in 2");
            }
            if (mainPackageName == null || mainApkSignatures == null || mainApkSignatures.length == 0) {
                //utils.progressDialog.setMessage("שגיאה: לא ניתן לקרוא שם חבילה או חתימה מקובץ ה-APK הראשי.");
                //dismissprogress(context);
                utils.prgmsg(context,"שגיאה: לא ניתן לקרוא שם חבילה או חתימה מקובץ ה-APK הראשי."+"pn="+(mainPackageName==null)+"s="+(mainApkSignatures==null),true);
                //Toast.makeText(context, "שגיאה: לא ניתן לקרוא שם חבילה או חתימה מקובץ ה-APK הראשי.", Toast.LENGTH_LONG).show();
                return;
            }

            // בדיקת חתימה מול האפליקציה המותקנת

            try {
                PackageInfo existingPackage;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    existingPackage = pm.getPackageInfo(mainPackageName, PackageManager.GET_SIGNING_CERTIFICATES);
                } else {
                    existingPackage = pm.getPackageInfo(mainPackageName, PackageManager.GET_SIGNATURES);
                }
                //LogUtil.logToFile("in 4");
                Signature[] existingSignatures = getSignaturesFromPackageInfo(existingPackage);

                if (!signaturesMatch(mainApkSignatures, existingSignatures)) {
                    //LogUtil.logToFile("in 5");
                    //utils.progressDialog.setMessage("שגיאה: חתימות האפליקציה אינן תואמות. העדכון בוטל.");
                    utils.prgmsg(context,"שגיאה: חתימות האפליקציה אינן תואמות. העדכון בוטל.",true);
                    //Toast.makeText(context, "שגיאה: חתימות האפליקציה אינן תואמות. העדכון בוטל.", Toast.LENGTH_LONG).show();
                    //session.abandon();//null...
                    return;
                }
            } catch (PackageManager.NameNotFoundException e) {
                try {
                    //if (utils.progressDialog != null && utils.progressDialog.isShowing())
                    //utils.progressDialog.setMessage("" + e.toString());
                    LogUtil.logToFile(e.toString());
                } catch (Exception ee) {
                    LogUtil.logToFile(e.toString());
                }
                //dismissprogress();
                // האפליקציה אינה מותקנת.
                // אם זו התקנה חדשה, הסיסמה כבר נבדקה מראש ב-utils
                // לכן אין צורך בבדיקה נוספת כאן.
                // אם רוצים לאכוף שרק אפליקציות חתומות ספציפית יוכלו להיות מותקנות,
                // יש לבדוק את ה-mainApkSignatures מול רשימת חתימות "לבנות" ידועות כאן.
            }
            //utils.progressDialog.setMessage("session create");
            utils.prgmsg(context,"יצור סשן",false);
            //LogUtil.logToFile("in 6");
            // יצירת סשן ההתקנה
            PackageInstaller.SessionParams params = new PackageInstaller.SessionParams(
                PackageInstaller.SessionParams.MODE_FULL_INSTALL);
            // אם isPasswordAlreadyChecked הוא true, זה אומר שהמשתמש אישר והסיסמה נבדקה (במקרה של התקנה חדשה)
            // לכן, ניתן לדרוש שההתקנה תתבצע ללא אינטראקציה נוספת אם האפליקציה מאושרת
          //  if (isPasswordAlreadyChecked) {
                // דגל INSTALL_REPLACE_EXISTING רלוונטי רק אם זהו עדכון לאפליקציה קיימת
                // אם זו התקנה ראשונית, הוא פשוט יתקין אותה.
                // אם אתה רוצה לאפשר התקנה שקטה לחלוטין (ללא דיאלוג התקנה למשתמש),
                // נדרשות הרשאות מערכת/MDM מתקדמות יותר ושיטות ספציפיות למכשיר.
                // עבור מצב רגיל, המערכת עדיין עשויה לבקש אישור.
                // params.setInstallerPackageName(context.getPackageName()); // יציין שהאפליקציה שלך היא המקור
          //  }

            params.setAppPackageName(mainPackageName);

            int sessionId = packageInstaller.createSession(params);
            session = packageInstaller.openSession(sessionId);

            //boolean deltempDir = new File(context.getCacheDir()+"/").delete();
            deleteTempDir(new File(context.getFilesDir().toString() + "/cach"),true);
            if(!isFolder){
            if (!sourceFile.getName().toLowerCase().endsWith(".apk")) {
                if (! addzipToSession(session, sourceFile)) {
                    try {
                        if (session != null)
                            session.abandon();
                        //utils.prgmsg(context,reserr,true);
                        //dismissprogress(context);
                    } catch (Exception e) {
                        LogUtil.logToFile("e3" + e);
                    }
                    return;
                }
            } else {
                if (!addApkToSession(session, sourceFile)) {
                    if (session != null)
                        session.abandon();
                    //utils.prgmsg(context,reserr,true);
                    //dismissprogress(context);
                    return;
                }
            }
            }else{
                for(File f:sourceFile.listFiles()){
                    if (!addApkToSession(session, f)) {
                        if (session != null)
                            session.abandon();
                        //utils.prgmsg(context,reserr,true);
                        //dismissprogress(context);
                        return;
                    }
                }
            }

            /*for (File apk : apksToInstall) {
             addApkToSession(session, apk);
             }*/
            //LogUtil.logToFile("in 5");
            //LogUtil.logToFile(mainPackageName);
            Intent callbackIntent = new Intent(context, InstallReceiver.class);
            callbackIntent.setAction(ACTION_INSTALL_COMPLETE);
            //callbackIntent.putExtra(EXTRA_PACKAGE_NAME, mainPackageName);

            int flags = 0;
            if (Build.VERSION.SDK_INT >= 31) {
                flags = android.app.PendingIntent.FLAG_IMMUTABLE;
                //flags = android.app.PendingIntent.FLAG_MUTABLE;
                flags = 33554432;
            } else {
                flags = android.app.PendingIntent.FLAG_UPDATE_CURRENT;
            }

            android.app.PendingIntent pendingIntent = android.app.PendingIntent.getBroadcast(
                context, 0, callbackIntent, flags);

            session.commit(pendingIntent.getIntentSender());
            //utils.progressDialog.setMessage("session commit");
            utils.prgmsg(context,"ביצוע סשן",true);
            //LogUtil.logToFile("starting installation...");
            //Toast.makeText(context, "מתחיל התקנת/עדכון APK...", Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            LogUtil.logToFile("failed to starting installation - " + e);
            //utils.progressDialog.setMessage("התחלת התקנה נכשלה " +reserr+ e);
            //dismissprogress(context);
            utils.prgmsg(context,"התחלת התקנה נכשלה " +reserr+ e,true);
            //Toast.makeText(context, "שגיאה בהתחלת התקנה/עדכון: " + e.getMessage(), Toast.LENGTH_LONG).show();
            if (session != null) {
                session.abandon();
            }
            e.printStackTrace();
        }
    }
    @Deprecated
    private static Signature[] getApkSignature(Context context, String apkFilePath) {
        try {
            PackageManager pm = context.getPackageManager();
            PackageInfo packageInfo;
            //if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) { // Android 9 (API 28)
            //packageInfo = pm.getPackageArchiveInfo(apkFilePath, PackageManager.GET_SIGNING_CERTIFICATES);

            //} else {
            packageInfo = pm.getPackageArchiveInfo(apkFilePath, PackageManager.GET_SIGNATURES | PackageManager.GET_META_DATA | PackageManager.GET_SIGNING_CERTIFICATES);
            //}
            return getSignaturesFromPackageInfo(packageInfo);
        } catch (Exception e) {
            e.printStackTrace();
            utils.prgmsg(context,"err " + e,true);
            LogUtil.logToFile("err "+e);

        }
        return null;
    }

    /**
     * חולץ חתימות מאובייקט PackageInfo, תוך התחשבות ב-API 28+
     * וב-GET_SIGNING_CERTIFICATES.
     */
    @Deprecated
    private static Signature[] getSignaturesFromPackageInfo(PackageInfo packageInfo) {
        if (packageInfo == null) {
            return null;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) { // Android 9 (API 28)
            SigningInfo signingInfo = packageInfo.signingInfo;
            if (signingInfo != null) {
                if (signingInfo.hasMultipleSigners()) {
                    // אם יש מספר חותמים, זהו מערך החתימות
                    return signingInfo.getApkContentsSigners();
                } else {
                    // אם יש חותם אחד, זהו מערך של חתימה בודדת
                    return signingInfo.getSigningCertificateHistory();
                }
            }
        } else {
            // לגרסאות ישנות יותר, השתמש בשדה signatures המיושן
            // אזהרה: ב-API 28+ זה יכול להחזיר רק את חתימת האפליקציה המותקנת כרגע
            // ולא את כל היסטוריית החתימות.
            if (packageInfo.signatures != null && packageInfo.signatures.length > 0) {
                return packageInfo.signatures;
            }
        }
        return null;
    }

    // קבלת שם החבילה (ללא שינוי)
    public static String getApkPackageName(Context context, String apkFilePath) {
        try {
            PackageManager pm = context.getPackageManager();
            PackageInfo packageInfo = pm.getPackageArchiveInfo(apkFilePath, 0);
            if (packageInfo != null) {
                return packageInfo.packageName;
            }
        } catch (Exception e) {
            LogUtil.logToFile(e.toString());
            e.printStackTrace();
        }
        return null;
    }

    /**
     * משווה שתי קבוצות של חתימות. המתודה מטפלת במקרים של Multi-Signers
     * ובשינויי חתימה לאורך זמן (אם מוגדר במניפסט).
     */
    private static boolean signaturesMatch(Signature[] sigs1, Signature[] sigs2) {
        if (sigs1 == null || sigs2 == null) {
            return false;
        }
        if (sigs1.length == 0 && sigs2.length == 0) { // שניהם ללא חתימות (נדיר/בעייתי)
            return true; 
        }
        if (sigs1.length == 0 || sigs2.length == 0) { // אחד מהם ריק, השני לא
            return false;
        }

        // אם ב-API 28 ומעלה, getSigningCertificateHistory/getApkContentsSigners
        // כבר מספקים את כל שרשרת החתימות או את החותמים הנוכחיים.
        // השוואה פשוטה של כל החתימות אמורה להיות מספקת.
        if (sigs1.length != sigs2.length) {
            return false;
        }

        // מיון החתימות כדי לוודא סדר זהה לפני ההשוואה
        // במקרה של מספר חותמים, הסדר לא מובטח
        // הדרך הנכונה היא להשוות סטים של חתימות
        List<Signature> list1 = new ArrayList<Signature>();
        List<Signature> list2 = new ArrayList<Signature>();
        Collections.addAll(list1, sigs1);
        Collections.addAll(list2, sigs2);

        // השוואת סטים (HashSet) כדי להתעלם מהסדר
        return new java.util.HashSet<Signature>(list1).equals(new java.util.HashSet<Signature>(list2));
    }



    // --- מתודות עזר חדשות לטיפול ב-ZIP (APKS) ---

    public static File createTempDir(Context context) {
        File tempDir = new File(context.getFilesDir() + "/cach/apks_temp/");
        if (!tempDir.exists()) {
            tempDir.mkdirs();
        }
        return tempDir;
    }
    @Deprecated
    public static List<File> extractApksFromZipo(File zipFile, File outputDir) throws IOException {
        List<File> extractedApks = new ArrayList<File>();
        byte[] buffer = new byte[1024];
        ZipInputStream zis = null;
        try {
            zis = new ZipInputStream(new FileInputStream(zipFile));
            ZipEntry zipEntry;
            int i=0;
            while ((zipEntry = zis.getNextEntry()) != null) {
                String fileName = zipEntry.getName();
                if (fileName.toLowerCase().endsWith(".apk") && !zipEntry.isDirectory()) {
                    i++;
                    File newFile = new File(outputDir, fileName);
                    FileOutputStream fos = null;
                    try {
                        fos = new FileOutputStream(newFile);
                        int len;
                        while ((len = zis.read(buffer)) > 0) {
                            fos.write(buffer, 0, len);
                        }
                        extractedApks.add(newFile);
                        utils.prgmsg(mcontext,"extrected " + i,false);
                    } catch (Exception e) {
                        LogUtil.logToFile("" + e);
                        //utils.progressDialog.setMessage("extrected e " + e);
                        utils.prgmsg(mcontext,"extrected e " + e,true);
                    } finally {
                        if (fos != null) fos.close();
                    }
                }
                zis.closeEntry();
            }
        } finally {
            if (zis != null) zis.close();
        }
        return extractedApks;
    }

    private static final int BUFFER_SIZE = 4096;
    @Deprecated
    public static List<File> extractApksFromZip(File zipFilep, File destDir) throws IOException {
        if (!destDir.exists()) {
            destDir.mkdirs();
        }
        List<File> extractedApks = new ArrayList<File>();
        InputStream is = null;
        OutputStream os = null;
        try {
            ZipFile zipFile = new ZipFile(zipFilep);
            List<FileHeader> fileHeaders = zipFile.getFileHeaders();
            if (fileHeaders == null || fileHeaders.isEmpty()) {
                System.out.println("The ZIP file is empty.");
                return null;
            }
            int i=0;
            for (FileHeader header : fileHeaders) {
                String fileName = header.getFileName();

                // בדיקה אם הקובץ הוא קובץ (ולא תיקייה) והאם הוא מסתיים ב-.apk
                if (!header.isDirectory() && fileName.toLowerCase().endsWith(".apk")) {
                    i++;
                    System.out.printf("Extracting: %s%n", fileName);

                    // 3. חילוץ הקובץ הספציפי לתיקיית היעד
                    // Zip4j יודעת לחלץ Entry בודד באמצעות השיטה extractFile(FileHeader, String destinationPath)
                    //zipFile.extractFile(header, destDirectoryPath);

                    is = zipFile.getInputStream(header);
                    os=new FileOutputStream(new File(destDir.getAbsolutePath()+"/"+fileName));
                    byte[] buffer = new byte[BUFFER_SIZE];
                    int len;

                    // 2. לולאת קריאה/כתיבה: קוראים מה-InputStream של Zip4j 
                    // וכותבים ישירות ל-OutputStream של הסשן.
                    while ((len = is.read(buffer)) > 0) {
                        os.write(buffer, 0, len);
                    }
                    extractedApks.add(new File(destDir+"/"+fileName));
                    utils.prgmsg(mcontext,"מחלץ " + i,false);
                }
            }

            System.out.println("Selective extraction complete.");

        } catch (Exception e) {
            LogUtil.logToFile("" + e);
            System.err.println("Zip4j Error: " + e.getMessage());
            //throw new IOException("Failed to extract specific files: " + e.getMessage(), e);
        } finally {
            // 3. סגירת ה-InputStream
            try{
                if (is != null) {
                    is.close();
                }
            }catch(Exception e){
                LogUtil.logToFile("" + e);
            }
            try{
                os.close();
            }catch(Exception e){
                LogUtil.logToFile("" + e);
            }
        }
        /*
         zis = new ZipInputStream(new FileInputStream(zipFile));
         ZipEntry zipEntry;
         int i=0;
         while ((zipEntry = zis.getNextEntry()) != null) {
         String fileName = zipEntry.getName();
         if (fileName.toLowerCase().endsWith(".apk") && !zipEntry.isDirectory()) {
         i++;
         File newFile = new File(outputDir, fileName);
         FileOutputStream fos = null;
         try {
         fos = new FileOutputStream(newFile);
         int len;
         while ((len = zis.read(buffer)) > 0) {
         fos.write(buffer, 0, len);
         }
         extractedApks.add(newFile);
         utils.prgmsg(mcontext,"extrected " + i,false);
         } catch (Exception e) {
         LogUtil.logToFile("" + e);
         //utils.progressDialog.setMessage("extrected e " + e);
         utils.prgmsg(mcontext,"extrected e " + e,true);
         } finally {
         if (fos != null) fos.close();
         }
         }
         zis.closeEntry();
         }
         } finally {
         if (zis != null) zis.close();
         }
         */
        return extractedApks;
    }

    public static File findBaseApk(Context context, List<File> apks) {
        // חפש את "base.apk" אם קיים, אחרת בחר את הגדול ביותר כ"ראשי"
        for (File apk : apks) {
            if (apk.getName().equalsIgnoreCase("base.apk")) {
                return apk;
            }
        }
        // אם אין base.apk, בחר את ה-APK הגדול ביותר
        if (!apks.isEmpty()) {
            Collections.sort(apks, new Comparator<File>() {
                    @Override
                    public int compare(File f1, File f2) {
                        if (f1.length() > f2.length()) {
                            return -1;
                        } else if (f1.length() < f2.length()) {
                            return 1;
                        } else {
                            return 0;
                        }
                    }
                });
            return apks.get(0);
        }
        return null;
    }

    // --- מתודות קיימות (ללא שינוי, רק העתקה לצורך קונטקסט) ---
    @Deprecated
    private static boolean addApkToSession(PackageInstaller.Session session, File apkFile) throws Exception {
        boolean suc=true;
        OutputStream out = null;
        InputStream in = null;
        try {
            // השם של הקובץ בסשן חשוב! ל-Split APKs, זה יכול להיות גם משהו כמו "split_config.xxx.apk"
            // הפורמט של PackageInstaller לוקח את השם מה-ZipEntry, כאן אנחנו רק מעבירים את שם הקובץ
            out = session.openWrite(apkFile.getName(), 0, apkFile.length());
            in = new FileInputStream(apkFile);
            byte[] buffer = new byte[65536];
            int c;
            while ((c = in.read(buffer)) != -1) {
                out.write(buffer, 0, c);
            }
            session.fsync(out);
        } catch (Exception e) {
            LogUtil.logToFile("" + e);
            //utils.progressDialog.setMessage("סשן ש " + e);
            utils.prgmsg(mcontext,"סשן ש " + e,true);
            if (session != null)
                session.abandon();

            suc = false;
        } finally {
            if (in != null) in.close();
            if (out != null) out.close();
        }
        return suc;
    }
    @Deprecated
    private static boolean addzipToSession(PackageInstaller.Session session, File zipFilep) {
        //OutputStream out = null;
        //InputStream in = null;
        // try {
        boolean suc=true;

        InputStream is = null;
        OutputStream os = null;
        try {
            ZipFile zipFile = new ZipFile(zipFilep);
            List<FileHeader> fileHeaders = zipFile.getFileHeaders();
            if (fileHeaders == null || fileHeaders.isEmpty()) {
                System.out.println("The ZIP file is empty.");
                return false;
            }
            int i=0;
            for (FileHeader header : fileHeaders) {
                String fileName = header.getFileName();

                // בדיקה אם הקובץ הוא קובץ (ולא תיקייה) והאם הוא מסתיים ב-.apk
                if (!header.isDirectory() && fileName.toLowerCase().endsWith(".apk")) {
                    i++;
                    System.out.printf("Extracting: %s%n", fileName);

                    // 3. חילוץ הקובץ הספציפי לתיקיית היעד
                    // Zip4j יודעת לחלץ Entry בודד באמצעות השיטה extractFile(FileHeader, String destinationPath)
                    //zipFile.extractFile(header, destDirectoryPath);

                    is = zipFile.getInputStream(header);
                    os=session.openWrite(fileName, 0, header.getUncompressedSize());
                    byte[] buffer = new byte[BUFFER_SIZE];
                    int len;

                    // 2. לולאת קריאה/כתיבה: קוראים מה-InputStream של Zip4j 
                    // וכותבים ישירות ל-OutputStream של הסשן.
                    while ((len = is.read(buffer)) > 0) {
                        os.write(buffer, 0, len);
                    }
                    session.fsync(os);
                    if (is != null) is.close();
                    if (os != null) os.close();
                    utils.prgmsg(mcontext,"מתקין - " + i,false);
                }
            }

            System.out.println("Selective extraction complete.");

        } catch (Exception e) {
            LogUtil.logToFile("ses err" + e);
            System.err.println("Zip4j Error: " + e.getMessage());
            try{
                if (os != null) os.close();
            }catch(Exception ee){}
            //LogUtil.logToFile("" + e);
            if (session != null)
                session.abandon();
            suc = false;
            //utils.progressDialog.setMessage("session e " + e);
            utils.prgmsg(mcontext,"סשן ש " + e,true);
            reserr=e.toString();
            //throw new IOException("Failed to extract specific files: " + e.getMessage(), e);
        } finally {
            try{
                if (is != null) is.close();
            }catch(Exception e){}
            //LogUtil.logToFile("in 9");
            if (!suc) {
                //LogUtil.logToFile("e1");
                if (session != null)
                    session.abandon();
                //LogUtil.logToFile("e2");
                suc = false;
                return suc;
            }
            // 3. סגירת ה-InputStream
            try{
                if (is != null) {
                    is.close();
                }
            }catch(Exception e){
                LogUtil.logToFile("" + e);
            }
            try{
                os.close();
            }catch(Exception e){
                LogUtil.logToFile("" + e);
            }
        }

        return suc;
    }
    public static void deleteTempDir(File dir,boolean delupdir) {
        if (dir != null && dir.isDirectory()) {
            String[] children = dir.list();
            for (int i = 0; i < children.length; i++) {
                File child = new File(dir, children[i]);
                if (child.isDirectory()) {
                    deleteTempDir(child,true);
                } else {
                    child.delete();
                }
            }
            if(delupdir)
                dir.delete();
        }
    }
   // public class DriveDownloader {

        private Context context;
       // private ProgressDialog progressDialog;
     //   private Handler mainHandler;

      /*  public DriveDownloader(Context context) {
            this.context = context;
            this.mainHandler = new Handler(Looper.getMainLooper());
        }*/

        public void startDownload(final String fileId) {
           /* progressDialog = new ProgressDialog(context);
            progressDialog.setMessage("בודק נתוני קובץ...");
            progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            progressDialog.setCancelable(false);
            progressDialog.show();
*/
            new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            // // שלב 1: השגת דף האישור וחילוץ שם, UUID ואישור
                            //get file name & generate link
                            String initialUrl = "https://drive.google.com/uc?export=download&id=" + fileId;
                            HttpsURLConnection conn = (HttpsURLConnection) new URL(initialUrl).openConnection();

                            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                            StringBuilder html = new StringBuilder();
                            String line;
                            while ((line = reader.readLine()) != null) {
                                html.append(line);
                            }
                            conn.disconnect();

                            String htmlStr = html.toString();
                            String confirm = extractValue(htmlStr, "name=\"confirm\" value=\"", "\"");
                            String uuid = extractValue(htmlStr, "name=\"uuid\" value=\"", "\"");
                            // // חילוץ שם הקובץ מה-HTML (נמצא בתוך ה-class "uc-name-size")
                            String fileName = extractValue(htmlStr, "class=\"uc-name-size\"><a href=\"/open?id=" + fileId + "\">", "</a>");

                            if (fileName == null || fileName.isEmpty()) fileName = "downloaded_file.apk";

                            // // שלב 2: בדיקת קובץ זמני קיים
                            File tempFile = new File(context.getExternalFilesDir(""), fileName + ".tmp");
                            long existingSize = 0;
                            if (tempFile.exists()) {
                                existingSize = tempFile.length();
                            }
                            //link
                            String url = "https://drive.usercontent.google.com/download?id=" + fileId 
                                + "&export=download&confirm=" + confirm + "&uuid=" + uuid;
                           // downloadWithResume(fileId, fileName, url, existingSize);
                            
                        } catch (Exception e) {
                            LogUtil.logToFile(e.toString());
                            //showError(fileId, "שגיאה בחיבור הראשוני");
                        }
                    }
                }).start();
        }
/*
        private void downloadWithResume(final String fileId, final String fileName, final String url, final long downloadedBytes) {
            new Thread(new Runnable() {
                    @Override
                    public void run() {
                        HttpsURLConnection conn = null;
                        try {
                            conn = (HttpsURLConnection) new URL(url).openConnection();

                            if (downloadedBytes > 0) {
                                conn.setRequestProperty("Range", "bytes=" + downloadedBytes + "-");
                            }

                            int responseCode = conn.getResponseCode();
                            // // גוגל מחזירה 206 להורדה חלקית או 200 להורדה חדשה
                            if (responseCode != 200 && responseCode != 206) {
                                throw new IOException("Server error: " + responseCode);
                            }

                            InputStream input = new BufferedInputStream(conn.getInputStream());
                            File tempFile = new File(context.getExternalFilesDir(""), fileName + ".tmp");

                            // // true = המשך כתיבה לסוף הקובץ
                            FileOutputStream output = new FileOutputStream(tempFile, true);

                            byte[] data = new byte[8192];
                            int count;
                            long total = downloadedBytes;
                            // // גודל הקובץ הכולל = מה שנשאר להוריד + מה שכבר הורד
                            final long totalFileSize = conn.getContentLength() + downloadedBytes;

                            updateMessage("מוריד: " + fileName);

                            while ((count = input.read(data)) != -1) {
                                total += count;
                                final long currentTotal = total;
                                final int progress = (int) ((total * 100) / totalFileSize);

                                mainHandler.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            progressDialog.setProgress(progress);
                                            progressDialog.setSecondaryProgress((int)progress); // // מראה את ה-Buffer
                                        }
                                    });
                                output.write(data, 0, count);
                            }

                            output.flush();
                            output.close();
                            input.close();

                            // // שלב סופי: שינוי שם הקובץ מ-.tmp לשם המקורי
                            File finalFile = new File(context.getExternalFilesDir(""), fileName);
                            if (tempFile.renameTo(finalFile)) {
                                dismissWithSuccess(fileName);
                            }

                        } catch (IOException e) {
                            showError(fileId, "ההורדה הופסקה ב-" + (downloadedBytes/1024/1024) + "MB. להמשיך אוטומטית?");
                        } finally {
                            if (conn != null) conn.disconnect();
                        }
                    }
                }).start();
        }
*/
        private static String extractValue(String html, String startTag, String endTag) {
            try {
                int start = html.indexOf(startTag) + startTag.length();
                int end = html.indexOf(endTag, start);
                return html.substring(start, end);
            } catch (Exception e) {
                return "";
            }
        }
/*
        private void updateMessage(final String msg) {
            mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        progressDialog.setMessage(msg);
                    }
                });
        }

        private void dismissWithSuccess(final String name) {
            mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        progressDialog.dismiss();
                        // // כאן אפשר להוסיף קוד להתקנה
                    }
                });
        }

        private void showError(final String fileId, final String msg) {
            mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        progressDialog.dismiss();
                        new android.app.AlertDialog.Builder(context)
                            .setTitle("הורדה")
                            .setMessage(msg)
                            .setPositiveButton("המשך", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface d, int w) {
                                    startDownload(fileId);
                                }
                            })
                            .setNegativeButton("בטל", null)
                            .show();
                    }
                });
        }*/
   // }
}
