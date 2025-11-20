package com.emanuelef.remote_capture.views;

import android.content.Context;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.view.View;
import java.util.ArrayList;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewGroup;
import android.view.MotionEvent;

public class chipgrp extends LinearLayout{
    public chipgrp(android.content.Context context){
        super(context);
        setOrientation(VERTICAL);
    }
    
    public chipgrp(android.content.Context context, android.util.AttributeSet attrs) {
        super(context,attrs);
        setOrientation(VERTICAL);
    }

    @Override
    public void setOrientation(int orientation) {
        super.setOrientation(orientation);
    }

    @Override
    public int getChildCount() {
        return ararb.size();
    }
    
    void uncheckall(){
        for(RadioButton rb:ararb){
            rb.setChecked(false);
        }
    }
    ArrayList<RadioButton> ararb=new ArrayList<>();
    int lastid=-1;
    int iid=0;
    @Override
    public void addView(View child, int index, ViewGroup.LayoutParams params) {
        
        if(child instanceof RadioButton){
            super.addView(child, index, params);
            //child.setId(child.generateViewId());
            ararb.add((RadioButton)child);
            ((RadioButton)child).setOnCheckedChangeListener(new OnCheckedChangeListener(){
                    @Override
                    public void onCheckedChanged(CompoundButton p1, boolean p2) {
                        //LogUtil.logToFile("ch,iid="+iid);
                        iid=0;
                        if(p2){
                            //LogUtil.logToFile("l-"+lastid+"pi="+p1.getId());
                            lastid=p1.getId();
                            for(RadioButton rb:ararb){
                                if(rb.getId()!=p1.getId()){
                                    rb.setChecked(!p2);
                                }
                            }
                        }
                    }
                });
            
           
            ((RadioButton)child).setOnClickListener(new OnClickListener(){

                    @Override
                    public void onClick(View p1) {
                        //LogUtil.logToFile("l-"+lastid+"pi="+p1.getId()+"iid="+iid);
                        if(lastid==p1.getId() &&iid++==1){
                            //LogUtil.logToFile("curiid="+iid);
                                ((RadioButton)p1).setChecked(false);
                                lastid=-1;
                        }
                        //LogUtil.logToFile("eiid="+iid);
                    }
                });
        }
    }
    

    @Override
    public void addView(View child) {
        if(child instanceof RadioButton){
            super.addView(child);
            child.setId(child.generateViewId());
            ararb.add((RadioButton)child);
            ((RadioButton)child).setOnCheckedChangeListener(new OnCheckedChangeListener(){
                    @Override
                    public void onCheckedChanged(CompoundButton p1, boolean p2) {
                        //LogUtil.logToFile("ch,iid="+iid);
                        iid=0;
                        if(p2){
                            //LogUtil.logToFile("l-"+lastid+"pi="+p1.getId());
                            lastid=p1.getId();
                            for(RadioButton rb:ararb){
                                if(rb.getId()!=p1.getId()){
                                    rb.setChecked(!p2);
                                }
                            }
                        }
                    }
                });


            ((RadioButton)child).setOnClickListener(new OnClickListener(){

                    @Override
                    public void onClick(View p1) {
                        //LogUtil.logToFile("l-"+lastid+"pi="+p1.getId()+"iid="+iid);
                        if(lastid==p1.getId() &&iid++==1){
                            //LogUtil.logToFile("curiid="+iid);
                            ((RadioButton)p1).setChecked(false);
                            lastid=-1;
                        }
                        //LogUtil.logToFile("eiid="+iid);
                    }
                });
        }
    }
    

}
