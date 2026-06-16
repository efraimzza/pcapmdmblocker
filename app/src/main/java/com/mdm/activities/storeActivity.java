package com.mdm.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.net.Uri;
import android.app.ProgressDialog;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Build;
import com.emanuelef.remote_capture.R;
import com.emanuelef.remote_capture.Utils;
import com.emanuelef.remote_capture.activities.PasswordManager;
import com.emanuelef.remote_capture.activities.picker;
import com.emanuelef.remote_capture.activities.LogUtil;
import android.content.ServiceConnection;
import android.content.ComponentName;
import android.os.IBinder;
import android.os.Handler;
import android.os.Looper;
import android.content.Context;
import java.util.concurrent.Executor;

public class storeActivity extends Activity {

    private ListView listView;
    private TextView emptyView;
    private static StoreItemAdapter adapter;
    public static ItemsManager itemsManager;
    public static ConfigManager configManager;

    private ImportExportManager importExportManager;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Utils.setTheme(this);
        setTitle("חנות");
        setContentView(R.layout.store_main);
        
        //for now to avoid install user files placed in the folder with the good name...
        //utils.deleteTempDir(getExternalFilesDir(""),false);
        configManager = new ConfigManager(this);
        itemsManager = new ItemsManager(this, configManager);
        importExportManager = new ImportExportManager(this, itemsManager);
        
        Dialogs.loadPriorityFromPrefs(this,true);
        
        listView = (ListView) findViewById(R.id.main_list_view);
        emptyView = (TextView) findViewById(R.id.empty_view);
        
        adapter = new StoreItemAdapter(this, itemsManager.getAllItemsvisible(), itemsManager);
        listView.setAdapter(adapter);

