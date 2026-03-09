package com.emanuelef.remote_capture.activities;


import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.widget.RemoteViews;
import android.content.Intent;
import android.app.PendingIntent;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.app.AlertDialog;
import android.app.Activity;
import android.os.Bundle;
import android.util.TypedValue;
import com.emanuelef.remote_capture.R;
import android.preference.PreferenceManager;

// 1. הגדרת המשתנים הקבועים לזיהוי הפעולה והעדפת המערכת
// 2. בדיקת המצב הנוכחי (On/Off) מתוך ה-SharedPreferences
// 3. עדכון ה-UI (טקסט/צבע) ללא שימוש ב-Lambda
// 4. שליחת ה-Broadcast לעצמנו כדי לשנות מצב בלחיצה

// Java code for a dynamic, resizable, and styled Widget
// Java code for synchronized Multiple Widgets
public class MyToggleWidget extends AppWidgetProvider {
    /*
     @Override
     public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
     for (int id : appWidgetIds) {
     updateUI(context, appWidgetManager, id);
     }
     }*/

    private void updateUI(Context context, AppWidgetManager mgr, int id) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        boolean isLocked = prefs.getBoolean("is_locked", true); // // ברירת מחדל נעול

        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_layout);

        if (isLocked) {
            // // מצב נעול -> מציג מנעול ירוק וטקסט לפתיחה
            views.setTextViewText(R.id.widget_state_display, "פתיחת אפליקציות משתמש");
            views.setTextColor(R.id.widget_state_display, 0xFF00FF00); // ירוק
            views.setImageViewResource(R.id.widget_status_icon, R.drawable.ic_lock_closed_green);

            // // לחיצה תפתח את הדיאלוג (Activity)
            Intent intent = new Intent(context, AuthActivity.class);
            PendingIntent pi = PendingIntent.getActivity(context, id, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            views.setOnClickPendingIntent(R.id.widget_container, pi);
        } else {
            // // מצב פתוח -> מציג מנעול אדום וטקסט לנעילה
            views.setTextViewText(R.id.widget_state_display, "נעילת אפליקציות משתמש");
            views.setTextColor(R.id.widget_state_display, 0xFFFF0000); // אדום
            views.setImageViewResource(R.id.widget_status_icon, R.drawable.ic_lock_open_red);

            // // לחיצה תנעל מיד (ללא דיאלוג)
            Intent intent = new Intent(context, MyToggleWidget.class);
            intent.setAction("ACTION_LOCK_NOW");
            PendingIntent pi = PendingIntent.getBroadcast(context, id, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            views.setOnClickPendingIntent(R.id.widget_container, pi);
        }

        mgr.updateAppWidget(id, views);
    }
    /*
     @Override
     public void onReceive(Context context, Intent intent) {
     super.onReceive(context, intent);
     if ("ACTION_LOCK_NOW".equals(intent.getAction())) {
     context.getSharedPreferences("WidgetPrefs", Context.MODE_PRIVATE).edit().putBoolean("is_locked", true).commit();
     // // רענון כל הווידג'טים
     AppWidgetManager mgr = AppWidgetManager.getInstance(context);
     int[] ids = mgr.getAppWidgetIds(new ComponentName(context, MyToggleWidget.class));
     onUpdate(context, mgr, ids);
     }
     }*/
    /*
     // // האזנה לשינוי גודל הוידג'ט על מסך הבית
     @Override
     public void onAppWidgetOptionsChanged(Context context, AppWidgetManager appWidgetManager, int appWidgetId, Bundle newOptions) {
     // // שליפת הגובה הנוכחי שהמשתמש קבע (ב-dp)
     int minHeight = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT);

     RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_layout);

     // // חישוב גודל טקסט יחסי (למשל: חמישית מהגובה של הוידג'ט)
     float dynamicTextSize = minHeight / 5; 

     // // הגבלה כדי שהטקסט לא יהיה קטן מדי או ענק מדי
     if (dynamicTextSize < 12) dynamicTextSize = 12;
     if (dynamicTextSize > 24) dynamicTextSize = 24;

     // // עדכון גודל הטקסט ב-RemoteViews
     views.setTextViewTextSize(R.id.widget_state_display, TypedValue.COMPLEX_UNIT_SP, dynamicTextSize);

     // // רענון ה-UI
     appWidgetManager.updateAppWidget(appWidgetId, views);

     // // קריאה לפונקציית העדכון הרגילה כדי לשמור על שאר הנתונים
     updateUI(context, appWidgetManager, appWidgetId);

     super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions);
     }
     */
    /*
     @Override
     public void onAppWidgetOptionsChanged(Context context, AppWidgetManager appWidgetManager, int appWidgetId, Bundle newOptions) {
     // // שימוש ב-AppWidgetManager כדי לעדכן רק את הוידג'ט הספציפי
     RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_layout);

     // // שליפת הגובה ועדכון גודל הטקסט
     int minHeight = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT);
     float size = (minHeight > 0) ? minHeight / 6f : 16f;

     // // חשוב: וודא ש-TypedValue מיובא מהחבילה android.util.TypedValue
     views.setTextViewTextSize(R.id.widget_state_display, android.util.TypedValue.COMPLEX_UNIT_SP, size);

     // // קריאה לפונקציית העדכון הכללית כדי למלא את הטקסט והאייקון הנכון
     refreshWidgetUI(context, appWidgetManager, appWidgetId, views);
     }

     // // יצרתי פונקציה מאוחדת כדי למנוע כפילויות קוד שגורמות לשגיאות טעינה
     private void refreshWidgetUI(Context context, AppWidgetManager mgr, int id, RemoteViews views) {
     SharedPreferences prefs = context.getSharedPreferences("WidgetPrefs", Context.MODE_PRIVATE);
     boolean isLocked = prefs.getBoolean("is_locked", true);

     if (isLocked) {
     views.setTextViewText(R.id.widget_state_display, "פתיחת אפליקציות משתמש");
     views.setTextColor(R.id.widget_state_display, 0xFF00FF00);
     views.setImageViewResource(R.id.widget_status_icon, R.drawable.ic_lock_closed_green);
     } else {
     views.setTextViewText(R.id.widget_state_display, "נעילת אפליקציות משתמש");
     views.setTextColor(R.id.widget_state_display, 0xFFFF0000);
     views.setImageViewResource(R.id.widget_status_icon, R.drawable.ic_lock_open_red);
     }

     // // חובה לעדכן את ה-PendingIntents בכל רענון
     mgr.updateAppWidget(id, views);
     }
     */
    // // ניהול הלוגיקה של המנעול ושינוי גודל דינמי

    private static final String ACTION_LOCK_NOW = "ACTION_LOCK_NOW";

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int id : appWidgetIds) {
            updateUI(context, appWidgetManager, id, null);
        }
    }

    // // פונקציה שנקראת כשהמשתמש משנה את גודל הוידג'ט במסך הבית
    @Override
    public void onAppWidgetOptionsChanged(Context context, AppWidgetManager appWidgetManager, int appWidgetId, Bundle newOptions) {
        updateUI(context, appWidgetManager, appWidgetId, newOptions);
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions);
    }
    /*
     private void updateUI(Context context, AppWidgetManager mgr, int id, Bundle options) {
     SharedPreferences prefs = context.getSharedPreferences("WidgetPrefs", Context.MODE_PRIVATE);
     boolean isLocked = prefs.getBoolean("is_locked", true);

     RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_layout);

     // // חישוב גודל טקסט דינמי אם יש מידע על הגודל
     if (options != null) {
     int minHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT);
     float textSize = Math.max(12, Math.min(minHeight / 6, 22)); // // הגבלה בין 12 ל-22
     views.setTextViewTextSize(R.id.widget_state_display, android.util.TypedValue.COMPLEX_UNIT_SP, textSize);
     }

     // // הגדרת מצב לפי הדרישה שלך
     if (isLocked) {
     // // מצב נעול -> מראה ירוק ומציע פתיחה
     views.setTextViewText(R.id.widget_state_display, "פתיחת אפליקציות משתמש");
     views.setTextColor(R.id.widget_state_display, 0xFF00FF00); // ירוק
     views.setImageViewResource(R.id.widget_status_icon, R.drawable.ic_lock_closed_green);

     // // לחיצה פותחת את ה-AuthActivity (הדיאלוג)
     Intent intent = new Intent(context, AuthActivity.class);
     PendingIntent pi = PendingIntent.getActivity(context, id, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
     views.setOnClickPendingIntent(R.id.widget_container, pi);
     } else {
     // // מצב פתוח -> מראה אדום ומציע נעילה
     views.setTextViewText(R.id.widget_state_display, "נעילת אפליקציות משתמש");
     views.setTextColor(R.id.widget_state_display, 0xFFFF0000); // אדום
     views.setImageViewResource(R.id.widget_status_icon, R.drawable.ic_lock_open_red);

     // // לחיצה נועלת מיד דרך ה-Broadcast
     Intent intent = new Intent(context, MyToggleWidget.class);
     intent.setAction(ACTION_LOCK_NOW);
     intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id);
     PendingIntent pi = PendingIntent.getBroadcast(context, id, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
     views.setOnClickPendingIntent(R.id.widget_container, pi);
     }

     mgr.updateAppWidget(id, views);
     }
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        if (ACTION_LOCK_NOW.equals(intent.getAction())) {
            // // עדכון המצב לנעול ושמירה
            PreferenceManager.getDefaultSharedPreferences(context).edit().putBoolean("is_locked", true).commit();
            PrAppManagementActivity.enadisapps(context,true,true);
            // // רענון כל המופעים של הוידג'ט
            AppWidgetManager mgr = AppWidgetManager.getInstance(context);
            int[] ids = mgr.getAppWidgetIds(new ComponentName(context, MyToggleWidget.class));
            onUpdate(context, mgr, ids);
        }
    }
    // // פונקציה לעדכון ה-UI עם חישוב יחסים משופר
    private void updateUI(Context context, AppWidgetManager mgr, int id, Bundle options) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        boolean isLocked = prefs.getBoolean("is_locked", true);

        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_layout);

        // // חישוב גודל טקסט יחסי לגובה הוידג'ט
        if (options != null) {
            int minHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT);
            int minWidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH);

            // // נוסחה שמתאימה את הטקסט לפי הממד הקטן ביותר (ריבועי)
            int referenceSize = Math.min(minHeight, minWidth);
            float textSize = referenceSize / 8f; // // הטקסט יהיה שמינית מגודל הוידג'ט

            // // הגבלת גודל: מינימום 10sp (לקטן מאוד) ומקסימום 18sp (לגדול מאוד)
            if (textSize < 10) textSize = 10;
            if (textSize > 18) textSize = 18;

            views.setTextViewTextSize(R.id.widget_state_display, android.util.TypedValue.COMPLEX_UNIT_SP, textSize);
        }

        // // עדכון המנעול והטקסט לפי המצב (נעול/פתוח)
        if (isLocked) {
            views.setTextViewText(R.id.widget_state_display, "פתיחת אפליקציות");
            views.setTextColor(R.id.widget_state_display, 0xFF00FF00); // ירוק
            views.setImageViewResource(R.id.widget_status_icon, R.drawable.ic_lock_closed_green);

            // // לחיצה פותחת את הדיאלוג
            Intent intent = new Intent(context, AuthActivity.class);
            PendingIntent pi = PendingIntent.getActivity(context, id, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            views.setOnClickPendingIntent(R.id.widget_container, pi);
        } else {
            views.setTextViewText(R.id.widget_state_display, "נעילת אפליקציות");
            views.setTextColor(R.id.widget_state_display, 0xFFFF0000); // אדום
            views.setImageViewResource(R.id.widget_status_icon, R.drawable.ic_lock_open_red);

            // // לחיצה נועלת מיד
            Intent intent = new Intent(context, MyToggleWidget.class);
            intent.setAction("ACTION_LOCK_NOW");
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id);
            PendingIntent pi = PendingIntent.getBroadcast(context, id, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            views.setOnClickPendingIntent(R.id.widget_container, pi);
        }

        mgr.updateAppWidget(id, views);
    }
}

