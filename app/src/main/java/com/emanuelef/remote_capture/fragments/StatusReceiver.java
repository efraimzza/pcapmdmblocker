package com.emanuelef.remote_capture.fragments;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

public class StatusReceiver extends BroadcastReceiver {
    @Deprecated
    @Override
    public void onReceive(Context context, Intent intent) {
        Exception exception;
       
        if(intent.getAction()== intent.ACTION_MY_PACKAGE_REPLACED){
            Toast.makeText(context, "עודכן בהצלחה", 1).show();
            try {
               // MainActivity.tv1.setText("עודכן בהצלחה");
            } catch (Exception e) {
                exception = e;
            }
        }else{
        switch (intent.getIntExtra("android.content.pm.extra.STATUS", -25)) {
            case -25:
                try {
                //    MainActivity.tv1.setText("אין");
                } catch (Exception e) {
                    exception = e;
                }
                return;
            case -1:
            try {
      Intent inte= intent.getParcelableExtra("android.intent.extra.INTENT");
      inte.addFlags(intent.FLAG_ACTIVITY_NEW_TASK);
      context.startActivity(inte);
   } catch (Exception e) {
      Toast.makeText(context, ""+e, 1).show();
   }
   
                Toast.makeText(context, "מחכה לפעולת משתמש", 1).show();
                try {
                   // MainActivity.tv1.setText("מחכה לפעולת משתמש");
                } catch (Exception e2) {
                    exception = e2;
                }
                return;
            case 0:
                Toast.makeText(context, "הצליח", 1).show();
                try {
                   // MainActivity.tv1.setText("הצליח");
                } catch (Exception e22) {
                    exception = e22;
                }
                return;
            case 1:
                Toast.makeText(context, "לא הצליח", 1).show();
                try {
                   // MainActivity.tv1.setText("לא הצליח");
                } catch (Exception e222) {
                    exception = e222;
                }
                return;
            case 2:
                Toast.makeText(context, "לא הצליח נחסם", 1).show();
                try {
                    //MainActivity.tv1.setText("לא הצליח נחסם");
                } catch (Exception e2222) {
                    exception = e2222;
                }
                return;
            case 3:
                Toast.makeText(context, "לא הצליח הופסק", 1).show();
                try {
                   // MainActivity.tv1.setText("לא הצליח הופסק");
                } catch (Exception e22222) {
                    exception = e22222;
                }
                return;
            case 4:
                Toast.makeText(context, "לא הצליח בעיית קובץ apk או apks", 1).show();
                try {
                   // MainActivity.tv1.setText("לא הצליח בעיית קובץ apk או apks");
                } catch (Exception e222222) {
                    exception = e222222;
                }
                return;
            case 5:
                Toast.makeText(context, "לא הצליח התנגשות", 1).show();
                try {
                   // MainActivity.tv1.setText("לא הצליח התנגשות");
                } catch (Exception e2222222) {
                    exception = e2222222;
                }
                return;
            case 6:
                Toast.makeText(context, "לא הצליח אין מספיק מקום", 1).show();
                try {
                   // MainActivity.tv1.setText("לא הצליח אין מספיק מקום");
                } catch (Exception e22222222) {
                    exception = e22222222;
                }
                return;
            case 7:
                Toast.makeText(context, "לא הצליח תאימות מכשיר", 1).show();
                try {
                    //MainActivity.tv1.setText("לא הצליח תאימות מכשיר");
                } catch (Exception e222222222) {
                    exception = e222222222;
                }
                return;
            default:
                return;
        }
    }
    }

    public StatusReceiver() {
    }
}
