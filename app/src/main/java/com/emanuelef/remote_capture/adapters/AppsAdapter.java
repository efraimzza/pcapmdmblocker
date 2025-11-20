/*
 * This file is part of PCAPdroid.
 *
 * PCAPdroid is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * PCAPdroid is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with PCAPdroid.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Copyright 2020-21 - Emanuele Faranda
 */

package com.emanuelef.remote_capture.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
/*
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
*/
import com.emanuelef.remote_capture.model.AppDescriptor;
import com.emanuelef.remote_capture.R;

import java.util.List;
import android.annotation.NonNull;
import android.widget.ArrayAdapter;
import com.emanuelef.remote_capture.activities.LogUtil;

public class AppsAdapter extends ArrayAdapter<AppDescriptor> {
    private final LayoutInflater mLayoutInflater;
    private View.OnClickListener mListener;
    private List<AppDescriptor> listStorage;

    public AppsAdapter(Context context, List<AppDescriptor> customizedListView) {
        super(context,R.layout.app_installed_item);
        mLayoutInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        listStorage = customizedListView;
        mListener = null;
    }

    public static class AppViewHolder  {
        TextView textInListView;
        ImageView imageInListView;
        TextView packageInListView;

        public AppViewHolder(View view) {
          //  super(view);
          try{
            textInListView = view.findViewById(R.id.app_name);
            imageInListView = view.findViewById(R.id.app_icon);
            packageInListView= view.findViewById(R.id.app_package);
            }catch(Exception e){
                LogUtil.logToFile(e.toString());
            }
        }
    }

 /*   @NonNull
    @Override
    public AppsAdapter.AppViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = mLayoutInflater.inflate(R.layout.app_installed_item, parent, false);
        AppViewHolder recyclerViewHolder = new AppViewHolder(view);

        if(mListener != null)
            view.setOnClickListener(mListener);

        return(recyclerViewHolder);
    }*/

  //  @Override
   /* public void onBindViewHolder(@NonNull AppViewHolder holder, int position) {
        AppDescriptor app = getItem(position);

        holder.textInListView.setText(app.getName());
        holder.packageInListView.setText(app.getPackageName());

        if(app.getIcon() != null)
            holder.imageInListView.setImageDrawable(app.getIcon());
    }*/

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if(convertView==null)
            convertView=mLayoutInflater.inflate(R.layout.app_installed_item, parent, false);
        AppViewHolder holder = new AppViewHolder(convertView);
        AppDescriptor app = getItem(position);

        holder.textInListView.setText(app.getName());
        holder.packageInListView.setText(app.getPackageName());

        if(app.getIcon() != null)
            holder.imageInListView.setImageDrawable(app.getIcon());
   
        //LogUtil.logToFile(app.getName()+app.getPackageName());
        if(mListener != null)
            convertView.setOnClickListener(mListener);
        return convertView;
    }
    
 //   @Override
    public int getItemCount() {
        return listStorage.size();
    }

    @Override
    public int getCount() {
        return listStorage.size();
    }
    
    public AppDescriptor getItem(int pos) {
        if((pos < 0) || (pos > listStorage.size()))
            return null;

        return listStorage.get(pos);
    }

    public void setApps(List<AppDescriptor> apps) {
        listStorage = apps;
        notifyDataSetChanged();
        //LogUtil.logToFile("si="+apps.size()+"c="+getCount());
    }

    public void setOnClickListener(final View.OnClickListener listener) {
        mListener = listener;
    }
}
