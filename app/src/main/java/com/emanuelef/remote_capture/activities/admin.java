package com.emanuelef.remote_capture.activities;

import android.app.admin.DeviceAdminReceiver;
import android.content.Intent;
import android.content.Context;
import android.os.PersistableBundle;
import android.util.Log;

public class admin extends DeviceAdminReceiver {

    private static final String TAG = "PCAPdroid_Admin";

    @Override
    public void onEnabled(Context context, Intent intent) {
        super.onEnabled(context, intent);
        Log.d(TAG, "Device Admin Enabled");
    }

    @Override
    public void onDisabled(Context context, Intent intent) {
        super.onDisabled(context, intent);
        Log.d(TAG, "Device Admin Disabled");
    }

    /**
     * פונקציה זו נקראת ברגע שהעברת הבעלות לאפליקציה זו הושלמה.
     */
    @Override
    public void onTransferOwnershipComplete(Context context, PersistableBundle bundle) {
        super.onTransferOwnershipComplete(context, bundle);
        Log.d(TAG, "Transfer Ownership Completed Successfully!");
        
        // כאן ניתן להוסיף לוגיקה נוספת להפעלת הגדרות מיד עם קבלת הניהול
    }
}
