package com.mdm.store;

import java.util.List;
import com.emanuelef.remote_capture.activities.LogUtil;
import android.content.Context;

/**
 * השירות המרכזי שמחפש קישור הורדה לפי רשימת קדימויות.
 * מחליף את ה-Flow/suspend logic של קוטלין בלולאה סינכרונית.
 */
public class ApkLinkResolverService {
    GPlayService gpls=null;
    Exception lastException = null;
    //ApkDownloadInfo adi=null;
    /**
     * מחפש את קישור ההורדה הראשון העובד לפי סדר הקדימויות.
     * @throws Exception אם לא נמצא קישור הורדה זמין.
     */
    public ApkDownloadInfo gplayLinkResolver(Context context,String packageName)throws Exception {
        if(gpls==null)gpls=new GPlayService(context);
        //        gpls.isReady(new GPlayService.interf(){
        //               @Override
        //              public void onsuccess() {
        //now continue get details
        //run on this thread...
              try {
        GPlayApplicationInfo info = gpls.getGPlayApplicationInfo(packageName,true);
        if (!info.downloadLink.isEmpty()) {
            return new ApkDownloadInfo(
                info.packageName, "GPlay", info.title, info.version, info.versionCode, 
                info.signature, info.downloadLink, 0L // fileSize לא נגיש
            );
        }
                        } catch (Exception e) {
        // שמירת השגיאה האחרונה לדיווח אם אף מקור לא הצליח
                          lastException = e; 
                            LogUtil.logToFile("Error processing source " + "GPlay" + ": " + e.toString());
                            throw e;
                      }
        throw new Exception("Could not find a download link for " + packageName + ". Last error: " + lastException.getMessage());
     }
    public ApkDownloadInfo getApkDownloadLink(Context context,final String packageName) throws Exception {
        List priority = ApkSourcePriority.getCurrentPriority();
        
        for (int i = 0; i < priority.size(); i++) {
            final String source = (String) priority.get(i);

            try {
                if ("GPlay".equals(source)) {
                    //need return adi
                    //need to wait for onsuccess... before continue
                    if(gpls==null)gpls=new GPlayService(context);
            //        gpls.isReady(new GPlayService.interf(){
             //               @Override
              //              public void onsuccess() {
                                //now continue get details
                                //run on this thread...
              //                  try {
                                GPlayApplicationInfo info = gpls.getGPlayApplicationInfo(packageName,false);
                                //if (!info.downloadLink.isEmpty()) {
                                    return new ApkDownloadInfo(
                                        info.packageName, source, info.title, info.version, info.versionCode, 
                                        info.signature, info.downloadLink, 0L // fileSize לא נגיש
                                    );
                                //}
              //                  } catch (Exception e) {
                                    // שמירת השגיאה האחרונה לדיווח אם אף מקור לא הצליח
              //                      lastException = e; 
                 //                   LogUtil.logToFile("Error processing source " + source + ": " + e.toString());
               //                 }
                //            }
           //         });
                    
                }
                else if ("APKPure".equals(source)) {
                    ApkPureApplicationInfo info = ApkPureService.getApkPureApplicationInfo(packageName);
                    if (!info.downloadLink.isEmpty()) {
                        return new ApkDownloadInfo(
                            info.appId, source, info.title, info.version, info.versionCode, 
                            info.signature, info.downloadLink, 0L // fileSize לא נגיש
                        );
                    }
                } 
                else if ("APKCombo".equals(source)) {
                    ApkComboApplicationInfo info = ApkComboService.getApkComboApplicationInfo(packageName);
                    if (!info.downloadLink.isEmpty()) {
                        return new ApkDownloadInfo(
                            info.appId, source, info.title, info.version, info.versionCode, 
                            "", info.downloadLink, 0L // signature ו-fileSize לא נגישים
                        );
                    }
                } 
                else if ("Aptoide".equals(source)) {
                    AptoideService.AptoideApplicationInfo info = AptoideService.getAppMetaByPackageName(packageName, "en");
                    if (!info.file.path.isEmpty()) {
                        return new ApkDownloadInfo(
                            info.packageName, source, info.name, info.file.vername, 
                            String.valueOf(info.file.vercode), info.file.signature.sha1, info.file.path, info.size
                        );
                    }
                }
                else if ("FDroid".equals(source)) {
                    FdroidApplicationInfo info = FdroidService.getFdroidApplicationInfo(packageName);
                    if (!info.downloadLink.isEmpty()) {
                        return new ApkDownloadInfo(
                            info.packageName, source, info.name, info.version, 
                            String.valueOf(info.versionCode), info.sig, info.downloadLink, info.fileSize
                        );
                    }
                }
                
            } catch (Exception e) {
                // שמירת השגיאה האחרונה לדיווח אם אף מקור לא הצליח
                lastException = e; 
                LogUtil.logToFile("Error processing source " + source + ": " + e.toString());
            }
        }

        // אם הלולאה הסתיימה ללא הצלחה, זורק את השגיאה האחרונה או שגיאה כללית
        if (lastException != null) {
            throw new Exception("Could not find a download link for " + packageName + ". Last error: " + lastException.getMessage());
        } else {
            throw new Exception("Could not find a download link for " + packageName + " using any source.");
        }
    }
}
