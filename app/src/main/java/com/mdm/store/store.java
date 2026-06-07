package com.mdm.store;

/*
 * This file is part of PCAPdroid.
 *
 * PCAPdroid is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * PCAPdroid is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with PCAPdroid.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Copyright 2020-21 - Emanuele Faranda
 */

import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;

import android.preference.PreferenceManager;


import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Set;
import android.os.Process;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import android.widget.Toast;
import com.mdm.activities.debug;

/* The PCAPdroid app class.
 * This class is instantiated before anything else, and its reference is stored in the mInstance.
 * Global state is stored into this class via singletons. Contrary to static singletons, this does
 * not require passing the localized Context to the singletons getters methods.
 *
 * IMPORTANT: do not override getResources() with mLocalizedContext, otherwise the Webview used for ads will crash!
 * https://stackoverflow.com/questions/56496714/android-webview-causing-runtimeexception-at-webviewdelegate-getpackageid
 */
public class store extends Application {

    @Override
    public void onCreate() {
        Thread.setDefaultUncaughtExceptionHandler(
            new Thread.UncaughtExceptionHandler() {
                @Override
                public void uncaughtException(Thread thread, Throwable throwable) {
                    //startActivity(new Intent(getApplicationContext(),debug.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
                  //  Intent intent = new Intent(getApplicationContext(), MDMSettingsActivity.class);
                  //  intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                   // intent.putExtra("err","");
                    //intent.putExtra("error", android.util.Log.getStackTraceString(throwable)+throwable.getStackTrace()[0].getClassName()+throwable.getStackTrace()[0].getMethodName()+throwable.getStackTrace()[0].getLineNumber()+throwable.getStackTrace()[0].getFileName());
                    //startActivity(intent);
                    Intent intent = new Intent(getApplicationContext(), debug.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    intent.putExtra("error", android.util.Log.getStackTraceString(throwable)+throwable.getStackTrace()[0].getClassName()+throwable.getStackTrace()[0].getMethodName()+throwable.getStackTrace()[0].getLineNumber()+throwable.getStackTrace()[0].getFileName());
                    startActivity(intent);
                    try {
                        String LOG_PATH = "/storage/emulated/0/log.txt";
                        FileWriter writer = new FileWriter(LOG_PATH, true);
                        String time = new SimpleDateFormat("HH:mm:ss").format(new Date());
                        writer.write("[" + time + "] " + throwable.toString() + "\n");
                        writer.close();
                    } catch (IOException ee) {
                        // silent
                    }
                    Process.killProcess(Process.myPid());
                    System.exit(1);
                }
            });
        super.onCreate();
        //Utils.setTheme(this);
        
        
        
    }

    
    
}
