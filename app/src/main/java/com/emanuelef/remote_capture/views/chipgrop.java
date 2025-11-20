package com.emanuelef.remote_capture.views;

import android.widget.LinearLayout;
import android.view.ViewGroup.LayoutParams;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import java.util.ArrayList;

public class chipgrop extends LinearLayout{
    /*
     public chipgrop(android.content.Context context){
     super(context);
     setOrientation(VERTICAL);
     }*/

    public chipgrop(android.content.Context context, android.util.AttributeSet attrs) {
        super(context,attrs);
        setOrientation(VERTICAL);
    }

    @Override
    public void setOrientation(int orientation) {
        super.setOrientation(orientation);
    }
    /*
     @Override
     public void addView(View child, int width, int height) {
     super.addView(child, width, height);
     }

     @Override
     public void addView(View child) {
     super.addView(child);
     }

     @Override
     public void addView(View child, ViewGroup.LayoutParams params) {
     super.addView(child, params);
     }

     @Override
     public void addView(View child, int index) {
     super.addView(child, index);
     }*/

    ArrayList<chipbu> aracb=new ArrayList<>();
    @Override
    public void addView(View child, int index, ViewGroup.LayoutParams params) {
        if(child instanceof chipbu){
            super.addView(child, index, params);
            aracb.add((chipbu)child);
            ((chipbu)child).setonchange(new chipbu.myl(){

                    @Override
                    public void onClick(chipbu p1, boolean p2) {
                        for(chipbu rb:aracb){
                            if(rb.getId()!=p1.getId()){
                                rb.setChecked(!p2);
                            }
                        }
                    }
                });
        }

    }



}
