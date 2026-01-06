package com.mdm.activities;

import android.content.Context;
import java.util.ArrayList;
import java.util.List;
import java.util.Collections;

import com.mdm.store.ApkLinkResolverService;
import com.mdm.store.ApkDownloadInfo;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.PackageInfo;
import android.content.pm.ApplicationInfo;
import org.json.JSONException;
import android.preference.PreferenceManager;
import android.graphics.drawable.Drawable;
import com.emanuelef.remote_capture.activities.LogUtil;

/**
 * ניהול רשימת הפריטים בחנות (במערכת אמיתית, נתונים אלו היו נשמרים במסד נתונים או קובץ).
 */
public class ItemsManager {
    private final List<StoreItem> allItems = new ArrayList<StoreItem>();
    private final Context context;

    public interface RefreshCompleteListener {
        void onComplete();
    }

/*
    public void addItem(StoreItem item) {
        StoreItem existing = findItemByPackageName(item.packageName);
        if (existing != null) {
            allItems.remove(existing); // החלפה
        }
        allItems.add(item);
        // שמירה
    }*/
/*
    public void removeItem(StoreItem item) {
        allItems.remove(item);
        // שמירה
    }*/
/*
    public StoreItem findItemByPackageName(String packageName) {
        for (StoreItem item : allItems) {
            if (item.packageName.equals(packageName)) {
                return item;
            }
        }
        return null;
    }*/

