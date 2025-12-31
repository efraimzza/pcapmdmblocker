package com.emanuelef.remote_capture.activities;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Paint; 
import android.graphics.Point;
import android.graphics.Typeface; 
import android.graphics.drawable.GradientDrawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Base64;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.MessageDigest;
import java.util.Date;
import java.util.Enumeration;
import java.util.StringTokenizer;
import android.annotation.Nullable;
import java.io.FileOutputStream;
import com.emanuelef.remote_capture.R;
import com.emanuelef.remote_capture.Utils;


public class MdmProvisioningActivity extends Activity {

    // --- 1. משתני רשת ושרת (Network & Server) ---
    private ServerSocket serverSocket;
    private Socket clientSocket;
    private static final int SERVER_PORT = 7777;
    private String deviceIpAddress = "";
    private boolean isNetworkAvailable = false;
    private String connectionState = ""; // "hotspot" or "wifi"
    private String downloadStatus = "";
    private String mdmApkPath = "";
    private static final String MDM_APK_NAME = "/mdm.apk";


    // --- 2. משתני UI ו-Context ---
    private Context context;
    private EditText inputEditText;
    private TextView statusTextView, titleTextView, instructionsTextView; 
    private LinearLayout mainLayout, buttonLayout;
    private LinearLayout titleContainer; 
    private Button instructionsButton, barcodeButton, wifiButton, hotspotButton;
    private HorizontalScrollView horizontalScrollView;
    private ScrollView verticalScrollView;

    // --- 3. משתני מימדי מסך (Screen Dimensions) ---
    private int deviceWidth, deviceHeight;
    private int qrCodeSize; // גודל מותאם אישית לקוד QR


    // --- 4. משתנים פנימיים למעקב ---
    private String currentScreenState = "home"; // "home" or "barcode"
    private Bitmap qrCodeBitmap;

    @Deprecated
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Utils.setTheme(this);
        context = this;
        mdmApkPath = getApplicationContext().getApplicationInfo().sourceDir;

        requestStoragePermissions();
        checkAndRequestFileAccess();

        initializeScreenDimensions();
        setupLayoutsAndViews();

        try {
            startServerSocketThread();
            displayHomeScreen();
        } catch (Exception e) {
            Toast.makeText(getApplicationContext(), "שגיאה באתחול: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onDestroy() {
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            // שגיאת סגירה
        }
        super.onDestroy();
    }

