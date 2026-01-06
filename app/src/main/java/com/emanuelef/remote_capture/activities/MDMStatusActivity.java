package com.emanuelef.remote_capture.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;
import android.content.SharedPreferences;
import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.content.ComponentName;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import android.widget.LinearLayout;
import android.content.ClipboardManager;
import android.content.DialogInterface;
import android.app.AlertDialog;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
//import androidx.annotation.NonNull;
import android.content.pm.PackageManager;
import android.os.UserManager;
import android.os.Build;
import android.Manifest;
import android.provider.Settings;
import android.os.Environment;
import android.net.Uri;
import android.net.VpnService;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ScrollView;
import android.view.Gravity;
//import androidx.core.view.MenuProvider;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import android.graphics.Color;
import java.util.zip.ZipInputStream;
import java.io.FileInputStream;
import java.util.zip.ZipEntry;
import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.io.DataOutputStream;
import android.content.pm.ApplicationInfo;
import android.app.ProgressDialog;
import android.os.AsyncTask;

import com.emanuelef.remote_capture.R;
import com.emanuelef.remote_capture.BuildConfig;
import com.emanuelef.remote_capture.Utils;

import java.util.Properties;

import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import com.mdm.activities.storeActivity;
import android.app.PendingIntent;
import android.app.AlarmManager;
import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.animation.ObjectAnimator;
import android.graphics.drawable.Drawable;
import android.graphics.PorterDuff;
import android.graphics.drawable.LayerDrawable;
import android.animation.PropertyValuesHolder;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.preference.PreferenceManager;

@Deprecated
public class MDMStatusActivity extends Activity {
    