   /* public void resetAndLoadDefaultItems(StoreConfiguration.DefaultSourceType sourceType, String link) {
        allItems.clear();

        // לוגיקת טעינת ברירת מחדל (מקומי/קישור)
        if (sourceType == StoreConfiguration.DefaultSourceType.LOCAL) {
            allItems.add(new StoreItem("com.local.default", "Local Default App", "1.0.0", "1.0.0", "Local", StoreItem.ItemSourceType.JSON_LINK, null, false, null, null, 100, false));
        } else if (sourceType == StoreConfiguration.DefaultSourceType.LINK) {
            allItems.add(new StoreItem("com.remote.default", "Remote Default App", "1.0.0", "1.0.0", "Remote", StoreItem.ItemSourceType.JSON_LINK, link, false, null, null, 100, false));
        }
        // שמירה
    }*/
/*
    public void updateItemLink(StoreItem oldItem, String newLink) {
        // יצירת פריט חדש עם הקישור המעודכן (מכיוון שה-POJO הוא final)
        StoreItem newItem = new StoreItem(oldItem.packageName, oldItem.title, oldItem.currentVersion, oldItem.latestVersion, 
                                          oldItem.source, oldItem.itemSourceType, newLink, oldItem.excludedFromUpdates, 
                                          oldItem.downloadLink, oldItem.signature, oldItem.versionCode, oldItem.updateAvailable);
        removeItem(oldItem);
        addItem(newItem);
    }
*/
    /** * רענון אסינכרוני של כל הפריטים. זה צריך לרוץ על Thread נפרד.
     */
  /*  public void refreshAllItems(final StoreConfiguration config, final RefreshCompleteListener listener) {
        // מריץ ברקע כדי לא לחסום את ה-UI
        new Thread(new Runnable() {
                public void run() {
                    // לולאה על כל הפריטים וקריאה ל-ApkLinkResolverService
                    // ...
                    // לאחר סיום:
                    listener.onComplete();
                }
            }).start();
    }*/
    // ItemsManager.java (עדכון)
  

    

  
        /*
        private boolean isAppInstalled(String packageName) {
            // Mock: com.mdm.zing ו-com.waze מותקנות
            return packageName.equals("com.mdm.zing") || packageName.equals("com.waze");
        }

      
        private String getInstalledVersion(String packageName) {
            if (packageName.equals("com.mdm.zing")) return "1.0.0";
            if (packageName.equals("com.waze")) return "4.80.0"; // מדמה גרסה ישנה
            return "N/A";
        }

     
        private List<String> getSystemInstalledApps() {
            List<String> list = new ArrayList<String>();
            list.add("com.mdm.zing");
            list.add("com.waze");
            list.add("com.google.android.inputmethod.latin"); 
            list.add("com.musicolet"); // דוגמה נוספת
            return list;
        }
        
        */
        /*
        
        public void refreshAllItems(final StoreConfiguration config, final RefreshCompleteListener listener) {

            new Thread(new Runnable() {
                    public void run() {
                        ApkLinkResolverService resolverService = new ApkLinkResolverService();

                        // --- 1. סריקת אפליקציות מותקנות (אם מופעל) ---
                        if (config.checkInstalledAppsForUpdates) {
                            List<String> installedApps = getSystemInstalledApps();

                            for (String pn : installedApps) {
                                // אם האפליקציה נבחרה לעדכון (select your installed apps whats you want to check)
                                if (config.installedAppsCheckList.contains(pn)) { 
                                    StoreItem existingItem = findItemByPackageName(pn);

                                    // אם עדיין לא קיים במאגר כפריט INSTALLED_APP, הוסף אותו
                                    if (existingItem == null) {
                                        String currentVersion = getInstalledVersion(pn);
                                        // הוספה זמנית למאגר, תעודכן בלולאה הבאה
                                        allItems.add(new StoreItem(pn, pn, currentVersion, currentVersion, "System", StoreItem.ItemSourceType.INSTALLED_APP, null, false, null, null, 0, false));
                                    }
                                }
                            }
                        }

                        // --- 2. לולאת עדכון על כל הפריטים במאגר ---
                        // יש ליצור עותק של allItems כדי להימנע מ-ConcurrentModificationException
                        // ולהשתמש באינדקסים/setItem כדי לעדכן את הרשימה המקורית.
                        List<StoreItem> itemsToUpdate = new ArrayList<StoreItem>(allItems);

                        for (int i = 0; i < itemsToUpdate.size(); i++) {
                            StoreItem item = itemsToUpdate.get(i);

                            // בדיקת חריגה מפורשת ל-INSTALLED_APP
                            if (item.itemSourceType == StoreItem.ItemSourceType.INSTALLED_APP && !config.installedAppsCheckList.contains(item.packageName)) {
                                // אם הוא INSTALLED_APP ולא נבחר לעדכון, דלג עליו
                                continue;
                            }

                            try {
                                // קריאה לשירות הרשת (ApkLinkResolverService)
                                ApkDownloadInfo downloadInfo = resolverService.getApkDownloadLink(item.packageName);

                                // קבלת גרסה נוכחית (אם מותקן, קח את הגרסה המותקנת; אחרת, השתמש בנתון הקיים)
                                boolean isInstalled = isAppInstalled(item.packageName);

                                if (downloadInfo != null) {

                                    // יצירת פריט מעודכן
                                    StoreItem updatedItem = StoreItem.createUpdatedItem(item, downloadInfo, isInstalled);

                                    // עדכון המאגר בפועל
                                    int originalIndex = allItems.indexOf(item);
                                    if (originalIndex != -1) {
                                        // מחליפים את הפריט הישן בחדש המעודכן
                                        allItems.set(originalIndex, updatedItem); 
                                    }

                                    // הדרישה: "if find latest - break... - no need to check all"
                                    // הדבר מושג באמצעות כך ש-ApkLinkResolverService מחזיר רק את המידע הטוב ביותר שהוא מצא.

                                } else {
                                    // לא נמצאו עדכונים מהרשת, הפריט נשאר כפי שהוא
                                }

                            } catch (Exception e) {
                                Log.e("ItemsManager", "Error updating " + item.packageName + ": " + e.getMessage());
                                // ניתן לטפל בשגיאות רשת או Parse
                            }
                        }

                        // סיום והחזרת השליטה ל-UI
                        listener.onComplete();
                    }
                }).start();
        }
    
   */
        private static final String PREFS_NAME = "StorePrefs";
        private static final String ITEMS_KEY = "StoreItemsJson";

