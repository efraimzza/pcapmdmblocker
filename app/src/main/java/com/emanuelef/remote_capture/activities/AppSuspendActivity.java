package com.emanuelef.remote_capture.activities;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageInstaller;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RadioGroup;
import android.widget.Toast;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import android.os.Handler;

import com.emanuelef.remote_capture.R;
import com.emanuelef.remote_capture.Utils;
import android.preference.PreferenceManager;
import java.util.Set;
import android.content.SharedPreferences;
import java.util.HashSet;
import java.util.Arrays;
import android.widget.RadioButton;
import android.widget.TextView;

@Deprecated
public class AppSuspendActivity extends Activity {

    private DevicePolicyManager mDpm;
    private ComponentName mAdminComponentName;
    private ListView lvApps;
    private List<AppItem> mOriginalAppList;
    private List<AppItem> mFilteredAppList;
    private AppListAdapter mAdapter;

    private String currentSearchText = "";
    private String currentSearchPackage = ""; // לחיפוש לפי שם חבילה
    private int currentFilterOptionId = R.id.rb_filter_all_dialog; // ID של כפתור הרדיו הנבחר
    private int currentSortOptionId = R.id.rb_sort_name_dialog; // ID של כפתור הרדיו הנבחר

    public static ProgressDialog progressDialog; // משתנה לדיאלוג התקדמות

    

    @Deprecated
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Utils.setTheme(this);
        setContentView(R.layout.activity_app_management);
        mDpm = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
        mAdminComponentName = new ComponentName(this, admin.class);
        try{
            if(getActionBar().isShowing())
                getActionBar().hide();
        }catch(Exception e){}
        lvApps = (ListView) findViewById(R.id.lv_apps);

        // טען את הרשימה באופן אסינכרוני
        new LoadAppsTask().execute();

        mFilteredAppList = new ArrayList<AppItem>();
        mAdapter = new AppListAdapter(this, mFilteredAppList);
        lvApps.setAdapter(mAdapter);

