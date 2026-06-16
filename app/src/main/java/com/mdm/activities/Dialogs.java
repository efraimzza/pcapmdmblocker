package com.mdm.activities;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;
import java.util.List;
import java.util.ArrayList;
import android.widget.RadioGroup;
import android.widget.RadioButton;
import android.content.Intent;
import android.view.View;
import android.net.Uri;
import android.os.Looper;
import android.os.Handler;
import android.widget.CheckBox;
import android.widget.TextView;
import com.mdm.store.ApkSourcePriority;
import android.os.Build;
import android.app.Activity;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.widget.Button;
import android.view.View.OnClickListener;
import android.preference.PreferenceManager;
import com.emanuelef.remote_capture.R;
import java.util.Arrays;
import android.content.SharedPreferences;
import com.emanuelef.remote_capture.activities.LogUtil;
import com.emanuelef.remote_capture.activities.picker;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.CompoundButton;
import org.json.JSONObject;
import java.util.Iterator;
import java.util.concurrent.Executor;
import com.emanuelef.remote_capture.model.Prefs;

public class Dialogs {

    // ... הגדרות ממשקים פנימיים (כמו DialogListener, ImportModeListener)

    // --- 1. דיאלוג הוספת פריט ---
    public static void showAddItemDialog(final Context context, final StoreItem.ItemSourceType type, 
                                         final ItemsManager itemsManager, final StoreItemAdapter adapter) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(type == StoreItem.ItemSourceType.MANUAL ? "הוסף שם חבילה" : "הוסף קישור מותאם אישית");

        final LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        
        final LinearLayout layoutLink= new LinearLayout(context);
        layoutLink.setOrientation(LinearLayout.HORIZONTAL);
        
        final LinearLayout layoutTitle = new LinearLayout(context);
        layoutTitle.setOrientation(LinearLayout.HORIZONTAL);
        
        final EditText inputPN = new EditText(context);
        inputPN.setHint("שם חבילה (com.example.app)");
        layout.addView(inputPN);
        
        final EditText inputLink = new EditText(context);
        final CheckBox cbdr=new CheckBox(context);
        cbdr.setOnCheckedChangeListener(new OnCheckedChangeListener(){
                @Override public void onCheckedChanged(CompoundButton p1, boolean isChecked) {
                    inputLink.setHint(isChecked?"הזיהוי של הדרייב (34 תווים...)":"קישור הורדה (http://...)");
                }
            });
        cbdr.setText("דרייב?");
        
        final EditText inputTitle = new EditText(context);
        final Button burefresh=new Button(context);
        burefresh.setBackgroundResource(R.drawable.ic_refresh);
        burefresh.setOnClickListener(new OnClickListener(){
                @Override
                public void onClick(View p1) {
                    inputTitle.setText(
                    getTitle(context, inputPN.getText().toString()));
                }
            });
        
        if (type == StoreItem.ItemSourceType.CUSTOM_LINK) {
            inputLink.setHint("קישור הורדה (http://...)");
            layoutLink.addView(cbdr);
            layoutLink.addView(inputLink);
            layout.addView(layoutLink);
            
        }
        inputTitle.setHint("כותרת (אופציונלי)");
        layoutTitle.addView(burefresh);
        layoutTitle.addView(inputTitle);
        //layout.addView(inputTitle);
        layout.addView(layoutTitle);
        builder.setView(layout);

        builder.setPositiveButton("הוסף", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    String pn = inputPN.getText().toString();
                    String link = type == StoreItem.ItemSourceType.CUSTOM_LINK ? inputLink.getText().toString() : "";
                    String title = inputTitle.getText().toString();
                    //if(type == StoreItem.ItemSourceType.CUSTOM_LINK){
                        if (!(title.length() > 0)||title.equals("N/A") ||!(pn.length() > 0)) {
                            Toast.makeText(context, "שם ושם חבילה חובה.", Toast.LENGTH_SHORT).show();
                        }else{
                            StoreItem newItem = new StoreItem(type,pn, title,link,cbdr.isChecked());
                            itemsManager.addItem(newItem);
                            adapter.updateData(itemsManager.getAllItemsvisible());
                        }
                    /*}else
                    if (pn.length() > 0) {
                        StoreItem newItem = new StoreItem( type, pn,title,link);
                        itemsManager.addItem(newItem);
                        adapter.updateData(itemsManager.getAllItemsvisible());
                    } else {
                        Toast.makeText(context, "שם חבילה חובה.", Toast.LENGTH_SHORT).show();
                    }*/
                }
            });
        builder.setNegativeButton("בטל", null);
        builder.show();
        layoutLink.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,LinearLayout.LayoutParams.WRAP_CONTENT));
        inputLink.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,LinearLayout.LayoutParams.WRAP_CONTENT));
        layoutTitle.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,LinearLayout.LayoutParams.WRAP_CONTENT));
        burefresh.setLayoutParams(new LinearLayout.LayoutParams(60,60));
        inputTitle.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,LinearLayout.LayoutParams.WRAP_CONTENT));
    }
    private static String getTitle(Context context,String packageName) {
        // [יישום קיים]
        PackageManager pm = context.getPackageManager();
        try {
            PackageInfo pInfo = pm.getPackageInfo(packageName, 0);
            return pInfo.applicationInfo.loadLabel(pm) != null ? pInfo.applicationInfo.loadLabel(pm).toString() : "N/A";
        } catch (PackageManager.NameNotFoundException e) {
            return "N/A";
        }
    }
    // --- 2. דיאלוג תפריט לחיצה ארוכה ---
    public static void showLongClickMenu(final Context context, final StoreItem item, 
                                         final ItemsManager itemsManager, final StoreItemAdapter adapter) {
        List<String> options = new ArrayList<String>();
        //LogUtil.logToFile(item.title+item.itemSourceType.name()+item.customLink);
        if (item.isLinkEditable()) {
            options.add("ערוך קישור");
        }
        if (item.isRemovable()) {
            options.add("הסר פריט");
        }

        final String[] items = options.toArray(new String[0]);

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(item.title);
        builder.setItems(items, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    String selection = items[which];
                    if ("ערוך קישור".equals(selection)) {
                        // הפעלת דיאלוג עריכת קישור (יישום בנפרד)
                        showEditLinkDialog(context, item, itemsManager, adapter);
                    } else if ("הסר פריט".equals(selection)) {
                        // דיאלוג אישור הסרה
                        showRemoveConfirmationDialog(context, item, itemsManager, adapter);
                    }
                }
            });
        if(options.size()>0)
        builder.show();
    }