        // נעילת סנכרון למניעת בעיות Multi-Threading
        private final Object listLock = new Object(); 

        
/*
        public ItemsManager(Context context) {
            this.context = context;
            loadItems();
        }*/

        // --- Persistence (SharedPreferences) ---
    String defitems="[\n {\n \"cl\": [\n \"com.whatsapp\"\n ]\n },\n {\n \"itemSourceType\": \"MANUAL\",\n \"packageName\": \"fm.jewishmusic.application\",\n \"title\": \"זינג\",\n \"customLink\": \"\",\n \"isDrive\": false\n },\n {\n \"itemSourceType\": \"MANUAL\",\n \"packageName\": \"kcm.fm.volume\",\n \"title\": \"מיוזיק ווליום\",\n \"customLink\": \"\",\n \"isDrive\": false\n },\n {\n \"itemSourceType\": \"MANUAL\",\n \"packageName\": \"in.krosbits.musicolet\",\n \"title\": \"Musicolet\",\n \"customLink\": \"\",\n \"isDrive\": false\n },\n {\n \"itemSourceType\": \"CUSTOM_LINK\",\n \"packageName\": \"com.waze\",\n \"title\": \"waze\",\n \"customLink\": \"1HDFIR0ki-STB0t22WJlr0lERvgliQkBo\",\n \"isDrive\": true\n },\n {\n \"itemSourceType\": \"CUSTOM_LINK\",\n \"packageName\": \"com.google.android.inputmethod.latin\",\n \"title\": \"Gboard\",\n \"customLink\": \"1OwaLDf2Piln72bMD7T2HmZySlq-ZaMVb\",\n \"isDrive\": true\n },\n {\n \"itemSourceType\": \"CUSTOM_LINK\",\n \"packageName\": \"com.alphainventor.filemanager\",\n \"title\": \"File Manager +\",\n \"customLink\": \"1pbl-LWLfi9cAFUB3_nDEg8A0qY3v8A9o\",\n \"isDrive\": true\n }\n]";
        private void loadItems() {
            synchronized (listLock) {
                SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
                String jsonString = prefs.getString(ITEMS_KEY, defitems);

                if (jsonString != null) {
                    try {
                        allItems.clear(); 
                        allItems.addAll(JsonUtil.deserializeStoreItems(jsonString,this));
                    } catch (JSONException e) {
                        LogUtil.logToFile("ItemsManager"+"Error loading items, starting fresh: " + e.getMessage());
                        // במקרה של שגיאה, מתחילים רשימה ריקה כדי למנוע קריסה
                        allItems.clear(); 
                    }
                }
            }
        }

        private void saveItems(boolean def) {
            synchronized (listLock) {
                try {
                    String jsonString = JsonUtil.serializeStoreItems(allItems,this);
                    if(def){
                        jsonString=defitems;
                    }
                    SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putString(ITEMS_KEY, jsonString);
                    editor.apply();
                    if(def){
                        loadItems();
                    }
                } catch (JSONException e) {
                    LogUtil.logToFile("ItemsManager"+"Error saving items: " + e.getMessage());
                }
            }
        }

        // --- Public Item Management ---

