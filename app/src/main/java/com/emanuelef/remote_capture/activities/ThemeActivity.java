package com.emanuelef.remote_capture.activities;

import android.app.Activity;
import android.os.Bundle;
import com.emanuelef.remote_capture.R;
import android.preference.PreferenceFragment;
import android.view.View;
import android.annotation.Nullable;
import android.annotation.NonNull;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.content.SharedPreferences;
import android.preference.Preference;
import android.content.Intent;

public class ThemeActivity extends BaseActivity {
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(R.string.title_activity_settings); // note: setting via manifest does not honor custom locale
        displayBackAction();

        setContentView(R.layout.fragment_activity);
        getFragmentManager()
            .beginTransaction()
            .replace(R.id.linfra,new ThemeFragment())
            .commit();
    }
    public static class ThemeFragment extends PreferenceFragment {

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.theme_preferences);
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
        }
        
        
        @Override
        public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);
            
            final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
            final PreferenceScreen screen = getPreferenceScreen();
            Preference pref_screen_theme = screen.findPreference("theme");
            String theme = prefs.getString("theme", "modern");
            String[] themeNames = getResources().getStringArray(R.array.themeNames);
            String[] themeValues = getResources().getStringArray(R.array.themeValues);
            for (int i = 0; i < themeNames.length; i++)
                if (theme.equals(themeValues[i])) {
                    pref_screen_theme.setTitle(getString(R.string.setting_theme, themeNames[i]));
                    break;
                }
            String dtheme = prefs.getString("dark_theme", "dark");
            String[] dthemeNames = getResources().getStringArray(R.array.dthemeNames);
            String[] dthemeValues = getResources().getStringArray(R.array.dthemeValues);
            for (int i = 0; i < dthemeNames.length; i++)
                if (dtheme.equals(dthemeValues[i])) {
                    screen.findPreference("dark_theme").setTitle(getString(R.string.setting_dtheme, dthemeNames[i]));
                    break;
                }
            pref_screen_theme.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener(){
                    @Override
                    public boolean onPreferenceChange(Preference p1, Object newValue) {
                        if (prefs.edit().putString("theme", newValue.toString()).commit()) {
                            Intent intent = new Intent(getContext(), MDMStatusActivity.class);
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                            Runtime.getRuntime().exit(0);
                        }
                        return false;

                    }
                });
            screen.findPreference("dark_theme").setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener(){
                    @Override
                    public boolean onPreferenceChange(Preference p1, Object newValue) {
                        if (prefs.edit().putString("dark_theme", newValue.toString()).commit()) {
                            Intent intent = new Intent(getContext(), MDMStatusActivity.class);
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                            Runtime.getRuntime().exit(0);
                        }
                        return false;

                    }
                });
            
        }

    }
}
