package com.mdm.store;


/**
 * מודל נתונים מאוחד עבור קישור הורדה.
 */
public class ApkDownloadInfo {
    public final String packageName;
    public final String source;
    public final String title;
    public final String version;
    public final String versionCode;
    public final String signature;
    public final String downloadLink;
    public final long fileSize;
    public final String iconLink;

    public ApkDownloadInfo(String packageName, String source, String title, String version, String versionCode, String signature, String downloadLink, long fileSize, String iconLink) {
        this.packageName = packageName != null ? packageName : "";
        this.source = source != null ? source : "";
        this.title = title != null ? title : "";
        this.version = version != null ? version : "";
        this.versionCode = versionCode != null ? versionCode : "";
        this.signature = signature != null ? signature : "";
        this.downloadLink = downloadLink != null ? downloadLink : "";
        this.fileSize = fileSize;
        this.iconLink = iconLink != null ? iconLink : "";
    }
}