        public List<StoreItem> getAllItems() {
            synchronized (listLock) {
                //return Collections.unmodifiableList(new ArrayList<>(allItems));
                return allItems;
            }
        }
    public List<StoreItem> getAllItemsvisible() {
        synchronized (listLock) {
            if(!PreferenceManager.getDefaultSharedPreferences(context).getBoolean("visible_all",false)){
            List<StoreItem> itemsvisible=new ArrayList<>();
            for(StoreItem si:allItems){
               // LogUtil.logToFile(si.downloadLink+si.customLink+si.currentVersion+si.latestVersion+(!si.currentVersion.equals(si.latestVersion))+(si.currentVersion.equals("N/A"))
               // +((si.downloadLink!=null&&!si.downloadLink.equals(""))||(si.customLink!=null&&!si.customLink.equals(""))&&(!si.currentVersion.equals(si.latestVersion)||(si.currentVersion.equals("N/A"))))
               // +(!si.currentVersion.equals(si.latestVersion)||(si.currentVersion.equals("N/A"))));
                if((si.downloadLink!=null&&!si.downloadLink.equals(""))||(si.customLink!=null&&!si.customLink.equals(""))){
                    if((!si.currentVersion.equals(si.latestVersion)||(si.currentVersion.equals("N/A")))){
                    //LogUtil.logToFile(si.packageName);
                    
                    itemsvisible.add(si);
                    }
                }
            }
            //return Collections.unmodifiableList(new ArrayList<>(itemsvisible));
            return itemsvisible;
            }else{
                //return Collections.unmodifiableList(new ArrayList<>(allItems));
                return allItems;
            }
        }
    }
     /*   public void addItem(StoreItem item) {
            synchronized (listLock) {
                // ודא שאין כפילויות לפני הוספה
                if (findItemByPackageName(item.packageName) == null) {
                    allItems.add(item);
                    saveItems();
                }
            }
        }
*/
        public void updateItemLink(StoreItem oldItem, String newLink) {
            synchronized (listLock) {
                int index = allItems.indexOf(oldItem);
                if (index != -1) {
                    // יצירת אובייקט חדש עם הקישור המעודכן (כדי לשמור על Immutability במידה מסוימת)
                    StoreItem updatedItem = new StoreItem(
                        oldItem.packageName, oldItem.title, oldItem.icon, oldItem.currentVersion, oldItem.latestVersion,
                        oldItem.source, oldItem.itemSourceType, newLink,oldItem.isDrive, // הקישור החדש
                        oldItem.excludedFromUpdates, oldItem.downloadLink, oldItem.signature,
                        oldItem.versionCode, oldItem.updateAvailable
                    );
                    allItems.set(index, updatedItem);
                    saveItems(false);
                }
            }
        }

        public void removeItem(StoreItem item) {
            synchronized (listLock) {
                if (allItems.remove(item)) {
                    saveItems(false);
                }
            }
        }

        public StoreItem findItemByPackageName(String packageName) {
            // אין צורך בסנכרון נוסף כאן כי הפונקציה נקראת מתוך מתודה מסונכרנת (synchronized)
            for (StoreItem item : allItems) {
                if (item.packageName.equals(packageName)) {
                    return item;
                }
            }
            return null;
        }

        // --- PackageManager Integration (ללא שינוי) ---

        private boolean isAppInstalled(String packageName) {
            PackageManager pm = context.getPackageManager();
            try {
                pm.getPackageInfo(packageName, 0);
                return true;
            } catch (PackageManager.NameNotFoundException e) {
                return false;
            }
        }

        public String getInstalledVersion(String packageName) {
            // [יישום קיים]
            PackageManager pm = context.getPackageManager();
            try {
                PackageInfo pInfo = pm.getPackageInfo(packageName, 0);
                return pInfo.versionName != null ? pInfo.versionName : "N/A";
            } catch (PackageManager.NameNotFoundException e) {
                return "N/A";
            }
        }
/*
        private List<String> getSystemInstalledApps() {
            // [יישום קיים]
            List<String> list = new ArrayList<String>();
            PackageManager pm = context.getPackageManager();
            List<PackageInfo> packages = pm.getInstalledPackages(PackageManager.GET_META_DATA); 

            for (PackageInfo packageInfo : packages) {
                // סינון אפליקציות מערכת
                if ((packageInfo.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) == 0) {
                    list.add(packageInfo.packageName);
                }
            }
            return list;
        }*/

