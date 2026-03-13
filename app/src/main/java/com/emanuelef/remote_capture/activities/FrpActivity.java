package com.emanuelef.remote_capture.activities;
import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import com.emanuelef.remote_capture.R;
import com.emanuelef.remote_capture.Utils;
import android.view.MenuItem;
import java.util.ArrayList;
import android.widget.Toast;
import android.widget.AdapterView;
import android.view.View;
import android.widget.Adapter;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Context;
import java.util.List;
import android.widget.EditText;
import android.text.InputType;
import android.widget.Button;
import android.content.Intent;
import android.os.Build;
import android.app.admin.FactoryResetProtectionPolicy;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;

public class FrpActivity extends Activity {
    ArrayAdapter<String> adapter;
    ListView listView;
    List<String> al;
    public static DevicePolicyManager mDpm;
    public static ComponentName mAdminComponentName;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Utils.setTheme(this);
        try{
            setContentView(R.layout.activity_frp);
            setTitle("ניהול חשבונות שחזור");
            
            mDpm = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
            mAdminComponentName = new ComponentName(this,admin.class);
            listView = findViewById(R.id.lv_accounts);
            al=new ArrayList<>();
            
            adapter = new ArrayAdapter<>(this,android.R.layout.simple_list_item_1 ,al );
            listView.setAdapter(adapter);
            listView.setOnItemClickListener(new AdapterView.OnItemClickListener(){
                    @Override
                    public void onItemClick(AdapterView<?> p1, View p2, final int p3, long p4) {
                        List<String> options = new ArrayList<String>();
                        //LogUtil.logToFile(item.title+item.itemSourceType.name()+item.customLink);
                        options.add("ערוך");
                        options.add("הסר");
                        final String[] items = options.toArray(new String[0]);
                        AlertDialog.Builder builder = new AlertDialog.Builder(FrpActivity.this);
                        builder.setTitle(al.get(p3));
                        builder.setItems(items, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    String selection = items[which];
                                    if ("ערוך".equals(selection)) {
                                        //edit
                                        showAddEditIdDialog(FrpActivity.this,p3);
                                    } else if ("הסר".equals(selection)) {
                                        
                                        al.remove(p3);
                                        adapter.notifyDataSetChanged();
                                    }
                                }
                            });
                        if(options.size()>0)
                            builder.show();
                    
                        
                    }
                });
            Button busave=findViewById(R.id.btn_save_frp);
            busave.setOnClickListener(new View.OnClickListener(){
                    @Override
                    public void onClick(View p1) {
                        PasswordManager.requestPasswordAndSave(new Runnable() {
                                @Override
                                public void run() {
                                    try{
                                        activatefrp();
                                    }catch(Exception e){}
                                }
                            },FrpActivity.this);
                    }
                });
                loadCurrentFrp();
        }catch(Exception e){
            LogUtil.logToFile(e.toString());
        }
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.frp_menu, menu);
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        super.onOptionsItemSelected(item);
        
        switch(item.getItemId()) {
            case R.id.menu_frp_add_account:
                if(al.size()>=10){
                    Toast.makeText(this,"כמה חשבונות יש לך?...",0).show();
                }else{
                    //dialog
                    showAddEditIdDialog(this,-1);
                    
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }

    }
    
    public void showAddEditIdDialog(final Context context,final int item) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(item!=-1 ? "עריכת זיהוי": "הוספת זיהוי");

        final EditText input = new EditText(context);
        input.setHint("הכנס זיהוי חשבון");
        input.setText(item!=-1 ? al.get(item): "");
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        builder.setView(input);

        builder.setPositiveButton("שמור", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    String newid = input.getText().toString();
                    if (newid.length() > 0) {
                        if(item!=-1){
                            al.remove(item);
                            adapter.notifyDataSetChanged();
                        }
                        
                        al.add(newid);
                        adapter.notifyDataSetChanged();
                        
                    } else {
                        Toast.makeText(context, "שדה ריק.", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        builder.setNegativeButton("בטל", null);
        builder.show();
    }
    private void loadCurrentFrp(){
        try {
            if (Build.VERSION.SDK_INT > 29) {
                try {
                    FactoryResetProtectionPolicy frp=
                        mDpm.getFactoryResetProtectionPolicy(mAdminComponentName);
                    String acc="";
                    if(frp!=null&&frp.getFactoryResetProtectionAccounts()!=null){
                        
                        for(String a:frp.getFactoryResetProtectionAccounts()){
                            acc+=a+",";
                            al.add(a);
                            adapter.notifyDataSetChanged();
                        }
                    }
                    if(frp!=null){
                        LogUtil.logToFile("ena="+frp.isFactoryResetProtectionEnabled());
                        LogUtil.logToFile("acc="+acc);
                    }
                } catch (Exception e) {
                    LogUtil.logToFile(e.toString());
                    //Toast.makeText(getApplicationContext(), "e-frp"+e , Toast.LENGTH_SHORT).show();
                }
            }

            //bundle=null;
            String str = "com.google.android.gms";
            Bundle bundle =
                mDpm.getApplicationRestrictions(mAdminComponentName, str);
            String acc="";
            if(bundle!=null){
                if(bundle.getStringArray("factoryResetProtectionAdmin")!=null){
                    
                    for(String a:bundle.getStringArray("factoryResetProtectionAdmin")){
                        acc+=a+",";
                        if(!al.contains(a)){
                            al.add(a);
                            adapter.notifyDataSetChanged();
                        }
                    }
                    LogUtil.logToFile("accold="+acc);
                }
            }

            //Toast.makeText(getApplicationContext(), "details frp..", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            LogUtil.logToFile(e.toString());
            //Toast.makeText(getApplicationContext(), "e-frp2"+e , Toast.LENGTH_SHORT).show();
        }
    }
    private void activatefrp(){
        try {
            
            if(mDpm.isDeviceOwnerApp(getPackageName())){
            List<String> arrayList = al;
            if(al.size()==0){
                removefrp(this);
                return;
            }
            //arrayList.add("116673918161076927085");
            //arrayList.add("107578790485390569043");
            //arrayList.add("105993588108835326457");
            
            if (Build.VERSION.SDK_INT > 29) {
                try {
                    FactoryResetProtectionPolicy frp=new FactoryResetProtectionPolicy.Builder()
                        .setFactoryResetProtectionAccounts(arrayList)
                        .setFactoryResetProtectionEnabled(true)
                        .build();
                    mDpm.setFactoryResetProtectionPolicy(mAdminComponentName, frp);
                } catch (Exception e) {
                    Toast.makeText(getApplicationContext(), "e-frp"+e , Toast.LENGTH_SHORT).show();
                }
            }
            Bundle bundle = new Bundle();

            bundle.putStringArray("factoryResetProtectionAdmin", arrayList.toArray(new String[0]));

            //bundle=null;
            String str = "com.google.android.gms";
            mDpm.setApplicationRestrictions(mAdminComponentName, str, bundle);
            Intent intent = new Intent("com.google.android.gms.auth.FRP_CONFIG_CHANGED");
            intent.setPackage(str);
            intent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
            sendBroadcast(intent);
            Toast.makeText(getApplicationContext(), "frp..", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(getApplicationContext(), "e-frp2"+e , Toast.LENGTH_SHORT).show();
        }
    }
    public static void removefrp(final Activity activity){
        if (Build.VERSION.SDK_INT > 29) {
            try {
                List<String> arrayList = new ArrayList<>();
                FactoryResetProtectionPolicy frp=new FactoryResetProtectionPolicy.Builder()
                    .setFactoryResetProtectionAccounts(arrayList)
                    .setFactoryResetProtectionEnabled(false)
                    .build();
                mDpm.setFactoryResetProtectionPolicy(mAdminComponentName, frp);
            } catch (Exception e) {
                Toast.makeText(activity.getApplicationContext(), "e-frp" , Toast.LENGTH_SHORT).show();
            }
        }
        try {
            Bundle bundle = new Bundle();
            bundle = null;
            String str = "com.google.android.gms";
            mDpm=(DevicePolicyManager)activity.getSystemService("device_policy");
            mDpm.setApplicationRestrictions(mAdminComponentName, str, bundle);
            Intent intent = new Intent("com.google.android.gms.auth.FRP_CONFIG_CHANGED");
            intent.setPackage(str);
            intent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
            activity.sendBroadcast(intent);
            Toast.makeText(activity.getApplicationContext(), "frp removed", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(activity.getApplicationContext(), "" + e, Toast.LENGTH_SHORT).show();
        }

    }
}
