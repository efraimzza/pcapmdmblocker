package com.emanuelef.remote_capture.activities;

import android.app.Activity;
import android.os.Bundle;
import com.emanuelef.remote_capture.R;
import com.emanuelef.remote_capture.Utils;
import android.widget.Switch;
import android.widget.Button;
import android.view.View.OnClickListener;
import android.view.View;
import android.preference.PreferenceManager;
import android.content.SharedPreferences;
import android.widget.CompoundButton;

public class AccessActivity extends Activity {
    
    Switch swenaccess, swdistatus, swdischannel, swdisforwardchannel;
    Button busave;
    SharedPreferences sp;
    SharedPreferences.Editor spe;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Utils.setTheme(this);
        setContentView(R.layout.activity_accessibility);
        try{
            if(getActionBar().isShowing())
                getActionBar().hide();
        }catch(Exception e){}
        swenaccess=findViewById(R.id.swenaccess);
        swdistatus=findViewById(R.id.swdistatus);
        swdischannel=findViewById(R.id.swdischannel);
        swdisforwardchannel=findViewById(R.id.swdisforwardchannel);
        busave=findViewById(R.id.btn_save_access);
        try{
            sp= PreferenceManager.getDefaultSharedPreferences(this);
            spe=sp.edit();
            busave.setOnClickListener(new OnClickListener(){
                    @Override
                    public void onClick(View p1) {
                        PasswordManager.requestPasswordAndSave(new Runnable() {
                                @Override
                                public void run() {
                                    try{
                                        if(swenaccess.isChecked()){
                                            //check is accessibility working? & save all
                                            spe.putBoolean("accessEnabled",swenaccess.isChecked())
                                                .putBoolean("distatus",swdistatus.isChecked())
                                                .putBoolean("dischannel",swdischannel.isChecked())
                                                .putBoolean("disforwardchannel",swdisforwardchannel.isChecked())
                                                .commit();
                                            //disable to refresh new policies
                                            if(accser.sinsta!=null){
                                                accser.sinsta.disableSelf();
                                            }else{
                                                accser.refreshacc.refreshacc(getApplicationContext());
                                            }
                                        }else{
                                            //close
                                            spe.putBoolean("accessEnabled",swenaccess.isChecked()).commit();
                                            if(accser.sinsta!=null)accser.sinsta.disableSelf();
                                        }
                                    }catch(Throwable t){LogUtil.logToFile(t.toString());}
                                }
                            },AccessActivity.this);
                        
                    }
                });
            swenaccess.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener(){
                    @Override
                    public void onCheckedChanged(CompoundButton p1, boolean isChecked) {
                        swdistatus.setEnabled(isChecked);
                        swdischannel.setEnabled(isChecked);
                        swdisforwardchannel.setEnabled(isChecked);
                    
                    }
                });
            swenaccess.setChecked(sp.getBoolean("accessEnabled",false));
            
            swdistatus.setChecked(sp.getBoolean("distatus",true));
            swdischannel.setChecked(sp.getBoolean("dischannel",true));
            swdisforwardchannel.setChecked(sp.getBoolean("disforwardchannel",false));
            
            swdistatus.setEnabled(swenaccess.isChecked());
            swdischannel.setEnabled(swenaccess.isChecked());
            swdisforwardchannel.setEnabled(swenaccess.isChecked());
        }catch(Throwable t){}
    }
    
    
    
}
