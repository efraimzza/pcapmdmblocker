package com.mdm.activities;
import com.mdm.store.ApkDownloadInfo;
import android.graphics.drawable.Drawable;


/**
 * מודל נתונים עבור פריט אחד בתצוגת החנות.
 */
public class StoreItem {

    // סוג המקור של הפריט (לצורך ניהול/עריכה)
    public enum ItemSourceType {
        MANUAL,          // הוסף ידנית לפי Package Name בלבד
        CUSTOM_LINK,     // הוסף ידנית עם קישור מותאם אישית
        INSTALLED_APP,   // נוצר אוטומטית כחלק מסריקת האפליקציות המותקנות
        JSON_LINK        // הגיע מקובץ JSON שיובא (מקומי או מרשת)
    }
    
    public enum ItemSource {
        GPlay("GPlay"),
        APKPure("APKPure"),
        APKCombo("APKCombo"),
        Aptoide("Aptoide"),
        FDroid("FDroid"),
        None("None");
        private final String description;
        ItemSource(String description){
            this.description = description;
        }
        public String getDescription() {
            return description;
        }
    }
    // --- נתונים בסיסיים (חובה) ---
    public final String packageName;
    public String title;
    public Drawable icon;
    public String currentVersion;
    public final String latestVersion;
    public final String source; // המקור שממנו נשאבו נתוני latestVersion

    // --- נתונים לניהול (נדרש לייבוא/יצוא ולעריכה) ---
    public final ItemSourceType itemSourceType;
    public final String customLink;      // קישור מותאם אישית (אם קיים)
    public final boolean excludedFromUpdates; // הוחרג מבדיקת עדכונים (אם הוא INSTALLED_APP)

    public final boolean isDrive;
    // --- נתונים נוספים נדרשים (כמו מ-ApkDownloadInfo) ---
    public final String downloadLink;
    public final String signature;
    public final int versionCode;

    // --- מצב עדכון ---
    public final boolean updateAvailable;

    // בנאי מלא - עבור טעינה מ-JSON או יצירת פריט חדש
    public StoreItem(String packageName, String title, Drawable icon, String currentVersion, String latestVersion, 
                     String source, ItemSourceType itemSourceType, String customLink, boolean isDrive,
                     boolean excludedFromUpdates, String downloadLink, String signature, 
                     int versionCode, boolean updateAvailable) {
        this.packageName = packageName;
        this.title = title;
        this.icon=icon;
        this.currentVersion = currentVersion;
        this.latestVersion = latestVersion;
        this.source = source;
        this.itemSourceType = itemSourceType;
        this.customLink = customLink;
        this.isDrive=isDrive;
        this.excludedFromUpdates = excludedFromUpdates;
        this.downloadLink = downloadLink;
        this.signature = signature;
        this.versionCode = versionCode;
        this.updateAvailable = updateAvailable;
    }

    /** בנאי חלקי (עבור הוספת פריט ידנית) */
    public StoreItem( ItemSourceType itemSourceType,String packageName,String title, String customLink,boolean isDrive) {
        this(packageName, title != null ? title : "", ItemsManager.getItemsManager().getIcon(packageName), ItemsManager.getItemsManager().getInstalledVersion(packageName), "N/A", !itemSourceType.equals(ItemSourceType.INSTALLED_APP)?"ידני":"מערכת", itemSourceType, customLink,isDrive, false, "", "", 0, false);
    }

    public boolean isRemovable() {
        // ניתן להסיר רק פריטים שנוספו ידנית או מיובאים
        return itemSourceType != ItemSourceType.INSTALLED_APP;
    }

    public boolean isLinkEditable() {
        // ניתן לערוך רק אם הפריט הוא קישור מותאם אישית
        return itemSourceType == ItemSourceType.CUSTOM_LINK || itemSourceType == ItemSourceType.JSON_LINK;
    }
// StoreItem.java (יש להוסיף את הפונקציה הסטטית הזו למחלקה הקיימת)
// ... [שאר הקוד הקיים של StoreItem] ...

    /**
     * יוצר פריט חדש המבוסס על נתוני עדכון מהרשת ומשווה לגרסה הנוכחית.
     */
    public static StoreItem createUpdatedItem(StoreItem currentItem, ApkDownloadInfo downloadInfo, boolean isInstalled) {
        String latestVersion = downloadInfo.version;
        currentItem.currentVersion=ItemsManager.getItemsManager().getInstalledVersion(currentItem.packageName);
        boolean updateAvailable = false;
        
        // בדיקת עדכון רק אם מותקן והגרסה מהרשת חדשה יותר מהגרסה הנוכחית
        if (isInstalled) {
            updateAvailable = VersionComparer.isNewer(latestVersion, currentItem.currentVersion);
        }

        // נשמר את הגרסה הנוכחית (של המותקן)
        String finalCurrentVersion = currentItem.currentVersion;

        // אם אין כותרת מקומית, נאמץ את הכותרת מהרשת
        String finalTitle = (currentItem.title == null || currentItem.title.isEmpty() || currentItem.title.equals(currentItem.packageName)) 
            ? downloadInfo.title : currentItem.title;
            
        if(downloadInfo.iconLink!=null&&!downloadInfo.iconLink.equals("")){
            currentItem.icon=ItemsManager.getItemsManager().getDownIcon(downloadInfo.iconLink,currentItem.packageName);
            //currentItem.icon=downloadFromIconLinkToTemproryStorage-Files/imgApps-fileName-pkgName.png(downloadInfo.iconLink);
        }
        // יצירת אובייקט חדש (POJO immutable)
        // יצירת אובייקט חדש
        return new StoreItem(
            currentItem.packageName, 
            finalTitle,
            currentItem.icon,
            finalCurrentVersion,             // גרסה נוכחית (של המותקן)
            latestVersion,                   // הגרסה האחרונה מהרשת
            downloadInfo.source,
            currentItem.itemSourceType, 
            currentItem.customLink,
            currentItem.isDrive,
            currentItem.excludedFromUpdates, 
            downloadInfo.downloadLink,       // קישור ההורדה החדש
            downloadInfo.signature,
            Integer.parseInt(downloadInfo.versionCode), // נדרש המרה
            updateAvailable
        );
    }
    
    public static StoreItem updateCurentVersion(StoreItem currentItem) {
        
        currentItem.currentVersion=ItemsManager.getItemsManager().getInstalledVersion(currentItem.packageName);
        String finalCurrentVersion = currentItem.currentVersion;
        //when also have last version from the last full refresh.. & with this function removing automaticaly the item from the items visible when oncomplete...
        
        boolean updateAvailable = VersionComparer.isNewer(currentItem.latestVersion, currentItem.currentVersion);
        // יצירת אובייקט חדש (POJO immutable)
        // יצירת אובייקט חדש
        return new StoreItem(
            currentItem.packageName, 
            currentItem.title,
            currentItem.icon,
            finalCurrentVersion,             // גרסה נוכחית (של המותקן)
            currentItem.latestVersion,                   // הגרסה האחרונה מהרשת
            currentItem.source,
            currentItem.itemSourceType, 
            currentItem.customLink,
            currentItem.isDrive,
            currentItem.excludedFromUpdates, 
            currentItem.downloadLink,       // קישור ההורדה החדש
            currentItem.signature,
            currentItem.versionCode,
            updateAvailable
        );
    }
    // פונקציה לייצוא ל-JSON (תמומש במחלקת הניהול)
}
