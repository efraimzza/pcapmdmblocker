package com.mdm.activities;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONException;
import org.json.JSONObject;
import android.preference.PreferenceManager;
import com.emanuelef.remote_capture.R;
import com.emanuelef.remote_capture.activities.LogUtil;

/**
 * ניהול שמירה וטעינה של הגדרות האפליקציה באמצעות SharedPreferences.
 */
public class ConfigManager {
  //  private static final String PREF_NAME = "StorePrefs";
  //  private static final String KEY_CONFIG = "AppConfig";
    //private final SharedPreferences prefs;

   // public ConfigManager(Context context) {
       // prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
   // }
/*
    public StoreConfiguration loadConfig() {
        String jsonString = prefs.getString(KEY_CONFIG, null);
        if (jsonString != null) {
            try {
                JSONObject json = new JSONObject(jsonString);
                return JsonUtil.configFromJson(json);
            } catch (JSONException e) {
                // שגיאת לוג: לא ניתן לטעון JSON, חוזרים לברירת מחדל
                return new StoreConfiguration();
            }
        }
        return new StoreConfiguration(); // הגדרה ראשונית
    }

    public void saveConfig(StoreConfiguration config) {
        try {
            JSONObject json = JsonUtil.configToJson(config);
            prefs.edit().putString(KEY_CONFIG, json.toString()).apply();
        } catch (JSONException e) {
            // שגיאת לוג: לא ניתן לשמור ל-JSON
        }
    }*/
 /*
        private StoreConfiguration config;

        public ConfigManager() {
            // טעינת הגדרות ברירת מחדל
            config = new StoreConfiguration();
            config.installedAppsCheckList.add("com.waze"); // דוגמה
        }

        public StoreConfiguration getConfig() {
            return config;
        }*/

        // ניתן להוסיף לוגיקה לשמירה וטעינה
    
        
        private static final String CONFIG_KEY = "StoreConfigJson";

        private final Context context;
        private StoreConfiguration config;

        public ConfigManager(Context context) {
            this.context = context;
            this.config = loadConfig(); 
        }

        public StoreConfiguration getConfig() {
            return config;
        }
/*
        public StoreConfiguration loadConfig() {
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            String jsonString = prefs.getString(CONFIG_KEY, null);

            if (jsonString != null) {
                try {
                    return JsonUtil.deserializeStoreConfiguration(jsonString);
                } catch (JSONException e) {
                    LogUtil.logToFile("ConfigManager", "Error loading config, returning default: " + e.getMessage());
                    return new StoreConfiguration();
                }
            }
            return new StoreConfiguration();
        }
*/
        /*public void saveConfig(StoreConfiguration configToSave) {
            try {
                String jsonString = JsonUtil.serializeStoreConfiguration(configToSave);
                SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = prefs.edit();
                editor.putString(CONFIG_KEY, jsonString);
                editor.apply();
                this.config = configToSave;
            } catch (JSONException e) {
                LogUtil.logToFile("ConfigManager", "Error saving config: " + e.getMessage());
            }
        }*/
/*
        public void updateConfig(StoreConfiguration newConfig) {
            saveConfig(newConfig);
        }*/
    // ConfigManager.java (עדכון לוגיקת הטעינה)
    
// ... [existing imports] ...

        // ... [existing fields and constructor] ...

        public StoreConfiguration loadConfig() {
            // [יישום קודם] טעינה מ-SharedPreferences ייעודי
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            String jsonString = prefs.getString(CONFIG_KEY, null);

            StoreConfiguration config;

            if (jsonString != null) {
                try {
                    config = JsonUtil.deserializeStoreConfiguration(jsonString);
                } catch (JSONException e) {
                    // אם יש שגיאה בטעינת ה-JSON, טען ברירת מחדל
                    LogUtil.logToFile("ConfigManager"+ "Error deserializing config, loading defaults.");
                    config = new StoreConfiguration();
                }
            } else {
                // אם אין קובץ שמור, טען ברירת מחדל
                config = new StoreConfiguration();
            }

            // --- חדש: עדכון ConfigManager מה-PreferenceManager עבור ערכי ברירת מחדל ---
            // כאשר משתמשים ב-PreferenceFragment, עלינו לוודא שערכי ברירת המחדל
            // מה-XML נטענים ל-SharedPreferences הגלובלי (של PreferenceManager)
            PreferenceManager.setDefaultValues(context, R.xml.storePreferences, false);
            //SharedPreferences defaultPrefs = PreferenceManager.getDefaultSharedPreferences(context);

            // עקיפה של ערכי ברירת המחדל של ConfigManager עם אלו של PreferenceManager
            // (בפעם הראשונה שהאפליקציה רצה, או אם המודל לא מכיל את השדות)
            config = new StoreConfiguration(
               // config.checkInstalledAppsForUpdates, // נשאר לשמור על תאימות לאחור
              //  defaultPrefs.getBoolean("check_system_apps", config.checkSystemApps), 
               // defaultPrefs.getBoolean("check_user_apps", config.checkUserApps), 
               // config.installedAppsCheckList, // ניהול רשימות עדיין דרך המנג'ר
                config.defaultSource, 
                config.defaultSourceLink
            );

            return config;
        }

        // ... [rest of the file: saveConfig remains the same] ...
    
}