/*
    // --- 3. דיאלוג ייצוא ---
    public static void exportData(Context context, ConfigManager configManager, ItemsManager itemsManager) {
        // ... יצירת ExportData והמרתו ל-JSON String באמצעות JsonUtil.exportDataToJson()
        // ... שמירת JSON String לקובץ (דורש הרשאות כתיבה והפעלת Intent)
        Toast.makeText(context, "מייצא נתונים...", Toast.LENGTH_SHORT).show();
    }

    // --- 4. דיאלוג הגדרת פריטי ברירת מחדל (אזהרה + בחירה) ---
    public static void showSetDefaultItemsDialog(final Context context, final ItemsManager itemsManager, final DefaultSourceSelectionListener listener) {
        // דיאלוג איפוס:
        AlertDialog.Builder warningBuilder = new AlertDialog.Builder(context);
        warningBuilder.setTitle("אזהרה: איפוס נתונים");
        warningBuilder.setMessage("פעולה זו תאפס את כל הפריטים הנוכחיים בחנות. האם להמשיך?");
        warningBuilder.setPositiveButton("המשך ואיפוס", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    // לאחר אישור, הצג דיאלוג בחירת מקור:
                    showDefaultSourceSelectionDialog(context, itemsManager, listener);
                }
            });
        warningBuilder.setNegativeButton("בטל", null);
        warningBuilder.show();
    }*/

    public interface DefaultSourceSelectionListener {
        void onSourceSelected(StoreConfiguration.DefaultSourceType sourceType, String link);
    }

    // --- פונקציות נוספות נדרשות (Edit Link, Download Confirmation, Import Selection) ---
    
        // --- ממשקי Callbacks נדרשים ---

        public interface ImportModeListener {
            void onModeSelected(ImportMode mode, String pathOrUrl);
        }

        

        /** משמש במחלקת ImportExportManager (אך מוגדר כאן לשימוש בדיאלוגים) */
     /*   public enum ImportMode {
            RESET_AND_ADD, ONLY_ADD
            }*/

        // --- 1. דיאלוג עריכת קישור (showEditLinkDialog) ---

        public static void showEditLinkDialog(final Context context, final StoreItem item, 
                                              final ItemsManager itemsManager, final StoreItemAdapter adapter) {
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setTitle("ערוך קישור לפריט: " + item.title);

            final EditText input = new EditText(context);
            input.setHint("הכנס קישור חדש");
            input.setText(item.customLink != null ? item.customLink : "");
            builder.setView(input);

            builder.setPositiveButton("שמור", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        String newLink = input.getText().toString();
                        if (newLink.length() > 0) {
                            itemsManager.updateItemLink(item, newLink);
                            adapter.updateData(itemsManager.getAllItemsvisible());
                            Toast.makeText(context, "הקישור עודכן בהצלחה.", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(context, "שדה הקישור ריק.", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            builder.setNegativeButton("בטל", null);
            builder.show();
        }

        // --- 2. דיאלוג אישור הסרה (showRemoveConfirmationDialog) ---

        public static void showRemoveConfirmationDialog(final Context context, final StoreItem item, 
                                                        final ItemsManager itemsManager, final StoreItemAdapter adapter) {
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setTitle("אשר הסרה");
            builder.setMessage("האם אתה בטוח שברצונך להסיר את הפריט '" + item.title + "'?");

            builder.setPositiveButton("הסר", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        itemsManager.removeItem(item);
                        adapter.updateData(itemsManager.getAllItemsvisible());
                        Toast.makeText(context, "הפריט הוסר.", Toast.LENGTH_SHORT).show();
                    }
                });
            builder.setNegativeButton("בטל", null);
            builder.show();
        }

        // --- 3. דיאלוג בחירת מקור ברירת מחדל (showDefaultSourceSelectionDialog) ---

        public static void showDefaultSourceSelectionDialog(final Context context, final ItemsManager itemsManager, final DefaultSourceSelectionListener listener) {
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setTitle("בחר מקור לפריטי ברירת מחדל");

            final LinearLayout layout = new LinearLayout(context);
            layout.setOrientation(LinearLayout.VERTICAL);

            final RadioGroup radioGroup = new RadioGroup(context);

            final RadioButton rbLocal = new RadioButton(context);
            rbLocal.setText("קובץ JSON מקומי מובנה");
            rbLocal.setId(1);
            radioGroup.addView(rbLocal);

            final RadioButton rbLink = new RadioButton(context);
            rbLink.setText("קישור JSON חיצוני");
            rbLink.setId(2);
            radioGroup.addView(rbLink);

            final EditText inputLink = new EditText(context);
            inputLink.setHint("הכנס URL לקישור JSON");
            inputLink.setVisibility(View.GONE);
            layout.addView(radioGroup);
            layout.addView(inputLink);

            radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
                    public void onCheckedChanged(RadioGroup group, int checkedId) {
                        if (checkedId == 2) { // Link selected
                            inputLink.setVisibility(View.VISIBLE);
                        } else {
                            inputLink.setVisibility(View.GONE);
                        }
                    }
                });

            builder.setView(layout);

            builder.setPositiveButton("טען", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        int selectedId = radioGroup.getCheckedRadioButtonId();
                        if (selectedId == 1) { // Local
                            listener.onSourceSelected(StoreConfiguration.DefaultSourceType.LOCAL, null);
                        } else if (selectedId == 2) { // Link
                            String link = inputLink.getText().toString();
                            if (link.length() > 0) {
                                listener.onSourceSelected(StoreConfiguration.DefaultSourceType.LINK, link);
                            } else {
                                Toast.makeText(context, "חובה להזין קישור.", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(context, "בחר מקור.", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            builder.setNegativeButton("בטל", null);
            builder.show();
        }

        // --- 4. דיאלוג בחירת ייבוא (showImportSelectionDialog) ---
/*
        public static void showImportSelectionDialog(final Context context, final ItemsManager itemsManager, 
                                                     final ConfigManager configManager, final StoreItemAdapter adapter) {

            final String[] options = {"ייבוא מקובץ JSON מקומי", "ייבוא מקישור JSON (URL)"};

            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setTitle("בחר סוג ייבוא");
            builder.setItems(options, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        if (which == 0) {
                            // ייבוא מקובץ מקומי: הפעלת File Picker Intent (נדרש יישום חיצוני)
                            Toast.makeText(context, "פותח בוחר קבצים... (המשך ביישום File Picker)", Toast.LENGTH_LONG).show();
                            // לצורך הדגמה, נניח שה-File Picker מחזיר נתיב קובץ:
                            // showImportModeDialog(context, itemsManager, configManager, adapter, "file:///path/to/local.json");
                        } else {
                            // ייבוא מקישור URL
                            showImportUrlInput(context, itemsManager, configManager, adapter);
                        }
                    }
                });
            builder.show();
        }

        // פונקציית עזר לייבוא מ-URL
        private static void showImportUrlInput(final Context context, final ItemsManager itemsManager, 
                                               final ConfigManager configManager, final StoreItemAdapter adapter) {
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setTitle("הוסף קישור JSON");

            final EditText input = new EditText(context);
            input.setHint("הכנס URL לקישור JSON (req warning)");
            builder.setView(input);

            builder.setPositiveButton("טען", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        final String url = input.getText().toString();
                        if (url.length() > 0) {
                            // הפעלת דיאלוג בחירת מצב ייבוא
                            showImportModeDialog(context, itemsManager, configManager, adapter, url);
                        } else {
                            Toast.makeText(context, "חובה להזין URL.", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            builder.setNegativeButton("בטל", null);
            builder.show();
        }
*/
        /** דיאלוג לבחירת מצב ייבוא: איפוס והוספה, או הוספה בלבד */
        private static void showImportModeDialog(final Context context, final ItemsManager itemsManager, 
                                                 final ConfigManager configManager, final StoreItemAdapter adapter, 
                                                 final String pathOrUrl) {

            final String[] options = {"איפוס כל הפריטים והוספה", "הוספה בלבד"};

            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setTitle("בחר מצב ייבוא (אזהרת איפוס)");
            builder.setItems(options, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        final ImportMode mode = (which == 0) ? ImportMode.RESET_AND_ADD : ImportMode.ONLY_ADD;

                        // --- לוגיקת הייבוא בפועל (מחייבת את ImportExportManager) ---
                        // ImportExportManager manager = new ImportExportManager(itemsManager, configManager);
                        // manager.importJson(pathOrUrl, mode); // זו קריאה ללוגיקת הייבוא

                        Toast.makeText(context, "מייבא נתונים... (נדרש יישום מלא של ImportExportManager)", Toast.LENGTH_LONG).show();

                        // עדכון ה-Adapter לאחר סיום הייבוא
                        adapter.updateData(itemsManager.getAllItemsvisible()); 
                    }
                });
            builder.show();
        }

    
    public static final Executor etMainExecutor = new Executor() {
        private final Handler handler = new Handler(Looper.getMainLooper());
        @Override
        public void execute(Runnable command) {
            handler.post(command);
        }
    };
    public static void showDownloadConfirmation(final storeActivity context, final StoreItem item, final ItemsManager itemsManager) {
            if ((item.downloadLink != null && !item.downloadLink.equals("")&&(item.downloadLink.startsWith("https:/")))||(item.customLink!=null&&!item.customLink.equals(""))||(item.source.equals("GPlay"))) {
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setTitle("התקנה / עדכון");
            builder.setMessage("האם ברצונך לפתוח את הקישור ולהתחיל בהורדה של '" + item.title + "'?");
            builder.setCancelable(false);
            builder.setPositiveButton("הורדה והתקנה", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        
                        try {
                            
                            String mlink="";
                                if(item.customLink != null && !item.customLink.equals("")){
                                    mlink = item.customLink;
                                }else{
                                    mlink = item.downloadLink;
                                }
                                if(!mlink.equals("")){
                                    //if(!Prefs.istest(PreferenceManager.getDefaultSharedPreferences(context)))
                                    //utils.startDownloadnew(context,mlink,item.isDrive);
                                    //else
                                    utils.startDownloadnew(context,mlink,item.packageName,item.isDrive?"drive":"normal");
                                }
                                else if((item.source.equals("GPlay"))){
                                    new Thread(){public void run(){
                                        try{
                                    //regenerate
                                    final String jstr=itemsManager.resolverService.gplayLinkResolver(context,item.packageName).downloadLink;
                                                etMainExecutor.execute(new Runnable(){
                                                        @Deprecated
                                                        @Override
                                                        public void run() {
                                                            if(!jstr.equals("")){
                                                                //if(!Prefs.istest(PreferenceManager.getDefaultSharedPreferences(context)))
                                                                //utils.startDownloadnewGplay(context,item.packageName, jstr);
                                                                //else
                                                                utils.startDownloadnew(context,jstr,item.packageName,"gplay");
                                                                /*JSONObject json = new JSONObject(jstr);
                                                                 Iterator<String> its=json.keys();
                                                                 while(its.hasNext()){
                                                                 json.getString(its.next());
                                                                 }*/
                                                            }
                                                        }
                                                    });
                                    
                                     } catch (Exception e) {LogUtil.logToFile(e);}
                                    }}.start();
                                }
                        } catch (Exception e) {
                            LogUtil.logToFile(e.getMessage()+"d:"+item.downloadLink+"c:"+item.customLink);
                            Toast.makeText(context, "שגיאה בפתיחת הקישור: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    }
                });
                builder.setNeutralButton("פתח קישור", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            // הדרישה: download option - open
                            try {
                                Intent browserIntent;
                                if(item.customLink != null && !item.customLink.equals("")){
                                    browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(item.customLink));
                                }else{
                                    browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(item.downloadLink));
                                }
                                context.startActivity(browserIntent);
                            } catch (Exception e) {
                                LogUtil.logToFile(e.getMessage()+"d:"+item.downloadLink+"c:"+item.customLink);
                                Toast.makeText(context, "שגיאה בפתיחת הקישור: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            }
                        }
                    });
                builder.setNegativeButton("ביטול", null);
            builder.show();
            }else{
                Toast.makeText(context, "אין קישור הורדה זמין לפריט זה. או קישור לא תקין", Toast.LENGTH_SHORT).show();
                return;
            }
        }

    // --- 6. דיאלוג הוספת אפליקציה לפי Package Name ---
/*
    public static void showAddAppDialog(final Context context, final ItemsManager itemsManager, final StoreItemAdapter adapter) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("הוסף אפליקציה חדשה");
        builder.setMessage("הכנס את שם חבילת האפליקציה (Package Name) לבדיקת עדכונים:");

        final EditText input = new EditText(context);
        input.setHint("לדוגמה: com.waze, com.mdm.zing");
        builder.setView(input);

        builder.setPositiveButton("הוסף", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    String packageName = input.getText().toString().trim();
                    if (packageName.isEmpty()) {
                        Toast.makeText(context, "שם חבילה ריק.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // יצירת פריט בסיסי והוספה למנג'ר
                    // הערך של currentVersion יתעדכן בפועל רק לאחר ריענון (refreshAllItems)
                    StoreItem newItem = new StoreItem(
                        packageName, 
                        packageName, // כותרת ראשונית זהה לשם החבילה
                        "N/A", 
                        "N/A", 
                        "Manual", 
                        StoreItem.ItemSourceType.CUSTOM_LINK, 
                        null, false, null, null, 0, false
                    );

                    itemsManager.addItem(newItem);
                    adapter.updateData(itemsManager.getAllItems());
                    Toast.makeText(context, packageName + " נוספה לרשימה. אנא רענן לבדיקת גרסה.", Toast.LENGTH_LONG).show();
                }
            });
        builder.setNegativeButton("בטל", null);
        builder.show();
    }*/
        // --- פונקציות נוספות שנותרו מהקו המנחה הקודם ---

        // (פונקציות showAddItemDialog ו-exportData נשארו כפי שהוגדרו בקו המנחה הקודם, ויש לשלב אותן במחלקת Dialogs המלאה)
    // Dialogs.java (Core Updates)

// ... [existing imports] ...

        // ... [existing interfaces, enums, showEditLinkDialog, showRemoveConfirmationDialog, showDefaultSourceSelectionDialog, showDownloadConfirmation] ...

        // --- 4. דיאלוג בחירת ייבוא (Updated to call File Picker) ---

        public static void showImportSelectionDialog(final Activity context, final ItemsManager itemsManager, 
                                                     final StoreItemAdapter adapter, final ImportExportManager importExportManager) {

            final String[] options = {"ייבוא מקובץ JSON מקומי (File Picker)", "ייבוא מקישור JSON (URL)"};
            final int PICK_FILE_REQUEST_CODE = 42;

            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setTitle("בחר סוג ייבוא");
            builder.setItems(options, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        if (which == 0) {
                            /*
                            // הפעלת File Picker Intent
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                                intent.addCategory(Intent.CATEGORY_OPENABLE);
                                intent.setType("application/json"); 
                                // הקוד ימשיך ב-onActivityResult של ה-MainActivity
                                context.startActivityForResult(intent, PICK_FILE_REQUEST_CODE); 
                            } else {
                                Toast.makeText(context, "נדרש API 19 ומעלה לבוחר קבצים מתקדם.", Toast.LENGTH_LONG).show();
                            }*/
                            context.startActivity(new Intent(context,picker.class).putExtra("from","storepickjson"));
                        } else {
                            // ייבוא מקישור URL
                            Toast.makeText(context, "ייבוא מ-URL דורש לוגיקת הורדה נוספת שלא מומשה עדיין.", Toast.LENGTH_SHORT).show();
                            // showImportUrlInput(context, itemsManager, adapter, importExportManager); // יישום חלקי
                        }
                    }
                });
            builder.show();
        }

        // --- 5. דיאלוג הוספת אפליקציה חדשה (עם בדיקות) ---
     /*   public static void showAddAppDialog(final Context context, final ItemsManager itemsManager, final StoreItemAdapter adapter) {
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setTitle("הוסף אפליקציה חדשה");
            builder.setMessage("הכנס את שם חבילת האפליקציה (Package Name) לבדיקת עדכונים:");

            final EditText input = new EditText(context);
            input.setHint("לדוגמה: com.waze, com.mdm.zing");
            builder.setView(input);

            builder.setPositiveButton("הוסף", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        String packageName = input.getText().toString().trim();
                        if (packageName.isEmpty()) {
                            Toast.makeText(context, "שם חבילה ריק.", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        StoreItem newItem = new StoreItem(
                            packageName, 
                            packageName,
                            "N/A", "N/A", "Manual", 
                            StoreItem.ItemSourceType.CUSTOM_LINK, 
                            "", false, "", "", 0, false
                        );

                        if (itemsManager.addItem(newItem)) { // קורא למתודה המעודכנת שכוללת בדיקות
                            adapter.updateData(itemsManager.getAllItemsvisible());
                            Toast.makeText(context, packageName + " נוספה לרשימה. אנא רענן לבדיקת גרסה.", Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(context, "הפריט " + packageName + " כבר קיים ברשימה ולא נוסף.", Toast.LENGTH_LONG).show();
                        }
                    }
                });
            builder.setNegativeButton("בטל", null);
            builder.show();
        }*/

        // --- 6. דיאלוג ייצוא (Export) ---
        /*public static void showExportDialog(final Context context, final ImportExportManager importExportManager) {
            new Thread(new Runnable() {
                    public void run() {
                        final String path = importExportManager.exportItemsToJson();

                        new Handler(Looper.getMainLooper()).post(new Runnable() {
                                public void run() {
                                    if (path != null) {
                                        AlertDialog.Builder builder = new AlertDialog.Builder(context);
                                        builder.setTitle("ייצוא הושלם");
                                        builder.setMessage("הפריטים יוצאו בהצלחה לקובץ:\n" + path + "\n(בתיקיית Downloads)");
                                        builder.setPositiveButton("אישור", null);
                                        builder.show();
                                    } else {
                                        // השגיאה מטופלת כבר ב-ImportExportManager
                                    }
                                }
                            });
                    }
                }).start();
        }*/
    public static void showExportDialog(final Context context, final ImportExportManager importExportManager,final String pathdir) {
        new Thread(new Runnable() {
                public void run() {
                    final String path = importExportManager.exportItemsToJsonFile(pathdir);
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                            public void run() {
                                if (path != null) {
                                    AlertDialog.Builder builder = new AlertDialog.Builder(context);
                                    builder.setTitle("ייצוא הושלם");
                                    builder.setMessage("הפריטים יוצאו בהצלחה לקובץ:\n" + path);
                                    builder.setPositiveButton("אישור", null);
                                    builder.show();
                                } else {
                                    // השגיאה מטופלת כבר ב-ImportExportManager
                                }
                            }
                        });
                }
            }).start();
    }
        // --- 7. דיאלוג בחירת מצב ייבוא (שלב שני, משמש את onActivityResult) ---
        public static void showImportModeDialog(final Context context, final ItemsManager itemsManager, 
                                                final StoreItemAdapter adapter, final String uriToImport, 
                                                final ImportExportManager importExportManager) {

            final String[] options = {"איפוס כל הפריטים והוספה", "הוספה בלבד (שמירת קיימים)"};

            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setTitle("בחר מצב ייבוא");
            //builder.setMessage("שים לב: 'איפוס והוספה' ימחק את כל הפריטים הקיימים ויטען רק את הקובץ.");
            builder.setItems(options, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        final ImportMode mode = (which == 0) ? ImportMode.RESET_AND_ADD : ImportMode.ONLY_ADD;

                        new Thread(new Runnable() {
                                public void run() {
                                    final boolean success = importExportManager.importItemsFromJsonFile(uriToImport, mode);
                                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                                            public void run() {
                                                if (success) {
                                                    adapter.updateData(itemsManager.getAllItemsvisible());
                                                    Toast.makeText(context, "ייבוא נתונים הושלם בהצלחה!", Toast.LENGTH_LONG).show();
                                                } else {
                                                    Toast.makeText(context, "שגיאה בייבוא נתונים. בדוק את פורמט הקובץ ואת ההרשאות.", Toast.LENGTH_LONG).show();
                                                }
                                            }
                                        });
                                }
                            }).start();
                    }
                });
            builder.create().show();
        }
