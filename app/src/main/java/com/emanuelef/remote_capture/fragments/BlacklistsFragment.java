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

package com.emanuelef.remote_capture.fragments;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import android.annotation.NonNull;
import android.annotation.Nullable;
/*
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;
*/
import com.emanuelef.remote_capture.CaptureService;
import com.emanuelef.remote_capture.Log;
import com.emanuelef.remote_capture.PCAPdroid;
import com.emanuelef.remote_capture.R;
import com.emanuelef.remote_capture.Utils;
import com.emanuelef.remote_capture.adapters.BlacklistsAdapter;
import com.emanuelef.remote_capture.interfaces.BlacklistsStateListener;
import com.emanuelef.remote_capture.Blacklists;
import com.emanuelef.remote_capture.model.BlacklistDescriptor;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.widget.AdapterView;
import android.widget.Adapter;
import android.app.Fragment;
import com.obsex.obseobj;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import com.emanuelef.remote_capture.activities.LogUtil;
import android.app.Activity;
import com.emanuelef.remote_capture.activities.picker;
import com.emanuelef.remote_capture.activities.PasswordManager;
import android.preference.PreferenceManager;
import android.content.SharedPreferences;
import android.app.AlertDialog;
import android.widget.LinearLayout;
import android.widget.EditText;
import android.content.DialogInterface;
import android.widget.Toast;

public class BlacklistsFragment extends Fragment implements BlacklistsStateListener {
    private static final String TAG = "BlacklistsFragment";
    private static BlacklistsAdapter mAdapter;
    private static Blacklists mBlacklists;
    private static MenuItem mUpdateItem;
    private static Handler mHandler;
    
    public static String blpickedfilepath="";
    public static String reqbl="";
    
