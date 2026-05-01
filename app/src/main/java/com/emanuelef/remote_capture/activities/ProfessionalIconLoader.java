package com.emanuelef.remote_capture.activities;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.util.Log;
import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.model.FileHeader;
import org.json.JSONObject;
import java.io.File;
import java.io.InputStream;
import java.util.List;
import java.util.Scanner;

public class ProfessionalIconLoader {

    public static Drawable getIcon(Context context, String path) {
        File file = new File(path);
        String name = file.getName().toLowerCase();

        if (name.endsWith(".apk")) {
            return loadApkIcon(context, path);
        }

        // טיפול ב-Bundles (XAPK, APKS, APKM, ZIP)
        return loadIconFromBundle(context, file);
    }

    private static Drawable loadIconFromBundle(Context context, File bundleFile) {
        ZipFile zipFile = new ZipFile(bundleFile);
        File tempFile = null;
        try {
            if(!new File(context.getFilesDir()+"/icontmp").exists()){
                new File(context.getFilesDir()+"/icontmp").mkdirs();
            }
            List<FileHeader> headers = zipFile.getFileHeaders();
            String baseApkName = null;

            // שלב א': בדיקת XAPK (חיפוש manifest.json)
            FileHeader jsonHeader = zipFile.getFileHeader("manifest.json");
            if (jsonHeader != null) {
                baseApkName = parseXapkManifest(zipFile, jsonHeader);
            }

            // שלב ב': אם לא XAPK, חפש base.apk (סטנדרט APKS/APKM)
            if (baseApkName == null) {
                for (FileHeader header : headers) {
                    if (header.getFileName().equalsIgnoreCase("base.apk")) {
                        baseApkName = header.getFileName();
                        break;
                    }
                }
            }

            // שלב ג': fallback מקצועי - זיהוי ה-APK הראשי לפי PackageInfo
            if (baseApkName == null) {
                baseApkName = findBaseByAnalysis(context, zipFile, headers);
            }

            if (baseApkName != null) {
                tempFile = new File(context.getFilesDir()+"/icontmp", "icon_extract_" + System.currentTimeMillis() + ".apk");
                zipFile.extractFile(baseApkName, context.getFilesDir()+"/icontmp", tempFile.getName());
                return loadApkIcon(context, tempFile.getAbsolutePath());
            }

        } catch (Exception e) {
            Log.e("IconLoader", "Failed to load bundle icon", e);
        } finally {
            if (tempFile != null && tempFile.exists()) tempFile.delete();
        }
        return null;
    }

    private static String parseXapkManifest(ZipFile zip, FileHeader header) {
        try {InputStream is = zip.getInputStream(header);
            Scanner s = new Scanner(is).useDelimiter("\\A");
            String result = s.hasNext() ? s.next() : "";
            JSONObject json = new JSONObject(result);
            // ב-XAPK לעיתים זה בשדה "package_name".apk או מפורש ב-split_apks
            if (json.has("package_name")) {
                return json.getString("package_name") + ".apk";
            }
        } catch (Exception e) { /* ignore */ }
        return null;
    }

    private static String findBaseByAnalysis(Context context, ZipFile zip, List<FileHeader> headers) {
        // אם הגענו לכאן, אנחנו צריכים לבדוק איזה APK הוא ה-Root.
        // ה-Base APK הוא היחיד שבו splitName במניפסט הוא null.
        for (FileHeader header : headers) {
            if (header.getFileName().toLowerCase().endsWith(".apk")) {
                File temp = null;
                try {
                    temp = new File(context.getFilesDir()+"/icontmp", "analyze_" + header.getFileName());
                    zip.extractFile(header, context.getFilesDir()+"/icontmp", temp.getName());

                    PackageManager pm = context.getPackageManager();
                    PackageInfo info = pm.getPackageArchiveInfo(temp.getAbsolutePath(), 0);

                    // בדיקה קריטית: ב-Split APKs, ה-Base הוא זה שאין לו splitName
                    // ובד"כ יש לו Launcher Activity מוגדר
                    if (info != null && info.applicationInfo != null) {
                        // ב-pure Java/Native SDK לפעמים ה-splitNames לא חוזר ב-ArchiveInfo
                        // אז נבדוק אם יש לו הגדרת icon/label תקינה (ל-Splits בד"כ אין)
                        if (info.applicationInfo.icon != 0) {
                            return header.getFileName();
                        }
                    }
                } catch (Exception e) {
                    // Skip
                } finally {
                    if (temp != null) temp.delete();
                }
            }
        }
        return null;
    }

    private static Drawable loadApkIcon(Context context, String path) {
        PackageManager pm = context.getPackageManager();
        PackageInfo info = pm.getPackageArchiveInfo(path, 0);
        if (info != null) {
            info.applicationInfo.sourceDir = path;
            info.applicationInfo.publicSourceDir = path;
            return info.applicationInfo.loadIcon(pm);
        }
        return null;
    }
}

