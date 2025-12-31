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
import android.content.ClipboardManager;
import com.emanuelef.remote_capture.R;
import com.emanuelef.remote_capture.Utils;
import android.content.res.Resources;
import android.util.TypedValue;


public class instructionactivity extends Activity {

    // --- 2. משתני UI ו-Context ---
    private Context context;
    private EditText inputEditText;
    private TextView statusTextView, titleTextView, instructionsTextView; 
    private LinearLayout mainLayout, buttonLayout;
    private LinearLayout titleContainer; 
    private Button instructionsButton, barcodeButton, wifiButton, hotspotButton;
    private HorizontalScrollView horizontalScrollView;
    private ScrollView verticalScrollView;
    
    private int deviceWidth, deviceHeight;
    private int qrCodeSize;

    private String currentScreenState = "home"; // "home" or "barcode"
    private Bitmap qrCodeBitmap;
    String extra="";
    
    @Deprecated
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Utils.setTheme(this);
        context = this;
        //requestStoragePermissions();
        //checkAndRequestFileAccess();
        
        String getextra=getIntent().getStringExtra("name");
        if(getextra!=null){
            extra=getextra;
        }
        initializeScreenDimensions();
        setupLayoutsAndViews();

        try {
           // startServerSocketThread();
            displayHomeScreen();
        } catch (Exception e) {
            Toast.makeText(getApplicationContext(), "שגיאה באתחול: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onDestroy() {
        
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
        //instructionsTextView.setTextAppearance(context,android.R.style.TextAppearance_Material_Title);
        instructionsTextView.setTextAppearance(context,R.style.TextTitle);
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
        //titleContainer.addView(titleTextView);

        // טקסט ההוראות
        LinearLayout.LayoutParams instructionsParams = new LinearLayout.LayoutParams(deviceWidth, LinearLayout.LayoutParams.WRAP_CONTENT);
        instructionsParams.topMargin = 30; // הגדלת המרווח מהכותרת
        instructionsParams.bottomMargin = 30;
        LinearLayout instructionsList = null;
        if(extra.equals("mdm")){
            instructionsList = createInstructionsList(getResources().getString(R.string.in4));
        }else if(extra.equals("vpn")){
            instructionsList = createInstructionsList(getResources().getString(R.string.in5));
        }
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
                 //   handleBarcodeGeneration();
                }
            });

        buttonLayout.addView(barcodeButton);
        //mainLayout.addView(buttonLayout);
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
                   // saveQrCodeBitmap();
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
                    //handleBarcodeGeneration();
                }
            });
        actionsLayout.addView(refreshButton);
        mainLayout.addView(actionsLayout);

        // 3. סטטוס חיבור
        
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
                 //   handleBarcodeGeneration();
                }
            });
        mainLayout.addView(refreshButton);
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
        if(extra.equals("mdm")){
            text.setText(getResources().getString(R.string.sq2));
        }else if(extra.equals("vpn")){
            text.setText(getResources().getString(R.string.sq3));
        }
         
        text.setGravity(Gravity.CENTER);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            //text.setTextAppearance(android.R.style.TextAppearance_DeviceDefault_Medium);
            text.setTextAppearance(android.R.style.TextAppearance_Material_Title);
        } else {
            //text.setTextAppearance(context, android.R.style.TextAppearance_DeviceDefault_Medium);
            text.setTextAppearance(context, android.R.style.TextAppearance_Material_Title);
        }
        text.setTextSize(20);
        TypedValue tv = new TypedValue();
        this.getTheme().resolveAttribute(android.R.attr.colorPrimary, tv, true);
        //text.setTextColor(tv.data);
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
        TypedValue tv = new TypedValue();
        this.getTheme().resolveAttribute(R.attr.colorBackItem, tv, true);
        drawable.setColor(tv.data); 
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
        isTitle = true;
        for (String segment : textSegments) {
            if (isTitle) {
                listLayout.addView(createTitleTextView(segment));
                isTitle = false;
            } else {
                //String cleanedSegment = segment.trim();
                String cleanedSegment=segment;
                if(cleanedSegment.contains("__")){
                    String[] textbus = segment.split("__");
                    boolean isbus = false;
                    for (String bus : textbus) {
                        if (isbus) {
                            //detect bu
                            listLayout.addView(createBu(bus));
                            isbus = false;
                        } else {
                            //String cleanedSegment = segment.trim();
                            String cleanedbus=bus;


                            listLayout.addView(createNormalTextView(cleanedbus));
                            isbus = true;
                        }
                    }
                }else{
                    listLayout.addView(createNormalTextView(cleanedSegment));
                }
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
            //textView.setTextAppearance(android.R.style.TextAppearance_DeviceDefault_Large);
            //textView.setTextAppearance(android.R.style.TextAppearance_Material_Title);
            
            textView.setTextAppearance(R.style.TextTitle);
        } else {
            //textView.setTextAppearance(context, android.R.style.TextAppearance_DeviceDefault_Large);
            //textView.setTextAppearance(context,android.R.style.TextAppearance_Material_Title);
            textView.setTextAppearance(context,R.style.TextTitle);
            
        }
        textView.setTextSize(25); 
        textView.setPaintFlags(titleTextView.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
        textView.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        textView.setText(text);
        TypedValue tv = new TypedValue();
        this.getTheme().resolveAttribute(android.R.attr.textColorPrimary, tv, true);
        //textView.setTextColor(tv.data);
        return textView;
    }

    public TextView createNormalTextView(String text) {
        TextView textView = new TextView(context);
        textView.setGravity(Gravity.CENTER);

        // טקסט רגיל: שימוש ב-TextAppearance_Medium (שבדרך כלל רגיל)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            //textView.setTextAppearance(android.R.style.TextAppearance_DeviceDefault_Medium);
            //textView.setTextAppearance(android.R.style.TextAppearance_Material_Title);
            textView.setTextAppearance(R.style.TextTitle);
        } else {
            //textView.setTextAppearance(context, android.R.style.TextAppearance_DeviceDefault_Medium);
            //textView.setTextAppearance(context,android.R.style.TextAppearance_Material_Title);
            textView.setTextAppearance(context,R.style.TextTitle);
        }
        textView.setTextSize(20); 

        textView.setText(text);
        TypedValue tv = new TypedValue();
        this.getTheme().resolveAttribute(android.R.attr.textColorPrimary, tv, true);
        //textView.setTextColor(tv.data);
        return textView;
    }
    public Button createBu(final String text) {
        Button button = new Button(context);
        button.setGravity(Gravity.CENTER);

        // טקסט רגיל: שימוש ב-TextAppearance_Medium (שבדרך כלל רגיל)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            //textView.setTextAppearance(android.R.style.TextAppearance_DeviceDefault_Medium);
            //textView.setTextAppearance(android.R.style.TextAppearance_Material_Title);
            button.setTextAppearance(R.style.TextTitle);
        } else {
            //textView.setTextAppearance(context, android.R.style.TextAppearance_DeviceDefault_Medium);
            //textView.setTextAppearance(context,android.R.style.TextAppearance_Material_Title);
            button.setTextAppearance(context,R.style.TextTitle);
        }
        button.setTextSize(20);
        button.setPadding(14,14,14,14);
        TypedValue tv = new TypedValue();
        this.getTheme().resolveAttribute(android.R.attr.colorPrimary, tv, true);
        //textView.setTextColor(tv.data);
        button.setBackgroundResource(R.drawable.rounded_button_background);
        button.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        switch(text){
            case "bucp":
                button.setText(R.string.bucppwd);
                break;
            case "budev":
                button.setText(R.string.budev);
                break;
            case "bumult":
                button.setText(R.string.buadbmult);
                break;
            case "budisaccmult":
                button.setText(R.string.budisaccmult);
                break;
            case "buactivmult":
                button.setText(R.string.buactivmult);
                break;
            case "buactenaccmult":
                button.setText(R.string.buactenaccmult);
                break;
            case "budisactenaccmult":
                button.setText(R.string.budisactenaccmult);
                break;
            case "buaccounts":
                button.setText(R.string.buaccount);
                break;
            case "buinfo":
                button.setText(R.string.budeviceinfo);
                break;
            case "buwifi":
                button.setText(R.string.buwifi);
                break;
            case "buadbpair":
                button.setText(R.string.buadbwifi);
                break;
        }
        //textView.setText(text);
        button.setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View p1) {
                    Intent intent;
                    switch(text){
                        case "bucp":
                            ClipboardManager clbo= (ClipboardManager)getSystemService(CLIPBOARD_SERVICE);
                            clbo.setText("john@tw-desktop");
                            Toast.makeText(getApplicationContext(), "הועתק ללוח!",1).show();
                            
                            break;
                        case "budev":
                            try{
                                intent = new Intent().setClassName("com.android.settings","com.android.settings.Settings$DevelopmentSettingsDashboardActivity");
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(intent);
                            }catch(Exception e){
                                try{
                                    intent = new Intent().setClassName("com.android.settings","com.android.settings.Settings$DevelopmentSettingsActivity");
                                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                    startActivity(intent);
                                }catch(Exception ee){}
                            }
                            break;
                        case "bumult":
                            intent = new Intent(instructionactivity.this, activityadbpair.class);
                            startActivity(intent);
                            break;
                        case "budisaccmult":
                            intent = new Intent(instructionactivity.this, activityadbpair.class).putExtra("butt","disaccmult");
                            startActivity(intent);
                            break;
                        case "buactivmult":
                            intent = new Intent(instructionactivity.this, activityadbpair.class).putExtra("butt","activmult");
                            startActivity(intent);
                            break;
                        case "buactenaccmult":
                            intent = new Intent(instructionactivity.this, activityadbpair.class).putExtra("butt","actenaccmult");
                            startActivity(intent);
                            break;
                        case "budisactenaccmult":
                            intent = new Intent(instructionactivity.this, activityadbpair.class).putExtra("butt","disactenaccmult");
                            startActivity(intent);
                            break;
                        case "buaccounts":
                            try{
                                intent = new Intent().setClassName("com.android.settings","com.android.settings.Settings$AccountDashboardActivity");
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(intent);
                            }catch(Exception e){
                                /*try{
                                 Intent intent = new Intent().setClassName("com.android.settings","com.android.settings.Settings$DevelopmentSettingsActivity");
                                 intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                 startActivity(intent);
                                 }catch(Exception ee){}*/
                            }
                            break;
                        case "buinfo":
                            try{
                                intent = new Intent().setClassName("com.android.settings","com.android.settings.Settings$MyDeviceInfoActivity");
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(intent);
                            }catch(Exception e){

                            }
                            break;
                        case "buwifi":
                            try{
                                intent = new Intent().setClassName("com.android.settings","com.android.settings.Settings$WifiSettingsActivity");
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(intent);
                            }catch(Exception e){

                            }
                            break;
                        case "buadbpair":
                            intent = new Intent(instructionactivity.this, nsdactivity.class);
                            startActivity(intent);
                            break;
                    }
                }
            });
        return button;
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
