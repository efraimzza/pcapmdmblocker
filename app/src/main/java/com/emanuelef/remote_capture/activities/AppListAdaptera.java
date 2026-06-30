package com.emanuelef.remote_capture.activities;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.Switch; // שינוי מ-CheckBox
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import com.emanuelef.remote_capture.R;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.view.View.OnLongClickListener;
import android.content.Intent;
import android.provider.Settings;
import android.net.Uri;
import android.content.pm.PackageManager;

public class AppListAdaptera extends ArrayAdapter<AppItem> {

    private final Context context;
    private final List<AppItema> appList;
    private String mdmPackageName;

    public AppListAdaptera(Context context, List<AppItema> appList) {
        super(context, R.layout.app_list_item_a, appList);
        this.context = context;
        this.appList = appList;
        this.mdmPackageName = context.getPackageName();
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        final ViewHolder holder;

        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.app_list_item_a, parent, false);
            holder = new ViewHolder();
            holder.appIcon = (ImageView) convertView.findViewById(R.id.app_icon);
            holder.appName = (TextView) convertView.findViewById(R.id.app_name);
            holder.appPackage = (TextView) convertView.findViewById(R.id.app_package);
            holder.appLastUpdated = (TextView) convertView.findViewById(R.id.app_last_updated);
            holder.hideSwitch = (Switch) convertView.findViewById(R.id.hide_switch);
            holder.suspendSwitch = (Switch) convertView.findViewById(R.id.suspend_switch);
            holder.suspendLinl = (LinearLayout) convertView.findViewById(R.id.suspend_linl);
            holder.removeSwitch = (Switch) convertView.findViewById(R.id.remove_switch);
            holder.removeLinl = (LinearLayout) convertView.findViewById(R.id.remove_linl);
            
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        final AppItema appItem = appList.get(position);

        holder.appIcon.setImageDrawable(appItem.getIcon());
        holder.appName.setText(appItem.getName());
        holder.appPackage.setText(appItem.getPackageName());

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
        String lastUpdated = "עדכון אחרון: " + sdf.format(new Date(appItem.getLastUpdateTime()));
        holder.appLastUpdated.setText(lastUpdated);

        
        //if (appItem.getPackageName().equals(mdmPackageName)&&!isPicker) {
            //holder.hideSwitch.setChecked(false); // תמיד לא מסומן עבור ה-MDM app
            //holder.hideSwitch.setEnabled(false); // בטל את האפשרות ללחוץ עליו ישירות
        //} else {
            holder.hideSwitch.setChecked(appItem.isHidden());
            //holder.hideSwitch.setEnabled(true);
        //}
        
        holder.suspendSwitch.setChecked(appItem.isSuspend());
        holder.suspendLinl.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    boolean newCheckedState = !holder.suspendSwitch.isChecked();
                    holder.suspendSwitch.setChecked(newCheckedState);
                    appItem.setSuspend(newCheckedState);
                    //change the another values
                    appItem.setHidden(false);
                    holder.hideSwitch.setChecked(false);
                    if(!appItem.isSystemApp()){
                        appItem.setRemove(false);
                        holder.removeSwitch.setChecked(false);
                    }
                }
            });
        if(appItem.isSystemApp()){
            //hide remove
            holder.removeLinl.setVisibility(View.INVISIBLE);
        }else{
            holder.removeLinl.setVisibility(View.VISIBLE);

            holder.removeSwitch.setChecked(appItem.isRemove());
            holder.removeLinl.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        boolean newCheckedState = !holder.removeSwitch.isChecked();
                        holder.removeSwitch.setChecked(newCheckedState);
                        appItem.setRemove(newCheckedState);
                        //change the another values
                        appItem.setHidden(false);
                        holder.hideSwitch.setChecked(false);
                        appItem.setSuspend(false);
                        holder.suspendSwitch.setChecked(false);
                    }
                });
        }
        convertView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    //if (appItem.getPackageName().equals(mdmPackageName)&&!isPicker) {
                        //Toast.makeText(context.getApplicationContext(), "לא ניתן להסתיר את אפליקציית ה-MDM.", Toast.LENGTH_SHORT).show();
                    //} else {
                        boolean newCheckedState = !holder.hideSwitch.isChecked();
                        holder.hideSwitch.setChecked(newCheckedState);
                        appItem.setHidden(newCheckedState);
                    //change the another values
                    appItem.setSuspend(false);
                    holder.suspendSwitch.setChecked(false);
                    if(!appItem.isSystemApp()){
                        appItem.setRemove(false);
                        holder.removeSwitch.setChecked(false);
                    }
                    //}
                }
            });
        convertView.setOnLongClickListener(new OnLongClickListener(){
                @Override
                public boolean onLongClick(View p1) {
                    //app info
                    if(isAppInstalled(appItem.getPackageName())){
                        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                        intent.setData(Uri.fromParts("package", appItem.getPackageName(), null));
                        context.startActivity(intent);
                    }
                    return true;
                }
            });

        return convertView;
    }
    private boolean isAppInstalled(String packageName) {

        PackageManager pm = context.getPackageManager();
        try {
            pm.getPackageInfo(packageName, 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        } 
    }
    static class ViewHolder {
        ImageView appIcon;
        TextView appName;
        TextView appPackage;
        TextView appLastUpdated;
        Switch hideSwitch;
        Switch suspendSwitch;
        LinearLayout suspendLinl;
        Switch removeSwitch;
        LinearLayout removeLinl;
    }
}
