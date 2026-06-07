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
 * Copyright 2020-22 - Emanuele Faranda
 */

package com.emanuelef.remote_capture.activities.prefs;

import android.os.Bundle;

import com.emanuelef.remote_capture.R;
import com.emanuelef.remote_capture.activities.BaseActivity;
import com.emanuelef.remote_capture.fragments.prefs.PortMapFragment;
import android.view.MenuItem;
import android.widget.Switch;
import android.view.Menu;
import android.view.MenuInflater;
import com.emanuelef.remote_capture.model.Prefs;
import android.preference.PreferenceManager;
import android.widget.CompoundButton;
import android.content.SharedPreferences;
import com.emanuelef.remote_capture.Log;
import android.app.Fragment;
import com.emanuelef.remote_capture.activities.LogUtil;

public class PortMapActivity extends BaseActivity {
    PortMapFragment portma;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(R.string.port_mapping);
        setContentView(R.layout.fragment_activity);
        portma=new PortMapFragment();
        getFragmentManager().beginTransaction()
         .add(R.id.linfra,portma )
        .commit();
      //  getSupportFragmentManager().beginTransaction()
               // .replace(R.id.fragment, new PortMapFragment())
                //.commit();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.port_mapping_menu, menu);
try{
        /*Switch toggle = (Switch) menu.findItem(R.id.toggle_btn).getActionView();
        toggle.setChecked(Prefs.isPortMappingEnabled(PreferenceManager.getDefaultSharedPreferences(portma.getActivity())));
        toggle.setOnCheckedChangeListener(new Switch.OnCheckedChangeListener() {

                @Override
                public void onCheckedChanged(CompoundButton p1, boolean isChecked) {
                    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(portma.getActivity());

                    if(isChecked == Prefs.isPortMappingEnabled(prefs))
                        return; // not changed

                    String TAG = "portmap";
                    Log.d(TAG, "Port mapping is now " + (isChecked ? "enabled" : "disabled"));
                    Prefs.setPortMappingEnabled(prefs, isChecked);
                }});*/
                }catch(Exception e){
                    LogUtil.logToFile(e.toString());
                }
        return true;
    }
    
   

    @Override
    public boolean onOptionsItemSelected( MenuItem menuItem) {
        //public boolean onMenuItemSelected( MenuItem menuItem) {
        if(menuItem.getItemId() == R.id.add_mapping) {
            try{
            portma.openAddDialog();
            }catch(Exception e){
                LogUtil.logToFile(e.toString());
            }
            return true;
        }

        return false;
    }
}