    private void requestStoragePermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission("android.permission.WRITE_EXTERNAL_STORAGE") != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{"android.permission.WRITE_EXTERNAL_STORAGE"}, 55);
            }
        }
    }

    private void checkAndRequestFileAccess() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!new File("/storage/emulated/0/").canWrite() && !android.os.Environment.isExternalStorageManager()) {
                Intent intent = new Intent(android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                intent.setData(Uri.parse("package:" + getPackageName()));
                startActivity(intent);
            }
        } else {
            try {
                if (!new File("/storage/emulated/0/").listFiles()[0].canWrite()) {
                    // לא עושה כלום - ההרשאה הכללית מספיקה
                }
            } catch (Exception e) {
                // במקרה של שגיאה, לא עושה כלום
            }
        }
    }

    private void initializeScreenDimensions() {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        Point point = new Point();
        WindowManager windowManager = (WindowManager) getApplicationContext().getSystemService(WINDOW_SERVICE);

        windowManager.getDefaultDisplay().getRealMetrics(displayMetrics);
        windowManager.getDefaultDisplay().getSize(point);

        try {
            if(getActionBar() != null) {
                getActionBar().hide();
            }
        }catch(Exception e){}

        deviceWidth = displayMetrics.widthPixels - 30; // רוחב מסך פחות שוליים קטנים
        deviceHeight = point.y;

        if (displayMetrics.widthPixels <= displayMetrics.heightPixels) {
            qrCodeSize = point.x;
        } else {
            qrCodeSize = point.y - 200; 
        }
        qrCodeSize -= 50; 
    }

    private void setupLayoutsAndViews() {
        horizontalScrollView = new HorizontalScrollView(context);
        verticalScrollView = new ScrollView(context);
        mainLayout = new LinearLayout(context);
        buttonLayout = new LinearLayout(context);
        titleContainer = new LinearLayout(context); 

        verticalScrollView.setLayoutParams(new FrameLayout.LayoutParams(deviceWidth, FrameLayout.LayoutParams.MATCH_PARENT));
        horizontalScrollView.setLayoutParams(new LinearLayout.LayoutParams(deviceWidth, FrameLayout.LayoutParams.MATCH_PARENT));

        mainLayout.setGravity(Gravity.CENTER);
        mainLayout.setOrientation(LinearLayout.VERTICAL);

        titleContainer.setGravity(Gravity.CENTER);
        titleContainer.setOrientation(LinearLayout.VERTICAL);

        instructionsButton = new Button(context);
        barcodeButton = new Button(context);
        wifiButton = new Button(context);
        hotspotButton = new Button(context);
        inputEditText = new EditText(context);

        statusTextView = new TextView(context);
        titleTextView = new TextView(context);
        instructionsTextView = new TextView(context);

        // הגדרות טקסט כלליות - שימוש ב-TextAppearance

        // Status Text (פונט גדול ומודגש)
        statusTextView.setGravity(Gravity.CENTER);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            statusTextView.setTextAppearance(android.R.style.TextAppearance_DeviceDefault_Medium);
        } else {
            statusTextView.setTextAppearance(context, android.R.style.TextAppearance_DeviceDefault_Medium);
        }
        statusTextView.setTextSize(20); // שמירת הגודל

        // Main Title Text (פונט גדול מאוד, מודגש, ועם קו תחתון)
        titleTextView.setGravity(Gravity.CENTER);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            titleTextView.setTextAppearance(android.R.style.TextAppearance_DeviceDefault_Large);
        } else {
            titleTextView.setTextAppearance(context, android.R.style.TextAppearance_DeviceDefault_Large);
        }
        titleTextView.setTextSize(35); // שמירת הגודל
        // **הוספת קו תחתון לכותרת הראשית**
        titleTextView.setPaintFlags(titleTextView.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG); 

        instructionsTextView.setTextSize(20);
        instructionsTextView.setGravity(Gravity.CENTER);
        instructionsTextView.setTextAppearance(context,android.R.style.TextAppearance_Material_Title);
        verticalScrollView.addView(mainLayout);
        horizontalScrollView.addView(verticalScrollView);
        setContentView(horizontalScrollView);
    }

    private void displayHomeScreen() {
        currentScreenState = "home";
        mainLayout.removeAllViews();
        titleContainer.removeAllViews();
        buttonLayout.removeAllViews();

        FrameLayout.LayoutParams scrollLayoutParams = new FrameLayout.LayoutParams(deviceWidth, FrameLayout.LayoutParams.MATCH_PARENT);
        scrollLayoutParams.setMargins(15, 15, 15, 15);
        horizontalScrollView.setLayoutParams(scrollLayoutParams);
        verticalScrollView.setLayoutParams(new FrameLayout.LayoutParams(deviceWidth, LinearLayout.LayoutParams.MATCH_PARENT));
        mainLayout.setLayoutParams(new FrameLayout.LayoutParams(deviceWidth, LinearLayout.LayoutParams.MATCH_PARENT));
        buttonLayout.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        buttonLayout.setGravity(Gravity.CENTER);
        buttonLayout.setOrientation(LinearLayout.HORIZONTAL);

        // 1. הבהרה (Warning)
        mainLayout.addView(createWarningLayout());

        // 2. כותרת והוראות

        // כותרת: "הוראות שימוש" - עכשיו עם קו תחתון מובנה
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(deviceWidth, LinearLayout.LayoutParams.WRAP_CONTENT);
        titleParams.topMargin = 30;
        titleTextView.setLayoutParams(titleParams);
        titleTextView.setText("הוראות שימוש");
        titleContainer.addView(titleTextView);

        // טקסט ההוראות
        LinearLayout.LayoutParams instructionsParams = new LinearLayout.LayoutParams(deviceWidth, LinearLayout.LayoutParams.WRAP_CONTENT);
        instructionsParams.topMargin = 30; // הגדלת המרווח מהכותרת
        instructionsParams.bottomMargin = 30;
        LinearLayout instructionsList = createInstructionsList(getResources().getString(R.string.in2));
        instructionsList.setLayoutParams(instructionsParams);
        titleContainer.addView(instructionsList);

        mainLayout.addView(titleContainer);

        // 3. כפתור ראשי
        LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(300, 100);
        buttonParams.gravity = Gravity.CENTER;

        barcodeButton.setLayoutParams(buttonParams);
        barcodeButton.setBackground(createButtonDrawable(barcodeButton));
        barcodeButton.setText("יצירת הברקוד");
        barcodeButton.setTextColor(Color.WHITE);
        barcodeButton.setOnClickListener(new OnClickListener(){
                @Override
                public void onClick(View v) {
                    mainLayout.removeAllViews();
                    verticalScrollView.scrollTo(0, 0);
                    currentScreenState = "barcode";
                    handleBarcodeGeneration();
                }
            });

        buttonLayout.addView(barcodeButton);
        mainLayout.addView(buttonLayout);
    }

    private void handleBarcodeGeneration() {
        deviceIpAddress = null;
        isNetworkAvailable = false;
        connectionState = null;

        detectDeviceIpAddress();

        if (!isNetworkAvailable || !new File(mdmApkPath).canRead()) {
            displayNetworkErrorScreen();
        } else {
            qrCodeBitmap = generateMdmQrCode(deviceIpAddress);
            displayBarcodeScreen();
        }
    }

    private void detectDeviceIpAddress() {
        String hotspotIp = getHotspotIpAddress();
        if (!TextUtils.isEmpty(hotspotIp)) {
            deviceIpAddress = hotspotIp;
            connectionState = "hotspot";
            isNetworkAvailable = true;
            return;
        }

        ConnectivityManager connManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connManager != null) {
            NetworkInfo mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
            if (mWifi != null && mWifi.isConnected()) {
                String wifiIp = getWifiIpAddress();
                if (!TextUtils.isEmpty(wifiIp)) {
                    deviceIpAddress = wifiIp;
                    connectionState = "wifi";
                    isNetworkAvailable = true;
                }
            }
        }
    }

    private String getWifiIpAddress() {
        final WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (wifiManager != null && wifiManager.isWifiEnabled()) {
            int ip = wifiManager.getConnectionInfo().getIpAddress();
            return (ip & 0xFF) + "." + ((ip >> 8) & 0xFF) + "." + ((ip >> 16) & 0xFF) + "." + ((ip >> 24) & 0xFF);
        }
        return null;
    }

    @Nullable
    public String getHotspotIpAddress() {
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
                NetworkInterface networkInterface = en.nextElement();
                if (networkInterface.getName().equals("ap0")) {
                    for (Enumeration<InetAddress> enumIpAddr = networkInterface.getInetAddresses(); enumIpAddr.hasMoreElements();) {
                        InetAddress inetAddress = enumIpAddr.nextElement();
                        if (!inetAddress.isLoopbackAddress() && inetAddress instanceof Inet4Address) {
                            String host = inetAddress.getHostAddress();
                            if (!TextUtils.isEmpty(host) && !host.equals(getWifiIpAddress())) {
                                return host;
                            }
                        }
                    }
                }
            }
        } catch (Exception ex) {
            // שגיאה בקריאת ממשקי רשת
        }
        return null;
    }

    private void startServerSocketThread() {
        new Thread(){
            @Override
            public void run() {
                try {
                    if(serverSocket != null && !serverSocket.isClosed()){
                        serverSocket.close();
                    }
                    serverSocket = new ServerSocket(SERVER_PORT);
                    while (true) {
                        serverSocket.setReuseAddress(true);
                        Socket socket = serverSocket.accept();
                        clientSocket = socket;
                        new Thread(new ClientConnectionHandler(clientSocket)).start();
                    }
                } catch (Exception e) {
                    // שגיאת שרת
                }
            }
        }.start();
    }

    // קלאס פנימי לטיפול בחיבור לקוח
    private class ClientConnectionHandler implements Runnable {
        private Socket socket;

        public ClientConnectionHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            BufferedReader reader = null;
            BufferedOutputStream bufferedOutputStream = null;
            PrintWriter writer = null;
            FileInputStream fileInputStream = null;

            try {
                writer = new PrintWriter(socket.getOutputStream());
                bufferedOutputStream = new BufferedOutputStream(socket.getOutputStream());
                reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                String requestLine = reader.readLine();
                if (requestLine == null) {
                    return;
                }

                StringTokenizer tokenizer = new StringTokenizer(requestLine);

                // שימוש מפורש ב-nextToken() לשמירה על המבנה הקודם
                String requestMethod = tokenizer.nextToken().toUpperCase(); 
                String nextTokenValue = tokenizer.nextToken().toLowerCase(); 

                String fileRequestedPath = nextTokenValue;

                // הסרת פרמטרים (query parameters)
                if (fileRequestedPath.contains("?")) {
                    fileRequestedPath = fileRequestedPath.split("\\?")[0];
                }

                // רק אם הבקשה היא לקובץ ה-MDM
                if (fileRequestedPath.equals(MDM_APK_NAME)) {
                    // עדכון סטטוס ב-UI
                    runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                downloadStatus = "זוהתה בקשת הורדת MDM";
                                statusTextView.setText(downloadStatus);
                            }
                        });

                    File mdmFile = new File(mdmApkPath);
                    if (!mdmFile.exists() || !mdmFile.canRead()) {
                        sendNotFoundResponse(writer);
                        return;
                    }

                    int fileLength = (int) mdmFile.length();
                    String mimeType = getMimeType(mdmApkPath, context);

                    // כותרות HTTP
                    writer.println("HTTP/1.1 200 OK");
                    writer.println("Server: MDM Provisioning Server");
                    writer.println("Date: " + new Date());
                    writer.println("Content-type: " + mimeType);
                    writer.println("Content-length: " + fileLength);
                    writer.println(); 
                    writer.flush();

                    // העברת תוכן הקובץ
                    fileInputStream = new FileInputStream(mdmFile);
                    byte[] buffer = new byte[8192];
                    int bytesRead;
                    while ((bytesRead = fileInputStream.read(buffer)) > 0) {
                        bufferedOutputStream.write(buffer, 0, bytesRead);
                    }
                    bufferedOutputStream.flush();

                    // עדכון סטטוס הצלחה
                    runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                downloadStatus = "נשלח בהצלחה";
                                statusTextView.setText(downloadStatus);
                            }
                        });
                }
            } catch (FileNotFoundException fnfe) {
                // שגיאת קובץ
            } catch (IOException ioe) {
                // שגיאת רשת/חיבור
            } finally {
                // סגירת משאבים
                try {
                    if (fileInputStream != null) fileInputStream.close();
                    if (reader != null) reader.close();
                    if (writer != null) writer.close();
                    if (bufferedOutputStream != null) bufferedOutputStream.close();
                    if (socket != null && !socket.isClosed()) socket.close();
                } catch (Exception e) {
                    // שגיאת סגירת stream
                }
            }
        }

        private void sendNotFoundResponse(PrintWriter writer) {
            writer.println("HTTP/1.1 404 Not Found");
            writer.println("Server: MDM Provisioning Server");
            writer.println("Content-type: text/html");
            writer.println();
            writer.println("<h1>404 File Not Found</h1>");
            writer.flush();
        }
    }

    public Bitmap generateMdmQrCode(String ipAddress) {
        String adminReceiverName = getMdmReceiverComponentName(mdmApkPath);
        String signatureChecksum = getMdmSignatureChecksum(mdmApkPath);

        // יצירת מחרוזת ה-JSON ל-DPC Provisioning כולל הפרמטר הריק
        String qrContent = "{\n" +
            "    \"android.app.extra.PROVISIONING_DEVICE_ADMIN_COMPONENT_NAME\":\n" +
            "    \"" + adminReceiverName + "\",\n" +
            "\"android.app.extra.PROVISIONING_DEVICE_ADMIN_SIGNATURE_CHECKSUM\":\n" +
            "\"" + signatureChecksum + "\",\n" +
            "\"android.app.extra.PROVISIONING_DEVICE_ADMIN_PACKAGE_DOWNLOAD_LOCATION\":\n" +
            "\"http://" + ipAddress + ":" + SERVER_PORT + MDM_APK_NAME + "\",\n" +
            "    \"android.app.extra.PROVISIONING_SKIP_ENCRYPTION\": true,\n" +
            "    \"android.app.extra.PROVISIONING_LEAVE_ALL_SYSTEM_APPS_ENABLED\": true\n" +
            "}";

        return encodeToQrCode(qrContent, qrCodeSize, qrCodeSize);
    }

    public static Bitmap encodeToQrCode(String text, int width, int height) {
        QRCodeWriter writer = new QRCodeWriter();
        try {
            BitMatrix matrix = writer.encode(text, BarcodeFormat.QR_CODE, width, height);
            Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);

            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    bmp.setPixel(x, y, matrix.get(x, y) ? Color.BLACK : Color.WHITE);
                }
            }
            return bmp;
        } catch (WriterException ex) {
            ex.printStackTrace();
            return null;
        }
    }

    private String getMdmReceiverComponentName(String apkPath){
        String receiverName = "";
        try{
            PackageInfo packageInfo = getPackageManager().getPackageArchiveInfo(apkPath, PackageManager.GET_SIGNATURES | PackageManager.GET_RECEIVERS);

            if (packageInfo != null && packageInfo.receivers != null) {
                for(ActivityInfo receiver : packageInfo.receivers){
                    if(receiver.name.toLowerCase().contains("admin")){
                        receiverName = receiver.name;
                        break;
                    }
                }
            }

            if(!receiverName.equals("")){
                return packageInfo.packageName + "/" + receiverName;
            }
        }catch(Exception e){
            Toast.makeText(getApplicationContext(), "שגיאה ב-MdmReceiverComponentName: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
        return "";
    }

    private String getMdmSignatureChecksum(String apkPath){
        String signatureBase64 = null;
        try{
            PackageInfo packageInfo = getPackageManager().getPackageArchiveInfo(apkPath, PackageManager.GET_SIGNATURES);

            if(packageInfo != null && packageInfo.signatures != null && packageInfo.signatures.length > 0) {
                Signature[] signatures = packageInfo.signatures;
                MessageDigest messageDigest = MessageDigest.getInstance("SHA256");

                for (Signature signature : signatures) {
                    messageDigest.update(signature.toByteArray());
                }

                byte[] digest = messageDigest.digest();
                signatureBase64 = Base64.encodeToString(digest, Base64.NO_WRAP);
            }
        } catch (Exception e) {
            Toast.makeText(getApplicationContext(), "שגיאה ב-MdmSignatureChecksum: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            return null;
        }

        if(signatureBase64 != null) {
            String checksum = signatureBase64
                .replace('+', '-')
                .replace('/', '_');

            if (checksum.endsWith("=")) {
                checksum = checksum.substring(0, checksum.lastIndexOf("="));
            }
            return checksum;
        }
        return null;
    }

    private void displayBarcodeScreen() {
        mainLayout.removeAllViews(); 

        // 1. קוד QR
        ImageView qrImageView = new ImageView(context);
        qrImageView.setImageBitmap(qrCodeBitmap);
        LinearLayout.LayoutParams qrParams = new LinearLayout.LayoutParams(qrCodeSize, qrCodeSize);
        qrParams.gravity = Gravity.CENTER;
        qrImageView.setLayoutParams(qrParams);
        mainLayout.addView(qrImageView);

        // 2. כפתורי שמירה ורענון
        LinearLayout actionsLayout = new LinearLayout(context);
        LinearLayout.LayoutParams actionsParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        actionsParams.topMargin = 50;
        actionsLayout.setLayoutParams(actionsParams);
        actionsLayout.setGravity(Gravity.CENTER);
        actionsLayout.setOrientation(LinearLayout.HORIZONTAL);

        Button saveButton = new Button(context);
        saveButton.setText("שמור ברקוד");
        saveButton.setTextColor(Color.WHITE);
        saveButton.setBackground(createButtonDrawable(saveButton));
        LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        buttonParams.setMargins(10, 0, 10, 0); 
        saveButton.setLayoutParams(buttonParams);

        saveButton.setOnClickListener(new OnClickListener(){
                @Override
                public void onClick(View v) {
                    saveQrCodeBitmap();
                }
            });
        actionsLayout.addView(saveButton);

        Button refreshButton = new Button(context);
        refreshButton.setText("רענון...");
        refreshButton.setTextColor(Color.WHITE);
        refreshButton.setBackground(createButtonDrawable(refreshButton));
        refreshButton.setLayoutParams(buttonParams);

        refreshButton.setOnClickListener(new OnClickListener(){
                @Override
                public void onClick(View v) {
                    mainLayout.removeAllViews();
                    handleBarcodeGeneration();
                }
            });
        actionsLayout.addView(refreshButton);
        mainLayout.addView(actionsLayout);

        // 3. סטטוס חיבור
        if (connectionState.equals("hotspot")) {
            statusTextView.setText("מצב נקודה חמה (IP: " + deviceIpAddress + ")");
        } else if (connectionState.equals("wifi")) {
            statusTextView.setText("מצב Wi-Fi (IP: " + deviceIpAddress + ")");
        }
        LinearLayout.LayoutParams statusParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        statusParams.topMargin = 30;
        statusTextView.setLayoutParams(statusParams);
        statusTextView.setGravity(Gravity.CENTER);
        mainLayout.addView(statusTextView);

        // 4. כותרת והוראות המשך 
        titleContainer.removeAllViews();
        LinearLayout.LayoutParams containerParams = new LinearLayout.LayoutParams(deviceWidth, LinearLayout.LayoutParams.WRAP_CONTENT);
        //containerParams.topMargin = 30;
        titleContainer.setLayoutParams(containerParams);
        titleContainer.setGravity(Gravity.CENTER);

        // כותרת
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(deviceWidth, LinearLayout.LayoutParams.WRAP_CONTENT);
        titleTextView.setLayoutParams(titleParams);
        titleTextView.setText("המשך");
        // **הוספת קו תחתון לכותרת המשך**
        titleTextView.setPaintFlags(titleTextView.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG); 
        titleContainer.addView(titleTextView);

        // טקסט ההוראות
        LinearLayout.LayoutParams instructionsParams = new LinearLayout.LayoutParams(deviceWidth, LinearLayout.LayoutParams.WRAP_CONTENT);
        //instructionsParams.topMargin = 30;
        //instructionsParams.bottomMargin = 10;
        LinearLayout instructionsList = createInstructionsList(getResources().getString(R.string.in3)); 
        instructionsList.setLayoutParams(instructionsParams);
        titleContainer.addView(instructionsList);

        mainLayout.addView(titleContainer);

        // הסרת קו תחתון לאחר השימוש בו כדי שה-titleTextView יחזור למצב רגיל במסך הבא
        // שמירה על הגודל המקורי באמצעות Appearance
        /*if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            titleTextView.setTextAppearance(android.R.style.TextAppearance_DeviceDefault_Large);
        } else {
            titleTextView.setTextAppearance(context, android.R.style.TextAppearance_DeviceDefault_Large);
        }
        titleTextView.setTextSize(35);*/
        //titleTextView.setPaintFlags(titleTextView.getPaintFlags() & ~Paint.UNDERLINE_TEXT_FLAG);
    }

    private void displayNetworkErrorScreen() {
        mainLayout.removeAllViews(); 
        mainLayout.setGravity(Gravity.CENTER);

        LinearLayout.LayoutParams statusParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        statusTextView.setLayoutParams(statusParams);
        statusTextView.setGravity(Gravity.CENTER);
        statusTextView.setText("פתח נקודה חמה או חבר לאותו ויפי");
        mainLayout.addView(statusTextView);

        Button refreshButton = new Button(context);
        refreshButton.setText("רענון...");
        refreshButton.setTextColor(Color.WHITE);
        refreshButton.setBackground(createButtonDrawable(refreshButton));
        LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(300, 100);
        buttonParams.gravity = Gravity.CENTER;
        refreshButton.setLayoutParams(buttonParams);

        refreshButton.setOnClickListener(new OnClickListener(){
                @Override
                public void onClick(View v) {
                    mainLayout.removeAllViews();
                    handleBarcodeGeneration();
                }
            });
        mainLayout.addView(refreshButton);
    }

    private void saveQrCodeBitmap() {
        if (qrCodeBitmap == null) return;

        String fileName = "ברקוד.png";
        File outputFile = new File("/storage/emulated/0/Pictures/", fileName); 

        try {OutputStream outputStream = new FileOutputStream(outputFile);
            qrCodeBitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
            Toast.makeText(getApplicationContext(), "הברקוד נשמר ב: " + outputFile.getAbsolutePath(), Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(getApplicationContext(), "שגיאה בשמירה: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private LinearLayout createWarningLayout() {
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        ViewGroup.MarginLayoutParams layoutParams = new ViewGroup.MarginLayoutParams(deviceWidth, LinearLayout.LayoutParams.WRAP_CONTENT);
        layoutParams.setMargins(0, 10, 0, 10); 
        layout.setLayoutParams(layoutParams);
        layout.setGravity(Gravity.CENTER);

        // title (מודגש)
        TextView title = new TextView(context);
        title.setText("הבהרה:");
        title.setGravity(Gravity.CENTER);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            //title.setTextAppearance(android.R.style.TextAppearance_DeviceDefault_Medium);
            title.setTextAppearance(android.R.style.TextAppearance_Material_Title);
        } else {
            //title.setTextAppearance(context, android.R.style.TextAppearance_DeviceDefault_Medium);
            title.setTextAppearance(context, android.R.style.TextAppearance_Material_Title);
        }
        title.setTextSize(25);

        // text (מודגש)
        TextView text = new TextView(context);
        text.setText(getResources().getString(R.string.sq)); 
        text.setGravity(Gravity.CENTER);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            //text.setTextAppearance(android.R.style.TextAppearance_DeviceDefault_Medium);
            text.setTextAppearance(android.R.style.TextAppearance_Material_Title);
        } else {
            //text.setTextAppearance(context, android.R.style.TextAppearance_DeviceDefault_Medium);
            text.setTextAppearance(context, android.R.style.TextAppearance_Material_Title);
        }
        text.setTextSize(20);

        //layout.addView(title);
        layout.addView(text);
        layout.setBackground(createWarningDrawable());
        return layout;
    }

    public GradientDrawable createButtonDrawable(final View view) {
        final GradientDrawable drawable = new GradientDrawable();
        drawable.setCornerRadius(35); 
        drawable.setColor(Color.parseColor("#ff006D52"));
        drawable.setShape(GradientDrawable.RECTANGLE);

        view.setOnTouchListener(new OnTouchListener(){
                @Override
                public boolean onTouch(final View v, MotionEvent event) {
                    if (event.getAction() == MotionEvent.ACTION_DOWN) {
                        drawable.setColor(Color.parseColor("#ff77ff92")); 
                    } else if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
                        new Handler().postDelayed(new Runnable(){
                                @Override
                                public void run() {
                                    drawable.setColor(Color.parseColor("#ff006D52"));
                                }
                            }, 300);
                    }
                    return false;
                }
            });
        return drawable;
    }

    public GradientDrawable createWarningDrawable() {
        final GradientDrawable drawable = new GradientDrawable();
        drawable.setCornerRadius(35); 
        drawable.setColor(Color.parseColor("#ffCEEBDE")); 
        drawable.setShape(GradientDrawable.RECTANGLE);
        return drawable;
    }

    public LinearLayout createInstructionsList(String instructionsText) {
        boolean isTitle = false;
        LinearLayout listLayout = new LinearLayout(context);
        listLayout.setOrientation(LinearLayout.VERTICAL);
        listLayout.setGravity(Gravity.CENTER);
        listLayout.setLayoutParams(new LinearLayout.LayoutParams(deviceWidth, LinearLayout.LayoutParams.WRAP_CONTENT));

        String[] textSegments = instructionsText.split("--");

        for (String segment : textSegments) {
            if (isTitle) {
                listLayout.addView(createTitleTextView(segment));
                isTitle = false;
            } else {
                //String cleanedSegment = segment.trim();
                String cleanedSegment=segment;
                /*if (cleanedSegment.contains("\n")) {
                    if (cleanedSegment.startsWith("\n")) {
                        cleanedSegment = cleanedSegment.replaceFirst("\n", "");
                    }
                    if (cleanedSegment.endsWith("\n")) {
                        cleanedSegment = cleanedSegment.substring(0, cleanedSegment.lastIndexOf("\n"));
                    }
                }*/
                /*// if (cleanedSegment.contains("\n")) {
                if (cleanedSegment.contains("\n")) {
                    //cleanedSegment = cleanedSegment.replaceFirst("\n", "");
                }
                if (cleanedSegment.contains("\n")) {
                    //cleanedSegment = cleanedSegment.substring(0, cleanedSegment.lastIndexOf("\n"));
                }
                //}*/
                listLayout.addView(createNormalTextView(cleanedSegment));
                isTitle = true;
            }
        }
        return listLayout;
    }

    public TextView createTitleTextView(String text) {
        TextView textView = new TextView(context);
        textView.setGravity(Gravity.CENTER);

        // כותרות קטנות: שימוש ב-TextAppearance_Large (שבדרך כלל מודגש)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            textView.setTextAppearance(android.R.style.TextAppearance_DeviceDefault_Large);
            textView.setTextAppearance(android.R.style.TextAppearance_Material_Title);
        } else {
            textView.setTextAppearance(context, android.R.style.TextAppearance_DeviceDefault_Large);
            textView.setTextAppearance(context,android.R.style.TextAppearance_Material_Title);
        }
        textView.setTextSize(25); 

        textView.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        textView.setText(text);
        return textView;
    }

    public TextView createNormalTextView(String text) {
        TextView textView = new TextView(context);
        textView.setGravity(Gravity.CENTER);

        // טקסט רגיל: שימוש ב-TextAppearance_Medium (שבדרך כלל רגיל)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            //textView.setTextAppearance(android.R.style.TextAppearance_DeviceDefault_Medium);
            textView.setTextAppearance(android.R.style.TextAppearance_Material_Title);
        } else {
            //textView.setTextAppearance(context, android.R.style.TextAppearance_DeviceDefault_Medium);
            textView.setTextAppearance(context,android.R.style.TextAppearance_Material_Title);
        }
        textView.setTextSize(20); 

        textView.setText(text);
        return textView;
    }

    public String getMimeType(String absolutePath, Context context) {
        String fileName = getFileNameFromPath(absolutePath);
        String extension = getFileExtension(fileName);

        if (extension == null) {
            return "application/octet-stream"; 
        }

        String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.toLowerCase());

        if (mimeType == null || (mimeType != null && mimeType.endsWith("3gpp"))) {
            Cursor cursor = null;
            try {
                String[] projection = new String[]{"mime_type"};
                cursor = context.getContentResolver().query(MediaStore.Files.getContentUri("external"), projection, "_data = ?", new String[]{absolutePath}, null);
                if (cursor != null && cursor.moveToNext()) {
                    String type = cursor.getString(cursor.getColumnIndexOrThrow("mime_type"));
                    if (type != null) {
                        mimeType = type;
                    }
                }
            } catch (Exception e) {
                // שגיאה ב-ContentResolver
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        }

        if (mimeType == null) {
            if (extension.equalsIgnoreCase("apk")) {
                return "application/vnd.android.package-archive";
            }
            return "application/octet-stream";
        }
        return mimeType;
    }

    public static String getFileExtension(String fileName) {
        if (fileName == null) {
            return null;
        }
        int lastDot = fileName.lastIndexOf('.');
        if (lastDot >= 0) {
            return fileName.substring(lastDot + 1).toLowerCase();
        }
        return null;
    }

    public static String getFileNameFromPath(String absolutePath) {
        int sepIndex = absolutePath.lastIndexOf(File.separator);
        if (sepIndex >= 0) {
            return absolutePath.substring(sepIndex + 1);
        }
        return absolutePath;
    }

    @Deprecated
    @Override
    public void onBackPressed() {
        if (currentScreenState.equals("barcode")) {
            currentScreenState = "home";
            verticalScrollView.scrollTo(0,0);
            displayHomeScreen();
        } else if (currentScreenState.equals("home")) {
            super.onBackPressed();
        }
    }
}
