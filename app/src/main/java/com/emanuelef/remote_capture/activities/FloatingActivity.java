package com.emanuelef.remote_capture.activities;

import android.annotation.Nullable;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.net.URISyntaxException;
import android.widget.TextView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.util.DisplayMetrics;
import android.view.WindowManager;
import android.graphics.Point;
import java.io.InputStream;
import com.emanuelef.remote_capture.R;
import com.emanuelef.remote_capture.Utils;
import android.widget.Switch;
import android.widget.CompoundButton;

public class FloatingActivity extends Activity {
    
    SeekBar sbleng;
    SharedPreferences sp;
    SharedPreferences.Editor spe;
    int mdeviwidth,mdeviheigth,mdeviwidthcal,mdeviheigthcal,gsize,ssize=0;
    
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        Utils.setTheme(this);
		setContentView(R.layout.wm_main);
        try{
            if(getActionBar().isShowing())
                getActionBar().hide();
        }catch(Exception e){}
        try{
        DisplayMetrics dm=new DisplayMetrics();
        Point poi=new Point();
        ((WindowManager) getApplicationContext().getSystemService(WINDOW_SERVICE)).getDefaultDisplay().getRealMetrics(dm);
        ((WindowManager) getApplicationContext().getSystemService(WINDOW_SERVICE)).getDefaultDisplay().getSize(poi);
        //Rect r=((WindowManager) getApplicationContext().getSystemService(WINDOW_SERVICE)).getCurrentWindowMetrics().getBounds();

        //Toast.makeText(getApplicationContext(), ""+poi.y+" "+dm.widthPixels+" "+dm.heightPixels+" "+poi.x+" "+poi.y, Toast.LENGTH_SHORT).show();
        //if(dm.widthPixels<=dm.heightPixels){
        mdeviwidth = dm.widthPixels;
        mdeviheigth = dm.heightPixels;
        sp= PreferenceManager.getDefaultSharedPreferences(this);
        spe=sp.edit();
        
		Bundle bundle = getIntent().getExtras();

		if(bundle != null && bundle.getString("LAUNCH").equals("YES")) {
			startService(new Intent(FloatingActivity.this, FloatingWindow.class));
		}

		Button launch = (Button)findViewById(R.id.button1);
		launch.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
                try {
                    stopService(new Intent(FloatingActivity.this, FloatingWindow.class));
                } catch (Exception e) {}
                spe.putBoolean("moveWindow",true).commit();
				startService(new Intent(FloatingActivity.this, FloatingWindow.class)
                .putExtra("strength",sp.getInt("strength",30))
                             .putExtra("scale",sp.getInt("scale",64)));
			}
		});
        Button launchnomove = (Button)findViewById(R.id.button3);
        launchnomove.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    try {
                        stopService(new Intent(FloatingActivity.this, FloatingWindow.class));
                    } catch (Exception e) {}
                    spe.putBoolean("moveWindow",false).commit();
                    startService(new Intent(FloatingActivity.this, FloatingWindow.class)
                    .putExtra("strength",sp.getInt("strength",30))
                                 .putExtra("scale",sp.getInt("scale",64)));
                }
            });
		Button stop = (Button)findViewById(R.id.button2);
		stop.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				stopService(new Intent(FloatingActivity.this, FloatingWindow.class));
			}
		});
		Button pickikmg = (Button)findViewById(R.id.button4);
        pickikmg.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    PasswordManager.requestPasswordAndSave(new Runnable() {
                            @Override
                            public void run() {
                    Intent imageIntent = new Intent();
                    imageIntent.setAction(Intent.ACTION_GET_CONTENT);
                    imageIntent.setType("image/*");
                    startActivityForResult(Intent.createChooser(imageIntent, "Select Image"), 438);

                }
            },FloatingActivity.this);
                }
            });
            
            
            Switch swflt=findViewById(R.id.swflt);
            swflt.setChecked(sp.getBoolean("fltstartauto",false));
            swflt.setOnCheckedChangeListener(new Switch.OnCheckedChangeListener(){
                    @Override
                    public void onCheckedChanged(CompoundButton p1, final boolean p2) {
                        PasswordManager.requestPasswordAndSave(new Runnable() {
                                @Override
                                public void run() {
                                    spe.putBoolean("fltstartauto",p2).commit();
                                }
                            },FloatingActivity.this);
                    }
                });
            
            Button btnclrdef = (Button)findViewById(R.id.btn_fltclrdef);
            btnclrdef.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        PasswordManager.requestPasswordAndSave(new Runnable() {
                                @Override
                                public void run() {

                        try{
                            String dst=getFilesDir().getPath() + "/" + "a.png";
                            byte[] bb=new byte[1024];
                            InputStream is=getAssets().open("ic_water.png");
                            FileOutputStream fop=new FileOutputStream(new File(dst));
                            while (is.read(bb) > 0) {
                                fop.write(bb);
                            }
                            is.close();
                            fop.close();
                        }catch(Throwable t){LogUtil.logToFile(t);}
                        spe.putInt("strength",20).
                            putInt("scale",20).
                            putInt("fwx",15).
                            putInt("fwy",15).
                            commit();
                        update();

                                }
                            },FloatingActivity.this);
                    }
                });
        LinearLayout linl=findViewById(R.id.linlmain);
        TextView tvst=new TextView(this);
        tvst.setText("חוזק");
        TextView tvscl=new TextView(this);
        tvscl.setText("גודל");
        linl.addView(tvst);
        linl.addView(sb("strength"));
        linl.addView(tvscl);
        linl.addView(sb("scale"));
        }catch(Throwable t){LogUtil.logToFile(t);}
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
	}
    private SeekBar sb(final String type){
        sbleng = new SeekBar(this);
        sbleng.setMin(1);
        sbleng.setMax(255);
        sbleng.setProgress(sp.getInt(type,20));
        sbleng.setOnSeekBarChangeListener(new OnSeekBarChangeListener(){

                @Override
                public void onProgressChanged(SeekBar p1, int p2, boolean p3) {

                    spe.putInt(type,p2).commit();
                    update();
                }

                @Override
                public void onStartTrackingTouch(SeekBar p1) {
                }

                @Override
                public void onStopTrackingTouch(SeekBar p1) {
                }
            });
        LinearLayout.LayoutParams sblp=new LinearLayout.LayoutParams(mdeviwidth, LinearLayout.LayoutParams.WRAP_CONTENT);
        sblp.setMargins(0, 20, 0, 0);

        sbleng.setLayoutParams(sblp);
        return sbleng;
    }
    void update() {
        int st=sbleng.getProgress();
        try {
            try {
                stopService(new Intent(this, FloatingWindow.class));
            } catch (Exception e) {}
            try {
                startService(new Intent(this, FloatingWindow.class)
                             .putExtra("strength",sp.getInt("strength",30))
                             .putExtra("scale",sp.getInt("scale",64)));
            } catch (Exception e) {}
        } catch (Exception e) {
            Toast.makeText(this, "" + e, 1).show();
        }
    }
	@Override
	protected void onResume() {
		Bundle bundle = getIntent().getExtras();

		if(bundle != null && bundle.getString("LAUNCH").equals("YES")) {
			startService(new Intent(FloatingActivity.this, FloatingWindow.class));
		}
		super.onResume();
	}
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 438 && resultCode == RESULT_OK && data != null && data.getData() != null) {
            try {
                String dst=getFilesDir().getPath() + "/" + "a.png";
                FileInputStream fip=new FileInputStream(new File(new PathUtil().getPath(this, data.getData())));
                FileOutputStream fop=new FileOutputStream(new File(dst));
                int read=0;
                byte[] b=new byte[1024];
                while (fip.read(b) > 0) {
                    fop.write(b);
                }
                fip.close();
                fop.close();
            } catch (Exception e) {
                Toast.makeText(this, "" + e, 1).show();
            }
            try {
                stopService(new Intent(this, FloatingWindow.class));
            } catch (Exception e) {}
            try {
                startService(new Intent(this, FloatingWindow.class));
            } catch (Exception e) {}

            /*loadingBar.setTitle("Sending Message");
             loadingBar.setMessage("Please wait...");
             loadingBar.setCanceledOnTouchOutside(false);
             loadingBar.show();
             imagefile = data.getData();*/
            //Uri.Builder ub=new Uri.Builder();
            //ub.path(data.getData().getPath().replace(":","/"));
            //bu.setText(getDataColumn(mcon,ub.build(),null,null));
            //bu.setText(ub.build().getPath());
            //bu.setText(data.getData().getPath());
            //bu.setText(getDataColumn(mcon,data.getData(),null,null));
            Uri uri=data.getData();
            try {
               // bu.setText(new PathUtil().getPath(mcon, data.getData()) );
            } catch (Exception e) {}
            // if (isMediaDocument(uri)) {
            /*   final String docId = DocumentsContract.getDocumentId(uri);
             final String[] split = docId.split(":");
             final String type = split[0];
             if ("image".equals(type)) {
             uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
             } else if ("video".equals(type)) {
             uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
             } else if ("audio".equals(type)) {
             uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
             }
             String selection = null;
             String[] selectionArgs = null;
             selection = "_id=?";
             selectionArgs = new String[]{ split[1] };
             //}

             if ("content".equalsIgnoreCase(uri.getScheme())) {
             String[] projection = { MediaStore.Images.Media.DATA };
             Cursor cursor = null;
             try {
             cursor = mcon.getContentResolver().query(uri, projection, selection, selectionArgs, null);
             int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
             if (cursor.moveToFirst()) {
             bu.setText(   cursor.getString(column_index));
             }
             } catch (Exception e) {
             }
             }*/
            /*String filepath = data.getData().getPath();

             if(checker.equals("pdf")){
             pdfFilemessage();
             }else if(checker.equals("image")){
             //imagefilemessage();
             Toast.makeText(personalChat.this,filepath,Toast.LENGTH_SHORT).show();
             }*/
        }
    }

    public static String getDataColumn(Context context, Uri uri, String selection,
                                       String[] selectionArgs) {

        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = {
            column
        };

        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs,
                                                        null);
            if (cursor != null && cursor.moveToFirst()) {
                final int index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(index);
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return null;
    }

    public class PathUtil {
        /*
         * Gets the file path of the given Uri.
         */
        @SuppressLint("NewApi")
        public  String getPath(Context context, Uri uri) throws URISyntaxException {
            final boolean needToCheckUri = Build.VERSION.SDK_INT >= 19;
            String selection = null;
            String[] selectionArgs = null;
            // Uri is different in versions after KITKAT (Android 4.4), we need to
            // deal with different Uris.
            if (needToCheckUri && DocumentsContract.isDocumentUri(context.getApplicationContext(), uri)) {
                if (isExternalStorageDocument(uri)) {
                    final String docId = DocumentsContract.getDocumentId(uri);
                    final String[] split = docId.split(":");
                    return Environment.getExternalStorageDirectory() + "/" + split[1];
                } else if (isDownloadsDocument(uri)) {
                    final String id = DocumentsContract.getDocumentId(uri);
                    uri = ContentUris.withAppendedId(
                        Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));
                } else if (isMediaDocument(uri)) {
                    final String docId = DocumentsContract.getDocumentId(uri);
                    final String[] split = docId.split(":");
                    final String type = split[0];
                    if ("image".equals(type)) {
                        uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                    } else if ("video".equals(type)) {
                        uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                    } else if ("audio".equals(type)) {
                        uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                    }
                    selection = "_id=?";
                    selectionArgs = new String[]{ split[1] };
                }
            }
            if ("content".equalsIgnoreCase(uri.getScheme())) {
                String[] projection = { MediaStore.Images.Media.DATA };
                Cursor cursor = null;
                try {
                    cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs, null);
                    int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                    if (cursor.moveToFirst()) {
                        return cursor.getString(column_index);
                    }
                } catch (Exception e) {
                }
            } else if ("file".equalsIgnoreCase(uri.getScheme())) {
                return uri.getPath();
            }
            return null;
        }


        /**
         * @param uri The Uri to check.
         * @return Whether the Uri authority is ExternalStorageProvider.
         */
        public  boolean isExternalStorageDocument(Uri uri) {
            return "com.android.externalstorage.documents".equals(uri.getAuthority());
        }

        /**
         * @param uri The Uri to check.
         * @return Whether the Uri authority is DownloadsProvider.
         */
        public  boolean isDownloadsDocument(Uri uri) {
            return "com.android.providers.downloads.documents".equals(uri.getAuthority());
        }

        /**
         * @param uri The Uri to check.
         * @return Whether the Uri authority is MediaProvider.
         */
        public  boolean isMediaDocument(Uri uri) {
            return "com.android.providers.media.documents".equals(uri.getAuthority());
        }
    }
}
