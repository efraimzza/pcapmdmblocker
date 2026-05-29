package com.mdm.store;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * מנהל את סדר הקדימות של מקורות ה-APK.
 * מחליף את ה-object ו-val mutableList של קוטלין.
 */
public final class ApkSourcePriority {

    // רשימת קדימויות ברירת מחדל (פנימית וקבועה)
    private static final List DEFAULT_PRIORITY = Collections.unmodifiableList(Arrays.asList(
                                                                                  "GPlay",
                                                                                  "APKPure",
                                                                                  "APKCombo",
                                                                                  "Aptoide",
                                                                                  "FDroid"
                                                                              ));

    // רשימת הקדימויות הניתנת לשינוי (משתמש ב-CopyOnWriteArrayList כתחליף בטוח לשינויים)
    private static List currentPriority = new CopyOnWriteArrayList(DEFAULT_PRIORITY);

    /**
     * מחזיר את רשימת הקדימות הנוכחית.
     */
    public static List getCurrentPriority() {
        return currentPriority;
    }

    /**
     * מאפס את רשימת הקדימות לברירת המחדל.
     */
    public static void resetToDefault() {
        currentPriority = new CopyOnWriteArrayList(DEFAULT_PRIORITY);
    }

    /**
     * מגדיר רשימת קדימויות חדשה (מחליף את הקיימת).
     */
    public static void setCustomPriority(List newPriority) {
        currentPriority = new CopyOnWriteArrayList(newPriority);
    }
}