        // --- רענון אסינכרוני (הלוגיקה הפנימית שונתה לשימוש ב-lock) ---
/*
        public void refreshAllItems(final StoreConfiguration config, final RefreshCompleteListener listener) {

            new Thread(new Runnable() {
                    public void run() {
                        ApkLinkResolverService resolverService = new ApkLinkResolverService();

                        synchronized (listLock) { // סנכרון על כל לוגיקת העדכון
                            // --- 1. סריקת אפליקציות מותקנות ---
                            if (config.checkInstalledAppsForUpdates) {
                                List<String> installedApps = getSystemInstalledApps();

                                for (String pn : installedApps) {
                                    if (config.installedAppsCheckList.contains(pn)) { 
                                        StoreItem existingItem = findItemByPackageName(pn);

                                        if (existingItem == null) {
                                            String currentVersion = getInstalledVersion(pn);
                                            StoreItem newItem = new StoreItem(pn, pn, currentVersion, currentVersion, "System", StoreItem.ItemSourceType.INSTALLED_APP, null, false, null, null, 0, false);
                                            allItems.add(newItem); 
                                        }
                                    }
                                }
                            }

                            // --- 2. לולאת עדכון על כל הפריטים במאגר ---
                            List<StoreItem> itemsToUpdate = new ArrayList<StoreItem>(allItems);

                            for (int i = 0; i < itemsToUpdate.size(); i++) {
                                StoreItem item = itemsToUpdate.get(i);

                                if (item.excludedFromUpdates) continue;
                                if (item.itemSourceType == StoreItem.ItemSourceType.INSTALLED_APP && !config.installedAppsCheckList.contains(item.packageName)) continue;

                                try {
                                    ApkDownloadInfo downloadInfo = resolverService.getApkDownloadLink(item.packageName);

                                    boolean isInstalled = isAppInstalled(item.packageName);

                                    if (downloadInfo != null) {
                                        StoreItem updatedItem = StoreItem.createUpdatedItem(item, downloadInfo, isInstalled);

                                        int originalIndex = allItems.indexOf(item);
                                        if (originalIndex != -1) {
                                            allItems.set(originalIndex, updatedItem); 
                                        }
                                    }

                                } catch (Exception e) {
                                    LogUtil.logToFile("ItemsManager", "Error updating " + item.packageName + ": " + e.getMessage());
                                }
                            }

                            // --- 3. שמירה וסיום ---
                            saveItems();
                        } // סוף synchronized block

                        listener.onComplete();
                    }
                }).start();
        }
        */
    // ItemsManager.java
// ... [existing imports] ...
    
        
        public void clearAllItems(boolean def) {
            synchronized (listLock) {
                allItems.clear();
                saveItems(def);
            }
        }

        
        public void addItems(List<StoreItem> newItems, boolean ignoreExisting) {
            synchronized (listLock) {
                boolean changed = false;
                for (StoreItem item : newItems) {
                    if (ignoreExisting && findItemByPackageName(item.packageName) != null) {
                        continue; // דלג אם כבר קיים
                    }

                    // עדכון פרטי התקנה לפני הוספה
                    StoreItem finalItem = applyInstallationInfo(item);
                    allItems.add(finalItem);
                    changed = true;
                }
                if (changed) {
                    saveItems(false);
                }
            }
        }

        
        public boolean addItem(StoreItem item) {
            synchronized (listLock) {
                // בדיקה 1: אם הפריט כבר קיים ברשימה הנוכחית
                if (findItemByPackageName(item.packageName) != null) {
                    LogUtil.logToFile("ItemsManager"+ "Item with package name " + item.packageName + " already exists.");
                    return false; 
                }

                // בדיקה 2: אם הפריט מותקן, עדכון פרטי הגרסה והמקור
                StoreItem finalItem = applyInstallationInfo(item);

                allItems.add(finalItem);
                saveItems(false);
                return true;
            }
        }

        
        private StoreItem applyInstallationInfo(StoreItem item) {
            boolean isInstalled = isAppInstalled(item.packageName);

            if (isInstalled) {
                LogUtil.logToFile("installed change source type..");
                
                // יצירת אובייקט חדש עם פרטי התקנה מעודכנים
                return new StoreItem(
                    item.packageName, 
                    getTitle(item.packageName),
                    getIcon(item.packageName),
                    getInstalledVersion(item.packageName),           // גרסה נוכחית מעודכנת
                    item.latestVersion, 
                    item.source, 
                    //StoreItem.ItemSourceType.INSTALLED_APP, // סוג המקור משתנה
                    item.itemSourceType,
                    item.customLink,
                    item.isDrive,
                    item.excludedFromUpdates, 
                    item.downloadLink, 
                    item.signature, 
                    item.versionCode, 
                    item.updateAvailable
                );
            }
            return item; // החזרת המקורי אם לא מותקן
        }

