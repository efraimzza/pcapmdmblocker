package com.emanuelef.remote_capture.activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.UserManager;
import android.preference.PreferenceManager;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import com.emanuelef.remote_capture.R;
import com.emanuelef.remote_capture.Utils;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import android.util.Base64;

public class mbackupActivity extends Activity {
    public static String pickedfilepath="";
    public static String pickeddirpath="";
    
    EditText edtxd,edtxe,edtxf;
    AlertDialog alertDialogb;
    TextView tvtc,tvc;
    Button bud;
    boolean conti=false;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Utils.setTheme(this);
        setContentView(R.layout.activity_backup);
        try{
            if(getActionBar().isShowing())
                getActionBar().hide();
        }catch(Exception e){}
        Button btnexport = findViewById(R.id.btn_export);
        if (btnexport != null) {
            btnexport.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        PasswordManager.requestPasswordAndSave(new Runnable() {
                                @Override
                                public void run() {
                                    startActivity(new Intent(mbackupActivity.this,picker.class).putExtra("from","mbackupseldir"));
                                    
                                }
                            },mbackupActivity.this);
                    }
                });
        }
        Button btnimport = findViewById(R.id.btn_import);
        if (btnimport != null) {
            btnimport.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        PasswordManager.requestPasswordAndSave(new Runnable() {
                                @Override
                                public void run() {
                                    startActivity(new Intent(mbackupActivity.this,picker.class).putExtra("from","mbackuppickzip"));
                                }
                            },mbackupActivity.this);
                    }
                });
                }
        
        Button btnseturl = findViewById(R.id.btn_set_url);
        if (btnseturl != null) {
            btnseturl.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        PasswordManager.requestPasswordAndSave(new Runnable() {
                                @Override
                                public void run() {
                                    try{
                                    HorizontalScrollView hsv=new HorizontalScrollView(mbackupActivity.this);
                                    FrameLayout.LayoutParams flp=new FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT,FrameLayout.LayoutParams.WRAP_CONTENT);
                                    flp.gravity=Gravity.CENTER;
                                    //flp.setMargins(20,0,20,0);
                                    ScrollView sv=new ScrollView(mbackupActivity.this);
                                    LinearLayout linl=new LinearLayout(mbackupActivity.this);
                                    linl.setOrientation(linl.VERTICAL);
                                    linl.setGravity(Gravity.CENTER);
                                    tvtc=new TextView(mbackupActivity.this);

                                    tvtc.setTextAppearance(R.style.TextTitle);
                                    edtxd = new EditText(mbackupActivity.this);
                                    edtxd.setHint("בעלים");
                                    edtxe = new EditText(mbackupActivity.this);
                                    edtxe.setHint("שם-ריפו");
                                    edtxf = new EditText(mbackupActivity.this);
                                    edtxf.setHint("שם-לקוח");
                                    //edtxd.setInputType(2);
                                    tvc = new TextView(mbackupActivity.this);
                                    bud = new Button(mbackupActivity.this);
                                    bud.setBackgroundResource(R.drawable.rounded_button_background);
                                    bud.setText("שמירה");
                                    linl.addView(tvtc);
                                    linl.addView(edtxd);
                                    linl.addView(edtxe);
                                    linl.addView(edtxf);
                                    linl.addView(tvc);
                                    linl.addView(bud);
                                    sv.addView(linl);
                                    hsv.addView(sv);
                                    final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(mbackupActivity.this);
                                    alertDialogBuilder.setView(hsv);
                                    alertDialogb = alertDialogBuilder.create();
                                    //alertDialoga.setContentView(hsv);
                                    //alertDialoga.setView(linl);

                                    bud.setOnClickListener(new OnClickListener(){
                                            @Override
                                            public void onClick(View p1) {
                                                if (edtxd == null||edtxe == null||edtxf == null) {
                                                } else {
                                                    String resa=edtxd.getText().toString();
                                                    String resb=edtxe.getText().toString();
                                                    String resc=edtxf.getText().toString();
                                                    if ((!resa.equals("")==true)&&(!resb.equals("")==true)&&(!resc.equals("")==true)) {
                                                        alertDialogb.hide();
                                                        String url="https://raw.githubusercontent.com/"+resa+"/"+resb+"/refs/heads/main/"+resc+"/";
                                                        mbackupActivity.this.getSharedPreferences("backup", Context.MODE_PRIVATE).edit().putString("giturl",url).commit();
                                                        //lets ready to parse & save...
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
                                    edtxe.setLayoutParams(llp);
                                    //edtxe.setWidth(100);
                                    edtxe.setTextSize(20);
                                    edtxe.setText("mdmupdates");
                                    edtxf.setLayoutParams(llp);
                                    //edtxf.setWidth(100);
                                    edtxf.setTextSize(20);
                                    tvtc.setLayoutParams(llp);
                                    tvc.setLayoutParams(llp);
                                    bud.setLayoutParams(llp);
                                    //linl.setLayoutParams(flp);
                                    tvtc.setText("הגדרת מיקום אתר ליבוא");
                                    tvtc.setTextSize(25);
                                    }catch(Exception e){}
                                    //dialog set url path - repo owner repo name client name
                                    //"https://raw.githubusercontent.com/[owner]/[repo name]/refs/heads/main/[client]/"
                                    //save on pref whats not in backup - backup.xml to can update 1 file to more then 1 devices...
                                }
                            },mbackupActivity.this);
                    }
                });
        }
        String client=mbackupActivity.this.getSharedPreferences("backup", Context.MODE_PRIVATE).getString("giturl","");
        if(client!=null&&!client.equals("")){
            try{
        client=client.split("/")[8];
        }catch(Exception e){}
        }
        TextView tvclient=findViewById(R.id.tv_client);
        
        tvclient.setText("שם לקוח - "+client);
        
        Button btnurlimport = findViewById(R.id.btn_url_import);
        if (btnurlimport != null) {
            btnurlimport.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        try{
                            HorizontalScrollView hsv=new HorizontalScrollView(mbackupActivity.this);
                            FrameLayout.LayoutParams flp=new FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT,FrameLayout.LayoutParams.WRAP_CONTENT);
                            flp.gravity=Gravity.CENTER;
                            //flp.setMargins(20,0,20,0);
                            ScrollView sv=new ScrollView(mbackupActivity.this);
                            LinearLayout linl=new LinearLayout(mbackupActivity.this);
                            linl.setOrientation(linl.VERTICAL);
                            linl.setGravity(Gravity.CENTER);
                            tvtc=new TextView(mbackupActivity.this);

                            tvtc.setTextAppearance(R.style.TextTitle);
                            edtxd = new EditText(mbackupActivity.this);
                            edtxd.setHint("הזן קוד");
                            
                            //edtxd.setInputType(2);
                            tvc = new TextView(mbackupActivity.this);
                            bud = new Button(mbackupActivity.this);
                            bud.setBackgroundResource(R.drawable.rounded_button_background);
                            bud.setText("אישור");
                            linl.addView(tvtc);
                            linl.addView(edtxd);
                            
                            linl.addView(tvc);
                            linl.addView(bud);
                            sv.addView(linl);
                            hsv.addView(sv);
                            final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(mbackupActivity.this);
                            alertDialogBuilder.setView(hsv);
                            alertDialogb = alertDialogBuilder.create();
                            //alertDialoga.setContentView(hsv);
                            //alertDialoga.setView(linl);

                            bud.setOnClickListener(new OnClickListener(){
                                    @Override
                                    public void onClick(View p1) {
                                        if (edtxd == null) {
                                        } else {
                                            final String resa=edtxd.getText().toString();
                                            
                                            if ((!resa.equals("")==true)) {
                                                alertDialogb.hide();
                                                final String murl=mbackupActivity.this.getSharedPreferences("backup", Context.MODE_PRIVATE).getString("giturl","")+resa+"/";
                                                final String workpath=getDataDir()+"/backup/";
                                                
                                                if(new File(workpath).exists())deleteDir(new File(workpath));
                                                new File(workpath).mkdirs();
                                                
                                                final String readypath=workpath+"ready.txt";
                                                Utils.startDownload(mbackupActivity.this, murl+"ready.txt", readypath, 
                                                    new Runnable() {
                                                        @Override
                                                        public void run() {
                                                            //parse if eq 1
                                                            try{
                                                                BufferedReader in=new BufferedReader(new InputStreamReader(new FileInputStream(readypath)));
                                                                String st=in.readLine();
                                                                if(st!=null&&st.equals("1")){
                                                                    conti=true;
                                                                    if(conti){
                                                                        conti=false;
                                                                        final String zippath=workpath+"backup.zipmdm.txt";
                                                                        Utils.startDownload(mbackupActivity.this, murl+resa+".zipmdm.txt", zippath, 
                                                                            new Runnable() {
                                                                                @Override
                                                                                public void run() {
                                                                                    Toast.makeText(getApplicationContext(), "הורדה הצליחה", Toast.LENGTH_SHORT).show();
                                                                                    conti=true;
                                                                                    if(conti){
                                                                                        conti=false;
                                                                                        Toast.makeText(getApplicationContext(), "מתחיל עדכון", Toast.LENGTH_SHORT).show();
                                                                                        try {
                                                                                            StringBuilder strbui=new StringBuilder();
                                                                                            try {
                                                                                                BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(zippath)));
                                                                                                String st;
                                                                                                do {
                                                                                                    st = in.readLine();
                                                                                                    if (st != null) {
                                                                                                        strbui.append(st);
                                                                                                        strbui.append(String.valueOf("\n"));
                                                                                                        
                                                                                                        continue;
                                                                                                    }
                                                                                                } while (st != null);
                                                                                                in.close();
                                                                                            } catch (Exception e) {
                                                                                                LogUtil.logToFile(e.toString());
                                                                                            } 
                                                                                            FileOutputStream fos= new FileOutputStream(workpath+"backup.zipmdm");
                                                                                            fos.write(Base64.decode(strbui.toString(),0));
                                                                                            fos.close();
                                                                                            
                                                                                        } catch (Exception e) {}               
                                                                                        unzip(workpath+"backup.zipmdm");
                                                                                        Toast.makeText(getApplicationContext(), "עודכן!", Toast.LENGTH_SHORT).show();
                                                                                        Intent intent = new Intent(mbackupActivity.this, MDMStatusActivity.class);
                                                                                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                                                                        startActivity(intent);
                                                                                        Runtime.getRuntime().exit(0);
                                                                                    }else{
                                                                                        Toast.makeText(getApplicationContext(), "שגיאת הורדת קובץ גיבוי", Toast.LENGTH_LONG).show();
                                                                                    }
                                                                                }
                                                                            }, 
                                                                            new Runnable() {
                                                                                @Override
                                                                                public void run() {
                                                                                    Toast.makeText(getApplicationContext(), "שגיאת הורדת קובץ גיבוי", Toast.LENGTH_LONG).show();
                                                                                }
                                                                            }
                                                                        );

                                                                    }
                                                                }else{
                                                                    Toast.makeText(getApplicationContext(), "לא זמין", Toast.LENGTH_LONG).show();
                                                                    return;
                                                                }
                                                            }catch(Exception e){LogUtil.logToFile(e.toString());}
                                                            
                                                        }
                                                    }, 
                                                    new Runnable() {
                                                        @Override
                                                        public void run() {
                                                            Toast.makeText(getApplicationContext(), "שגיאת הורדה או שאין עדכון", Toast.LENGTH_LONG).show();
                                                        }
                                                    }
                                                );
                                                
                                                
                                                //lets ready to load & import...
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
                            tvtc.setText("עדכון הגדרות מרחוק");
                            tvtc.setTextSize(25);
                        }catch(Exception e){}
                        //dialog set folder name for client use
                        //"(https://raw.githubusercontent.com/[owner]/[repo name]/refs/heads/main/[client]/)=from pref set url+"[code]/ready.txt eq 1 & [code].zipmdm"
                        //path to download - getdatadir/backup
                        //check if code is available
                    }
                });
        }
    }
    protected void onResume() {
        super.onResume();
        if(pickedfilepath!=null&&!pickedfilepath.equals("")){
            String uri=pickedfilepath;
            pickedfilepath="";
            
            unzip(uri);

            Intent intent = new Intent(mbackupActivity.this, MDMStatusActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            Runtime.getRuntime().exit(0);
            //in main - if is restrictions file in pref = rest & dis ownerset apps
            
        }else if(pickeddirpath!=null&&!pickeddirpath.equals("")){
            String uri=pickeddirpath;
            pickeddirpath="";
            
            saverestrictions();
            boolean isHiddenByMDM =false;
            PackageManager pm = getPackageManager();
            List<ApplicationInfo> installedApps = pm.getInstalledApplications(PackageManager.GET_META_DATA | PackageManager.MATCH_UNINSTALLED_PACKAGES);
            List<String> ss=new ArrayList<>();

            Set<String> us= new HashSet<>(Arrays.asList());
            SharedPreferences mpref= PreferenceManager.getDefaultSharedPreferences(mbackupActivity.this);
            us=mpref.getStringSet("setapps", us);

            for (ApplicationInfo appInfo : installedApps) {
                try{
                    isHiddenByMDM = mDpm.isApplicationHidden(mAdminComponentName, appInfo.packageName);
                }catch(Exception e){}
                if(isHiddenByMDM&&!us.contains(appInfo.packageName))//isnt users hiden apps...
                    ss.add(appInfo.packageName);
            }
            String[] sa={};
            sa=ss.toArray(sa);
            Set<String> s= new HashSet<>(Arrays.asList(sa));
            SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(mbackupActivity.this).edit();
            editor.putStringSet("ownsetapps", s).commit();
            ziping(uri);
            deleteSharedPreferences("restriction");
            //add restrictions file
        }
        
    }
    void unzip(String uri){
        try {
            String zipname=uri;
            ZipInputStream zis=new ZipInputStream(new FileInputStream(zipname));
            ZipEntry ze= null;
            while((ze=zis.getNextEntry())!=null){
                if(!ze.getName().contains("backup.xml")){
                File f=new File(ze.getName());
                if(f.isDirectory())f.mkdirs();else if(!f.getParentFile().exists())f.getParentFile().mkdirs();
                FileOutputStream fos=new FileOutputStream(ze.getName());
                int i=0;
                byte[] byt=new byte[4096];
                while((i=zis.read(byt))!=-1){
                    fos.write(byt,0,i);
                }
                }
            }
        } catch (Exception e) {LogUtil.logToFile(e.toString());}
    }
    void ziping(String uri){
        ZipOutputStream zos=null;
        try{
            String zipname=uri+"/backup.zipmdm";
            List<String> filestocp=new ArrayList<>();
            List<String> prefstocp=new ArrayList<>();
            String path=getDataDir().getPath()+"/files/";
            if(new File(path).list()!=null)
                for(String f:new File(path).list()){
                    if(!new File(path+f).isDirectory())
                        filestocp.add(path+f);
                    else{
                        if(new File(path+f).list()!=null)
                        for(String ff:new File(path+f).list()){
                            if(!new File(path+f+"/"+ff).isDirectory())
                                filestocp.add(path+f+"/"+ff);
                        }
                    }
                }
            path=getDataDir().getPath()+"/shared_prefs/";
            if(new File(path).list()!=null)
                for(String f:new File(path).list()){
                    if(!f.contains("backup.xml"))
                    prefstocp.add(path+f);
                }

            zos=new ZipOutputStream(new FileOutputStream(zipname));

            for(String fn:filestocp){
                ZipEntry ze= new ZipEntry(fn);
                //ze.setComment("log");
                zos.putNextEntry(ze);
                int i=0;
                FileInputStream fos= new FileInputStream(fn);
                byte[] byt=new byte[4096];
                while((i=fos.read(byt))!=-1){
                    zos.write(byt,0,i);
                }

                zos.closeEntry();
            }
            for(String fn:prefstocp){
                ZipEntry ze= new ZipEntry(fn);
                //ze.setComment("log");
                zos.putNextEntry(ze);
                int i=0;
                FileInputStream fos= new FileInputStream(fn);
                byte[] byt=new byte[4096];
                while((i=fos.read(byt))!=-1){
                    zos.write(byt,0,i);
                }

                zos.closeEntry();
            }
        } catch (Exception e) {
            LogUtil.logToFile(e.toString());
        }
        finally{
            if(zos!=null){
                try{
                    zos.close();
                }catch(Exception e){}
            }
        }
    }
    private DevicePolicyManager mDpm;
    private ComponentName mAdminComponentName;
    SharedPreferences sp;
    SharedPreferences sprst;
    SharedPreferences.Editor sprste;
    
    void saverestrictions(){
        try{
            mDpm = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
            if(mDpm.isDeviceOwnerApp(getPackageName())){
                mAdminComponentName = new ComponentName(this, admin.class);
                sprst = this.getSharedPreferences("restriction", this.MODE_PRIVATE);
                sprste = sprst.edit();
                if (Build.VERSION.SDK_INT >= 24){
                    boolean vpnenabled=false;
                    String strpkgvpn= mDpm.getAlwaysOnVpnPackage(mAdminComponentName);
                    if(strpkgvpn!=null){
                        vpnenabled=strpkgvpn.equals(getPackageName());
                    }
                    sprste.putBoolean("DISALLOW_ALWAYS_ON_VPN",
                                      vpnenabled);
                }
                if (Build.VERSION.SDK_INT >= 20) {
                    sprste.putBoolean(UserManager.DISALLOW_CONFIG_VPN,
                                      mDpm.getUserRestrictions(mAdminComponentName).getBoolean(UserManager.DISALLOW_CONFIG_VPN));
                }
                if (Build.VERSION.SDK_INT >= 27) {
                    sprste.putBoolean(UserManager.DISALLOW_CONFIG_DATE_TIME,
                                      mDpm.getUserRestrictions(mAdminComponentName).getBoolean(UserManager.DISALLOW_CONFIG_DATE_TIME));
                }
                if (Build.VERSION.SDK_INT >= 20) {
                    sprste.putBoolean(UserManager.DISALLOW_CONFIG_TETHERING,
                                      mDpm.getUserRestrictions(mAdminComponentName).getBoolean(UserManager.DISALLOW_CONFIG_TETHERING));
                }
                if (Build.VERSION.SDK_INT >= 17) {
                    sprste.putBoolean(UserManager.DISALLOW_CONFIG_WIFI,
                                      mDpm.getUserRestrictions(mAdminComponentName).getBoolean(UserManager.DISALLOW_CONFIG_WIFI));
                }
                if (Build.VERSION.SDK_INT >= 20) {
                    sprste.putBoolean(UserManager.DISALLOW_DEBUGGING_FEATURES,
                                      mDpm.getUserRestrictions(mAdminComponentName).getBoolean(UserManager.DISALLOW_DEBUGGING_FEATURES));
                }
                if (Build.VERSION.SDK_INT >= 17) {
                    sprste.putBoolean(UserManager.DISALLOW_INSTALL_APPS,
                                      mDpm.getUserRestrictions(mAdminComponentName).getBoolean(UserManager.DISALLOW_INSTALL_APPS));
                }
                if (Build.VERSION.SDK_INT >= 17) {
                    sprste.putBoolean(UserManager.DISALLOW_INSTALL_UNKNOWN_SOURCES,
                                      mDpm.getUserRestrictions(mAdminComponentName).getBoolean(UserManager.DISALLOW_INSTALL_UNKNOWN_SOURCES));
                }
                if (Build.VERSION.SDK_INT >= 17) {
                    sprste.putBoolean(UserManager.DISALLOW_UNINSTALL_APPS,
                                      mDpm.getUserRestrictions(mAdminComponentName).getBoolean(UserManager.DISALLOW_UNINSTALL_APPS));
                }
                if (Build.VERSION.SDK_INT >= 20) {
                    sprste.putBoolean(UserManager.DISALLOW_APPS_CONTROL,
                                      mDpm.getUserRestrictions(mAdminComponentName).getBoolean(UserManager.DISALLOW_APPS_CONTROL));
                }
                if (Build.VERSION.SDK_INT >= 20) {
                    sprste.putBoolean(UserManager.DISALLOW_FACTORY_RESET,
                                      mDpm.getUserRestrictions(mAdminComponentName).getBoolean(UserManager.DISALLOW_FACTORY_RESET));
                }
                if (Build.VERSION.SDK_INT >= 25) {
                    sprste.putBoolean(UserManager.DISALLOW_ADD_MANAGED_PROFILE,
                                      mDpm.getUserRestrictions(mAdminComponentName).getBoolean(UserManager.DISALLOW_ADD_MANAGED_PROFILE));
                }
                if (Build.VERSION.SDK_INT >= 27) {
                    sprste.putBoolean(UserManager.DISALLOW_USER_SWITCH,
                                      mDpm.getUserRestrictions(mAdminComponentName).getBoolean(UserManager.DISALLOW_USER_SWITCH));
                }
                if (Build.VERSION.SDK_INT >= 20) {
                    sprste.putBoolean(UserManager.DISALLOW_ADD_USER,
                                      mDpm.getUserRestrictions(mAdminComponentName).getBoolean(UserManager.DISALLOW_ADD_USER));
                }
                if (Build.VERSION.SDK_INT >= 22) {
                    sprste.putBoolean(UserManager.DISALLOW_SAFE_BOOT,
                                      mDpm.getUserRestrictions(mAdminComponentName).getBoolean(UserManager.DISALLOW_SAFE_BOOT));
                }
                if (Build.VERSION.SDK_INT >= 17) {
                    sprste.putBoolean(UserManager.DISALLOW_REMOVE_USER,
                                      mDpm.getUserRestrictions(mAdminComponentName).getBoolean(UserManager.DISALLOW_REMOVE_USER));
                }
                if (Build.VERSION.SDK_INT >= 25) {
                    sprste.putBoolean(UserManager.DISALLOW_BLUETOOTH,
                                      mDpm.getUserRestrictions(mAdminComponentName).getBoolean(UserManager.DISALLOW_BLUETOOTH));
                }
                if (Build.VERSION.SDK_INT >= 17) {
                    sprste.putBoolean(UserManager.DISALLOW_CONFIG_BLUETOOTH,
                                      mDpm.getUserRestrictions(mAdminComponentName).getBoolean(UserManager.DISALLOW_CONFIG_BLUETOOTH));
                }
                if (Build.VERSION.SDK_INT >= 25) {
                    sprste.putBoolean(UserManager.DISALLOW_BLUETOOTH_SHARING,
                                      mDpm.getUserRestrictions(mAdminComponentName).getBoolean(UserManager.DISALLOW_BLUETOOTH_SHARING));
                }
                if (Build.VERSION.SDK_INT >= 28) {
                    sprste.putBoolean(UserManager.DISALLOW_CONFIG_PRIVATE_DNS,
                                      mDpm.getUserRestrictions(mAdminComponentName).getBoolean(UserManager.DISALLOW_CONFIG_PRIVATE_DNS));
                }
                if (Build.VERSION.SDK_INT >= 20) {
                    sprste.putBoolean(UserManager.DISALLOW_SMS,
                                      mDpm.getUserRestrictions(mAdminComponentName).getBoolean(UserManager.DISALLOW_SMS));
                }
                if (Build.VERSION.SDK_INT >= 20) {
                    sprste.putBoolean(UserManager.DISALLOW_OUTGOING_CALLS,
                                      mDpm.getUserRestrictions(mAdminComponentName).getBoolean(UserManager.DISALLOW_OUTGOING_CALLS));
                }
                if (Build.VERSION.SDK_INT >= 20) {
                    sprste.putBoolean(UserManager.DISALLOW_CONFIG_MOBILE_NETWORKS,
                                      mDpm.getUserRestrictions(mAdminComponentName).getBoolean(UserManager.DISALLOW_CONFIG_MOBILE_NETWORKS));
                }
                if (Build.VERSION.SDK_INT >= 23) {
                    sprste.putBoolean(UserManager.DISALLOW_DATA_ROAMING,
                                      mDpm.getUserRestrictions(mAdminComponentName).getBoolean(UserManager.DISALLOW_DATA_ROAMING));
                }
                if (Build.VERSION.SDK_INT >= 17) {
                    sprste.putBoolean(UserManager.DISALLOW_USB_FILE_TRANSFER,
                                      mDpm.getUserRestrictions(mAdminComponentName).getBoolean(UserManager.DISALLOW_USB_FILE_TRANSFER));
                }
                if (Build.VERSION.SDK_INT >= 14) {
                    sprste.putBoolean("DISALLOW_CAMERA",
                                      mDpm.getCameraDisabled(mAdminComponentName));
                }
                if (Build.VERSION.SDK_INT >= 23) {
                    sp = PreferenceManager.getDefaultSharedPreferences(this);
                    sprste.putBoolean("DISALLOW_STATUSBAR",
                                      sp.getBoolean("dis_statusbar",false));
                }
                sprste.commit();
            }
        }catch(Exception e){LogUtil.logToFile(e.toString());}
    }
    public static boolean deleteDir(File var0) {
        boolean var3;
        if(var0.isDirectory()) {
            String[] var1 = var0.list();

            for(int var2 = 0; var2 < var1.length; ++var2) {
                if(!deleteDir(new File(var0, var1[var2]))) {
                    var3 = false;
                    return var3;
                }
            }
        }
        var3 = var0.delete();
        return var3;
    }
}
