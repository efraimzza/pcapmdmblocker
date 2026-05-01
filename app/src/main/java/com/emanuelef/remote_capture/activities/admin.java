package com.emanuelef.remote_capture.activities;

import android.app.admin.DeviceAdminReceiver;
import android.content.Intent;
import android.content.Context;
import android.os.PersistableBundle;
import android.widget.Toast;

public class admin extends DeviceAdminReceiver {

    @Override
    public void onEnabled(Context context, Intent intent) {
        super.onEnabled(context, intent);
        Toast.makeText(context,"חסימת mdm הופעל",1).show();
    }

    @Override
    public void onDisabled(Context context, Intent intent) {
        super.onDisabled(context, intent);
        Toast.makeText(context,"חסימת mdm הוסר",1).show();
    }

    @Override
    public void onTransferOwnershipComplete(Context context, PersistableBundle bundle) {
        super.onTransferOwnershipComplete(context, bundle);
        Toast.makeText(context,"חסימת mdm הופעל באמצעות העברה",1).show();
    }
    
}