/*
        // --- 8. דיאלוג הגדרות (Settings) - עם אפשרויות מלאות ---
        public static void showSettingsDialog(final Context context, final ConfigManager configManager) {
            final StoreConfiguration currentConfig = configManager.getConfig();
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setTitle("הגדרות המערכת");

            final LinearLayout mainLayout = new LinearLayout(context);
            mainLayout.setOrientation(LinearLayout.VERTICAL);
            mainLayout.setPadding(30, 30, 30, 30);

            // 1. בדיקת עדכונים לאפליקציות מותקנות
            final CheckBox checkInstalledApps = new CheckBox(context);
            checkInstalledApps.setText("אפשר בדיקת עדכונים לאפליקציות מותקנות במכשיר");
            checkInstalledApps.setChecked(currentConfig.checkInstalledAppsForUpdates);
            mainLayout.addView(checkInstalledApps);

            // 2. סדר קדימות לבדיקת עדכונים
            final TextView priorityTitle = new TextView(context);
            priorityTitle.setText("\nסדר קדימות לבדיקת עדכונים (מופרד בפסיקים):");
            mainLayout.addView(priorityTitle);

            final EditText priorityInput = new EditText(context);
            priorityInput.setHint("FDroid, APKPure, Aptoide...");
            priorityInput.setText(listToString(ApkSourcePriority.getCurrentPriority()));
            mainLayout.addView(priorityInput);

            // 3. רשימת אפליקציות מותקנות לבדיקה
            final TextView installedCheckListTitle = new TextView(context);
            installedCheckListTitle.setText("\nאפליקציות מותקנות לבדיקה (Package Names, פסיק):");
            mainLayout.addView(installedCheckListTitle);

            final EditText installedCheckListInput = new EditText(context);
            installedCheckListInput.setHint("com.waze, com.mdm.zing...");
            installedCheckListInput.setText(listToString(currentConfig.installedAppsCheckList));
            mainLayout.addView(installedCheckListInput);

            builder.setView(mainLayout);

            builder.setPositiveButton("שמור הגדרות", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {

                        // עדכון קדימות המקורות במחלקה הסטטית
                        List<String> newPriorityList = stringToList(priorityInput.getText().toString());
                        ApkSourcePriority.setCustomPriority(newPriorityList);

                        // שמירת רשימת אפליקציות מותקנות לבדיקה
                        List<String> newInstalledCheckList = stringToList(installedCheckListInput.getText().toString());

                        // יצירת אובייקט קונפיגורציה חדש ושמירה ב-ConfigManager
                        StoreConfiguration newConfig = new StoreConfiguration(
                            checkInstalledApps.isChecked(),
                            newInstalledCheckList,
                            currentConfig.defaultSource, 
                            currentConfig.defaultSourceLink
                        );

                        configManager.saveConfig(newConfig);
                        Toast.makeText(context, "ההגדרות נשמרו בהצלחה.", Toast.LENGTH_SHORT).show();
                    }
                });
            builder.setNegativeButton("בטל", null);
            builder.show();
        }

        // --- עזרי המרה (listToString, stringToList) ---

        private static List<String> stringToList(String s) {
            // ... [implementation from previous response] ...
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
        */
    // Dialogs.java (יש להוסיף את המתודה הבאה)

