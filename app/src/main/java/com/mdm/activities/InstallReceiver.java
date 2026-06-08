package com.mdm.activities;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInstaller;
import android.widget.Toast;

import java.io.File;
import android.os.Handler;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import com.emanuelef.remote_capture.activities.LogUtil;
import android.os.Looper;

public class InstallReceiver extends BroadcastReceiver {
    @Deprecated
    @Override
    public void onReceive(Context context, Intent intent) {
        if (utils.ACTION_INSTALL_COMPLETE.equals(intent.getAction())) {
            String packageName = utils.mainPackageName;
            //packageName=intent.getStringExtra(utils.EXTRA_PACKAGE_NAME);
            int status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, -1);
            String message = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE);

            // ודא שאתה מוחק את התיקיה הזמנית בכל מקרה
            //File tempDir = new File(context.getFilesDir()+ "/cach/apks_temp");
            //utils.deleteTempDir(tempDir,true);

            // בנוסף, אם ההתקנה בוטלה בגלל סיסמה שגויה, אנו צריכים למחוק את קובץ ה-APK המקורי
            // זה קצת מורכב כי ה-Receiver לא יודע איזה קובץ APK נבחר
            // דרך טובה יותר תהיה להעביר את נתיב הקובץ הזמני כ-Extra ל-InstallReceiver
            // למען הפשטות כרגע, ננקה רק את התיקיה הזמנית הכללית.

            switch (status) {
                case PackageInstaller.STATUS_PENDING_USER_ACTION:
                    /*Intent confirmationIntent = intent.getParcelableExtra(Intent.EXTRA_INTENT);
                     //if (confirmationIntent != null) {
                     try{
                     confirmationIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                     context.startActivity(confirmationIntent);
                     }catch(Exception e){}*/
                    try{
                        Intent inte=new Intent(context,confirmationinstall.class);
                        inte.putExtra("inte",(Parcelable)intent.getParcelableExtra("android.intent.extra.INTENT"));
                        inte.addFlags(intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_REORDER_TO_FRONT|Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT);
                        context.startActivity(inte);
                    }catch(Exception e){
                        Toast.makeText(context, ""+e, Toast.LENGTH_LONG).show();
                    }
                    //}
                    if (utils.progressDialog != null)
                    utils.progressDialog.setMessage("התקנה ממתינה לאישור משתמש עבור " + packageName + ": " + message);
                    Toast.makeText(context, "התקנה ממתינה לאישור משתמש עבור " + packageName, Toast.LENGTH_LONG).show();
                    break;
                case PackageInstaller.STATUS_SUCCESS:
                    if(PreferenceManager.getDefaultSharedPreferences(context).getBoolean("clear_files",false)){
                        utils.deleteTempDir(new File(context.getExternalFilesDir("")+"/"),false);
                    }
                    if (utils.progressDialog != null)
                    utils.progressDialog.setMessage("התקנה/עדכון הושלם בהצלחה עבור " + packageName + ": " + message);
                    dismissprogress();
                    Toast.makeText(context, "התקנה/עדכון הושלם בהצלחה עבור " + packageName, Toast.LENGTH_LONG).show();
                    // ייתכן שתרצה לרענן את רשימת האפליקציות ב-UI (לדוגמה, לשלוח ברודקאסט חזרה ל-utils)
                    //send to update current ui items...
                    try{
                        Runnable r = new Runnable() {
                            @Override
                            public void run() {
                                try{
                                    /*Intent intent = new Intent(mactivity, DownloadService.class);
                                     mactivity. bindService(intent,mactivity.serviceConnection, Context.BIND_AUTO_CREATE);
                                     */
                                    //dont bind beacause has can bind when ui is stopped & isnt have onstop to make binder null to stop the runnable when isnt in ui...
                                    LogUtil.logToFile("update ui from install receiver");//if isnt already connected
                                    storeActivity.startUiUpdater();
                                }catch(Throwable t){LogUtil.logToFile(t);}
                            }};
                        new Handler(Looper.getMainLooper()).post(r);
                    }catch(Throwable t){LogUtil.logToFile(t);}
                    break;
                case PackageInstaller.STATUS_FAILURE:
                case PackageInstaller.STATUS_FAILURE_ABORTED:
                case PackageInstaller.STATUS_FAILURE_BLOCKED:
                case PackageInstaller.STATUS_FAILURE_CONFLICT:
                case PackageInstaller.STATUS_FAILURE_INCOMPATIBLE:
                case PackageInstaller.STATUS_FAILURE_INVALID:
                case PackageInstaller.STATUS_FAILURE_STORAGE:
                    Toast.makeText(context, "התקנה נכשלה עבור " + packageName + ": " + message, Toast.LENGTH_LONG).show();
                    if (utils.progressDialog != null)
                    utils.progressDialog.setMessage("התקנה נכשלה עבור " + packageName + ": " + message);
                    dismissprogress();
                    LogUtil.logToFile("התקנה נכשלה עבור " + packageName + ": " + message);
                    break;
                default:
                    Toast.makeText(context, "סטטוס התקנה לא ידוע: " + message, Toast.LENGTH_LONG).show();
                    break;
            }
        }
    }
    @Deprecated
    static void dismissprogress(){
        new Handler().postDelayed(new Runnable(){
                @Deprecated
                @Override
                public void run() {
                    if (utils.progressDialog != null && utils.progressDialog.isShowing()) {
                        utils.progressDialog.dismiss();
                    }
                }
            }, 5000);
    }
}