        // ... [rest of the methods: updateItemLink, removeItem, findItemByPackageName, isAppInstalled, getInstalledVersion, getSystemInstalledApps, refreshAllItems - existing] ...
    // ItemsManager.java

// ... [existing imports] ...
 
        // ... [existing code] ...

        /*
        public List<PackageInfo> getInstalledApps(boolean isSystem) {
            List<PackageInfo> filteredList = new ArrayList<>();
            PackageManager pm = context.getPackageManager();
            List<PackageInfo> packages = pm.getInstalledPackages(PackageManager.GET_META_DATA); 

            for (PackageInfo packageInfo : packages) {
                boolean isSystemApp = (packageInfo.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0;

                if (isSystem && isSystemApp) {
                    filteredList.add(packageInfo);
                } else if (!isSystem && !isSystemApp) {
                    filteredList.add(packageInfo);
                }
            }
            return filteredList;
        }
*/
        // ... [existing methods] ...

        // --- רענון אסינכרוני (לוגיקת עדכון פשוטה) ---
/*
        public void refreshAllItems(final StoreConfiguration config, final RefreshCompleteListener listener) {

            new Thread(new Runnable() {
                    public void run() {
                        ApkLinkResolverService resolverService = new ApkLinkResolverService();

                        synchronized (listLock) { 

                            // --- 1. סריקת אפליקציות מותקנות (לצורך הוספה למאגר) ---
                            // טוען את כל האפליקציות כדי לבדוק מול הרשימה הספציפית
                            List<PackageInfo> allInstalledApps = getInstalledApps(true); 
                            allInstalledApps.addAll(getInstalledApps(false)); 

                            for (PackageInfo packageInfo : allInstalledApps) {
                                String pn = packageInfo.packageName;

                                // רק אם ה-package name מופיע ברשימה הלבנה
                                if (config.installedAppsCheckList.contains(pn)) { 

                                    StoreItem existingItem = findItemByPackageName(pn);

                                    if (existingItem == null) {
                                        String currentVersion = getInstalledVersion(pn);

                                        StoreItem newItem = new StoreItem(
                                            pn, pn, currentVersion, currentVersion, "System", 
                                            StoreItem.ItemSourceType.INSTALLED_APP, null, 
                                            false, null, null, 0, false
                                        );
                                        allItems.add(newItem); 
                                    }
                                }
                            }

                            // --- 2. לולאת עדכון על כל הפריטים במאגר ---
                            List<StoreItem> itemsToUpdate = new ArrayList<StoreItem>(allItems);

                            for (int i = 0; i < itemsToUpdate.size(); i++) {
                                StoreItem item = itemsToUpdate.get(i);

                                if (item.excludedFromUpdates) continue;

                                // סינון: רק אם זהו פריט מותקן והוא נמצא ברשימה הלבנה
                                if (item.itemSourceType == StoreItem.ItemSourceType.INSTALLED_APP && 
                                    !config.installedAppsCheckList.contains(item.packageName)) {

                                    continue; 
                                }

                                // ... [rest of the update logic] ...
                            }

                            saveItems();
                        } 

                        listener.onComplete();
                    }
                }).start();
        }*/
    // ItemsManager.java

// ... [existing imports] ...

   
        // ... [existing fields] ...
    private static ItemsManager itemsManager;
        private ConfigManager configManager; // הוספת שדה

