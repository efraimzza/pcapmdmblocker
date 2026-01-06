package com.emanuelef.remote_capture.activities;

import android.app.Activity;
import android.app.admin.DeviceAdminInfo;
import android.app.admin.DeviceAdminReceiver;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import com.emanuelef.remote_capture.R;
import java.io.IOException;
import java.util.ArrayList;
import org.xmlpull.v1.XmlPullParserException;
import com.emanuelef.remote_capture.Utils;
import android.content.DialogInterface;
import android.widget.Toast;
import android.widget.ScrollView;
import android.app.AlertDialog;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.LinearLayout;
import android.view.Gravity;
import android.graphics.Color;

public class transferOwner extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PasswordManager.requestPasswordAndSave(new Runnable(){
                @Override
                public void run() {
                    Utils.setTheme(transferOwner.this);
                    setContentView(R.layout.activity_transfer_owner);
                    ListView lv=findViewById(R.id.actranlv);
                    PackageManager pm=getPackageManager();
                    final ArrayList<DeviceAdminInfo> dai=new ArrayList<>();
                    for(ResolveInfo ri: pm.queryBroadcastReceivers(new   Intent(DeviceAdminReceiver.ACTION_DEVICE_ADMIN_ENABLED), PackageManager.GET_META_DATA)){
                        try {
                            dai.add( new DeviceAdminInfo(transferOwner.this, ri));
                        } catch (IOException e) {LogUtil.logToFile(e.toString());} catch (XmlPullParserException e) {LogUtil.logToFile(e.toString());}
                    }
                    ArrayList<String> dais=new ArrayList<>();
                    for(DeviceAdminInfo inf : dai){
                        dais.add("name="+inf.loadLabel(pm)+"\npkgname="+inf.getPackageName());
                    }
                    final Button bucomptext=findViewById(R.id.actranbucomptext);
                    bucomptext.setBackgroundResource(R.drawable.rounded_button_background);
                    ArrayAdapter<String> adapter = new ArrayAdapter<String>(transferOwner.this, 
                                                                            android.R.layout.simple_list_item_1, dais);
                    lv.setAdapter(adapter);
                    lv.setOnItemClickListener(new android.widget.AdapterView.OnItemClickListener() {
                            @Override
                            public void onItemClick(android.widget.AdapterView<?> parent, View view, int position, long id) {
                                bucomptext.setText(dai.get(position).getComponent().flattenToShortString());
                                //DevicePolicyManager mDpm=(DevicePolicyManager) getSystemService(DEVICE_POLICY_SERVICE);
                                //ComponentName mComp=new ComponentName(transferOwner.this, admin.class);
                                try{
                                    //mDpm.transferOwnership(mComp,dai.get(position).getComponent(),null);
                                    showMoveMDMConfirmationDialog(transferOwner.this,dai.get(position));
                                }catch(Exception e){LogUtil.logToFile(e.toString());}
                            }
                        });
                    Button burm=findViewById(R.id.actranburm);
                    burm.setBackgroundResource(R.drawable.rounded_button_background);
                    burm.setOnClickListener(new OnClickListener(){
                            @Override
                            public void onClick(View p1) {
                                try{
                                    ((DevicePolicyManager) getSystemService(DEVICE_POLICY_SERVICE))
                                        .clearDeviceOwnerApp(getPackageName());
                                }catch(Exception e){LogUtil.logToFile(e.toString());}
                            }
                        });
                }
            }, this);
    }
    public static void showMoveMDMConfirmationDialog(final Activity activity,final DeviceAdminInfo dai) {
        LinearLayout linl=new LinearLayout(activity);
        linl.setOrientation(LinearLayout.VERTICAL);
        TextView tvdes=new TextView(activity);
        tvdes.setGravity(Gravity.CENTER);
        tvdes.setTextSize(20);
        tvdes.setTextColor(Color.parseColor("#ffff0000"));
        tvdes.setText("האם אתה בטוח שברצונך להעביר את הרשאות הניהול ל-"+
                      dai.loadLabel(activity.getPackageManager())+"\n(שם חבילה="+dai.getPackageName()+")"
                      +"?\nאזהרה! יש אפשרות שלא תוכלו להסיר את הרשאות הניהול מהאפליקציה (לדוגמא אם לא יושם הסרת הרשאות ניהול).\nיש אפשרות שלא תוכלו לנהל הגבלות באפליקציה החדשה (לדוגמא אם אין אפשרויות ניהול הגבלות מכשיר!).");

        ImageView imvi=new ImageView(activity);
        imvi.setImageDrawable(dai.loadIcon(activity.getPackageManager()));
        ScrollView scl=new ScrollView(activity);

        linl.addView(imvi);
        linl.addView(tvdes);
        scl.addView(linl);
        new AlertDialog.Builder(activity)
            .setTitle("העברת ניהול מכשיר?")
            .setView(scl)
            //.setMessage("האם אתה בטוח שברצונך להסיר את אפליקציית ה-MDM כמנהל המכשיר?\nאזהרה: אם הסתרת אפליקציות תצטרך להסיר את ההסתרה ידנית בניהול אפליקציות לפני ההסרה!")
            .setPositiveButton("כן, העבר", new DialogInterface.OnClickListener() {
                @Deprecated
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    try{
                        DevicePolicyManager mDpm=(DevicePolicyManager) activity.getSystemService(DEVICE_POLICY_SERVICE);
                        ComponentName mComp=new ComponentName(activity, admin.class);
                        mDpm.transferOwnership(mComp,dai.getComponent(),null);
                        Toast.makeText(activity.getApplicationContext(), "mdm moved", Toast.LENGTH_SHORT).show();
                    }catch(Exception e){
                        LogUtil.logToFile(e.toString());
                        Toast.makeText(activity.getApplicationContext(), "" + e, Toast.LENGTH_SHORT).show();
                    }

                }
            })
            .setNegativeButton("ביטול", null)
            .show();
    }
}
