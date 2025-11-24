package com.emanuelef.remote_capture.activities;

import android.content.Context;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.util.Log;

public class AdbNsdResolver {

    private static final String TAG = "AdbNsdResolver";
    // משתמשים ב-Pairing שכן זה נדרש לחיבורים אלחוטיים
    public static final String SERVICE_TYPE_ADB_CONNECTING = "_adb-tls-connect._tcp.";
    
    //private static final String SERVICE_TYPE_ADB_PAIRING = "_adb_pairing._tcp.";
    public static final String SERVICE_TYPE_ADB_PAIRING = "_adb-tls-pairing._tcp.";
   // private static final String SERVICE_TYPE_ADB_PAIRING = "_adb-connect._tcp";
//_adb-tls-pairing._tcp
    // 💡 ממשק Callback ללא תלות באנדרואידX/Lambda
    public interface AdbServiceFoundListener {
        void onAdbServiceResolved(String ipAddress, int port,String type);
        void onDiscoveryFailed(String message);
    }

    private NsdManager mNsdManager;
    private NsdManager.DiscoveryListener mpairDiscoveryListener;
    private NsdManager.DiscoveryListener mconDiscoveryListener;
    private AdbServiceFoundListener mListener;
    public boolean pairactive=false;

    public AdbNsdResolver(Context context, AdbServiceFoundListener listener) {
        mNsdManager = (NsdManager) context.getSystemService(Context.NSD_SERVICE);
        mListener = listener;
    }

    public void startDiscovery() {
        stopDiscovery();
        pairactive=true;
        mpairDiscoveryListener = new initializeDiscoveryListener(SERVICE_TYPE_ADB_PAIRING);
       // mconDiscoveryListener = new initializeDiscoveryListener(SERVICE_TYPE_ADB_CONNECTING);
        mNsdManager.discoverServices(
            SERVICE_TYPE_ADB_PAIRING, 
            NsdManager.PROTOCOL_DNS_SD, 
            mpairDiscoveryListener);
    }

    public void stopDiscovery() {
        pairactive=false;
        if (mpairDiscoveryListener != null) {
            try {
                mNsdManager.stopServiceDiscovery(mpairDiscoveryListener);
            } catch (IllegalArgumentException ignored) {
                // מתעלמים משגיאות עצירה כאשר הדיסקברי אינו פעיל
            }
            mpairDiscoveryListener = null;
        }
    }
    public void stopconDiscovery() {
        if (mconDiscoveryListener != null) {
            try {
                mNsdManager.stopServiceDiscovery(mconDiscoveryListener);
            } catch (IllegalArgumentException ignored) {
                // מתעלמים משגיאות עצירה כאשר הדיסקברי אינו פעיל
            }
            mconDiscoveryListener = null;
        }
    }
// 💡 הוספת מתודה חדשה להפעלת גילוי שירות ה-CONNECT
    public void startConnectDiscovery() {
        // עוצר את ה-Pairing Discovery כדי למנוע כפילויות מיותרות
         stopconDiscovery(); // אפשרות: לעצור את ה-Pairing, אך נשאיר את זה ב-PairingReceiver כדי שיוכל לעצור
        mconDiscoveryListener = new initializeDiscoveryListener(SERVICE_TYPE_ADB_CONNECTING);
         mNsdManager.discoverServices(
             SERVICE_TYPE_ADB_CONNECTING,
             NsdManager.PROTOCOL_DNS_SD,
             mconDiscoveryListener);
    }


    private class initializeDiscoveryListener implements NsdManager.DiscoveryListener {
        private final String type;
        initializeDiscoveryListener(String type){
            this.type=type;
        }
            @Override
            public void onStartDiscoveryFailed(String serviceType, int errorCode) {
                LogUtil.logToFile( "Discovery failed: Error code:" + errorCode);
                mListener.onDiscoveryFailed("התחלת גילוי נכשלה.");
            }

            @Override
            public void onDiscoveryStarted(String serviceType) {
                LogUtil.logToFile( "Service discovery started."+serviceType);
            }

            @Override
            public void onServiceFound(NsdServiceInfo service) {
                LogUtil.logToFile("fau"+service.getServiceType()+service.getHost()+service.getPort());
                if (service.getServiceType().equals(type)) {
                    LogUtil.logToFile( "ADB Pairing Service found. Resolving...");
                    // מתחילים את תהליך ה-Resolve
                    mNsdManager.resolveService(service, new NsdManager.ResolveListener() {

                            // Resolve Listener - Anonymous Inner Class
                            @Override
                            public void onResolveFailed(NsdServiceInfo serviceInfo, int errorCode) {
                                LogUtil.logToFile( "Resolve failed: " + errorCode);
                                mListener.onDiscoveryFailed("פתרון IP/Port נכשל.");
                            }

                            @Override
                            public void onServiceResolved(NsdServiceInfo serviceInfo) {
                                // עצירת הדיסקברי מיד לאחר הפתרון המוצלח
                                LogUtil.logToFile("resolved for type: " + type);
                                 // 🛑 נשלח את הנתונים חזרה ל-Activity עם ה-TYPE
                                 String ipAddress = serviceInfo.getHost().getHostAddress();
                                 int port = serviceInfo.getPort();
                                 mListener.onAdbServiceResolved(ipAddress, port, type); // 💡 שולח את ה-Type
                                 // אם זה Pairing, ניתן לעצור את ה-Pairing Discovery כאן, אך עדיף לעצור רק אחרי שה-Connect יתחיל.
                            }
                        });
                }
            }

            // שאר המתודות הנדרשות
            @Override public void onStopDiscoveryFailed(String serviceType, int errorCode) {
                LogUtil.logToFile("fail"+errorCode);
            }
            @Override public void onDiscoveryStopped(String serviceType) {
                LogUtil.logToFile("stop"+serviceType);
            }
            @Override public void onServiceLost(NsdServiceInfo service) {
                LogUtil.logToFile("lost"+service.getServiceType()+ service.getHost()+ service.getPort());
            }
        };
    
}
