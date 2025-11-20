package com.emanuelef.remote_capture.views;

import android.widget.Button;
import android.graphics.Color;
import android.view.View.OnClickListener;
import android.view.View;
import com.emanuelef.remote_capture.R;

public class chipbu extends Button{
    /*
     public chipbu(android.content.Context context) {
     super(context);
     }
     */
    public chipbu(android.content.Context context, android.util.AttributeSet attrs) {
        super(context,attrs);
        setBackgroundResource(R.drawable.red_button_background);
        //setBackgroundColor(Color.parseColor("#FFD52424"));
        this.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View p1) {
                    //LogUtil.logToFile("uu");
                    setChecked(!ischecked);
                }
            });
    }
    /*
     public chipbu(android.content.Context context, android.util.AttributeSet attrs, int defStyleAttr) {
     super(context,attrs,defStyleAttr);
     }

     public chipbu(android.content.Context context, android.util.AttributeSet attrs, int defStyleAttr, int defStyleRes) {
     super(context,attrs,defStyleAttr,defStyleRes);
     }*/
    boolean ischecked=false;
    public void setChecked(boolean p0) {
        if(p0)
            ll.onClick(this,p0);
        ischecked=p0;
        if(p0){
            setBackgroundResource(R.drawable.green_button_background);
            //setBackgroundColor(Color.parseColor("#FF288052"));
        }else{
            setBackgroundResource(R.drawable.red_button_background);
            //setBackgroundColor(Color.parseColor("#FFD52424"));
        }
    }
    public boolean getChecked() {
        return ischecked;
    }
    public boolean isChecked() {
        return ischecked;
    }
    myl ll=new myl(){

        @Override
        public void onClick(chipbu p1, boolean p2) {
        }
    };
    public void setonchange(myl l){
        ll=l;
    }
    public static abstract interface myl
    {
        public abstract void onClick(chipbu p1,boolean p2);

    }

    
}