// ... [existing enums and imports] ...

    // --- 9. דיאלוג אישור איפוס פריטים ---
    /*
    public static void showClearItemsConfirmationDialog(final Context context, final ItemsManager itemsManager) {
        new AlertDialog.Builder(context)
            .setTitle("איפוס רשימת הפריטים")
            .setMessage("האם אתה בטוח שברצונך למחוק לצמיתות את כל הפריטים הרשומים במאגר? פעולה זו אינה ניתנת לביטול.")
            .setPositiveButton("מחק הכל", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    itemsManager.clearAllItems();
                    Toast.makeText(context, "רשימת הפריטים נוקתה.", Toast.LENGTH_SHORT).show();

                    // רצוי לעדכן את ה-UI של ה-MainActivity אם הוא פעיל
                    // (זה ידרוש מנגנון BroadcastReceiver או EventBus, אבל כרגע נסתפק ב-Toast)
                }
            })
            .setNegativeButton("בטל", null)
            .show();
    }*/
   
  
        // --- ממשקים נדרשים ל-Callback ---

        public interface MultiSelectListener {
            /**
             * נקרא לאחר שהמשתמש אישר את הבחירה שלו בדיאלוג.
             * @param selectedPackages רשימת ה-Package Names שנבחרו.
             * @param isSystemApps האם רשימה זו היא של אפליקציות מערכת (True) או משתמש (False).
             */
            void onSelectionSaved(List<String> selectedPackages, boolean isSystemApps);
        }

        public enum ImportMode {
            RESET_AND_ADD, ONLY_ADD
            }

        // --- 1. דיאלוג בחירה מרובה של אפליקציות מותקנות (הליבה החדשה) ---

        /**
         * מציג דיאלוג לבחירה מרובה של אפליקציות מותקנות (System/User) במכשיר.
         */
        public static void showAppMultiSelectDialog(final Context context, final ItemsManager itemsManager, 
                                                    final boolean isSystemApps, final MultiSelectListener listener) {
            new Thread(){public void run(){
                
            // טען את רשימת האפליקציות המותקנות לפי סוג (System/User)
            final List<PackageInfo> installedApps = itemsManager.getInstalledApps(isSystemApps);

            if (installedApps.isEmpty()) {
                Toast.makeText(context, "לא נמצאו אפליקציות מסוג זה במכשיר.", Toast.LENGTH_SHORT).show();
                return;
            }
            boolean faund=true;
            while(faund){
                faund=false;
                for(PackageInfo pi:installedApps){
                    for(StoreItem si:itemsManager.getAllItems()){
                        if(!si.itemSourceType.equals(StoreItem.ItemSourceType.INSTALLED_APP)&&si.packageName.equals(pi.packageName)){
                            installedApps.remove(pi);
                            faund=true;
                            break;
                        }
                    }
                    if(faund)
                        break;
                }
            }
            
            final CharSequence[] items = new CharSequence[installedApps.size()];
            final boolean[] checkedItems = new boolean[installedApps.size()];
            //final List<String> currentCheckList = itemsManager.getConfigManager().getConfig().installedAppsCheckList;
           // final List<String> currentCheckList = itemsManager.installedAppsCheckList(context);
            
                    final List<String> currentCheckList =new ArrayList<>();
                    
            for(StoreItem si: itemsManager.getAllItems()){
                if(si.itemSourceType.equals(StoreItem.ItemSourceType.INSTALLED_APP)){
                    currentCheckList.add(si.packageName);
                }
            }
            for (int i = 0; i < installedApps.size(); i++) {
                
                PackageInfo info = installedApps.get(i);
                String appName = info.applicationInfo.loadLabel(context.getPackageManager()).toString();
                
                // הצגת שם האפליקציה ושם החבילה
                items[i] = appName + " (" + info.packageName + ")";

                // סימון פריטים שנמצאים כבר ברשימת הבדיקה המאוחדת
                checkedItems[i] = currentCheckList.contains(info.packageName);
            }
            //old bug if isnt scrooling
            //final List<String> selectedPackages = new ArrayList<>(); // רשימה זמנית לשמירת הבחירה הנוכחית
                    final List<String> selectedPackages = currentCheckList;
                    new Handler(context.getMainLooper()).post(new Runnable(){@Override
                            public void run() {
           
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setTitle(isSystemApps ? "בחר אפליקציות מערכת לעדכון" : "בחר אפליקציות משתמש לעדכון");

            builder.setMultiChoiceItems(items, checkedItems, new DialogInterface.OnMultiChoiceClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int index, boolean isChecked) {
                        String packageName = installedApps.get(index).packageName;
                        if (isChecked) {
                            selectedPackages.add(packageName);
                        } else {
                            selectedPackages.remove(packageName);
                        }
                    }
                });

            builder.setPositiveButton("שמור בחירה", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //old bug if isnt scrooling
                        /*
                        // הדיאלוג דורש לולאה נוספת כדי לבדוק מי סומן סופית אם המשתמש לא שינה (Bug fix)
                        List<String> finalSelection = new ArrayList<>();
                        for (int i = 0; i < installedApps.size(); i++) {
                            if (((AlertDialog) dialog).getListView().isItemChecked(i)) {
                                finalSelection.add(installedApps.get(i).packageName);
                            }
                        }

                        // קריאה חוזרת ל-SettingsFragment
                        listener.onSelectionSaved(finalSelection, isSystemApps);
                        */
                        listener.onSelectionSaved(selectedPackages, isSystemApps);
                    }
                });

            builder.setNegativeButton("בטל", null);
            builder.show();
                            }
                        });
                }}.start();
        }

        
        public static void showClearItemsConfirmationDialog(final Context context, final ItemsManager itemsManager) {
            new AlertDialog.Builder(context)
                .setTitle("איפוס רשימת הפריטים")
                .setMessage("האם אתה בטוח שברצונך למחוק לצמיתות את כל הפריטים הרשומים במאגר המקומי? פעולה זו אינה ניתנת לביטול.")
                .setPositiveButton("מחק הכל", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        itemsManager.clearAllItems(true);
                        //PreferenceManager.getDefaultSharedPreferences(context).edit().putString(storeSettingsFragment.KEY_CHECK_LIST, "").commit();
                        Toast.makeText(context, "רשימת הפריטים נוקתה.", Toast.LENGTH_SHORT).show();
                        // הערה: נדרש מנגנון נוסף לעדכון ה-UI של ה-MainActivity לאחר איפוס.
                    }
                })
                .setNegativeButton("בטל", null)
                .show();
        }
    final static String[] originalStores = {"GPlay","APKPure","APKCombo","Aptoide","FDroid"};
    final static String originalStoresPref = "GPlay,APKPure,APKCombo,Aptoide,FDroid";
    static List availableOptions;
    static List finalPriorityList = new ArrayList();
        public static void selectPriority(Activity activity){
            availableOptions = new ArrayList(Arrays.asList(originalStores));
            finalPriorityList.clear();
            showPriorityDialog(activity);
        }
    private static void showPriorityDialog(final Activity activity) {
        final String[] currentArray = (String[]) availableOptions.toArray(new String[0]);

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle("בחר עדיפות מס' " + (finalPriorityList.size() + 1));

        // הגדרת הרשימה לבחירה //
        builder.setItems(currentArray, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    String selection = currentArray[which];
                    finalPriorityList.add(selection);
                    availableOptions.remove(selection);

                    // אם נשארו עוד אפשרויות, נמשיך לבחירה הבאה //
                    if (availableOptions.size() > 0) {
                        showPriorityDialog(activity);
                    } else {
                        showSummary(activity,true);
                    }
                }
            });

        // הוספת אפשרות לסיום מוקדם - רק אם נבחר לפחות פריט אחד //
        if (finalPriorityList.size() >= 1) {
            builder.setPositiveButton("סיום וסיכום", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        showSummary(activity,true); // המשתמש בחר לעצור ולשמור את מה שיש //
                    }
                });
        }

        // כפתור ביטול כללי שסוגר הכל //
        builder.setNegativeButton("ביטול", null);

        builder.setCancelable(false);
        builder.show();
    }

    private static void showSummary(Activity activity,boolean save) {
        if (finalPriorityList.isEmpty()) return;
        if(save){
        ApkSourcePriority.setCustomPriority(finalPriorityList);
        savePriorityToPrefs(activity);
        }
        StringBuilder sb = new StringBuilder("הדירוג שנקבע:\n\n");
        for (int i = 0; i < finalPriorityList.size(); i++) {
            sb.append((i + 1)).append(". ").append(finalPriorityList.get(i)).append("\n");
        }

        AlertDialog.Builder resBuilder = new AlertDialog.Builder(activity);
        resBuilder.setTitle("סיכום בחירה");
        resBuilder.setMessage(sb.toString());
        resBuilder.setPositiveButton("אישור", null);
        resBuilder.show();
    }
        // ... [other dialog methods: showImportSelectionDialog, showImportModeDialog, etc.] ...
    private static final String PREF_PRIORITY_KEY = "custom_priority_list";
    private static void savePriorityToPrefs(Activity activity) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(activity);
        SharedPreferences.Editor editor = prefs.edit();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < finalPriorityList.size(); i++) {
            sb.append((String)finalPriorityList.get(i));
            if (i < finalPriorityList.size() - 1) sb.append(",");
        }

        editor.putString(PREF_PRIORITY_KEY, sb.toString());
        editor.commit();
        Toast.makeText(activity, "העדיפויות נשמרו!", Toast.LENGTH_SHORT).show();
    }

    // טעינת המחרוזת והפיכתה חזרה לרשימה //
    public static void loadPriorityFromPrefs(Activity activity,boolean create) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(activity);
        String savedData = prefs.getString(PREF_PRIORITY_KEY, originalStoresPref);

        if (!savedData.isEmpty()) {
            String[] splitData = savedData.split(",");
            finalPriorityList = new ArrayList(Arrays.asList(splitData));

            // עדכון הקלאס החיצוני כבר ב-OnCreate //
            if(create)
                 ApkSourcePriority.setCustomPriority(finalPriorityList);
             else
                 showSummary(activity,false);
        }
    }
    public static void showClearAuthWarning(final Activity activity) {
        
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle("מחיקת חשבון");
        builder.setMessage("האם למחוק את החשבון האנונימי השמור?");
        builder.setPositiveButton("מחק", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    SharedPreferences.Editor prefs = PreferenceManager.getDefaultSharedPreferences(activity).edit();
                    prefs.putString("StoreAuthJson", "").commit();
                    Toast.makeText(activity, "נמחק!", Toast.LENGTH_SHORT).show();
                }
            });
        builder.setNegativeButton("ביטול", null);

        builder.setCancelable(false);
        builder.show();
    }
// ... [rest of the file] ...
    
}
