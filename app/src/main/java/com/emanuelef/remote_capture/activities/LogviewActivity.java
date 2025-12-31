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

package com.emanuelef.remote_capture.activities;

import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
/*
import androidx.annotation.NonNull;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;
*/
import com.emanuelef.remote_capture.Log;
import com.emanuelef.remote_capture.R;
import com.emanuelef.remote_capture.Utils;
import com.emanuelef.remote_capture.fragments.LogviewFragment;
//import com.google.android.material.tabs.TabLayout;
//import com.google.android.material.tabs.TabLayoutMediator;
import android.annotation.NonNull;
import android.app.ActionBar;
import android.app.Fragment;
import android.app.FragmentTransaction;
import java.io.FileWriter;
import java.io.IOException;

public class LogviewActivity extends BaseActivity  {
    private static final String TAG = "LogviewActivity";
  //  private ViewPager2 mPager;
  //  private StateAdapter mPagerAdapter;

    private static final int POS_APP_LOG = 0;
    private static final int POS_ROOT_LOG = 1;
    private static final int POS_MITM_LOG = 2;
    private static final int NUM_POS = 3;
    
    LogviewFragment curfra;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Utils.setTheme(this);
        setTitle(R.string.app_log);
        setContentView(R.layout.fragment_activity);
      //  addMenuProvider(this);

     //   mPager = findViewById(R.id.pager);
    //    Utils.fixViewPager2Insets(mPager);
        setupTabs();
    }

    private void setupTabs() {
       /* mPagerAdapter = new StateAdapter(this);
        mPager.setAdapter(mPagerAdapter);

        var tabLayout = (TabLayout) findViewById(R.id.tablayout);
        Utils.fixScrollableTabLayoutInsets(tabLayout);
        new TabLayoutMediator(tabLayout, mPager, (tab, position) ->
                tab.setText(getString(mPagerAdapter.getPageTitle(position)))
        ).attach();*/
        try{
            maddtab(LogviewFragment.newInstance(getFilesDir().getAbsolutePath() + "/" + Log.DEFAULT_LOGGER_PATH),getText(R.string.app));
            maddtab(LogviewFragment.newInstance(getFilesDir().getAbsolutePath() + "/" + Log.MITM_LOGGER_PATH),getText(R.string.mitm_addon));
            maddtab(LogviewFragment.newInstance("/storage/emulated/0/log.txt"),"log");
            getActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        }catch(Exception e){}
    }
    private ActionBar.Tab maddtab(final Fragment f,CharSequence tname){
        ActionBar a=getActionBar();
        ActionBar.Tab ta=a.newTab().setText(tname);
        ta.setTabListener(new ActionBar.TabListener(){

                @Override
                public void onTabSelected(ActionBar.Tab p1, FragmentTransaction p2) {
                    try{
                        //LogUtil.logToFile("comt="+ 
                        getFragmentManager().beginTransaction().replace(R.id.linfra,f).commit();
                        curfra=(LogviewFragment) f;
                        //p2.replace(R.id.linfrapag,new FirewallStatus()).commit();
                    }catch(Exception e){
                        LogUtil.logToFile(e.toString());
                    }
                }

                @Override
                public void onTabUnselected(ActionBar.Tab p1, FragmentTransaction p2) {
                }

                @Override
                public void onTabReselected(ActionBar.Tab p1, FragmentTransaction p2) {
                    //p2.replace(R.id.linfrapag,new FirewallStatus()).commit();
                }
            });
        a.addTab(ta);
        return ta;
    }
/*
    private static class StateAdapter extends FragmentStateAdapter {
        final String mCacheDir;

        StateAdapter(final FragmentActivity fa) {
            super(fa);
            mCacheDir = fa.getCacheDir().getAbsolutePath();
        }

        @NonNull
        @Override
        public Fragment createFragment(int pos) {
            switch (pos) {
                case POS_APP_LOG:
                    return LogviewFragment.newInstance(mCacheDir + "/" + Log.DEFAULT_LOGGER_PATH);
                case POS_ROOT_LOG:
                    return LogviewFragment.newInstance(mCacheDir + "/" + Log.ROOT_LOGGER_PATH);
                case POS_MITM_LOG:
                default:
                    return LogviewFragment.newInstance(mCacheDir + "/" + Log.MITM_LOGGER_PATH);
            }
        }

        @Override
        public int getItemCount() {
            return NUM_POS;
        }

        public int getPageTitle(final int pos) {
            switch (pos) {
                case POS_APP_LOG:
                    return R.string.app;
                case POS_ROOT_LOG:
                    return R.string.root;
                case POS_MITM_LOG:
                default:
                    return R.string.mitm_addon;
            }
        }
    }*/

   // @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // This is required to properly handle the DPAD down press on Android TV, to properly
        // focus the tab content
    /*    if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
            View view = getCurrentFocus();

            Log.d(TAG, "onKeyDown focus " + view.getClass().getName());

            if (view instanceof TabLayout.TabView) {
                int pos = mPager.getCurrentItem();
                View focusOverride = null;

                Log.d(TAG, "TabLayout.TabView focus pos " + pos);

                focusOverride = findViewById(R.id.scrollView);

                if (focusOverride != null) {
                    focusOverride.requestFocus();
                    return true;
                }
            }
        }*/

        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.log_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if(curfra == null)
            return false;

        String logText = curfra.getLog();

        if(id == R.id.reload) {
            curfra.reloadLog();
            return true;
        } else if(id == R.id.copy_to_clipboard) {
            Utils.copyToClipboard(this, logText);
            return true;
        } else if(id == R.id.share) {
            Utils.shareText(this, getString(R.string.app_log), logText);
            return true;
        } else if(id == R.id.clear) {
            try {
                FileWriter writer = new FileWriter(curfra.getLogPath(), false);
                writer.write("\n");
                writer.close();
            } catch (IOException e) {
                // silent
            }
            curfra.reloadLog();
            return true;
        }

        return false;
    }
    

   // @Override
    public void onCreateMenu(@NonNull Menu menu, MenuInflater inflater) {
      //  inflater.inflate(R.menu.log_menu, menu);
    }

    //@Override
    public boolean onMenuItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
     /*   LogviewFragment fragment = (LogviewFragment) getFragmentAtPos(mPager.getCurrentItem());
        if(fragment == null)
            return false;

        String logText = fragment.getLog();

        if(id == R.id.reload) {
            fragment.reloadLog();
            return true;
        } else if(id == R.id.copy_to_clipboard) {
            Utils.copyToClipboard(this, logText);
            return true;
        } else if(id == R.id.share) {
            Utils.shareText(this, getString(R.string.app_log), logText);
            return true;
        }*/

        return false;
    }
}