        listView.setEmptyView(emptyView);
    }
    public static String pickedfilepath="";
    public static String pickeddirpath="";
    protected void onResume() {
        super.onResume();
        if(pickedfilepath!=null&&!pickedfilepath.equals("")){
            String uri=pickedfilepath;
            pickedfilepath="";
            Dialogs.showImportModeDialog(this, itemsManager, adapter, uri, importExportManager);
        }else if(pickeddirpath!=null&&!pickeddirpath.equals("")){
            String uri=pickeddirpath;
            pickeddirpath="";
            Dialogs.showExportDialog(storeActivity.this, importExportManager,uri);
        }
        refreshData(false);
    }
    public static ProgressDialog progressDialog;
    boolean refreshinfocon=false;
    private synchronized void refreshData(final boolean refreshinfo) {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
        progressDialog = new ProgressDialog(this);
        progressDialog.getWindow().setBackgroundDrawableResource(R.drawable.rounded_button_background);
        progressDialog.setMessage("טוען עדכונים לאפליקציות...");
        progressDialog.setCancelable(false);
        progressDialog.show();
        if(refreshinfocon)return;
        final ConnectivityManager cm=(ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        boolean connected=false;
        for(Network ne:cm.getAllNetworks()){
            NetworkInfo ni=cm.getNetworkInfo(ne);
            if(ni.isConnected()&&ni.getType()!=cm.TYPE_VPN){
                connected=true;
                break;
            }

        }
        if(connected){
            NetworkInfo ni=cm.getNetworkInfo(cm.getActiveNetwork());
            //LogUtil.logToFile("def==ln="+cm.getAllNetworks().length+"ei="+ni.getExtraInfo()+"re="+ni.getReason()+"st="+ni.getSubtypeName()+"t="+ni.getTypeName()+"ds="+ni.getDetailedState().name()+"s="+ni.getState().name());

            for(Network ne:cm.getAllNetworks()){
                ni=cm.getNetworkInfo(ne);
                //LogUtil.logToFile("ln="+cm.getAllNetworks().length+"ei="+ni.getExtraInfo()+"re="+ni.getReason()+"st="+ni.getSubtypeName()+"t="+ni.getTypeName()+"ds="+ni.getDetailedState().name()+"s="+ni.getState().name());
                NetworkCapabilities nc=cm.getNetworkCapabilities(ne);
                //LogUtil.logToFile("cvpn="+nc.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_VPN)+"tvpn="+nc.hasTransport(NetworkCapabilities.TRANSPORT_VPN)+"up="+nc.getLinkUpstreamBandwidthKbps()+"do="+nc.getLinkDownstreamBandwidthKbps()+(Build.VERSION.SDK_INT>=29?("stren="+nc.getSignalStrength()):""));
            }
            if(refreshinfo){
                refreshinfocon=true;
                Toast.makeText(this, "מתחיל בדיקת עדכונים...", Toast.LENGTH_SHORT).show();
            }
        }else if(refreshinfo){
            Toast.makeText(this, "התחבר לאינטרנט ונסה שוב...", Toast.LENGTH_SHORT).show();
        }
        itemsManager.refreshAllItems(refreshinfocon, configManager.loadConfig(), new ItemsManager.RefreshCompleteListener() {
                public void onComplete() {
                    // עדכון ה-UI על ה-Main Thread
                    runOnUiThread(new Runnable() {
                            public void run() {
                                if (progressDialog != null && progressDialog.isShowing()) {
                                    progressDialog.dismiss();
                                }
                                adapter.updateData(itemsManager.getAllItemsvisible());
                                
                                if(refreshinfocon){
                                    refreshinfocon=false;
                                Toast.makeText(storeActivity.this, "בדיקת עדכונים הסתיימה.", Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
                }
            });
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_store, menu); 
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.menu_launch_settings) {
           // startActivity(new Intent(MainActivity.this, StoreSettingsActivity.class));
            Intent settingsIntent = new Intent(this, storeSettingsActivity.class);
            startActivity(settingsIntent);
            return true;
        } else if (id == R.id.menu_add_item_pn) {
            PasswordManager.requestPasswordAndSave(new Runnable(){
                    @Override
                    public void run() {
            Dialogs.showAddItemDialog(storeActivity.this, com.mdm.activities.StoreItem.ItemSourceType.MANUAL, itemsManager, adapter);
            
             } }, this);
            return true;
        } else if (id == R.id.menu_add_item_link) {
            PasswordManager.requestPasswordAndSave(new Runnable(){
                    @Override
                    public void run() {
                        Dialogs.showAddItemDialog(storeActivity.this, com.mdm.activities.StoreItem.ItemSourceType.CUSTOM_LINK, itemsManager, adapter);
            //Dialogs.showAddAppDialog(this, itemsManager, adapter);
            } }, this);
            return true;
        } else if (id == R.id.menu_import_json) {
            PasswordManager.requestPasswordAndSave(new Runnable(){
                    @Override
                    public void run() {
            // פעולה זו תפתח דיאלוג לבחירת סוג ייבוא (קובץ / קישור)
                        Dialogs.showImportSelectionDialog(storeActivity.this, itemsManager, adapter, importExportManager); 
            } }, this);
            return true;
          
        } else if (id == R.id.menu_export_json) {
            PasswordManager.requestPasswordAndSave(new Runnable(){
                    @Override
                    public void run() {
                        startActivity(new Intent(storeActivity.this,picker.class).putExtra("from","storeseldir"));
            } }, this);
            return true;
        } else if (id == R.id.menu_refresh) {
            refreshData(true);
            return true;
        } else if (id == R.id.menu_update_all) {
            //hide if is current down or queue...
            if(pkgName.equals("")&&(binder==null||(binder!=null&&binder.getService()==null))){
                updateAll();
                Toast.makeText(storeActivity.this, "מעדכן...", Toast.LENGTH_SHORT).show();
            }else{
                Toast.makeText(storeActivity.this, "עדיין מעדכן...", Toast.LENGTH_SHORT).show();
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    void updateAll(){
        /*if ((item.downloadLink != null && !item.downloadLink.equals("")&&(item.downloadLink.startsWith("https:/")))||(item.customLink!=null&&!item.customLink.equals(""))||(item.source.equals("GPlay"))) {
            }
            //or gplay or downlink startsWith https or customlink is available
        boolean isInstalled = isAppInstalled(item.packageName); // פונקציה לבדיקת התקנה
        if (isInstalled) {
            if (item.updateAvailable||item.itemSourceType.equals(StoreItem.ItemSourceType.CUSTOM_LINK)) {
                holder.button.setText("עדכון");
                holder.button.setVisibility(View.VISIBLE);
            } else {
                //not update
                holder.button.setVisibility(View.GONE);
            }
        } else {
            //need logic update - only if is update available meens version isnt 0... , or item.itemSourceType.equals(StoreItem.ItemSourceType.CUSTOM_LINK)) 
            holder.button.setText("התקן");
            holder.button.setVisibility(View.VISIBLE);
        }*/
        for(final StoreItem item:itemsManager.AllItemsAvailable()){
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
                utils.startDownloadnew(this,mlink,item.packageName,item.isDrive?"drive":"normal");
            }
            else if((item.source.equals("GPlay"))){
                new Thread(){public void run(){
                        try{
                            //regenerate
                            final String jstr=itemsManager.resolverService.gplayLinkResolver(storeActivity.this,item.packageName).downloadLink;
                            etMainExecutor.execute(new Runnable(){
                                    @Deprecated
                                    @Override
                                    public void run() {
                                        if(!jstr.equals("")){
                                            //if(!Prefs.istest(PreferenceManager.getDefaultSharedPreferences(context)))
                                            //utils.startDownloadnewGplay(context,item.packageName, jstr);
                                            //else
                                            utils.startDownloadnew(storeActivity.this,jstr,item.packageName,"gplay");
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
            Toast.makeText(this, "שגיאה בפתיחת הקישור: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
        }
    }
    public static final Executor etMainExecutor = new Executor() {
        private final Handler handler = new Handler(Looper.getMainLooper());
        @Override
        public void execute(Runnable command) {
            handler.post(command);
        }
    };
   /*
    private static final int PICK_FILE_REQUEST_CODE = 42;
    
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_FILE_REQUEST_CODE && resultCode == RESULT_OK) {
            if (data != null && data.getData() != null) {
                Uri uri = data.getData();
                
                Dialogs.showImportModeDialog(this, itemsManager, adapter, uri, importExportManager);
            }
        }
    }*/
    private boolean isBound = false;
    private static final Handler uiHandler = new Handler(Looper.getMainLooper());
    static DownloadService.LocalBinder binder=null;
    public final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            binder = (DownloadService.LocalBinder) service;
            //downloadService = binder.getService();
            LogUtil.logToFile("onconnected="+binder.getService());
            //if(downloadService!=null){
            isBound = true;
            startUiUpdater(); 
            /*}else{
             pkgName="";
             adapter.notifyDataSetChanged();
             }*/
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            binder=null;

            isBound = false;
            //downloadService = null;
        }
    };
    static String st="";
    @Override
    protected void onStart() {
        super.onStart();
        /*if(downloadService!=null){
         if(!downloadService.isDownloading){
         LogUtil.logToFile("onst");
         pkgName="";
         adapter.notifyDataSetChanged();
         return;
         }
         LogUtil.logToFile("onst1"+downloadService);
         }//this creating a new servise?
         LogUtil.logToFile("onst2"+downloadService);*/
        if(binder!=null){
            if(binder.getService()!=null){
                if(!binder.getService().isDownloading){
                    LogUtil.logToFile("onst");
                    pkgName="";
                    adapter.notifyDataSetChanged();
                    return;
                }
                LogUtil.logToFile("onst1"+binder.getService());
            }//this creating a new servise?
            LogUtil.logToFile("onst2");
        }
        LogUtil.logToFile("onst3");
        Intent intent = new Intent(this, DownloadService.class);
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
        st="start";
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (isBound) {
            unbindService(serviceConnection);
            isBound = false;
        }
        binder=null;//to stop the runnable if isnt in ui...
        uiHandler.removeCallbacksAndMessages(null);
        st="stop";
    }
    public static String stat="";
    public static String statusinf="";
    public static int progre=0;
    public static String pkgName="";
    private static final Runnable uiUpdaterRunnable = new Runnable() {
        @Override
        public void run() {
            try{
                //if(linl_catd_down.getVisibility()==View.VISIBLE)
                if (binder!=null && binder.getService()!=null && binder.getService().isDownloading) {
                    // הצגת הכרטיס במקרה שהוא היה מוסתר
                    //linl_catd_down.setVisibility(View.VISIBLE);
                    long downloaded = binder.getService().bytesDownloaded;
                    long total = binder.getService().totalBytes;
                    long speed = binder.getService().bytesPerSecond;

                    int progress = (total > 0) ? (int) ((downloaded * 100) / total) : 0;
                    /*

                     txtPkgName.setText(downloadService.packageName);
                     txtFileName.setText(downloadService.finalFilename);
                     */
                    String etaStr = "מחשב...";
                    if (speed > 0 && total > 0) {
                        long bytesRemaining = total - downloaded;
                        long secondsRemaining = bytesRemaining / speed;

                        // // פורמט זמן קריא (דקות:שניות) //
                        if (secondsRemaining < 60) {
                            etaStr = secondsRemaining + " שניות";
                        } else {
                            etaStr = (secondsRemaining / 60) + " דקות ו-" + (secondsRemaining % 60) + " שניות";
                        }
                    }
                    // בניית מחרוזת הסטטוס המעוצבת
                    String speedStr = formatFileSize(speed) + "/s";
                    String info = "התקדמות: " + progress + "% (" + formatFileSize(downloaded) + " / " + formatFileSize(total) + ")\n" +
                        "מהירות: " + speedStr + " | נותר: " + etaStr;
                    statusinf=info;
                    progre=progress;
                    stat=binder.getService().state;
                    pkgName=binder.getService().packageName;
                    if(adapter!=null)
                        adapter.notifyDataSetChanged();
                    /*
                     txtStatusInfo.setText(info);

                     // עדכון ה-ProgressBar הויזואלי
                     progressDownload.setProgress(progress);

                     btnCancelDown.setVisibility(View.VISIBLE);
                     */
                    LogUtil.logToFile(""+binder.getService());
                    uiHandler.postDelayed(this, 500); 
                }else if (binder!=null&&binder.getService()!=null){
                    pkgName="";
                    if(adapter!=null)
                        adapter.notifyDataSetChanged();
                    LogUtil.logToFile("ll"+binder.getService());
                    uiHandler.postDelayed(this, 2000);
                } else {
                    /*txtStatusInfo.setText("אין הורדות פעילות ברקע.");
                     progressDownload.setProgress(0);
                     btnCancelDown.setVisibility(View.GONE);
                     progressDownload.setVisibility(View.GONE);
                     txtPkgName.setVisibility(View.GONE);
                     txtFileName.setVisibility(View.GONE);*/
                     if(st!=null&&st.equals("start")){
                         itemsManager.refreshAllItems(false, configManager.loadConfig(), new ItemsManager.RefreshCompleteListener() {
                                 public void onComplete() {
                                     // עדכון ה-UI על ה-Main Thread
                                     uiHandler.post((new Runnable() {
                                             public void run() {
                                                 if(adapter!=null)
                                                     adapter.updateData(itemsManager.getAllItemsvisible());
                                             }
                                         }));
                                 }
                             });
                     }
                    pkgName="";
                    if(adapter!=null)
                        adapter.notifyDataSetChanged();
                    //uiHandler.postDelayed(this, 2000);
                    LogUtil.logToFile("lllb"+binder+st);
                    if(binder!=null)
                        LogUtil.logToFile("lllbs"+binder.getService()+st);
                }
            }catch(Throwable t){LogUtil.logToFile(t);}
        }
    };

    public static void startUiUpdater() {
        uiHandler.removeCallbacksAndMessages(null);
        uiHandler.post(uiUpdaterRunnable);
    }

    private static String formatFileSize(long bytes) {
        if (bytes <= 0) return "0 B";
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp - 1) + "";
        return String.format("%.1f %sB", bytes / Math.pow(1024, exp), pre);
    }
}