    public static DevicePolicyManager mDpm;
    public static ComponentName mAdminComponentName;
    SharedPreferences sp;
    SharedPreferences.Editor spe;
    public static final String modesp="mode";
    public static final String locksp="lock";
    LinearLayout linlidenta,linlidentb,linlbefact,linlactivatemult,linlinfo,linlactivate,linldetails;
    TextView tvappname,tvstate,tvtinst,tvtlogin,tvroute,tvdescription,tvremoveroot,tvstartbarcode;
    Button buactivatemult,bucpcmd,bucert,bucppwd,buinstruction,budeviceinfo,budev,buaccount,busavebarcode,bustartroot,buadbmult,buadbwifi,buqrmdm;
    ImageView ivbarcode;
    Bitmap bmp;
    InputStream is;
    Context mcon=this;
    EditText edtxd;
    AlertDialog alertDialogb;
    TextView tvtc,tvc;
    Button bud;
    private static ProgressDialog progressDialog;
    private static Activity mactivity;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Utils.setTheme(this);
        setContentView(R.layout.activity_mdm_status);
        mactivity=this;
        if(!hasManageExternalStoragePermission(this)){
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                requestManageExternalStoragePermission(this);
            } else if (!hasWriteExternalStoragePermission(this)) {
                requestWriteExternalStoragePermission(this);
            }
        }
        try{
            requestPermissions(new String[]{"android.permission.WRITE_EXTERNAL_STORAGE"},55);
        }catch(Exception e){}
        mDpm = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
        mAdminComponentName = new ComponentName(this,admin.class);
        sp=this.getSharedPreferences(this.getPackageName(),this.MODE_PRIVATE);
        spe=sp.edit();
        try{
        if(sp.getString(modesp,"").equals("")){
            if(AppState.getInstance()!=null){
            AppState.getInstance().setCurrentPath(PathType.MULTIMEDIA);
            spe.putString(modesp,AppState.getInstance().getCurrentPath().name());
            spe.commit();
                Toast.makeText(getApplicationContext(),AppState.getInstance().getCurrentPath().name()+" is default",1).show();
            }
        }else{
            try{
                AppState.getInstance().setCurrentPath(PathType.valueOf(sp.getString(modesp,"")));
                //Toast.makeText(this, AppState.getInstance().getCurrentPath().name()+ " is now",1).show();
            }catch(Exception e){
                Toast.makeText(getApplicationContext(), e+"",1).show();
                //importnt if it isnt found like old version
                AppState.getInstance().setCurrentPath(PathType.MULTIMEDIA);
                spe.putString(modesp,AppState.getInstance().getCurrentPath().name());
                spe.commit();
                Toast.makeText(getApplicationContext(),AppState.getInstance().getCurrentPath().name()+" is default",1).show();
            }
        }
        }catch(Exception e){}
        linlidenta=findViewById(R.id.act_stat_identa);
        linlidentb=findViewById(R.id.act_stat_identb);
        linlbefact=findViewById(R.id.act_stat_linlbefact);
        linlactivatemult=findViewById(R.id.act_stat_linlactivate_mult);
        buactivatemult=findViewById(R.id.btn_activate_mult);
        linlinfo=findViewById(R.id.act_stat_info);
        tvappname=findViewById(R.id.act_stat_tvappname);
        tvstate=findViewById(R.id.act_stat_tvstate);
        tvtinst=findViewById(R.id.act_stat_tvtinst);
        tvtlogin=findViewById(R.id.act_stat_tvtlogin);
        
        linlactivate=findViewById(R.id.act_stat_linlactivate);
        linldetails=findViewById(R.id.act_stat_linldetails);
        bucpcmd=findViewById(R.id.act_stat_bucpcmd);
        bucert=findViewById(R.id.act_stat_bucert);
        bucppwd=findViewById(R.id.act_stat_bucppwd);
        buinstruction=findViewById(R.id.act_stat_buinstruction);
        budeviceinfo=findViewById(R.id.act_stat_budeviceinfo);
        budev=findViewById(R.id.act_stat_budev);
        buaccount=findViewById(R.id.act_stat_buaccount);
        buadbmult=findViewById(R.id.act_stat_buadbmult);
        buadbwifi=findViewById(R.id.act_stat_buadbwifi);
        buqrmdm=findViewById(R.id.act_stat_buqrmdm);
        tvstartbarcode=findViewById(R.id.act_stat_tvstartbarcode);
        ivbarcode=findViewById(R.id.act_stat_ivbarcode);
        busavebarcode=findViewById(R.id.act_stat_busavebarcode);
        bustartroot=findViewById(R.id.act_stat_bustartroot);
        tvremoveroot=findViewById(R.id.act_stat_tvremove_root);
        tvroute=findViewById(R.id.act_stat_tvroute);
        tvdescription=findViewById(R.id.act_stat_tvdescription);
        
        tvappname.setText(getResources().getString(R.string.pcapdroid_app_name)+"\n"+Utils.getAppVersion(this));
        
        buactivatemult.setOnClickListener(new OnClickListener(){
                @Deprecated
                @Override
                public void onClick(View p1) {
                    Intent intent = new Intent(MDMStatusActivity.this, activityadbpair.class).putExtra("butt","disactenaccmult");
                    startActivity(intent);
                }
            });
        
        bucpcmd.setOnClickListener(new OnClickListener(){
                @Deprecated
                @Override
                public void onClick(View p1) {
                    ClipboardManager clbo= (ClipboardManager)getSystemService(CLIPBOARD_SERVICE);
                    clbo.setText("dpm set-device-owner com.emanuelef.remote_capture.debug/com.emanuelef.remote_capture.activities.admin");
                    Toast.makeText(getApplicationContext(), "הועתק ללוח!",1).show();
                    
                }
            });
        bucppwd.setOnClickListener(new OnClickListener(){
                @Deprecated
                @Override
                public void onClick(View p1) {
                    ClipboardManager clbo= (ClipboardManager)getSystemService(CLIPBOARD_SERVICE);
                    clbo.setText("john@tw-desktop");
                    Toast.makeText(getApplicationContext(), "הועתק ללוח!",1).show();
                    
                }
            });
        buinstruction.setOnClickListener(new OnClickListener(){
                @Deprecated
                @Override
                public void onClick(View p1) {
                    Intent intent = new Intent(MDMStatusActivity.this, instructionactivity.class).putExtra("name","mdm");
                    startActivity(intent);
                }
            });
        budeviceinfo.setOnClickListener(new OnClickListener(){
                @Deprecated
                @Override
                public void onClick(View p1) {
                    try{
                        Intent intent = new Intent().setClassName("com.android.settings","com.android.settings.Settings$MyDeviceInfoActivity");
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                    }catch(Exception e){
                        
                    }
                }
            });
        budev.setOnClickListener(new OnClickListener(){
                @Deprecated
                @Override
                public void onClick(View p1) {
                    try{
                    Intent intent = new Intent().setClassName("com.android.settings","com.android.settings.Settings$DevelopmentSettingsDashboardActivity");
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    }catch(Exception e){
                        try{
                            Intent intent = new Intent().setClassName("com.android.settings","com.android.settings.Settings$DevelopmentSettingsActivity");
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(intent);
                        }catch(Exception ee){}
                    }
                }
            });
        buaccount.setOnClickListener(new OnClickListener(){
                @Deprecated
                @Override
                public void onClick(View p1) {
                    try{
                    Intent intent = new Intent().setClassName("com.android.settings","com.android.settings.Settings$AccountDashboardActivity");
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    }catch(Exception e){
                        /*try{
                            Intent intent = new Intent().setClassName("com.android.settings","com.android.settings.Settings$DevelopmentSettingsActivity");
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(intent);
                        }catch(Exception ee){}*/
                    }
                }
            });
        buadbmult.setOnClickListener(new OnClickListener(){

                @Override
                public void onClick(View p1) {
                    Intent intent = new Intent(MDMStatusActivity.this, activityadbpair.class);
                    startActivity(intent);
                }
            });
        buadbwifi.setOnClickListener(new OnClickListener(){

                @Override
                public void onClick(View p1) {
                    Intent intent = new Intent(MDMStatusActivity.this, nsdactivity.class);
                    startActivity(intent);
                }
            });
        buqrmdm.setOnClickListener(new OnClickListener(){

                @Override
                public void onClick(View p1) {
                    Intent intent = new Intent(MDMStatusActivity.this, MdmProvisioningActivity.class);
                    startActivity(intent);
                }
            });
        bucert.setOnClickListener(new OnClickListener(){
                @Override
                public void onClick(View p1) {
                    try {
                        new Thread(){public void run() {
                                try {
                                    List<File> extractedApks = new ArrayList<File>();
                                    byte[] buffer = new byte[1024];
                                    ZipInputStream zis = null;
                                    try {
                                        zis = new ZipInputStream(getAssets().open("canetfree.zip"));
                                        ZipEntry zipEntry;
                                        while ((zipEntry = zis.getNextEntry()) != null) {
                                            String fileName = zipEntry.getName();
                                            File newFile = new File(getFilesDir() + "/", fileName);
                                            FileOutputStream fos = null;
                                            try {
                                                if (zipEntry.isDirectory()) {
                                                    newFile.mkdirs();
                                                } else {
                                                    fos = new FileOutputStream(newFile);
                                                    int len;
                                                    while ((len = zis.read(buffer)) > 0) {
                                                        fos.write(buffer, 0, len);
                                                    }
                                                }
                                                extractedApks.add(new File(fileName));
                                                mcp(newFile.toString(), "/data/adb/modules/canetfree/" + new File(fileName), newFile.isDirectory());
                                                mchmod("/data/adb/modules/canetfree/" + new File(fileName));
                                            } catch (Exception e) {
                                            } finally {
                                                if (fos != null) fos.close();
                                            }
                                            zis.closeEntry();
                                        }
                                    } finally {
                                        if (zis != null) zis.close();
                                    }
                                    /*String a="";
                                    for (File aa:extractedApks) {
                                        a += aa.getPath() + " " + (aa.isDirectory() ?"d": "f") + "\n";
                                    }
                                    String saveFile="/storage/emulated/0/a.txt";
                                    BufferedWriter out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(saveFile), "UTF_8"));//encoding="UTF_8"
                                    out.write(a);
                                    out.close();*/
                                    getMainExecutor().execute(new Runnable(){
                                            @Override
                                            public void run() {
                                                Toast.makeText(getApplicationContext(),"הצליח",1).show();
                                            }
                                        });
                                } catch (Exception e) {
                                }
                            }}.start();
                    } catch (Exception e) {}
                }
            });
        try {
            is= getAssets().open("barcode.png");
            
            if (null != is) {bmp = BitmapFactory.decodeStream(is);}
            
        } catch (IOException e) {}
        finally{
            try {
                if(is!=null){
                is.close();
                }
            } catch (Exception e) {}
        }
        if(bmp!=null){
            ivbarcode.setImageBitmap(bmp);
        }
        busavebarcode.setOnClickListener(new OnClickListener(){

                @Override
                public void onClick(View p1) {
                    
                    int read=0;
                    byte[] buf=new byte[1024];
                    try {
                        FileOutputStream fos= new FileOutputStream(new File("/storage/emulated/0/barcode.png"));
                        is= getAssets().open("barcode.png");
                    while ((read = is.read(buf))>0) {
                        fos.write(buf,0,read);
                    }
                        Toast.makeText(getApplicationContext(), "נשמר באיחסון פנימי!",1).show();
                    }catch(Exception e){
                        
                    }finally{
                        try {
                            if(is!=null){
                                is.close();
                            }
                        } catch (Exception e) {}
                    }
                }
            });
        bustartroot.setOnClickListener(new OnClickListener(){

                @Override
                public void onClick(View p1) {
                    startwithroot(MDMStatusActivity.this);
                }
            });
        refresh();
        
    }

    @Override
    protected void onStop() {
        super.onStop();
        if(sp.getBoolean("needChange",false)){
            changeIconAndForceRestart(mDpm.isDeviceOwnerApp(getPackageName()));
        }
    }
    private void checkAndFixLauncherIcon(boolean isActive) {
        // // 1. נניח שיש לך משתנה או פונקציה שבודקת אם ה-MDM פעיל באמת
        boolean isMdmActuallyActive = isActive; 

        PackageManager pm = getPackageManager();

        // // 2. הגדרת רכיבי ה-Alias (זוכר את השרשור: ירוק ואדום)
        ComponentName greenAlias = new ComponentName(this, "com.emanuelef.remote_capture.LauncherGreen");
        ComponentName redAlias = new ComponentName(this, "com.emanuelef.remote_capture.LauncherRed");

        // // 3. בדיקה איזה Alias מופעל כרגע במערכת
        int greenState = pm.getComponentEnabledSetting(greenAlias);
        int redState = pm.getComponentEnabledSetting(redAlias);

        // // 4. לוגיקת בדיקה ותיקון
        if (isMdmActuallyActive) {
            // // אם ה-MDM פעיל אבל האייקון הירוק לא מוגדר כ-Enabled
            if (greenState != PackageManager.COMPONENT_ENABLED_STATE_ENABLED) {
                /*
                 // // תיקון: הפעלת ירוק וכיבוי אדום
                 pm.setComponentEnabledSetting(greenAlias, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);
                 pm.setComponentEnabledSetting(redAlias, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
                 // // הערה: זה עלול לגרום לסגירה רגעית של האפליקציה כפי שדיברנו
                 */
                spe.putBoolean("needChange",true).commit();
            }
        } else {
            // // אם ה-MDM כבוי אבל האייקון האדום לא מוגדר כ-Enabled
            if (redState != PackageManager.COMPONENT_ENABLED_STATE_ENABLED) {
                /*
                 // // תיקון: הפעלת אדום וכיבוי ירוק
                 pm.setComponentEnabledSetting(redAlias, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);
                 pm.setComponentEnabledSetting(greenAlias, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
                 */
                spe.putBoolean("needChange",true).commit();
            }
        }
    }

