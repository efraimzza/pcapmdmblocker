package com.mdm.activities;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import com.mdm.store.ApkSourcePriority;
import java.util.HashSet;
import java.util.Set;
import com.emanuelef.remote_capture.R;
import android.content.pm.PackageInfo;
import com.emanuelef.remote_capture.activities.LogUtil;
/*
public class SettingsFragment extends PreferenceFragment 
implements SharedPreferences.OnSharedPreferenceChangeListener {

    private ConfigManager configManager;
    private ItemsManager itemsManager;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);

        // אתחול המנג'רים
        configManager = new ConfigManager(getActivity());
        itemsManager = new ItemsManager(getActivity());

        // עדכון סיכומי ברירת המחדל
        updatePreferenceSummaries();
    }

    @Override
    public void onResume() {
        super.onResume();
        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
    }

    
    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        Preference pref = findPreference(key);
        if (pref instanceof EditTextPreference) {
            updatePreferenceSummary(pref, sharedPreferences.getString(key, ""));
        }

        // לאחר כל שינוי, יש לשמור את כל הקונפיגורציה ב-ConfigManager כדי לעדכן את המודל
        saveConfigFromPreferences();

        // עדכון סדר הקדימות הגלובלי (סטטי)
        if (key.equals("source_priority_list")) {
            String priorityString = sharedPreferences.getString(key, "");
            List<String> newPriorityList = stringToList(priorityString);
            ApkSourcePriority.setCustomPriority(newPriorityList);
        }
    }

    
    private void updatePreferenceSummaries() {
        SharedPreferences sharedPrefs = getPreferenceScreen().getSharedPreferences();

        updatePreferenceSummary(findPreference("source_priority_list"), 
                                sharedPrefs.getString("source_priority_list", ""));

        updatePreferenceSummary(findPreference("installed_apps_check_list"), 
                                sharedPrefs.getString("installed_apps_check_list", ""));

        // טיפול בכפתור האיפוס
        Preference clearPref = findPreference("clear_all_items");
        if (clearPref != null) {
            clearPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        // הצגת דיאלוג אישור לפני איפוס
                        Dialogs.showClearItemsConfirmationDialog(getActivity(), itemsManager);
                        return true;
                    }
                });
        }

        // ודא שההגדרות הנוכחיות של המערכת (ConfigManager) משתקפות ב-UI
        // אם לא קיימות, שמור את ברירות המחדל של ה-XML
        saveConfigFromPreferences();
    }

    
    private void updatePreferenceSummary(Preference pref, String value) {
        if (pref instanceof EditTextPreference) {
            if (value != null && !value.isEmpty()) {
                pref.setSummary(value);
            } else {
                // הצג ברירת מחדל אם הרישום ריק
                pref.setSummary(((EditTextPreference) pref).getEditText().getHint());
            }
        }
    }

    
    private void saveConfigFromPreferences() {
        SharedPreferences sharedPrefs = getPreferenceScreen().getSharedPreferences();

        boolean checkSystemApps = sharedPrefs.getBoolean("check_system_apps", false);
        boolean checkUserApps = sharedPrefs.getBoolean("check_user_apps", false);
        String installedAppsCheckListString = sharedPrefs.getString("installed_apps_check_list", "");

        List<String> installedAppsCheckList = stringToList(installedAppsCheckListString);

        // יצירת אובייקט קונפיגורציה חדש
        StoreConfiguration newConfig = new StoreConfiguration(
            checkSystemApps || checkUserApps, // checkInstalledAppsForUpdates הופך להיות OR
            checkSystemApps,
            checkUserApps,
            installedAppsCheckList,
            StoreConfiguration.DefaultSourceType.LOCAL, // נשאר כרגע קבוע
            null // נשאר כרגע קבוע
        );

        configManager.saveConfig(newConfig);
    }

    // --- עזרי המרה ---
    private List<String> stringToList(String s) {
        if (s == null || s.trim().isEmpty()) {
            return new ArrayList<>();
        }
        String[] parts = s.split(",");
        List<String> list = new ArrayList<>();
        for (String part : parts) {
            list.add(part.trim());
        }
        return list;
    }
    */

    public class storeSettingsFragment extends PreferenceFragment 
    implements SharedPreferences.OnSharedPreferenceChangeListener, Preference.OnPreferenceClickListener {

        private ConfigManager configManager;
        private ItemsManager itemsManager;
        private SharedPreferences defaultPrefs;

        private static final String KEY_SELECT_SYSTEM_APPS = "select_system_apps";
        private static final String KEY_SELECT_USER_APPS = "select_user_apps";
       // public static final String KEY_CHECK_LIST = "installed_apps_check_list";
        private static final String KEY_PRIORITY_LIST = "source_priority_list";
    private static final String KEY_VIEW_PRIORITY_LIST = "view_priority_list";
        private static final String KEY_CLEAR_ITEMS = "clear_all_items";
    private static final String KEY_CLEAR_AUTH = "clear_auth";

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.storePreferences);

            //configManager = new ConfigManager(getActivity());
            //itemsManager = new ItemsManager(getActivity(),configManager);
            
            itemsManager = ItemsManager.getItemsManager();
            configManager=itemsManager.getConfigManager();
            defaultPrefs = getPreferenceScreen().getSharedPreferences();

            // 1. הגדרת Click Listeners
            findPreference(KEY_SELECT_SYSTEM_APPS).setOnPreferenceClickListener(this);
            findPreference(KEY_SELECT_USER_APPS).setOnPreferenceClickListener(this);
            findPreference(KEY_CLEAR_ITEMS).setOnPreferenceClickListener(this);
            findPreference(KEY_PRIORITY_LIST).setOnPreferenceClickListener(this);
            findPreference(KEY_VIEW_PRIORITY_LIST).setOnPreferenceClickListener(this);
            findPreference(KEY_CLEAR_AUTH).setOnPreferenceClickListener(this);
            updatePreferenceSummaries();
        }

        @Override
        public void onResume() {
            super.onResume();
            getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
        }

        @Override
        public void onPause() {
            super.onPause();
            getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
        }

        // --- טיפול בלחיצה על Preferences ---
        @Override
        public boolean onPreferenceClick(Preference preference) {
            String key = preference.getKey();

            if (key.equals(KEY_SELECT_SYSTEM_APPS)) {
                // פתח דיאלוג בחירת אפליקציות מערכת
                Dialogs.showAppMultiSelectDialog(getActivity(), itemsManager, true, multiSelectListener);
                return true;
            } else if (key.equals(KEY_SELECT_USER_APPS)) {
                // פתח דיאלוג בחירת אפליקציות משתמש
                Dialogs.showAppMultiSelectDialog(getActivity(), itemsManager, false, multiSelectListener);
                return true;
            } else if (key.equals(KEY_CLEAR_ITEMS)) {
                // פתח דיאלוג איפוס רשימה
                Dialogs.showClearItemsConfirmationDialog(getActivity(), itemsManager);
                return true;
            } else if (key.equals(KEY_PRIORITY_LIST)) {
               
                Dialogs.selectPriority(getActivity());
                return true;
            } else if (key.equals(KEY_VIEW_PRIORITY_LIST)) {
                
                Dialogs.loadPriorityFromPrefs(getActivity(),false);
                return true;
            }else if (key.equals(KEY_CLEAR_AUTH)) {
                Dialogs.showClearAuthWarning(getActivity());
                return true;
            }
            return false;
        }

        void f(){
            boolean isSystemApps=true;
            List<PackageInfo> currentInstalledApps = itemsManager.getInstalledApps(isSystemApps);
            for(PackageInfo pi: currentInstalledApps){
                
            }
            
            List<String> selectedPackages=new ArrayList();
            for(String pn: selectedPackages){
                itemsManager.addItem(
                new StoreItem(
                        pn, itemsManager.getTitle( pn),itemsManager.getIcon(pn), itemsManager.getInstalledVersion(pn), "N/A", "מערכת", 
                    StoreItem.ItemSourceType.INSTALLED_APP, "", false,
                    false, "", "", 0, false
                ));
            }
            List<String> curinstpn=new ArrayList<>();
            for(PackageInfo pi: currentInstalledApps){
                curinstpn.add(pi.packageName);
                if(!selectedPackages.contains(pi.packageName)){
                    
                }
            }
            List<String> curls=new ArrayList<>();
            for(StoreItem si: itemsManager.getAllItems()){
                if(si.itemSourceType.equals(StoreItem.ItemSourceType.INSTALLED_APP)&&currentInstalledApps.contains(si.packageName)&&!selectedPackages.contains(si.packageName)){
                    itemsManager.removeItem(si);
                }
            }
        }
        // --- טיפול בקריאה חוזרת מהדיאלוג (שמירת הבחירה המאוחדת) ---
        private final Dialogs.MultiSelectListener multiSelectListener = new Dialogs.MultiSelectListener() {
            @Override
            public void onSelectionSaved(List<String> selectedPackages, boolean isSystemApps) {
                
                for(String pn: selectedPackages){
                    itemsManager.addItem(
                        new StoreItem(
                            pn, itemsManager.getTitle( pn),itemsManager.getIcon(pn), itemsManager.getInstalledVersion(pn), "N/A", "מערכת",
                            StoreItem.ItemSourceType.INSTALLED_APP, "",false,
                            false, "", "", 0, false
                        ));
                }
                List<PackageInfo> currentInstalledApps = itemsManager.getInstalledApps(isSystemApps);
                List<String> curinstpn=new ArrayList<>();
                for(PackageInfo pi: currentInstalledApps){
                    curinstpn.add(pi.packageName);
                }
                boolean found=true;
                while(found){
                    found=false;
                for(StoreItem si: itemsManager.getAllItems()){
                    LogUtil.logToFile(si.itemSourceType.name()+si.packageName+selectedPackages.contains(si.packageName)+curinstpn.contains(si.packageName));
                    if(si.itemSourceType.equals(StoreItem.ItemSourceType.INSTALLED_APP)&&curinstpn.contains(si.packageName)&&!selectedPackages.contains(si.packageName)){
                        itemsManager.removeItem(si);
                        found=true;
                        break;
                    }
                }
                }
                /*String currentListString = defaultPrefs.getString(KEY_CHECK_LIST, "");
                List<String> currentList = stringToList(currentListString);
                Set<String> newCheckListSet = new HashSet<>(currentList);

                // 1. מחיקת כל הפריטים שהם מהסוג הנוכחי (System או User) מהרשימה המאוחדת
                // זה מבטיח שפריטים מסומנים בעבר שלא נבחרו שוב - ימחקו.
                List<android.content.pm.PackageInfo> currentInstalledApps = itemsManager.getInstalledApps(isSystemApps);
                for(android.content.pm.PackageInfo info : currentInstalledApps) {
                    newCheckListSet.remove(info.packageName);
                }

                // 2. הוספת הפריטים החדשים שנבחרו
                newCheckListSet.addAll(selectedPackages);

                // 3. שמירת הרשימה המאוחדת החדשה ב-SharedPreferences
                String finalListString = listToString(new ArrayList<>(newCheckListSet));
                defaultPrefs.edit().putString(KEY_CHECK_LIST, finalListString).apply();

                // הפעלת onSharedPreferenceChanged ידנית כדי לעדכן את הסיכומים והמודל
                onSharedPreferenceChanged(defaultPrefs, KEY_CHECK_LIST);
                
                
                */

                Toast.makeText(getActivity(), "הבחירה נשמרה ברשימת העדכון.", Toast.LENGTH_SHORT).show();
            }
        };

        // --- טיפול בשינויים בהעדפות המשתמש ---
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            Preference pref = findPreference(key);
            if (pref instanceof EditTextPreference) {
                updatePreferenceSummary(pref, sharedPreferences.getString(key, ""));
            }

            // שמירה תמיד לאחר שינוי כדי לעדכן את המודל של ConfigManager
            //saveConfigFromPreferences();

            // עדכון סדר הקדימות הגלובלי (סטטי)
           /* if (key.equals(KEY_PRIORITY_LIST)) {
                String priorityString = sharedPreferences.getString(key, "");
                List<String> newPriorityList = stringToList(priorityString);
                ApkSourcePriority.setCustomPriority(newPriorityList);
            }*/

            // עדכון הסיכומים של בוררי האפליקציות (System/User)
            /*if (key.equals(KEY_CHECK_LIST)) {
                String listString = sharedPreferences.getString(KEY_CHECK_LIST, "");
                Preference systemPref = findPreference(KEY_SELECT_SYSTEM_APPS);
                Preference userPref = findPreference(KEY_SELECT_USER_APPS);

                int count = stringToList(listString).size();
                String summary = "כרגע נבחרו " + count + " פריטים לבדיקה.";

                if (systemPref != null) systemPref.setSummary(summary);
                if (userPref != null) userPref.setSummary(summary);
            }*/
        }

        // ... [updatePreferenceSummaries, updatePreferenceSummary, stringToList, listToString נשארים כפי שהם] ...

        /**
         * ממיר את כל ה-Preferences השמורים לאובייקט StoreConfiguration ושומר אותו.
         */
        /*private void saveConfigFromPreferences() {
            SharedPreferences sharedPrefs = getPreferenceScreen().getSharedPreferences();

            String installedAppsCheckListString = sharedPrefs.getString(KEY_CHECK_LIST, "");
            List<String> installedAppsCheckList = stringToList(installedAppsCheckListString);

            // יצירת אובייקט קונפיגורציה חדש
            StoreConfiguration newConfig = new StoreConfiguration(
                !installedAppsCheckList.isEmpty(), // checkInstalledAppsForUpdates יהיה TRUE אם הרשימה אינה ריקה
                installedAppsCheckList,
                StoreConfiguration.DefaultSourceType.LOCAL, 
                null
            );

            configManager.saveConfig(newConfig);
        }*/
    private List<String> stringToList(String s) {
        if (s == null || s.trim().isEmpty()) {
            return new ArrayList<>();
        }
        String[] parts = s.split(",");
        List<String> list = new ArrayList<>();
        for (String part : parts) {
            list.add(part.trim());
        }
        return list;
    }
    private static String listToString(List<String> list) {
        // ... [implementation from previous response] ...
        if (list == null || list.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < list.size(); i++) {
            sb.append(list.get(i));
            if (i < list.size() - 1) {
                sb.append(", ");
            }
        }
        return sb.toString();
    }
    private void updatePreferenceSummary(Preference pref, String value) {
        if (pref instanceof EditTextPreference) {
            if (value != null && !value.isEmpty()) {
                pref.setSummary(value);
            } else {
                // הצג ברירת מחדל אם הרישום ריק
                pref.setSummary(((EditTextPreference) pref).getEditText().getHint());
            }
        }
    }
    private void updatePreferenceSummaries() {
  /*      SharedPreferences sharedPrefs = getPreferenceScreen().getSharedPreferences();

        updatePreferenceSummary(findPreference("source_priority_list"), 
                                sharedPrefs.getString("source_priority_list", ""));
*/
        //updatePreferenceSummary(findPreference("installed_apps_check_list"), 
          //                      sharedPrefs.getString("installed_apps_check_list", ""));

        // טיפול בכפתור האיפוס
        Preference clearPref = findPreference("clear_all_items");
        if (clearPref != null) {
            clearPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        // הצגת דיאלוג אישור לפני איפוס
                        Dialogs.showClearItemsConfirmationDialog(getActivity(), itemsManager);
                        return true;
                    }
                });
        }

        // ודא שההגדרות הנוכחיות של המערכת (ConfigManager) משתקפות ב-UI
        // אם לא קיימות, שמור את ברירות המחדל של ה-XML
        //saveConfigFromPreferences();
    }
}