    static View mvi;
    static ListView listView;
    static Activity act;
    private SharedPreferences mPrefs;
    
    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        //requireActivity().addMenuProvider(this, getViewLifecycleOwner(), Lifecycle.State.RESUMED);
        return inflater.inflate(R.layout.malware_detection_blacklists, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        setHasOptionsMenu(true);
        mPrefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        mvi=view;
        mBlacklists = PCAPdroid.getInstance().getBlacklists();
        mAdapter = new BlacklistsAdapter(view.getContext(), PCAPdroid.getInstance().getBlacklists().iter());
        listView = view.findViewById(R.id.listview);
        listView.setAdapter(mAdapter);
        listView.setOnItemClickListener(new ListView.OnItemClickListener() {

                @Override
                public void onItemClick(AdapterView<?> p1, View view1, int position, long p4) {
                    final BlacklistDescriptor bl = mAdapter.getItem(position);
                    if (bl != null){
                        if(bl.url.equals("manual")){
                            PasswordManager.requestPasswordAndSave(new Runnable(){

                                    @Override
                                    public void run() {
                                        reqbl=bl.fname;
                                        blpickedfilepath="";
                                        Intent intent=new Intent(getActivity(),picker.class).putExtra("from","blf");
                                        startActivity(intent);  
                                    }
                                },getActivity());
                        }else if(bl.url.equals("manualink")){

                            String murl="";
                            String mnurl="";
                            if(bl.fname.equals("manualdomlink.txt")){
                                mnurl="manualdomlink";
                                murl= mPrefs.getString(mnurl, "");

                            }else if(bl.fname.equals("manualiplink.txt")){
                                mnurl="manualiplink";
                                murl= mPrefs.getString(mnurl, "");
                            }
                            final String resmnurl=mnurl;
                            final String resmurl=murl;
                            //dialog with edtx to select link
                            LinearLayout linl=new LinearLayout(getContext());
                            final EditText edtx=new EditText(getContext());
                            edtx.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,LinearLayout.LayoutParams.WRAP_CONTENT));
                            edtx.setText(murl);
                            edtx.setHint("קישור");
                            linl.addView(edtx);
                            AlertDialog.Builder ad=new AlertDialog.Builder(getContext())
                                .setView(linl)
                                .setPositiveButton("אישור", new DialogInterface.OnClickListener(){

                                    @Override
                                    public void onClick(DialogInterface p1, int p2) {

                                        if(!edtx.getText().toString().equals("")&&!edtx.getText().toString().equals(resmurl)){
                                            PasswordManager.requestPasswordAndSave(new Runnable(){
                                                    @Override
                                                    public void run() {
                                                        mPrefs.edit().putString(resmnurl, edtx.getText().toString()).commit();
                                                        Toast.makeText(getContext().getApplicationContext(),"הקישור עודכן!",1).show();
                                                    }
                                                },getActivity());
                                        }

                                    }
                                })
                                .setNegativeButton("ביטול", null);
                            ad.show();

                        } else{
                            openUrl(view1.getContext(), bl.url);
                        }
                    }
        }});
        mHandler = new Handler(Looper.getMainLooper());
        obseobj ob = new obseobj() {
            @Override
            public void update(Object arg) {
                refreshStatus();
            }
        };
        
        CaptureService.observeStatus(ob);
        //CaptureService.observeStatus(this, serviceStatus -> refreshStatus());
    }
    public static void refreshbl(){
        mBlacklists = PCAPdroid.getInstance().getBlacklists();
        try{
        mAdapter = new BlacklistsAdapter(mvi.getContext(), PCAPdroid.getInstance().getBlacklists().iter());
        listView.setAdapter(mAdapter);
        listView.setOnItemClickListener(new ListView.OnItemClickListener() {

                @Override
                public void onItemClick(AdapterView<?> p1, View view1, int position, long p4) {
                    final BlacklistDescriptor bl = mAdapter.getItem(position);
                    if (bl != null){
                        if(bl.url.equals("manual")){
                            PasswordManager.requestPasswordAndSave(new Runnable(){

                                    @Override
                                    public void run() {
                                        reqbl=bl.fname;
                                        blpickedfilepath="";
                                        Intent intent=new Intent(act,picker.class).putExtra("from","blf");
                                        act.startActivity(intent);  
                                    }
                                },act);
                        }else
                            openUrl(view1.getContext(), bl.url);
                    }
                }});
        mHandler = new Handler(Looper.getMainLooper());
        obseobj ob = new obseobj() {
            @Override
            public void update(Object arg) {
                refreshStatus();
            }
        };

        CaptureService.observeStatus(ob);
        }catch(Exception e){
            LogUtil.logToFile(e.toString());
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        act=getActivity();
    }
    
    @Override
    public void onStart() {
        super.onStart();
        
        //LogUtil.logToFile("started");
        if(!blpickedfilepath.equals("")){
            String picked=blpickedfilepath;
            blpickedfilepath="";
            if(reqbl.equals("manualdom.txt")){
                //copying dom.txt for the first check

                try{
                    FileInputStream in = new FileInputStream(picked);
                    FileOutputStream out= new FileOutputStream(getListPath(reqbl));
                    byte[] bytesIn = new byte[4096];
                    int read;
                    while((read = in.read(bytesIn)) != -1)
                        out.write(bytesIn, 0, read);
                }catch(Exception e){
                    LogUtil.logToFile(reqbl+picked+ e.toString());
                }
            }else if(reqbl.equals("manualip.txt")){
                //copying ip.txt for the first check
                try{
                    FileInputStream in = new FileInputStream(picked);
                    FileOutputStream out= new FileOutputStream(getListPath(reqbl));
                    byte[] bytesIn = new byte[4096];
                    int read;
                    while((read = in.read(bytesIn)) != -1)
                        out.write(bytesIn, 0, read);
                }catch(Exception e){
                    LogUtil.logToFile(reqbl+picked+ e.toString());
                }
            }
            CaptureService.requestBlacklistsUpdate();
            //self select your file
            //bl.setUpdated(System.currentTimeMillis());
            //notifyListeners();
        }
    }
    
    
    
    
    
    private String getListPath(String mreqbl) {
        return getContext().getFilesDir().getPath() + "/malware_bl/" + mreqbl;
    }
    private static void openUrl(Context ctx, String url) {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        Utils.startActivity(ctx, intent);
    }

    @Override
    public void onResume() {
        super.onResume();
        mBlacklists.addOnChangeListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        mBlacklists.removeOnChangeListener(this);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        //super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.blacklists_menu, menu);
        mUpdateItem = menu.findItem(R.id.update);
        refreshStatus();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        
        if(id == R.id.update) {
            CaptureService.requestBlacklistsUpdate();
            return true;
        }

        return false;
    }
    
   // @Override
    public void onCreateMenu(@NonNull Menu menu, MenuInflater inflater) {
        //inflater.inflate(R.menu.blacklists_menu, menu);
      //  mUpdateItem = menu.findItem(R.id.update);
        refreshStatus();
    }

    //@Override
    public boolean onMenuItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

      /*  if(id == R.id.update) {
            CaptureService.requestBlacklistsUpdate();
            return true;
        }*/

        return false;
    }

    private static void refreshStatus() {
        if(mAdapter != null)
            mAdapter.notifyDataSetChanged();

        if(mUpdateItem != null) {
            mUpdateItem.setVisible(CaptureService.isServiceActive());
            mUpdateItem.setEnabled(!mBlacklists.isUpdateInProgress());
        }
    }

    @Override
    public void onBlacklistsStateChanged() {
        Log.d(TAG, "onBlacklistsStateChanged");
        mHandler.post(new Runnable(){
                @Override
                public void run() {
            refreshStatus();}});
    }
}
