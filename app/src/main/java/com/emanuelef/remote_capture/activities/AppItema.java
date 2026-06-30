package com.emanuelef.remote_capture.activities;

import android.graphics.drawable.Drawable;

public class AppItema {
    private String name;
    private String packageName;
    private Drawable icon;
    private boolean isHidden;
    private boolean isSuspend;
    private boolean isRemove;
    private boolean isSystemApp;
    private boolean hasLauncherIcon;
    private long lastUpdateTime;

    public AppItema(String name, String packageName, Drawable icon, boolean isHidden, boolean isSuspend, boolean isRemove, boolean isSystemApp, boolean hasLauncherIcon, long lastUpdateTime) {
        this.name = name;
        this.packageName = packageName;
        this.icon = icon;
        this.isHidden = isHidden;
        this.isSuspend = isSuspend;
        this.isRemove = isRemove;
        this.isSystemApp = isSystemApp;
        this.hasLauncherIcon = hasLauncherIcon;
        this.lastUpdateTime = lastUpdateTime;
    }
    
    public String getName() {
        return name;
    }

    public String getPackageName() {
        return packageName;
    }

    public Drawable getIcon() {
        return icon;
    }

    public boolean isHidden() {
        return isHidden;
    }
    
    public boolean isSuspend() {
        return isSuspend;
    }
    
    public boolean isRemove() {
        return isRemove;
    }
    
    public void setHidden(boolean hidden) {
        isHidden = hidden;
    }
    public void setSuspend(boolean suspend) {
        isSuspend = suspend;
    }
    public void setRemove(boolean remove) {
        isRemove = remove;
    }
    public boolean isSystemApp() {
        return isSystemApp;
    }

    public boolean hasLauncherIcon() {
        return hasLauncherIcon;
    }

    public long getLastUpdateTime() {
        return lastUpdateTime;
    }
}
