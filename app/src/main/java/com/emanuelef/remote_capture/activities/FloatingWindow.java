package com.emanuelef.remote_capture.activities;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.IBinder;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.webkit.WebView.FindListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.Toast;
import android.view.ViewGroup.LayoutParams;
import android.net.Uri;
import android.os.Build;
import android.preference.PreferenceManager;
import android.content.SharedPreferences;
import java.io.File;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.graphics.Matrix;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import java.io.FileInputStream;
import com.emanuelef.remote_capture.R;
import java.io.FileOutputStream;
import java.io.InputStream;

public class FloatingWindow extends Service {
	//public static final String MY_SERVICE = "tk.eatheat.floatingexample.FlyBitch";

	private WindowManager windowManager;
	private ImageView chatHead;
	private Boolean _enable = true;
    int strength=30;
    int scale=64;
	@Override
	public IBinder onBind(Intent intent) {
		 _enable=false;
		return null;
	}
    
    @Override
    public void onStart(Intent intent, int startId) {
        super.onStart(intent, startId);
        try{
        try{
            String dst=getFilesDir().getPath() + "/" + "a.png";
            boolean mforce=false;
            if (!new File(dst).exists()||mforce) {
                byte[] bb=new byte[1024];
                InputStream is=getAssets().open("ic_water.png");
                FileOutputStream fop=new FileOutputStream(new File(dst));
                while (is.read(bb) > 0) {
                    fop.write(bb);
                }
                is.close();
                fop.close();
            }
        }catch(Throwable t){LogUtil.logToFile(t);}
        if(intent!=null){
        strength= intent.getIntExtra("strength",30);
        scale= intent.getIntExtra("scale",64);
        }
        final SharedPreferences sp= PreferenceManager.getDefaultSharedPreferences(FloatingWindow.this);
        try{
            boolean mvw=sp.getBoolean("moveWindow",true);
            windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

            chatHead = new ImageView(this);

            //chatHead.setImageResource(R.drawable.circle);
            String mpath="/storage/emulated/0/picture.jpeg";
            mpath = getFilesDir() + "/" + "a.png";
            Uri u= Uri.fromFile(new File(mpath));
            //chatHead.setImageURI(u);
            byte[] rawArt;
            Bitmap art = null;
            String fullPath="";
            //fullPath="/storage/emulated/0/b.png";
            fullPath = mpath;
            BitmapFactory.Options bfo=new BitmapFactory.Options();
            String hrr="" + fullPath;
            FileInputStream f=new FileInputStream("" + hrr);
            rawArt = new byte[f.available()];
            f.read(rawArt);
            if (null != rawArt) {art = BitmapFactory.decodeByteArray(rawArt, 0, rawArt.length, bfo);}
            if (rawArt == null) {
                Toast.makeText(getApplicationContext(), "not faund image!", Toast.LENGTH_SHORT).show();
                return ;}
            
            int hig=art.getHeight();
            int wig=art.getWidth();
            chatHead.setAlpha(strength);
            if(scale<1){scale=1;}
            float h=art.getHeight()/64*scale;
            float w=art.getWidth()/64*scale;
            art = art.createScaledBitmap(art,(int) w,(int) h, false);
            chatHead.setImageBitmap(art);
            LogUtil.logToFile("ho="+h+"wow="+w);
            
            chatHead.setLayoutParams(new ViewGroup.LayoutParams((int)w,(int)h));
            
            /*Matrix m= chatHead.getImageMatrix();
            m.setScale(3,scale,scale,scale);
            chatHead.setImageMatrix(m);*/
            final WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                (Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY : WindowManager.LayoutParams.TYPE_PHONE),
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE|
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED|
                ((mvw)?0: WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE),
                PixelFormat.A_8);

            params.gravity = Gravity.TOP | Gravity.LEFT;
            params.x = 0;
            params.y = 100;

            params.x = sp.getInt("fwx",0);
            params.y = sp.getInt("fwy",100);
            windowManager.addView(chatHead, params);
            chatHead.setOnClickListener(new OnClickListener() {
                    @Override public void onClick(View arg0) {
                        LayoutInflater layoutInflater   = (LayoutInflater)getBaseContext()
                            .getSystemService(LAYOUT_INFLATER_SERVICE);  
                        View popupView = layoutInflater.inflate(R.layout.wm_popup, null); 
                        final PopupWindow popupWindow = new PopupWindow( popupView, LayoutParams.WRAP_CONTENT,  
                                                                        LayoutParams.WRAP_CONTENT);  
                        // popupWindow.setFocusable(true);
                        popupWindow.update();

                        Button btnDismiss = (Button)popupView.findViewById(R.id.dismiss);
                        Button btnSaveLocation = (Button)popupView.findViewById(R.id.saveLocation);
                        Button endService= (Button) popupView.findViewById(R.id.endService);
                        endService.setOnClickListener(new OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    stopSelf();
                                    Toast.makeText(getApplicationContext(), "כבוי", Toast.LENGTH_LONG).show();
                                }
                            });
                        btnDismiss.setOnClickListener(new OnClickListener() {
                                @Override
                                public void onClick(View arg0) {
                                    Toast.makeText(getApplicationContext(), "דיאלוג הוסתר", Toast.LENGTH_LONG).show();
                                    popupWindow.dismiss();
                                    _enable=true;
                                }
                            });     
                        btnSaveLocation.setOnClickListener(new OnClickListener() {
                                @Override
                                public void onClick(View arg0) {
                                    sp.edit().putInt("fwx",params.x).putInt("fwy",params.y).commit();
                                    Toast.makeText(getApplicationContext(), "מיקום נשמר", Toast.LENGTH_LONG).show();
                                    popupWindow.dismiss();
                                    _enable=true;
                                }
                            });

                        if(_enable){
                            popupWindow.showAsDropDown(chatHead, 50, -30);
                            _enable=false;
                        }
                        /*else if(!_enable) {
                         //Toast.makeText(getApplicationContext(), " Popup Terminated", Toast.LENGTH_LONG).show();
                         //not working dismiss here sad :(    popupWindow.dismiss();
                         Log.d("FALSE", "FALSE");

                         }*/
                        //chatHead.setImageResource(R.drawable.ic_launcher);
                    }
                });
            chatHead.setOnLongClickListener(new OnLongClickListener(){
                    @Override public boolean onLongClick(View p1) {
                        sp.edit().putInt("fwx",params.x).putInt("fwy",params.y).commit();
                        Toast.makeText(getApplicationContext(), "מיקום נשמר", Toast.LENGTH_SHORT).show();
                        sp.edit().putBoolean("moveWindow",false).commit();
                        stopSelf();
                        try {
                            getApplicationContext().startService(new Intent(getApplicationContext(), FloatingWindow.class)
                                         .putExtra("strength",sp.getInt("strength",30))
                                         .putExtra("scale",sp.getInt("scale",64)));
                        } catch (Exception e) {}
                        return true;
                    }
                });
            try {

                chatHead.setOnTouchListener(new View.OnTouchListener() {
                        private WindowManager.LayoutParams paramsF = params;
                        private int initialX;
                        private int initialY;
                        private float initialTouchX;
                        private float initialTouchY;
                        @Override public boolean onTouch(View v, MotionEvent event) {
                            switch (event.getAction()) {
                                case MotionEvent.ACTION_DOWN:
                                    // Get current time in nano seconds.
                                    initialX = paramsF.x;
                                    initialY = paramsF.y;
                                    initialTouchX = event.getRawX();
                                    initialTouchY = event.getRawY();
                                    break;
                                case MotionEvent.ACTION_UP:
                                    LogUtil.logToFile("x="+paramsF.x+"y="+paramsF.y);
                                    break;
                                case MotionEvent.ACTION_MOVE:
                                    paramsF.x = initialX + (int) (event.getRawX() - initialTouchX);
                                    paramsF.y = initialY + (int) (event.getRawY() - initialTouchY);
                                    windowManager.updateViewLayout(chatHead, paramsF);
                                    break;
                            }
                            return false;
                        }
                    });
            } catch (Exception e) {
                LogUtil.logToFile(e);
            }
        }catch(Exception e){
            LogUtil.logToFile(e);
            startOverlayPermissionActivity();
        }
        }catch(Throwable t){LogUtil.logToFile(t);}
    }
    
	
    private void startOverlayPermissionActivity() {
        try{
            Intent intent = new Intent("android.settings.action.MANAGE_OVERLAY_PERMISSION");
            intent.setFlags(intent.FLAG_ACTIVITY_NEW_TASK);
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("package:");
            stringBuilder.append(getPackageName());
            intent.setData(Uri.parse(stringBuilder.toString()));
            startActivity(intent);
        }catch(Exception e){
            Toast.makeText(this, "" + e, Toast.LENGTH_LONG).show();
        }
    }
	@Override
	public void onDestroy() {
		super.onDestroy();
		if (chatHead != null) windowManager.removeView(chatHead);
	}

}