        Button btnShowFilterOptions = (Button) findViewById(R.id.btn_filter_apps);
        btnShowFilterOptions.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showFilterOptionsDialog();
                }
            });
        Button btnRefreshApps = findViewById(R.id.btn_refresh_apps);
        btnRefreshApps.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    new LoadAppsTask().execute();
                    //Toast.makeText(AppManagementActivity.this, "רשימת אפליקציות עודכנה.", Toast.LENGTH_SHORT).show();
                }
            });
        Button btnSaveAppChanges = (Button) findViewById(R.id.btn_save_app_changes);
        btnSaveAppChanges.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    PasswordManager.requestPasswordAndSave(new Runnable() {
                            @Override
                            public void run() {
                                try{
                                    applyAppVisibilityChanges();
                                }catch(Exception e){}
                            }
                        },AppSuspendActivity.this);
                }
            });

        findViewById(R.id.btn_install_apk).setVisibility(Button.GONE);
        ((TextView)findViewById(R.id.AppMngTitle)).setText("השהיית אפליקציות");
    }

    // הוסף AsyncTask חדש לטעינת אפליקציות
    @Deprecated
    private class LoadAppsTask extends AsyncTask<Void, Void, List<AppItem>> {

        @Deprecated
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressDialog = new ProgressDialog(AppSuspendActivity.this);
            progressDialog.getWindow().setBackgroundDrawableResource(R.drawable.rounded_button_background);
            progressDialog.setMessage("טוען רשימת אפליקציות...");
            progressDialog.setCancelable(false);
            progressDialog.show();
        }

        @Override
        protected List<AppItem> doInBackground(Void... voids) {
            PackageManager pm = getPackageManager();
            List<ApplicationInfo> installedApps = pm.getInstalledApplications(PackageManager.GET_META_DATA | PackageManager.MATCH_UNINSTALLED_PACKAGES);
            List<AppItem> appList = new ArrayList<AppItem>();

            for (ApplicationInfo appInfo : installedApps) {
                boolean isHiddenByMDM =false;
                try{
                    isHiddenByMDM = mDpm.isPackageSuspended(mAdminComponentName, appInfo.packageName);
                }catch(Exception e){}
                long lastUpdateTime = 0;
                try {
                    PackageInfo packageInfo = pm.getPackageInfo(appInfo.packageName, 0);
                    lastUpdateTime = packageInfo.lastUpdateTime;
                } catch (PackageManager.NameNotFoundException e) {
                    LogUtil.logToFile(""+e);
                    e.printStackTrace();
                }

                appList.add(new AppItem(
                                appInfo.loadLabel(pm).toString(),
                                appInfo.packageName,
                                appInfo.loadIcon(pm),
                                isHiddenByMDM,
                                (appInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0,
                                hasLauncherIcon(pm, appInfo.packageName),
                                lastUpdateTime
                            ));
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
            mOriginalAppList = result; // עדכן את הרשימה המקורית
            applyFiltersAndSort(); // בצע סינון ומיון ראשוני לאחר הטעינה
        }
    }

    // ... (loadAppList, hasLauncherIcon)


    private void showFilterOptionsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_filter_options, null);
        builder.setView(dialogView);

        final EditText etSearchApps = dialogView.findViewById(R.id.et_search_apps_dialog);
        final EditText etSearchPackage = dialogView.findViewById(R.id.et_search_package_dialog); // מצא את ה-EditText החדש
        final RadioGroup rgFilterApps = dialogView.findViewById(R.id.rg_filter_apps_dialog);
        final RadioGroup rgSortApps = dialogView.findViewById(R.id.rg_sort_apps_dialog);

        // הגדר את הערכים הנוכחיים בדיאלוג
        etSearchApps.setText(currentSearchText);
        etSearchPackage.setText(currentSearchPackage); // הגדר את טקסט חיפוש החבילה
        rgFilterApps.check(currentFilterOptionId);
        rgSortApps.check(currentSortOptionId);

        // עדכן את טקסט חיפוש השם כאשר המשתמש מקליד
        etSearchApps.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                    currentSearchText = s.toString();
                }
                @Override public void afterTextChanged(Editable s) {}
            });

        // עדכן את טקסט חיפוש החבילה כאשר המשתמש מקליד
        etSearchPackage.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                    currentSearchPackage = s.toString();
                }
                @Override public void afterTextChanged(Editable s) {}
            });

        rgFilterApps.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(RadioGroup group, int checkedId) {
                    currentFilterOptionId = checkedId;
                }
            });

        rgSortApps.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(RadioGroup group, int checkedId) {
                    currentSortOptionId = checkedId;
                }
            });

        builder.setPositiveButton("החל סינון", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    applyFiltersAndSort();
                }
            });
        builder.setNegativeButton("ביטול", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    // אין צורך לעשות כלום, הדיאלוג ייסגר
                }
            });
        ((RadioButton)dialogView.findViewById(R.id.rb_filter_hidden_dialog)).setText("מושהות");
        builder.show();
    }

    private void applyFiltersAndSort() {
        mFilteredAppList.clear();
        String searchTextLower = currentSearchText.toLowerCase();
        String searchPackageLower = currentSearchPackage.toLowerCase(); // טקסט חיפוש חבילה ב-lowercase

        // 1. סינון
        for (AppItem appItem : mOriginalAppList) {
            boolean matchesFilter = false;
            boolean isSystemApp = appItem.isSystemApp();
            boolean isHidden = appItem.isHidden();
            boolean hasLauncher = appItem.hasLauncherIcon();

            if (currentFilterOptionId == R.id.rb_filter_all_dialog) {
                matchesFilter = true;
            } else if (currentFilterOptionId == R.id.rb_filter_user_dialog) {
                matchesFilter = !isSystemApp;
            } else if (currentFilterOptionId == R.id.rb_filter_system_dialog) {
                matchesFilter = isSystemApp;
            } else if (currentFilterOptionId == R.id.rb_filter_hidden_dialog) {
                matchesFilter = isHidden;
            } else if (currentFilterOptionId == R.id.rb_filter_launcher_dialog) {
                matchesFilter = hasLauncher;
            }

            // שילוב חיפוש לפי שם אפליקציה וגם לפי שם חבילה
            boolean matchesName = appItem.getName().toLowerCase().contains(searchTextLower);
            boolean matchesPackage = appItem.getPackageName().toLowerCase().contains(searchPackageLower);

            if (matchesFilter && matchesName && matchesPackage) { // חייב להתאים לשניהם
                mFilteredAppList.add(appItem);
            }
        }

        // 2. מיון
        if (currentSortOptionId == R.id.rb_sort_name_dialog) {
            Collections.sort(mFilteredAppList, new Comparator<AppItem>() {
                    @Override
                    public int compare(AppItem item1, AppItem item2) {
                        return item1.getName().compareToIgnoreCase(item2.getName());
                    }
                });
        } else if (currentSortOptionId == R.id.rb_sort_last_installed_dialog) {
            Collections.sort(mFilteredAppList, new Comparator<AppItem>() {
                    @Override
                    public int compare(AppItem item1, AppItem item2) {
                        if (item1.getLastUpdateTime() > item2.getLastUpdateTime()) {
                            return -1;
                        } else if (item1.getLastUpdateTime() < item2.getLastUpdateTime()) {
                            return 1;
                        } else {
                            return 0;
                        }
                    }
                });
        }

        mAdapter.notifyDataSetChanged();
    }
    

    

    

    private boolean hasLauncherIcon(PackageManager pm, String packageName) {
        Intent intent = new Intent(Intent.ACTION_MAIN, null);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        List<android.content.pm.ResolveInfo> resolveInfoList = pm.queryIntentActivities(intent, 0);
        for (android.content.pm.ResolveInfo resolveInfo : resolveInfoList) {
            if (resolveInfo.activityInfo != null && packageName.equals(resolveInfo.activityInfo.packageName)) {
                return true;
            }
        }
        return false;
    }

    final private static String mownsetapps="ownsetapps";

    private void applyAppVisibilityChanges() {
        List<String> ss=new ArrayList<>();
        List<String> sus=new ArrayList<>();
        List<String> unsus=new ArrayList<>();
        for (AppItem appItem : mOriginalAppList) { // עבר על הרשימה המקורית
            // אם מצב ה-hidden השתנה עבור האפליקציה הזו
            boolean currentHiddenState = false;
            try{
             currentHiddenState = mDpm.isPackageSuspended(mAdminComponentName, appItem.getPackageName());
            }catch(Exception e){}
            if (currentHiddenState != appItem.isHidden()) {
                if(appItem.isHidden()){
                    sus.add(appItem.getPackageName());
                }else{
                    unsus.add(appItem.getPackageName());
                }
            }
            
        }
        String[] susa={};
        susa=sus.toArray(susa);
        mDpm.setPackagesSuspended(mAdminComponentName,susa,true);
        String[] unsusa={};
        unsusa=unsus.toArray(unsusa);
        mDpm.setPackagesSuspended(mAdminComponentName,unsusa,false);
        Toast.makeText(getApplicationContext(), "שינויים באפליקציות נשמרו!", Toast.LENGTH_SHORT).show();
        // רענן את הרשימה לאחר שמירה כדי לשקף שינויים (לדוגמה, בסינון 'מוסתרות')
        // טען מחדש את הרשימה המקורית מהמערכת
        new LoadAppsTask().execute();
        applyFiltersAndSort(); // סנן ומיין אותה מחדש
    }

    /*
     // מתודת עזר להעתקת URI לקובץ זמני
     private File copyUriToTempFile(Context context, Uri uri) {
     File tempFile = null;
     InputStream inputStream = null;
     FileOutputStream outputStream = null;
     try {
     String fileName = getFileNameFromUri(context, uri); // פונקציית עזר לקבלת שם קובץ
     if (fileName == null) {
     fileName = "temp_package"; // שם ברירת מחדל
     }
     if (!fileName.contains(".")) { // הוסף סיומת אם חסרה
     fileName += ".apk"; // נניח APK כברירת מחדל אם לא זוהה
     }

     tempFile = new File(context.getCacheDir(), fileName);
     inputStream = context.getContentResolver().openInputStream(uri);
     outputStream = new FileOutputStream(tempFile);

     byte[] buffer = new byte[1024];
     int read;
     while ((read = inputStream.read(buffer)) != -1) {
     outputStream.write(buffer, 0, read);
     }
     outputStream.flush();
     return tempFile;
     } catch (Exception e) {
     LogUtil.logToFile(""+e);
     e.printStackTrace();
     if (tempFile != null && tempFile.exists()) {
     tempFile.delete(); // מחיקת קובץ חלקי במקרה של שגיאה
     }
     return null;
     } finally {
     try {
     if (inputStream != null) inputStream.close();
     if (outputStream != null) outputStream.close();
     } catch (IOException e) {
     LogUtil.logToFile(""+e);
     e.printStackTrace();
     }
     }
     }

     // פונקציית עזר לקבלת שם קובץ מ-URI (לא תמיד אמין לחלוטין)
     private String getFileNameFromUri(Context context, Uri uri) {
     String result = null;
     if (uri.getScheme().equals("content")) {
     android.database.Cursor cursor = context.getContentResolver().query(uri, null, null, null, null);
     try {
     if (cursor != null && cursor.moveToFirst()) {
     int nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME);
     if (nameIndex != -1) {
     result = cursor.getString(nameIndex);
     }
     }
     } finally {
     if (cursor != null) {
     cursor.close();
     }
     }
     }
     if (result == null) {
     result = uri.getLastPathSegment();
     }
     return result;
     }
     */
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
}
