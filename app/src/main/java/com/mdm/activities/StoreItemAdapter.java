package com.mdm.activities;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import java.util.List;
import android.content.pm.PackageManager;
import com.emanuelef.remote_capture.R;
import com.emanuelef.remote_capture.activities.PasswordManager;
import android.util.TypedValue;
import android.widget.ProgressBar;
import android.content.Intent;

public class StoreItemAdapter extends BaseAdapter {

    private final storeActivity context;
    private List<StoreItem> itemList;
    private final ItemsManager itemsManager;
    private final LayoutInflater inflater;

    public StoreItemAdapter(storeActivity context, List<StoreItem> itemList, ItemsManager itemsManager) {
        this.context = context;
        this.itemList = itemList;
        this.itemsManager = itemsManager;
        this.inflater = LayoutInflater.from(context);
    }

    public void updateData(List<StoreItem> newItemList) {
        this.itemList = newItemList;
        notifyDataSetChanged();
    }

    public int getCount() {
        return itemList.size();
    }

    public Object getItem(int position) {
        return itemList.get(position);
    }

    public long getItemId(int position) {
        return position;
    }

    public View getView(final int position, View convertView, ViewGroup parent) {
        final StoreItemViewHolder holder;

        if (convertView == null) {
            convertView = inflater.inflate(R.layout.item_store, parent, false);
            holder = new StoreItemViewHolder(convertView);
            convertView.setTag(holder);
        } else {
            holder = (StoreItemViewHolder) convertView.getTag();
        }

        final StoreItem item = itemList.get(position);

        // 1. img icon app
        holder.icon.setImageDrawable(item.icon);

        // 2. line title
        holder.title.setText(item.title);

        // 3. line version: מציג את גרסה נוכחית והגרסה האחרונה
        String versionText = "נוכחי: " + item.currentVersion;
        if (item.updateAvailable) {
            versionText += " | אחרון: " + item.latestVersion;
            holder.version.setTextColor(context.getResources().getColor(android.R.color.holo_red_dark));
        } else {
            versionText += " | אחרון: " + item.latestVersion;
            TypedValue tv = new TypedValue();
            context.getTheme().resolveAttribute(android.R.attr.colorPrimary, tv, true);
            holder.version.setTextColor(tv.data);
        }
        holder.version.setText(versionText);

        // 4. line packagename
        holder.packageName.setText(item.packageName);

        // 5. line source
        holder.source.setText("מקור: " + item.source);

        // 6. Button Logic (Install/Update)
        boolean isInstalled = isAppInstalled(item.packageName); // פונקציה לבדיקת התקנה
        if (isInstalled) {
            if (item.updateAvailable||item.itemSourceType.equals(StoreItem.ItemSourceType.CUSTOM_LINK)) {
                holder.button.setText("עדכון");
                holder.button.setVisibility(View.VISIBLE);
            } else {
                holder.button.setVisibility(View.GONE);
            }
        } else {
            holder.button.setText("התקן");
            holder.button.setVisibility(View.VISIBLE);
        }
        if(DownloadService.isinqueue(item.packageName)){
            holder.button.setText("ביטול");
            //holder.button.setEnabled(true);
            holder.state.setVisibility(View.VISIBLE);
            holder.state.setText("בתור");
        }else
        if(item.packageName.equals(context.pkgName)){
            holder.button.setText("ביטול");
            //holder.button.setVisibility(View.VISIBLE);
            //holder.button.setEnabled(false);
            holder.state.setVisibility(View.VISIBLE);
            holder.statusInfo.setVisibility(View.VISIBLE);
            holder.progressDownload.setVisibility(View.VISIBLE);
            holder.state.setText(context.stat);
            holder.statusInfo.setText(context.statusinf);
            holder.progressDownload.setProgress(context.progre);
        }else{
            //holder.button.setEnabled(true);
            holder.state.setVisibility(View.GONE);
            holder.statusInfo.setVisibility(View.GONE);
            holder.progressDownload.setVisibility(View.GONE);
        }
        // שימוש ב-OnClickListener מסורתי (ללא Lambda)
        holder.button.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    // לוגיקת הורדה: פתיחת הקישור הפנימי של הפריט
                    if(((Button)v).getText().equals("ביטול")){
                        /*if(DownloadService.isinqueue(item.packageName)){
                         //remove
                         //only need to send cancel with pkg...

                         }else{
                         //cancel

                         }*/
                        Intent cancelIntent = new Intent(context, DownloadService.class);
                        cancelIntent.setAction(DownloadService.ACTION_CANCEL_DOWNLOAD);
                        cancelIntent.putExtra(DownloadService.EXTRA_PKG,item.packageName);
                        context.startService(cancelIntent);
                    }else
                    Dialogs.showDownloadConfirmation(context, item, itemsManager);
                }
            });

        // 7. Long Click (edit link, remove item) - פתיחת Context Menu או דיאלוג
        convertView.setOnLongClickListener(new View.OnLongClickListener() {
                public boolean onLongClick(View v) {
                    PasswordManager.requestPasswordAndSave(new Runnable(){
                            @Override
                            public void run() {
                    Dialogs.showLongClickMenu(context, item, itemsManager, StoreItemAdapter.this);
                    } }, context);
                    return true;
                }
            });

        return convertView;
    }

    /** Placeholder for checking if app is installed (requires PackageManager) */
    private boolean isAppInstalled(String packageName) {
        
        PackageManager pm = context.getPackageManager();
        try {
            pm.getPackageInfo(packageName, 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        } 
    }

    // --- ViewHolder Class (For efficiency with ListView) ---

    private static class StoreItemViewHolder {
        public final ImageView icon;
        public final TextView title;
        public final TextView version;
        public final TextView packageName;
        public final TextView source;
        public final Button button;
        public final TextView state;
        public final TextView statusInfo;
        public final ProgressBar progressDownload;
        
        public StoreItemViewHolder(View view) {
            icon = (ImageView) view.findViewById(R.id.item_icon);
            title = (TextView) view.findViewById(R.id.item_title);
            version = (TextView) view.findViewById(R.id.item_version);
            packageName = (TextView) view.findViewById(R.id.item_packagename);
            source = (TextView) view.findViewById(R.id.item_source);
            button = (Button) view.findViewById(R.id.item_button);
            state = (TextView) view.findViewById(R.id.item_state);
            statusInfo = (TextView) view.findViewById(R.id.item_status_info);
            progressDownload = (ProgressBar) view.findViewById(R.id.item_progress_download);
        }
    }
}
