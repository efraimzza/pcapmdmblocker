package com.emanuelef.remote_capture.activities;

import android.accessibilityservice.AccessibilityService;
import android.view.accessibility.AccessibilityEvent;
import android.content.Intent;
import java.util.List;
import java.util.ArrayList;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.TaskInfo;
import android.app.TaskStackBuilder;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityRecord;
import android.os.Build;
import android.graphics.Path;
import android.accessibilityservice.GestureDescription;
import android.os.Handler;
import android.content.Context;
import android.content.ContentResolver;
import android.provider.Settings;

public class accser extends AccessibilityService {

    public static accser sinsta;
    static boolean access=true;
    boolean mcureve=false;
    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if(event!=null){
        //LogUtil.logToFile("eve");
        try{
            String pkg = event.getPackageName().toString();
            String cls = event.getClassName().toString();

            // if (cls == null || cls.trim().isEmpty())
            //  return;
            if(access){
                //LogUtil.logToFile("acc pkg - "+ pkg+" cls - "+cls);
                if(cls!=null){
                    if(cls.equals("com.dofun.carsetting.activity.apkinstall.InstallActivity")||cls.contains("NewsletterDirectoryCategoriesActivity")||cls.contains("StatusPlaybackActivity")){
                        //quitApplication.quitApplication(this);
                        performSystemBack();
                        LogUtil.logToFile("killappd class install"+cls);
                    }
                }
            }
        }catch(Exception e){}
        try{
            if(access){
                try {
                    mcureve=false;
                    //LogUtil.logToFile("eve"+event.getPackageName());
                    if(event.getPackageName()!=null)
                        if(event.getPackageName().equals("com.whatsapp")){
                            AccessibilityEvent ae=event;
                                ani(ae.getSource(),ae);
                                for (int i=0;i < ae.getRecordCount();i++) {
                                    AccessibilityRecord ar=ae.getRecord(i);
                                    if (ar != null) {
                                        ani(ar.getSource(),ae);
                                    }
                                }
                        }

                } catch (Exception e) {
                    LogUtil.logToFile(e.toString());
                }
            }
        }catch(Exception e){
            LogUtil.logToFile(e.toString());
        }
        }
    }
    public static void performSystemBack() {
        if (sinsta != null) {
            // זו הפקודה היחידה ב-Android שלוחצת Back למכשיר כולו
            sinsta.performGlobalAction(GLOBAL_ACTION_BACK);
            sinsta.performGlobalAction(GLOBAL_ACTION_BACK);
            // אפשרויות נוספות שניתן לבצע (בתוך הערות):
            sinsta.performGlobalAction(GLOBAL_ACTION_HOME);    // לחיצה על הבית
            // instance.performGlobalAction(GLOBAL_ACTION_RECENTS); // פתיחת אפליקציות אחרונות
            // instance.performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS); // פתיחת התראות
            //removeCurrentAppFromRecents();
        }
    }
    // פונקציה שמבצעת את כל התהליך: פתיחת רשימה והסרה

    @Override
    public void onInterrupt() {
        //LogUtil.logToFile("intrupt");
        try{
            sinsta=null;
            disableSelf();
            stopSelf();
            refreshacc.refreshacc(getApplicationContext());
        }catch(Exception e){LogUtil.logToFile(e.toString());}
    }

    @Override
    public void onDestroy() {
        //LogUtil.logToFile("dest");
        super.onDestroy();
        //LogUtil.logToFile("dest");
        try{
            disableSelf();
            stopSelf();
            stopForeground(true);
            refreshacc.refreshacc(getApplicationContext());
        }catch(Exception | Throwable e){LogUtil.logToFile(e.toString());}

    }

    @Override
    public boolean onUnbind(Intent intent) {
        //LogUtil.logToFile("unb");
        try{
            refreshacc.refreshacc(getApplicationContext());
            sinsta=null;
            stopSelf();
            disableSelf();
            refreshacc.refreshacc(getApplicationContext());
        }catch(Exception e){LogUtil.logToFile(e.toString());}
        return super.onUnbind(intent);
    }

    @Override
    protected void onServiceConnected() {
        //LogUtil.logToFile("con");
        super.onServiceConnected();
        try{
            sinsta=this;
            //LogUtil.logToFile("connected");
            refreshacc.refreshacc(getApplicationContext());
        }catch(Exception e){LogUtil.logToFile(e.toString());}
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        //LogUtil.logToFile("tarm");
        super.onTaskRemoved(rootIntent);
        try{
            refreshacc.refreshacc(getApplicationContext());
        }catch(Exception e){LogUtil.logToFile(e.toString());}
    }

    @Override
    public void onRebind(Intent intent) {
        super.onRebind(intent);
        //LogUtil.logToFile("rb");
        try{
            refreshacc.refreshacc(getApplicationContext());
        }catch(Exception e){LogUtil.logToFile(e.toString());}
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        refreshacc.refreshacc(getApplicationContext());
    }

    public static accser getinsta(){
        return sinsta;
    }

    
    
    void ani(AccessibilityNodeInfo ani,AccessibilityEvent event) {
        try {
            if (ani != null) {
              //  List<CharSequence> li=new ArrayList<>();
                //li.add(ani.getContentDescription());
                //li.add(ani.getHintText());
                //li.add(ani.getPaneTitle());
                //li.add(ani.getStateDescription());
                //li.add(ani.getTooltipText());
              //  li.add(ani.getViewIdResourceName());
                //li.add(ani.getWindowId() + "");
                //for (CharSequence c:li) {
                String c=ani.getViewIdResourceName();
                    if (c != null) {
                     //   if (!c.equals("") || !c.equals("null")) {
                            //if(c.toString().contains(":id/")&&!c.toString().contains("android:id/")){
                                //LogUtil.logToFile(c+" type="+typestr( event.getEventType()));
                                //  if(c.toString().contains("rb_system")&&event.getEventType()==1){
                                if(c.contains("newsletter_")){

                                    //quitApplication.quitApplication(this);
                                    performSystemBack();
                                    LogUtil.logToFile("killappd click rb system "+c);
                                    mcureve=true;
                                   // break;
                                }
                           // }
                            //detect(c,"צ'אטים");
                            /*if (c.equals("צ'אטים")) {
                             quitApplication.quitApplication(this);
                             LogUtil.logToFile("killapp");
                             }*/
                        //}
                    }
               // }
                /*if (ani.getExtras() != null) {
                 Object[] ext=ani.getExtras().keySet().toArray();
                 for (Object o:ext) {
                 //LogUtil.logToFile(o);
                 }

                 }*/

                for (int i=0;i < ani.getChildCount();i++) {
                    //LogUtil.logToFile("overfor1");
                    if(mcureve)break;
                    //LogUtil.logToFile("overfor2");
                    ani(ani.getChild(i),event);
                }
            }
        } catch (Exception e) {
            LogUtil.logToFile(e.toString());
        }
    }
    void detect(CharSequence c,CharSequence detect){
        if(c.equals(detect.toString())){
            //quitApplication.quitApplication(this);
            performSystemBack();
            LogUtil.logToFile("killappd");
        }
    }
    public String typestr(int type){
        switch(type){
            case 1:
                return "click";
            case 2:
                return "lclick";
            case 8:
                return "focus";
            case 4:
                return "select";
            case 16:
                return "txchang";
            case 32:
                return "windstchang";
            case 64:
                return "notstchang";
            case 512:
                return "tochexplogestrstart";
            case 1024:
                return "tochexplogestrend";
            case 128:
                return "hoverenter";
            case 256:
                return "hoverexit";
            case 4096:
                return "scrolled";
            case 8192:
                return "selecchang";
            case 2048:
                return "windcontchang";
            case 1048576:
                return "tochinterstart";
            case 2097152:
                return "tochinterend";
            case 16384:
                return "announcement";
            case 262144:
                return "gstrdetectstart";
            case 524288:
                return "gstrdetectend";
            case 32768:
                return "accfocs";
            case 65536:
                return "accfocsclear";
            case 131072:
                return "texttraversed";
            case 4194304:
                return "windchang";

            default:
                return "na";

        }
    }
    public static class refreshacc {

        static Handler mhandler=null;
        static Runnable mrunnable=null;
        public static void refreshacc(final Context mcontext){
            final ContentResolver contentResolver =mcontext.getContentResolver();
            final String pkg=mcontext.getPackageName();
            if(mhandler!=null&&mrunnable!=null)
                mhandler.removeCallbacks(mrunnable);
            if(mhandler==null)
                mhandler=new Handler(mcontext.getMainLooper());

            mrunnable=new Runnable(){@Override public void run() {
                LogUtil.logToFile("loopacc");
                    /*if(accser.sinsta==null){
                        try{
                            mcontext.startService(new Intent().setClass(mcontext. getApplicationContext(), accser.class));
                        }catch(Exception | Throwable e){
                            LogUtil.logToFile(e.toString());
                            
                        }
                    }*/
                    if(accser.sinsta==null){
                        
                        try {
                            Settings.Secure.putString(contentResolver,"enabled_accessibility_services", pkg+"/com.emanuelef.remote_capture.activities.accser");
                        } catch (Throwable th) {
                            th.printStackTrace();
                            LogUtil.logToFile("msec"+th.toString());
                            //Toast.makeText(getApplicationContext(), "sec" + th, 1).show();
                            //this.d.a(Application.a(2131296265));
                        }
                        /*
                         Intent intent = new Intent();
                         intent.setAction("android.settings.ACCESSIBILITY_SETTINGS");
                         startActivity(intent);
                         */
                    }
                    if(AppState.getInstance().getCurrentPath().equals(PathType.WHATSAPP)){
                        mhandler.postDelayed(this,5000);
                    }
                }};
            if(AppState.getInstance().getCurrentPath().equals(PathType.WHATSAPP)){
                mhandler.post(mrunnable);
            }
        }
    }
}

