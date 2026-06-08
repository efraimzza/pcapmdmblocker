package com.mdm.store;

//package io.github.kdroidfilter.storekit.gplay.core.model;

/**
 * מודל נתונים עבור מידע ציבורי של Google Play.
 */
public class GPlayApplicationInfo {
    public final String packageName;
    public final String title;
    public final String versionCode;
    public final String version;
    public final String signature;
    public final String downloadLink;
    public final String url;
    public final String sizeText;
    public final String iconLink;
    
    public GPlayApplicationInfo(String packageName, String title, String versionCode, String version,String signature, String downloadLink, String url, String sizeText, String iconLink) {
        this.packageName = packageName != null ? packageName : "";
        this.title = title != null ? title : "";
        this.versionCode = versionCode != null ? versionCode : "";
        this.version = version != null ? version : "";
        this.signature = signature != null ? signature : "";
        this.downloadLink = downloadLink != null ? downloadLink : "";
        this.url = url != null ? url : "";
        this.sizeText = sizeText != null ? sizeText : "";
        this.iconLink = iconLink != null ? iconLink : "";
    }
}
