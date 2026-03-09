package com.emanuelef.remote_capture.activities;

import android.app.Activity;
import android.os.Bundle;
import android.widget.EditText;
import android.app.AlertDialog;
import android.text.InputType;
import android.content.DialogInterface;
import android.widget.Toast;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.content.ComponentName;
import android.view.WindowManager;
import android.graphics.drawable.ColorDrawable;
import android.graphics.Color;
import com.emanuelef.remote_capture.Utils;
import android.preference.PreferenceManager;

public class AuthActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Utils.setTheme(this);
        getWindow().setLayout(WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT);
        // // 2. הגדרת הרקע של ה-Window להיות שקוף לחלוטין (windowBackground = transparent)
        // // משתמשים ב-ColorDrawable כדי להעביר צבע שקוף כ-Drawable
        getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        // // 3. החלת מאפייני שקיפות (Translucent) על החלון דרך ה-Flags
        // // זה מאפשר לראות את ה-Activity שמתחת
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);

        // // הגדרת רמת העמימות (Dim) של מה שמאחורי ה-Activity (אופציונלי)
        WindowManager.LayoutParams layoutParams = getWindow().getAttributes();
        layoutParams.dimAmount = 0.5f; // 0.5 אומר שמה שמאחור יהיה חשוך ב-50%
        getWindow().setAttributes(layoutParams);
        //  setTranslucent(true);
        if(getActionBar()!=null&&getActionBar().isShowing())getActionBar().hide();
        super.onCreate(savedInstanceState);


        PrPasswordManager.requestPasswordAndSave(new Runnable(){
                @Override
                public void run() {
                    updateStateAndFinish(false);
                }
            }, this);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);  
        finish();    
    }

    @Override
    protected void onStop() {
        super.onStop();
        PrPasswordManager.pwopen=false;
    }

    private void updateStateAndFinish(boolean isLocked) {
        // // עדכון ה-Preference ושליחת רענון לווידג'טים
        PreferenceManager.getDefaultSharedPreferences(this).edit().putBoolean("is_locked", isLocked).commit();
        PrAppManagementActivity.enadisapps(this,isLocked,true);
        Intent intent = new Intent(this, MyToggleWidget.class);
        intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        int[] ids = AppWidgetManager.getInstance(this).getAppWidgetIds(new ComponentName(this, MyToggleWidget.class));
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);
        sendBroadcast(intent);

        finish();
    }
    /*
     package com.widget;

     import android.app.Activity;
     import android.os.Bundle;
     import android.appwidget.AppWidgetManager;
     import android.content.Intent;
     import android.content.ComponentName;

     public class AuthActivity extends Activity {

     // // בוליאן פנימי שבודק אם הפעולה הושלמה בהצלחה
     private boolean actionCompleted = false;

     @Override
     protected void onCreate(Bundle savedInstanceState) {
     Utils.setTheme(this);
     super.onCreate(savedInstanceState);

     // // קריאה לדיאלוג הסיסמה
     PrPasswordManager.requestPasswordAndSave(new Runnable() {
     @Override
     public void run() {
     // // אישור סיסמה הצליח
     actionCompleted = true;
     updateStateAndFinish(false); // // פתיחת הנעילה
     }
     }, this);
     }

     // // מכיוון שלא תמיד יש Pause/Stop, נשתמש ב-Focus
     // // אם המשתמש לוחץ 'חזרה' או מחוץ לדיאלוג, הפוקוס יחזור ל-Activity לרגע ואז היא תיסגר
     @Override
     public void onWindowFocusChanged(boolean hasFocus) {
     super.onWindowFocusChanged(hasFocus);

     // // אם חזר הפוקוס ל-Activity והפעולה לא הושלמה - סימן שהדיאלוג נסגר/בוטל
     if (hasFocus && !actionCompleted) {
     // // הוספת דיליי קטן כדי לוודא שזה לא קפיצת פוקוס רגעית
     new android.os.Handler().postDelayed(new Runnable() {
     @Override
     public void run() {
     if (!actionCompleted) {
     finish();
     }
     }
     }, 100);
     }
     }

     private void updateStateAndFinish(boolean isLocked) {
     // // עדכון המצב ב-SharedPreferences
     getSharedPreferences("WidgetPrefs", MODE_PRIVATE)
     .edit()
     .putBoolean("is_locked", isLocked)
     .commit();

     // // רענון כל הווידג'טים בסנכרון מלא
     AppWidgetManager mgr = AppWidgetManager.getInstance(this);
     int[] ids = mgr.getAppWidgetIds(new ComponentName(this, MyToggleWidget.class));

     Intent intent = new Intent(this, MyToggleWidget.class);
     intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
     intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);
     sendBroadcast(intent);

     // // סגירת ה-Activity
     finish();
     }
     }*/
}