// // פונקציית עזר לבדיקת הסטטוס האמיתי (למשל מתוך ה-DeviceAdmin או SharedPreferences)

    // // פונקציה להחלפת אייקון והפעלה מחדש עוקפת חסימות
    public void changeIconAndForceRestart(boolean isActive) {
        Context context = getApplicationContext();
        PackageManager pm = context.getPackageManager();

        ComponentName greenAlias = new ComponentName(context, "com.emanuelef.remote_capture.LauncherGreen");
        ComponentName redAlias = new ComponentName(context, "com.emanuelef.remote_capture.LauncherRed");
        // // הכנת ה-Intent שיפעיל את עצמנו מחדש
        Intent restartIntent = context.getPackageManager().getLaunchIntentForPackage(getPackageName());
        restartIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        // // שימוש ב-PendingIntent עם FLAG_IMMUTABLE
        PendingIntent pendingIntent = PendingIntent.getActivity(
            context, 0, restartIntent, PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // // הגדרת ה-Alarm בדיוק לעוד שנייה אחת
        AlarmManager mgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        mgr.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 1000, pendingIntent);
        spe.putBoolean("needChange",false).commit();
        // // ביצוע ההחלפה (זוכר את השרשור: מגן ירוק/אדום עם מנעול מוגבה)
        if (isActive) {
            pm.setComponentEnabledSetting(greenAlias, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);
            pm.setComponentEnabledSetting(redAlias, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
        } else {
            pm.setComponentEnabledSetting(redAlias, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);
            pm.setComponentEnabledSetting(greenAlias, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
        }
        
        // // סגירה כפויה של התהליך כדי שהשינוי יתפוס בלאונצ'ר
        android.os.Process.killProcess(android.os.Process.myPid());
    }
    public void updateMdmUltimateStatus(boolean isProtected) {
        ImageView shieldImg = findViewById(R.id.mdm_shield_view);
        LayerDrawable layers = (LayerDrawable) shieldImg.getDrawable();

        // // שליפת שכבות הצבע לשינוי (מילוי, מסגרת וחור מפתח)
        Drawable fill = layers.findDrawableByLayerId(R.id.shield_fill);
        Drawable stroke = layers.findDrawableByLayerId(R.id.shield_stroke);
        Drawable keyhole = layers.findDrawableByLayerId(R.id.keyhole);

        if (isProtected) {
            // // מצב מוגן (ירוק)
            int colorMain = 0xFF4CAF50;
            int colorDark = 0xFF1B5E20;

            fill.setColorFilter(colorMain, PorterDuff.Mode.SRC_ATOP);
            stroke.setColorFilter(colorDark, PorterDuff.Mode.SRC_ATOP);
            keyhole.setColorFilter(colorMain, PorterDuff.Mode.SRC_ATOP); // // החור הופך לירוק

            // // הפעלת אנימציה "חיה מאוד": ציפה + נשימה + הבהוב פנימי
            startUltimateAnimation(shieldImg, fill);

            // // כאן תוכל להוסיף לוגיקת DevicePolicyManager פעילה //
        } else {
            // // מצב לא מוגן (אדום)
            int colorMain = 0xFFE53935;
            int colorDark = 0xFFB71C1C;
            
            fill.setColorFilter(colorMain, PorterDuff.Mode.SRC_ATOP);
            stroke.setColorFilter(colorDark, PorterDuff.Mode.SRC_ATOP);
            keyhole.setColorFilter(colorMain, PorterDuff.Mode.SRC_ATOP);

            // // עצירת אנימציה וביצוע רעידת "אזהרה" קצרה
            shieldImg.clearAnimation();
            ObjectAnimator.ofFloat(shieldImg, "translationX", 0, 15, -15, 10, -10, 0).setDuration(500).start();

            // // כאן תוכל להוסיף לוגיקת ביטול הגבלות //
        }
    }

// // פונקציית האנימציה המורכבת: "חי מאוד" מכל הכיוונים
    private void startUltimateAnimation(ImageView view, final Drawable innerFill) {
        view.clearAnimation();

        // // 1. אנימציה חיצונית: "נשימה" (גדילה/קטנה) ו"ציפה" (למעלה/למטה)
        ObjectAnimator outerEffect = ObjectAnimator.ofPropertyValuesHolder(
            view,
            PropertyValuesHolder.ofFloat("scaleX", 1.0f, 1.07f),
            PropertyValuesHolder.ofFloat("scaleY", 1.0f, 1.07f),
            PropertyValuesHolder.ofFloat("translationY", 0f, -20f)
        );
        outerEffect.setDuration(1800);
        outerEffect.setRepeatCount(ValueAnimator.INFINITE);
        outerEffect.setRepeatMode(ValueAnimator.REVERSE);
        outerEffect.setInterpolator(new AccelerateDecelerateInterpolator());

        // // 2. אנימציה פנימית: "הבהוב אנרגיה" (שינוי שקיפות המילוי ליצירת Glow)
        ValueAnimator innerGlow = ValueAnimator.ofInt(180, 255);
        innerGlow.setDuration(900);
        innerGlow.setRepeatCount(ValueAnimator.INFINITE);
        innerGlow.setRepeatMode(ValueAnimator.REVERSE);
        innerGlow.addUpdateListener(new ValueAnimator.AnimatorUpdateListener(){

                @Override
                public void onAnimationUpdate(ValueAnimator anim) {

                    innerFill.setAlpha((int) anim.getAnimatedValue());
                }});

        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(outerEffect, innerGlow);
        animatorSet.start();
    }
    private void refresh(){
        try{
        PasswordManager.pwopen=false;
        boolean mdmstate=mDpm.isDeviceOwnerApp(getPackageName());
        tvstate.setText("מצב mdm - "+(mdmstate?"פעיל":"כבוי"));
        try {
        tvstate.setTextColor(mdmstate?Color.parseColor("#FF00FF00") :Color.parseColor("#ffff0000"));
        tvstate.setTextSize(25);
        try{
        String inst=new SimpleDateFormat("yyddMMHHmmss").format(new Date(getPackageManager().getPackageInfo(getApplicationInfo().packageName, 0).firstInstallTime));
        tvtinst.setText("תאריך התקנה - " + timestr(inst));
        } catch (Exception e) {}
        tvtinst.setTextSize(20);
        tvtinst.setGravity(Gravity.CENTER);
        tvtlogin.setText("תאריך הזדהות אחרונה - "+timestr(sp.getString("timepw","000000000000")));
        tvtlogin.setTextSize(20);
        tvtlogin.setGravity(Gravity.CENTER);
        } catch (Exception e) {}
            checkAndFixLauncherIcon(mdmstate);
            updateMdmUltimateStatus(mdmstate);
        if(mdmstate){
            try{
                if(Build.VERSION.SDK_INT >= 24){
                    mDpm.setDeviceOwnerLockScreenInfo(mAdminComponentName,""+tvappname.getText());
                    mDpm.setOrganizationName(mAdminComponentName,getResources().getString(R.string.pcapdroid_app_name));
                }
            }catch (Exception e) {}
            linlidenta.setVisibility(View.GONE);
            linlidentb.setVisibility(View.GONE);
            linlbefact.setVisibility(View.GONE);
            linlactivatemult.setVisibility(View.GONE);
            linlactivate.setVisibility(View.GONE);
            linlinfo.setVisibility(View.VISIBLE);
            linldetails.setVisibility(View.VISIBLE);
            tvroute.setText("המסלול הפעיל - "+AppState.getInstance().getCurrentPath().getDescription());
            boolean vpnenabled=false;
            String strpkgvpn= mDpm.getAlwaysOnVpnPackage(mAdminComponentName);
            if(strpkgvpn!=null){
                vpnenabled=strpkgvpn.equals(getPackageName());
            }
            tvdescription.setText("מצב vpn - "+(vpnenabled?"פעיל":"כבוי"));
            tvdescription.setTextColor(vpnenabled?Color.parseColor("#FF00FF00") :Color.parseColor("#ffff0000"));
            tvdescription.setTextSize(25);
            if(vpnenabled){
                try {
                   VpnService.prepare(this);
                   p(mDpm, mAdminComponentName, this.getPackageName(), true);
                } catch (Exception e) {}
            }
        } else {
            Button bunextorskip=findViewById(R.id.btn_next_start);
            bunextorskip.setOnClickListener(new OnClickListener(){
                    @Override
                    public void onClick(View p1) {
                        linlidenta.setVisibility(View.GONE);
                        linlidentb.setVisibility(View.GONE);
                        linlactivatemult.setVisibility(View.GONE);
                        linlinfo.setVisibility(View.VISIBLE);
                        linlactivate.setVisibility(View.VISIBLE);
                    }
                });
            TextView buident= findViewById(R.id.welcomeidentify);
            buident.setText("המכשיר שזוהה - "+//Build.BOARD+"\n"+
                            "מכשיר="+Build.DEVICE+"\n"+
                            //  Build.DISPLAY+"\n"+
                            "מודל="+Build.MODEL+"\n"+
                            "בראנד="+Build.BRAND+"\n"+
                            "זיהוי="+Build.ID
                            //     Build.BOOTLOADER+"\n"+
                            

                            );
            String[] listidentmult={"Fanvace M36",
                "s9863a1h10",
                "uis7865_6h10_go",
                ""};
            boolean found= false;
            String curdevice=Build.DEVICE;
            for(String device:listidentmult){
                if(curdevice.toLowerCase().equals(device.toLowerCase())){

                    found=true;
                    break;
                }
            }
            
            if(found){
                buident.append("\nלמכשיר שלך יש אפשרות להפעיל את החסימה בלחצן אחד אחרי הכנת המכשיר! (-הפעלה למולטימדיה ועוד)");
                linlactivatemult.setVisibility(View.VISIBLE);
                bunextorskip.setText("בחירת סוג הפעלה ידנית");
            }else{
                buident.append("\nהמכשיר שלך לא נמצא כעת ברשימה שאפשר להפעיל עם לחצן אחד. תצטרך לבדוק איזה אפשרות מתאימה למכשיר שלך!"+"\n"+
                               "אם כן הצלחת להפעיל עם הפעלה למולטימדיה - שלח את פרטי המכשיר שזוהו באמצעות שלח מייל (-שיש למעלה ב3 נקודות)");
                linlactivatemult.setVisibility(View.GONE);
                bunextorskip.setText("עבור לבחירת סוג ההפעלה");
            }
            
            linlidenta.setVisibility(View.VISIBLE);
            linlidentb.setVisibility(View.VISIBLE);
            linlbefact.setVisibility(View.VISIBLE);
            linlinfo.setVisibility(View.GONE);
            linlactivate.setVisibility(View.GONE);
            linldetails.setVisibility(View.GONE);
        }
        }catch(Exception e){}
    }
    private void startwithroot(final Activity activity) {
            // אם לא פעיל, נבקש להפעיל
            /*
            Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
            intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, mAdminComponentName);
            intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "יישום זה דורש הרשאת מנהל מכשיר כדי ליישם מדיניות אבטחה.");
            startActivityForResult(intent, 0);
            */
            try {
                //String[] strar = {"/system/bin/sh","-c",""};
                String[] strar = {"su","-c",""};
                String ed="";
                //ed = edtx1.getText().toString();
                ed="dpm set-device-owner com.emanuelef.remote_capture.debug/com.emanuelef.remote_capture.activities.admin";
                //int i = 0;
                //ed.split(" ", i++);
                strar[2] = ed;
                //strar[i]=ed;
                String c ="";

                try {
                    Process exec=Runtime.getRuntime().exec(strar);
                    c += (exec.waitFor() == 0) ?"success:": "fail:";
                    exec.getOutputStream();
                    //c = exec.getInputStream().toString();
                    //c=exec.getOutputStream().toString();
                    BufferedReader bufferedReader=new BufferedReader(new InputStreamReader(exec.getInputStream()));
                    BufferedReader in=bufferedReader;
                    String st;
                    StringBuilder edtx1=new StringBuilder();
                    do {
                        st = in.readLine();
                        if (st != null) {
                            edtx1.append(st);
                            edtx1.append(String.valueOf("\n"));
                            continue;
                        }
                    } while (st != null);
                    in.close();
                    c += edtx1.toString();
                    bufferedReader = new BufferedReader(new InputStreamReader(exec.getErrorStream()));
                    in = bufferedReader;
                    st = "";
                    edtx1 = new StringBuilder();
                    do {
                        st = in.readLine();
                        if (st != null) {
                            edtx1.append(st);
                            edtx1.append(String.valueOf("\n"));
                            continue;
                        }
                    } while (st != null);
                    in.close();
                    c += edtx1.toString();
                    Toast.makeText(getApplicationContext(), "" + c, Toast.LENGTH_LONG).show();
                } catch (Exception e) {
                    Toast.makeText(getApplicationContext(), "error" + e, Toast.LENGTH_LONG).show();
                }
            } catch (Exception e) {
                Toast.makeText(getApplicationContext(), "error" + e, Toast.LENGTH_LONG).show();
            }
    }
       public  static  void p(DevicePolicyManager devicePolicyManager, ComponentName componentName, String string, boolean bl) throws PackageManager.NameNotFoundException {
        //   try {
        devicePolicyManager.setAlwaysOnVpnPackage(componentName, string, bl);
        //  } catch (Exception e) {
        //   Toast.makeText(getApplicationContext(),""+e,Toast.LENGTH_SHORT).show();
        // }
    }
    public static void showRemoveMDMConfirmationDialog(final Activity activity) {
        LinearLayout linl=new LinearLayout(activity);
        linl.setOrientation(LinearLayout.VERTICAL);
        TextView tvdes=new TextView(activity);
        tvdes.setGravity(Gravity.CENTER);
        tvdes.setTextSize(20);
        tvdes.setTextColor(Color.parseColor("#ffff0000"));
        tvdes.setText("האם אתה בטוח שברצונך להסיר את אפליקציית ה-MDM כמנהל המכשיר?\nאזהרה: אם הסתרת אפליקציות תצטרך להסיר את ההסתרה לפני ההסרה!\nלפעמים ישארו גם כל מיני השבתות שתצטרכו להסיר ידנית בניהול השבתות(כמו השבתת סטטוס בר שאם לא הסרתם את ההשבתה בפעם הבאה תצטרכו להשבית את זה שוב בהגדרות אפילו שזה מושבת ורק אחר כך תצליחו להסיר...)");
        Button buenall=new Button(activity);
        buenall.setBackgroundResource(R.drawable.rounded_button_background);
        buenall.setText("הסרת הסתרת כל האפליקציות");
        buenall.setOnClickListener(new OnClickListener(){
                @Override
                public void onClick(View p1) {
                    activity.getMainExecutor().execute(new Runnable(){
                            @Deprecated
                            @Override
                            public void run() {
                               new EnableAppsTask().execute();
                            }
                        });
                    
                }
            });
        Button butran=new Button(activity);
        butran.setBackgroundResource(R.drawable.rounded_button_background);
        butran.setText("העברת בעלות לאפליקציה אחרת");
        butran.setOnClickListener(new OnClickListener(){
                @Override
                public void onClick(View p1) {
                    Intent intent = new Intent(activity, transferOwner.class);
                    activity.startActivity(intent);
                }
            });
        linl.addView(buenall);
        linl.addView(butran);
        linl.addView(tvdes);
        new AlertDialog.Builder(activity)
            .setTitle("הסר ניהול מכשיר")
            .setView(linl)
            //.setMessage("האם אתה בטוח שברצונך להסיר את אפליקציית ה-MDM כמנהל המכשיר?\nאזהרה: אם הסתרת אפליקציות תצטרך להסיר את ההסתרה ידנית בניהול אפליקציות לפני ההסרה!")
            .setPositiveButton("כן, הסר", new DialogInterface.OnClickListener() {
                @Deprecated
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    mDpm = (DevicePolicyManager) activity.getSystemService(Context.DEVICE_POLICY_SERVICE);
                    mAdminComponentName = new ComponentName(activity, admin.class);
                    MDMSettingsActivity.removefrp(activity);
                    try{
                        mDpm.clearDeviceOwnerApp(activity.getPackageName());
                        Toast.makeText(activity.getApplicationContext(), "mdm removed", Toast.LENGTH_SHORT).show();
                    }catch(Exception e){
                        Toast.makeText(activity.getApplicationContext(), "" + e, Toast.LENGTH_SHORT).show();
                    }
                    try {
                        StringBuilder stringBuilder = new StringBuilder("package:");
                        stringBuilder.append(activity.getPackageName());
                        Uri parse = Uri.parse(stringBuilder.toString());
                        activity.startActivity(new Intent(Intent.ACTION_UNINSTALL_PACKAGE, parse).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                    } catch (Exception e) {
                        Toast.makeText(activity.getApplicationContext(), "" + e, Toast.LENGTH_SHORT).show();
                    }
                }
            })
            .setNegativeButton("ביטול", null)
            .show();
    }
    @Deprecated
    private static class EnableAppsTask extends AsyncTask<Void, Void, List<AppItem>> {
        @Deprecated
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressDialog = new ProgressDialog(mactivity);
            progressDialog.getWindow().setBackgroundDrawableResource(R.drawable.rounded_button_background);
            progressDialog.setMessage("מסיר הסתרת אפליקציות...");
            progressDialog.setCancelable(false);
            progressDialog.show();
        }
        @Deprecated
        @Override
        protected List<AppItem> doInBackground(Void... voids) {
            PackageManager pm = mactivity.getPackageManager();
            List<ApplicationInfo> installedApps = pm.getInstalledApplications(PackageManager.GET_META_DATA | PackageManager.MATCH_UNINSTALLED_PACKAGES);
            List<AppItem> appList = new ArrayList<AppItem>();

            for (ApplicationInfo appInfo : installedApps) {
                boolean isHiddenByMDM =false;
                try{
                    isHiddenByMDM = mDpm.isApplicationHidden(mAdminComponentName, appInfo.packageName);
                    if(isHiddenByMDM){
                        mDpm.setApplicationHidden(mAdminComponentName,appInfo.packageName,false);
                    }
                }catch(Exception e){}
            }
            return appList;
        }
        @Deprecated
        @Override
        protected void onPostExecute(List<AppItem> result) {
            super.onPostExecute(result);
            if (progressDialog != null && progressDialog.isShowing()) {
                progressDialog.dismiss();
            }
        }
    }
    @Override
    protected void onResume() {
        super.onResume();
        refresh();
    }
    
    @Override
    protected void onStart() {
        super.onStart();
        refresh();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        refresh();
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu_status, menu);
        return true;
    }
    /*@Override
    public void onCreateMenu(@NonNull Menu menu, MenuInflater menuInflater) {
        menuInflater.inflate(R.menu.main_menu_status, menu);
    }
    @Override
    public boolean onMenuItemSelected(@NonNull MenuItem item) {
        return false;
    }*/
    @Deprecated
    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        super.onOptionsItemSelected(item);
        Intent intent;
        switch(item.getItemId()) {
            case R.id.men_ite_sett:
                if(!sp.getBoolean(locksp,false)){
                    intent = new Intent(MDMStatusActivity.this, MDMSettingsActivity.class);
                    startActivity(intent);
                }
                return true;
            case R.id.men_ite_store:
                intent = new Intent(MDMStatusActivity.this, storeActivity.class);
                startActivity(intent);
                return true;
            case R.id.men_ite_instruct:
                intent = new Intent(MDMStatusActivity.this, InstructionsActivity.class);
                startActivity(intent);
                return true;
            case R.id.men_ite_about:
                intent = new Intent(MDMStatusActivity.this, AboutActivitya.class);
                startActivity(intent);
                return true;
            case R.id.men_ite_theme:
                intent = new Intent(MDMStatusActivity.this, ThemeActivity.class);
                startActivity(intent);
                return true;
            case R.id.men_ite_mail:
                sendm();
                return true;
            case R.id.men_ite_remove:
                if(!sp.getBoolean(locksp,false)){
                    PasswordManager.requestPasswordAndSave(new Runnable() {
                        @Deprecated
                        @Override
                        public void run() {
                            showRemoveMDMConfirmationDialog(MDMStatusActivity.this);
                        }
                    },MDMStatusActivity.this);
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
        
    }
	// Check if Manage External Storage permission is granted (for Android 11+)
    public static boolean hasManageExternalStoragePermission(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return Environment.isExternalStorageManager();
        } else {
            // For below Android 11, use normal READ/WRITE permissions
            int writePermission = context.checkPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, android.os.Process.myPid(), android.os.Process.myUid());
            return writePermission == PackageManager.PERMISSION_GRANTED;
        }
    }

    public static void requestManageExternalStoragePermission(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                intent.setData(Uri.parse("package:" + context.getPackageName()));
                context.startActivity(intent);
            } catch (Exception e) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                context.startActivity(intent);
            }
        }
    }
    public static boolean hasWriteExternalStoragePermission(Context context) {
        int permissionCheck = context.checkPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, android.os.Process.myPid(), android.os.Process.myUid());
        return permissionCheck == PackageManager.PERMISSION_GRANTED;
    }
    public static void requestWriteExternalStoragePermission(Activity activity) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            activity.requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 101);
        }
    }
    
    private String timestr(String mtime){
        try{
        String y=mtime.substring(0,2);
        String d=mtime.substring(2,4);
        String M=mtime.substring(4,6);
        String H=mtime.substring(6,8);
        String m=mtime.substring(8,10);
        String s=mtime.substring(10,12);
        mtime=y+"/"+d+"/"+M+" "+H+":"+m+":"+s;
        } catch (Exception e){}
        return mtime;
    }
        public void msendmail(final String md_email, final String md_password,final String body,final String[] recipients) {
        new Thread(){public void run() {
                try {
                    Properties props = new Properties();
                    props.put("mail.smtp.user", md_email);
                    props.put("mail.smtp.host", "smtp.gmail.com");
                    props.put("mail.smtp.port", "587");
                    props.put("mail.smtp.starttls.enable", "true");
                    props.put("mail.smtp.auth", "true");
                    props.put("mail.smtp.socketFactory.port", "587");
                    props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
                    props.put("mail.smtp.socketFactory.fallback", "true");
                    try {
                        Authenticator auth = new javax.mail.Authenticator() {
                            protected PasswordAuthentication getPasswordAuthentication() {
                                return new PasswordAuthentication(md_email, md_password);
                            }
                        };
                        Session session = Session.getInstance(props, auth);
                        MimeMessage msg = new MimeMessage(session);
                        String sub =mcon.getResources().getString(R.string.mailsub);
                        msg.setSubject(sub);
                        msg.setText(body);
                        /*
                         BodyPart messageBodyPart1 = new MimeBodyPart();
                         messageBodyPart1.setText("update"); 
                         MimeBodyPart messageBodyPart2 = new MimeBodyPart();
                         String filename = "/storage/emulated/0/Download/a.csv";//change accordingly
                         DataSource source = new FileDataSource(filename);
                         messageBodyPart2.setDataHandler(new DataHandler(source));
                         messageBodyPart2.setFileName("a.csv"); 
                         //5) create Multipart object and add MimeBodyPart objects to this object    
                         Multipart multipart = new MimeMultipart();
                         multipart.addBodyPart(messageBodyPart1);
                         multipart.addBodyPart(messageBodyPart2); 
                         //6) set the multiplart object to the message object
                         msg.setContent(multipart ); 
                         */

                        msg.setFrom(new InternetAddress(md_email));
                        //msg.addRecipient(Message.RecipientType.TO, new InternetAddress(md_targetemail));

                        InternetAddress[] recipientAddresses = new InternetAddress[recipients.length];
                        for (int i = 0; i < recipients.length; i++) {
                            recipientAddresses[i] = new InternetAddress(recipients[i]);
                        }
                        msg.addRecipients(Message.RecipientType.TO, recipientAddresses);
                        Transport.send(msg);
                        mcon.getMainLooper().myLooper().prepare();
                            Toast.makeText(getApplicationContext(), R.string.send_successful, 1).show();
                        mcon.getMainLooper().myLooper().loop();
                    } catch (MessagingException mex) {
                        mex.printStackTrace();
                        //res += mex;
                        mcon.getMainLooper().myLooper().prepare();
                            Toast.makeText(getApplicationContext(), "" + mex, 1).show();
                        mcon.getMainLooper().myLooper().loop();
                    }

                } catch (Exception e) {
                    mcon.getMainLooper().myLooper().prepare();
                        Toast.makeText(getApplicationContext(), "" + e, 1).show();
                    mcon.getMainLooper().myLooper().loop();
                }
            }}.start();
    }
    void sendm() {
        try {
            HorizontalScrollView hsv=new HorizontalScrollView(mcon);
            FrameLayout.LayoutParams flp=new FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT,FrameLayout.LayoutParams.WRAP_CONTENT);
            flp.gravity=Gravity.CENTER;
            //flp.setMargins(20,0,20,0);
            ScrollView sv=new ScrollView(mcon);
            LinearLayout linl=new LinearLayout(mcon);
            linl.setOrientation(linl.VERTICAL);
            linl.setGravity(Gravity.CENTER);
            tvtc=new TextView(mcon);
            
            tvtc.setTextAppearance(R.style.TextTitle);
            edtxd = new EditText(mcon);
            String mailbod =mcon.getResources().getString(R.string.mailbod);
            edtxd.setHint(mailbod);
            //edtxd.setInputType(2);
            tvc = new TextView(mcon);
            bud = new Button(mcon);
            bud.setBackgroundResource(R.drawable.rounded_button_background);
            bud.setText(R.string.send);
            linl.addView(tvtc);
            linl.addView(edtxd);
            linl.addView(tvc);
            linl.addView(bud);
            sv.addView(linl);
            hsv.addView(sv);
            final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(mcon);
            alertDialogBuilder.setView(hsv);
            alertDialogb = alertDialogBuilder.create();
            //alertDialoga.setContentView(hsv);
            //alertDialoga.setView(linl);
            
            bud.setOnClickListener(new OnClickListener(){
                    @Override
                    public void onClick(View p1) {
                        if (edtxd == null) {
                        } else {
                            String resa=edtxd.getText().toString();
                            if (!resa.equals("")) {
                                alertDialogb.hide();
                                String md_email="";
                                String md_password="";
                                md_email = "md_mail";
                                md_password = "md_pwd";
                                
                                String ad="****@gmail.com";
                                String mail_to = "";
                                mail_to = "md_mail_to";
                                String[] recipients = { mail_to };
                                msendmail(md_email, md_password,resa,recipients);
                            } else {
                                tvc.setText(R.string.empty);
                                Toast.makeText(getApplicationContext(), R.string.empty, Toast.LENGTH_LONG).show();
                            }
                        }
                    }
                });
                
                alertDialogb.show();
                hsv.setLayoutParams(flp);
                LinearLayout.LayoutParams llp=new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,LinearLayout.LayoutParams.WRAP_CONTENT);
                
                edtxd.setLayoutParams(llp);
                //edtxd.setWidth(100);
                edtxd.setTextSize(20);
                tvtc.setLayoutParams(llp);
                tvc.setLayoutParams(llp);
                bud.setLayoutParams(llp);
                //linl.setLayoutParams(flp);
                tvtc.setText(R.string.send_mail);
            tvtc.setTextSize(25);
            
        } catch (Exception e) {
            Toast.makeText(getApplicationContext(), e + "", Toast.LENGTH_LONG).show();
            //finish();
        }
    }
    String mount="mount -o rw,remount /vendor\nmount -o rw,remount /\nmount -o rw,remount /product\n";
    void mcp(String mfile, String mfiledestination, boolean isdir) {
        try {
            //String mfile="a";
            String mfilefrom=getFilesDir() + "/" + mfile;
            mfilefrom = mfile;

            //String mfiledestination="/system/system_ext/priv-app/Settings/a";
            //copyFile(mfile, mfilefrom);
            Process pr= Runtime.getRuntime().exec("su");
            DataOutputStream dos=new DataOutputStream(pr.getOutputStream());
            dos.writeBytes(mount);
            if (isdir) {
                dos.writeBytes("mkdir -p " + mfiledestination + "\nchmod 777 " + mfiledestination + "\n");
            } else {
                if (!new File(mfiledestination).getParentFile().exists()) {
                    dos.writeBytes("mkdir -p " + new File(mfiledestination).getParentFile().getAbsolutePath() + "\nchmod 777 " + mfiledestination + "\n");
                }
                dos.writeBytes("cp " + mfilefrom + " " + mfiledestination + "\nchmod 777 " + mfiledestination + "\n");
            }
            dos.writeBytes("exit\n");
            dos.flush();
            try {
                pr.waitFor();
                String c ="";
                BufferedReader bufferedReader=new BufferedReader(new InputStreamReader(pr.getInputStream()));
                BufferedReader in=bufferedReader;
                String st;
                StringBuilder edtx1=new StringBuilder();
                do {
                    st = in.readLine();
                    if (st != null) {
                        edtx1.append(st);
                        edtx1.append(String.valueOf("\n"));

                        continue;
                    }
                } while (st != null);
                in.close();
                c += edtx1.toString();
                bufferedReader = new BufferedReader(new InputStreamReader(pr.getErrorStream()));
                in = bufferedReader;
                st = "";
                edtx1 = new StringBuilder();
                do {
                    st = in.readLine();
                    if (st != null) {
                        edtx1.append(st);
                        edtx1.append(String.valueOf("\n"));

                        continue;
                    }
                } while (st != null);
                in.close();
                c += edtx1.toString();
                /*String saveFile="/storage/emulated/0/ab.txt";
                BufferedWriter out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(saveFile), "UTF_8"));//encoding="UTF_8"
                out.write(c);
                out.close();*/
                Toast.makeText(getApplicationContext(), "" + c, Toast.LENGTH_LONG).show();
            } catch (InterruptedException e) {}
        } catch (IOException e) {
            Toast.makeText(getApplicationContext(), "" + e, Toast.LENGTH_LONG).show();
        }
    }
    void mchmod(String mfiledestination) {
        try {
            Process pr= Runtime.getRuntime().exec("su");
            DataOutputStream dos=new DataOutputStream(pr.getOutputStream());
            dos.writeBytes(mount);
            dos.writeBytes("chmod 777 " + mfiledestination + "\n");
            dos.writeBytes("exit\n");
            dos.flush();
            try {
                pr.waitFor();
            } catch (InterruptedException e) {}
        } catch (IOException e) {
            Toast.makeText(getApplicationContext(), "" + e, Toast.LENGTH_LONG).show();
        }
    }
}