        public ItemsManager(Context context, ConfigManager configManager) { // עדכון הבנאי
            this.context = context;
            this.itemsManager=this;
            this.configManager = configManager;
            loadItems();
        }
    public static ItemsManager getItemsManager() { // מתודה חדשה
        return itemsManager;
    }
        public ConfigManager getConfigManager() { // מתודה חדשה
            return configManager;
        }

        /**
         * קבלת רשימת PackageInfo של אפליקציות מותקנות לפי סוג (System/User).
         * נדרש עבור דיאלוג הבחירה המרובה.
         */
        public List<PackageInfo> getInstalledApps(boolean isSystem) {
            // [יישום זהה למה שסופק בתשובה הקודמת, המפריד בין System ל-User]
            List<PackageInfo> filteredList = new ArrayList<>();
            PackageManager pm = context.getPackageManager();
            List<PackageInfo> packages = pm.getInstalledPackages(PackageManager.GET_META_DATA); 

            for (PackageInfo packageInfo : packages) {
                boolean isSystemApp = (packageInfo.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0;

                if (isSystem && isSystemApp) {
                    filteredList.add(packageInfo);
                } else if (!isSystem && !isSystemApp) {
                    filteredList.add(packageInfo);
                }
            }
            return filteredList;
        }

        // --- רענון אסינכרוני (Refresh) ---
    /*public static List<String> installedAppsCheckList(Context context){
            String installedAppsCheckListString = PreferenceManager.getDefaultSharedPreferences(context).getString(storeSettingsFragment. KEY_CHECK_LIST, "");
            return stringToList(installedAppsCheckListString);
        }*/
    private static List<String> stringToList(String s) {
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
        public void refreshAllItems(final boolean refreshinfo, final StoreConfiguration config, final RefreshCompleteListener listener) {
            new Thread(new Runnable() {
                    public void run() {
                        // ... [existing initialization code] ...
                        ApkLinkResolverService resolverService = new ApkLinkResolverService();
                        synchronized (listLock) { 
                            // --- 1. סריקת אפליקציות מותקנות (לצורך הוספה למאגר) ---
                            // סורק את כל האפליקציות המותקנות במכשיר
                            List<PackageInfo> allInstalledApps = getInstalledApps(true); 
                            allInstalledApps.addAll(getInstalledApps(false)); 
                            boolean faund=true;
                            /*while(faund){
                                faund=false;
                                for(PackageInfo pi:allInstalledApps){
                                    for(StoreItem si:allItems){
                                        if(si.itemSourceType.equals(StoreItem.ItemSourceType.CUSTOM_LINK)&&si.packageName.equals(pi.packageName)){
                                            allInstalledApps.remove(pi);
                                            faund=true;
                                            break;
                                        }
                                    }
                                    if(faund)
                                        break;
                                }
                            }*/
                            for (PackageInfo packageInfo : allInstalledApps) {
                                String pn = packageInfo.packageName;
                                
                                // הוספה למאגר רק אם נמצא ברשימה הלבנה
                               /* if (installedAppsCheckList(context).contains(pn)) {
                                    StoreItem existingItem = findItemByPackageName(pn);

                                    if (existingItem == null) {
                                        // ... [creation and addition of newItem] ...
                                        String currentVersion = getInstalledVersion(pn);
                                        
                                        StoreItem newItem = new StoreItem(
                                            pn, getTitle(pn),getIcon(pn), currentVersion, "N/A", "מערכת", 
                                            StoreItem.ItemSourceType.INSTALLED_APP, "", 
                                            false, "", "", 0, false
                                        );
                                        allItems.add(newItem); 
                                     }else{
                                         //is in check list but is already in the items - like custom link...
                                         //if title eq pn replace to title
                                         if(existingItem.title.equals(pn)){
                                         allItems.remove(existingItem);
                                         existingItem.title=getTitle(pn);
                                         allItems.add(existingItem);
                                         }
                                         
                                     }
                                }else{
                                    //is old item remove now
                                    StoreItem existingItem = findItemByPackageName(pn);
                                    if(existingItem!=null&&existingItem.itemSourceType.equals(StoreItem.ItemSourceType.INSTALLED_APP)){
                                        allItems.remove(existingItem);
                                    }
                                }*/
                            }
                            List<String> pns=new ArrayList<>();
                            PackageInfo[] pis={};
                            pis= allInstalledApps.toArray(pis); 
                            for(PackageInfo pi:pis){
                                pns.add(pi.packageName);
                            }
                            
                            faund=true;
                            while(faund){
                                //isnt curect pn...
                            faund=false;
                            for(StoreItem sif:allItems){
                                    if(sif.itemSourceType.equals(StoreItem.ItemSourceType.INSTALLED_APP)&&!pns.contains(sif.packageName)){
                                        allItems.remove(sif);
                                        faund=true;
                                        break;
                                    }
                                //is multiple...
                            for(StoreItem sis:allItems){
                                if(sis.equals(sif))continue;
                                if(sis.packageName.equals(sif.packageName)&&sis.itemSourceType.equals(StoreItem.ItemSourceType.INSTALLED_APP)&&sif.itemSourceType.equals(StoreItem.ItemSourceType.INSTALLED_APP)){
                                    allItems.remove(sis);
                                    faund=true;
                                    break;
                                    //do not break... continue removing all...
                                    //break because modification list exception...
                                }
                            }
                            if(faund)
                            break;
                            }
                            
                            }
                            if(refreshinfo){
                            List<StoreItem> itemsToUpdate = new ArrayList<StoreItem>(allItems);
                            // --- 2. לולאת עדכון על כל הפריטים במאגר ---
                            // ... [existing update loop] ...
                            for (StoreItem item : itemsToUpdate) {
                                if (item.excludedFromUpdates) continue;

                                
                                if (item.itemSourceType == StoreItem.ItemSourceType.CUSTOM_LINK) {
                                    continue; 
                                }
                                    try {
                                        ApkDownloadInfo downloadInfo = resolverService.getApkDownloadLink(item.packageName);

                                        boolean isInstalled = isAppInstalled(item.packageName);

                                        if (downloadInfo != null) {
                                            StoreItem updatedItem = StoreItem.createUpdatedItem(item, downloadInfo, isInstalled);

                                            int originalIndex = allItems.indexOf(item);
                                            if (originalIndex != -1) {
                                                allItems.set(originalIndex, updatedItem); 
                                            }
                                        }

                                    } catch (Exception e) {
                                        LogUtil.logToFile("ItemsManager"+ "Error updating " + item.packageName + ": " + e.getMessage());
                                    }
                                
                                // ... [rest of the update logic] ...
                            }
                            }

                            saveItems(false);
                        } 
                        listener.onComplete();
                    }
                }).start();
        }
    public String getTitle(String packageName) {
        // [יישום קיים]
        PackageManager pm = context.getPackageManager();
        try {
            PackageInfo pInfo = pm.getPackageInfo(packageName, 0);
            return pInfo.applicationInfo.loadLabel(pm) != null ? pInfo.applicationInfo.loadLabel(pm).toString() : packageName;
        } catch (PackageManager.NameNotFoundException e) {
            return "N/A";
        }
    }
    public Drawable getIcon(String packageName) {
        // [יישום קיים]
        PackageManager pm = context.getPackageManager();
        try {
            PackageInfo pInfo = pm.getPackageInfo(packageName, 0);
            return pInfo.applicationInfo.loadIcon(pm) != null ? pInfo.applicationInfo.loadIcon(pm) : context.getResources().getDrawable(android.R.drawable.sym_def_app_icon);
        } catch (PackageManager.NameNotFoundException e) {
            return context.getResources().getDrawable(android.R.drawable.sym_def_app_icon);
        }
    }
}
