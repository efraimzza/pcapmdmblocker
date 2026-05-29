package com.mdm.store;

//package io.github.kdroidfilter.storekit.gplay.scraper.services;
/*
import io.github.kdroidfilter.storekit.gplay.core.model.GPlayApplicationInfo;
import io.github.kdroidfilter.storekit.utils.HttpService;
*/
import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.FeatureInfo;
import android.content.res.Configuration;
import android.opengl.GLES10;
import android.os.Build;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import com.emanuelef.remote_capture.activities.LogUtil;
import com.github.yeriomin.playstoreapi.AndroidAppDeliveryData;
import com.github.yeriomin.playstoreapi.BuyResponse;
import com.github.yeriomin.playstoreapi.DeliveryResponse;
import com.github.yeriomin.playstoreapi.DetailsResponse;
import com.github.yeriomin.playstoreapi.GooglePlayAPI;
import com.github.yeriomin.playstoreapi.PlayStoreApiBuilder;
import com.github.yeriomin.playstoreapi.PropertiesDeviceInfoProvider;
import com.github.yeriomin.playstoreapi.SplitDeliveryData;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;
import org.json.JSONObject;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class GPlayService {

    private final Context context;
    GooglePlayAPI api=null;
    static String packageName="";
    private static final String BASE_GPLAY_URL = "https://play.google.com/store/apps/details";

    private static String buildGPlayUrl(String packageName) {
        return BASE_GPLAY_URL + "?id=" + packageName + "&hl=en&gl=US";
    }

    /**
     * מחלץ מידע על אפליקציית Google Play באמצעות סקראפינג (מוגבל).
     * הערה: שירות זה מבוסס על סקראפינג של HTML ציבורי, מה שהופך אותו לשביר.
     */
     public GPlayService(Context context){
         this.context=context;
         
     }
     //public void isReady(interf listener){
         //if login complete listener.onsuccess else login
         //login(listener);
     //}
    public GPlayApplicationInfo getGPlayApplicationInfo(String packageName, boolean getLinks) throws Exception {
        this.packageName=packageName;
        detresponse=null;//reset
        login();
        
        if (detresponse==null) {
            throw new Exception("Failed to fetch Google Play page");
        }
        return new GPlayApplicationInfo(
            packageName, 
            detresponse.getItem().getTitle(), 
            versionCode+"",
            detresponse.getItem().getDetails().getAppDetails().getVersionString(),
            "",
            ((getLinks)?getLinks(detresponse):""),
            "", ""
        );
        /*
        //old
        String url = buildGPlayUrl(packageName);

        HttpService.HttpResponse response = HttpService.executeRequest(url, "GET", null);

        if (response.status == 404 || response.body.contains("Page not found")) {
            throw new IllegalArgumentException("Application not found on Google Play: " + packageName);
        }
        if (!response.isSuccess()) {
            throw new Exception("Failed to fetch Google Play page. HTTP status: " + response.status);
        }

        String html = response.body;

        String title = extractTitle(html);
        String versionCode = extractVersionCode(html);
        String version = extractVersion(html);
        String signature = extractSignature(html);
        String downloadLink = extractDownloadLink(html);
        String sizeText = extractSize(html);

        return new GPlayApplicationInfo(
            packageName, title, versionCode, version,signature,downloadLink, url, sizeText
        );
        */
    }

    private static String extractTitle(String html) {
        // מחפש את הכותרת ב-Meta tag או h1
        Pattern p = Pattern.compile("<h1[^>]*>\\s*<span[^>]*>(.*?)</span>\\s*</h1>", Pattern.DOTALL);
        Matcher m = p.matcher(html);
        if (m.find()) return m.group(1).trim();

        p = Pattern.compile("<meta\\s*itemprop=\"name\"\\s*content=\"(.*?)\"");
        m = p.matcher(html);
        if (m.find()) return m.group(1).trim();

        return "";
    }

    private static String extractVersion(String html) {
        // מחפש את "Current Version" ואת הערך שלו
        Pattern p = Pattern.compile("Current Version</div><span[^>]*>([^<]*)</span>");
        Matcher m = p.matcher(html);
        if (m.find()) return m.group(1).trim();

        return "";
    }
    private static String extractSignature(String html) {
        // מחפש את "Current Version" ואת הערך שלו
        Pattern p = Pattern.compile("Current Version</div><span[^>]*>([^<]*)</span>");
        Matcher m = p.matcher(html);
        if (m.find()) return m.group(1).trim();

        return "";
    }
    private static String extractDownloadLink(String html) {
        // מחפש את "Current Version" ואת הערך שלו
        Pattern p = Pattern.compile("Current Version</div><span[^>]*>([^<]*)</span>");
        Matcher m = p.matcher(html);
        if (m.find()) return m.group(1).trim();

        return "";
    }
    private static String extractVersionCode(String html) {
        // מחפש את "Current Version" ואת הערך שלו
        Pattern p = Pattern.compile("Current Version</div><span[^>]*>([^<]*)</span>");
        Matcher m = p.matcher(html);
        if (m.find()) return m.group(1).trim();

        return "";
    }
    private static String extractSize(String html) {
        // מחפש את "Size" ואת הערך שלו
        Pattern p = Pattern.compile("Size</div><span[^>]*>([^<]*)</span>");
        Matcher m = p.matcher(html);
        if (m.find()) return m.group(1).trim();
        return "";
    }
    private static final String AUTH_KEY = "StoreAuthJson";
    void login(){//final interf listener){
        //if(alreadyloggedin) {getDetails(); return;}
        //if(notsavedauth)anonymousauthgood();
        
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String jsonString = prefs.getString(AUTH_KEY, "");
        if(jsonString.equals("")){
            getnewlogin();
            jsonString = prefs.getString(AUTH_KEY, "");
            if(jsonString.equals("")){
                LogUtil.logToFile("login failed. return");
                return;
            }
        }
        
     //   new Thread(){public void run(){
        /*Properties properties = new Properties();
         try {
         //properties.load(getClass().getClassLoader().getResourceAsStream("device-honami.properties"));
         properties.load(getClass().getClassLoader().getResourceAsStream("gplayapi_xm_11a.properties"));
         }catch (Exception e) {
         LogUtil.logToFile(e);
         }*/
        try{
            JSONObject json = new JSONObject(jsonString);
            
            PropertiesDeviceInfoProvider deviceInfoProvider = new PropertiesDeviceInfoProvider();
            //deviceInfoProvider.setProperties(properties);
            deviceInfoProvider.setProperties(new NativeDeviceInfoProvider().getNativeDeviceProperties(context,false));
            deviceInfoProvider.setLocaleString(Locale.ENGLISH.toString());
            // Provide valid google account info
            PlayStoreApiBuilder builder = new PlayStoreApiBuilder()
                // Extend HttpClientAdapter using a http library of your choice
                .setHttpClient(new NativeHttpClientAdapter())
                .setDeviceInfoProvider(deviceInfoProvider)
                //.setPassword("7777777")//depracted...
                //.setGsfId("3f1abe856b0fa7fd")
                
               
                .setEmail(json.getString("email"))
                .setGsfId(json.getString("gsfId"))
                .setToken(json.getString("authToken"))
                ;
            if(builder!=null){
                api = builder.build();
                //listener.onsuccess();
                //notify - api ready to continue getting details etc...
                //only callback interface doing it?...
                //how callback works?
                //interface
                //main extends interface
                //another becomes the instance of the class interface and call to run methods of it
                //try now...
                //wait on initalize max 10 secnds to time out
                //
                //
                getDetails();
                String deviceCheckinConsistencyToken=api.getDeviceCheckinConsistencyToken();
                // We are logged in now
                // Save and reuse the generated auth token and gsf id,
                // unless you want to get banned for frequent relogins
                /*LogUtil.logToFile(
                 api.getToken()+"; "+
                 api.getGsfId()
                 );*/
            }else{
                LogUtil.logToFile("builder null");
            }
            } catch (Throwable e) {
                LogUtil.logToFile(e);
            }
    //    }}.start();
    }
    public interface interf{
        void onsuccess();
    }
    
    void getnewlogin(){
        //if not have old login or if err is err (not err disconnected many requests etc...)
        try {
            String resp=anonymousauthgood();
            if(!resp.equals("not")){
                SharedPreferences.Editor prefsedit = PreferenceManager.getDefaultSharedPreferences(context).edit();
                prefsedit.putString(AUTH_KEY, resp).commit();
                
                JSONObject json = new JSONObject(resp);
                LogUtil.logToFile(json.getString("authToken")+"\n"+json.getString("email")+"\n"+json.getString("gsfId"));
            }
        } catch (Throwable e) {LogUtil.logToFile(e);}
        
    }
    DetailsResponse detresponse=null;
    void getDetails(){
        
       // new Thread(){public void run(){
            try{
        // API wrapper instance is ready
        detresponse = api.details(packageName);
        if(detresponse!=null)
            if(detresponse.getItem()!=null)
                        if(detresponse.getItem().getAppInfo()!=null)
                            if(detresponse.getItem().getAppInfo().getTitle()!=null)
                                LogUtil.logToFile(detresponse.getItem().getAppInfo().getTitle()
                                +detresponse.getItem().getDescriptionHtml()
                                                  +detresponse.getPostAcquireDetailsStreamUrl()
                                                  +detresponse.getFooterHtml()
                                                  +detresponse.getDetailsStreamUrl()
                                                  +detresponse.getUserReview().getTitle());
                versionCode=detresponse.getItem().getDetails().getAppDetails().getVersionCode();
        
                //if (not have update) no need to get links... & ret no need update
                //else get option to install (getLinks();) (not get links now for all beacause the err too many requests
                //if click install && source is gplay - then reEntry to get links
                //dont forgat to save the api on the full store to avoid relogins
    } catch (Throwable e) {
        LogUtil.logToFile(e);
    }
   // }}.start();
    }
    String getLinks(DetailsResponse response){
        //purchase etc
        //return text or jsonFormat download links - name,link,name,link
        //save the links only for this sessions - save to privat variable (when relaunch to store delete all links...)
        JSONObject json = new JSONObject();
        try{
        if(response!=null&&response.getItem().getOfferCount()>0){
            offerType= response.getItem().getOffer(0).getOfferType();
            AndroidAppDeliveryData aadd=getResult(api,packageName);
            //res+=offerType+" ot;"+versionCode+" vc;"+response.getItem().getTitle();
            //res+="\n"+aadd.getDownloadUrl();
            json.put("base.apk",aadd.getDownloadUrl());
            LogUtil.logToFile(offerType+" ot;"+versionCode+" vc;"+response.getItem().getTitle());
            LogUtil.logToFile( aadd.getDownloadUrl());
            if(aadd.getSplitDeliveryDataCount()>0)
                for(SplitDeliveryData s: aadd.getSplitDeliveryDataList()){
                    //res+="\n"+s.getName();
                    //res+="\n"+s.getDownloadUrl();
                    json.put(s.getName(),s.getDownloadUrl());
                    LogUtil.logToFile(s.getName());
                    LogUtil.logToFile(s.getDownloadUrl());
                }
            LogUtil.logToFile(json.toString(4));
            return json.toString(4);
        }
        } catch (Throwable e) {
            LogUtil.logToFile(e);
        }
        return "";
    }
    void downloadpkg(final String packageName){
        //res="";
        new Thread(){public void run(){
                
                /*Properties properties = new Properties();
                try {
                    //properties.load(getClass().getClassLoader().getResourceAsStream("device-honami.properties"));
                    properties.load(getClass().getClassLoader().getResourceAsStream("gplayapi_xm_11a.properties"));
                }catch (Exception e) {
                    LogUtil.logToFile(e);
                }*/
                try{
                    PropertiesDeviceInfoProvider deviceInfoProvider = new PropertiesDeviceInfoProvider();
                    //deviceInfoProvider.setProperties(properties);
                    deviceInfoProvider.setProperties(new NativeDeviceInfoProvider().getNativeDeviceProperties(context,false));
                    deviceInfoProvider.setLocaleString(Locale.ENGLISH.toString());
                    // Provide valid google account info
                    PlayStoreApiBuilder builder = new PlayStoreApiBuilder()
                        // Extend HttpClientAdapter using a http library of your choice
                        .setHttpClient(new NativeHttpClientAdapter())
                        .setDeviceInfoProvider(deviceInfoProvider)
                          ;
                    if(builder!=null){
                        GooglePlayAPI api = builder.build();

                        String deviceCheckinConsistencyToken=api.getDeviceCheckinConsistencyToken();
                        // We are logged in now
                        // Save and reuse the generated auth token and gsf id,
                        // unless you want to get banned for frequent relogins
                        /*LogUtil.logToFile(
                         api.getToken()+"; "+
                         api.getGsfId()
                         );*/

                        // API wrapper instance is ready
                        DetailsResponse response = api.details(packageName);
                        if(response!=null)
                            if(response.getItem()!=null)
                                if(response.getItem().getAppInfo()!=null)
                                    if(response.getItem().getAppInfo().getTitle()!=null)
                                        LogUtil.logToFile(response.getItem().getAppInfo().getTitle()+response.getItem().getDescriptionHtml()+response.getPostAcquireDetailsStreamUrl()+response.getFooterHtml()+response.getDetailsStreamUrl()+response.getUserReview().getTitle());
                        versionCode=response.getItem().getDetails().getAppDetails().getVersionCode();
                        if(response!=null&&response.getItem().getOfferCount()>0){
                            offerType= response.getItem().getOffer(0).getOfferType();
                            AndroidAppDeliveryData aadd=getResult(api,packageName);
                            //res+=offerType+" ot;"+versionCode+" vc;"+response.getItem().getTitle();
                            //res+="\n"+aadd.getDownloadUrl();
                            LogUtil.logToFile(offerType+" ot;"+versionCode+" vc;"+response.getItem().getTitle());
                            LogUtil.logToFile( aadd.getDownloadUrl());
                            if(aadd.getSplitDeliveryDataCount()>0)
                                for(SplitDeliveryData s: aadd.getSplitDeliveryDataList()){
                                    //res+="\n"+s.getName();
                                    //res+="\n"+s.getDownloadUrl();
                                    LogUtil.logToFile(s.getName());
                                    LogUtil.logToFile(s.getDownloadUrl());
                                }
                        }
                    }else{
                        LogUtil.logToFile("builder null");
                    }
                } catch (Throwable e) {
                    LogUtil.logToFile(e);
                }
            }}.start();
    }
    protected String downloadToken;
    protected AndroidAppDeliveryData deliveryData;

    //protected String packageName="com.google.android.apps.docs";
    protected int versionCode;
    protected int offerType;

    protected AndroidAppDeliveryData getResult(GooglePlayAPI api,String packageName) throws IOException {
        //api.acquire(packageName,versionCode,offerType);
        purchase(api,packageName);
        delivery(api,packageName);
        return deliveryData;
    }
    void save(){
        //dont purchase all apps... (to avoid the error you are making too many requests...) only get the details
        //only purchase when you want to download & save the links after purchasing
        //source - gplay
        //if source eq gplay... then seva the links (&names) with the time when is getting the links
        //
    }
    String authres="";
    String anonymousauthgood(){
        //new Thread(){public void run(){
                //Map<String, String> params = new HashMap<String, String>();
                try{
                    /*Properties properties= new Properties();
                     properties.load(getClass().getClassLoader().getResourceAsStream("gplayapi_xm_11a.properties"));
                     JSONObject json = new JSONObject();
                     //json.put("lok", "hy");
                     //json.put("yyg","b");

                     for(Object gg:properties.keySet()){
                     json.put(gg.toString(), properties.get(gg));
                     }
                     LogUtil.logToFile( json.toString(4));*/
                    JSONObject json = new JSONObject();
                    //json.put("lok", "hy");
                    //json.put("yyg","b");
                    Properties propertiesnew=new NativeDeviceInfoProvider().getNativeDeviceProperties(context,false);
                    for(Object gg:propertiesnew.keySet()){
                        json.put(gg.toString(), propertiesnew.get(gg));
                    }
                    //LogUtil.logToFile( json.toString(4));
                    //now
                    //json.encodeToString(spoofProvider.deviceProperties).toByteArray()
                    //SerializationStrategy<Properties> s = null;
                    //Object o=null;
                    //Json.encodeToString(o);
                    //String g=new Json.Default().encodeToString(null,properties);
                    //String ft="{\n    \"HasHardKeyboard\": \"false\",\n    \"Build.HARDWARE\": \"mt6762\",\n    \"Build.BRAND\": \"MECHEN X56\",\n    \"Build.VERSION.SDK_INT\": \"33\",\n    \"Roaming\": \"mobile-notroaming\",\n    \"Build.MODEL\": \"MECHEN X56\",\n    \"Vending.versionString\": \"21.5.17-21 [0] [PR] 326734551\",\n    \"Vending.version\": \"82151710\",\n    \"CellOperator\": \"310\",\n    \"Build.FINGERPRINT\": \"Win/Q9/Q9:10/QP1A.190711.020/13113:user/release-keys\",\n    \"Build.DEVICE\": \"MECHEN X56\",\n    \"Screen.Height\": \"1859\",\n    \"SimOperator\": \"38\",\n    \"ScreenLayout\": \"2\",\n    \"SharedLibraries\": \"libmcv_runtime_usdk.mtk.so,libapuwareapusys.mtk.so,libcmdl_ndk.mtk.so,android.test.base,android.test.mock,libarmnn_ndk.mtk.so,libtflite_mtk.mtk.so,com.mediatek.wfo.legacy,android.hidl.manager-V1.0-java,libteeservice_client.trustonic.so,libapuwareutils.mtk.so,libOpenCL.so,libnir_neon_driver_ndk.mtk.so,android.hidl.base-V1.0-java,libneuronusdk_adapter.mtk.so,libapuwareutils_v2.mtk.so,libmvpu_pattern_pub.mtk.so,com.android.location.provider,libmvpu_engine_pub.mtk.so,libapuwarexrp_v2.mtk.so,libmvpuop_mtk_cv.mtk.so,libmvpu_runtime_pub.mtk.so,android.net.ipsec.ike,com.android.future.usb.accessory,android.ext.shared,libmvpuop_mtk_nn.mtk.so,javax.obex,com.google.android.gms,libmvpu_runtime_25.mtk.so,libmvpu_engine_25_pub.mtk.so,libneuron_graph_delegate.mtk.so,libmvpu_config.mtk.so,libapuwarehmp.mtk.so,libmvpu_pattern_25_pub.mtk.so,android.test.runner,libmvpu_runtime_25_pub.mtk.so,libmvpuop25_mtk_cv.mtk.so,libapuwareapusys_v2.mtk.so,org.apache.http.legacy,com.android.cts.ctsshim.shared_library,com.android.nfc_extras,com.android.media.remotedisplay,libapuwarexrp.mtk.so,libmvpu_runtime.mtk.so,com.android.mediadrm.signer,libmvpuop25_mtk_nn.mtk.so\",\n    \"Features\": \"android.hardware.sensor.proximity,android.software.adoptable_storage,android.hardware.sensor.accelerometer,android.software.controls,android.hardware.faketouch,com.google.android.feature.D2D_CABLE_MIGRATION_FEATURE,android.hardware.usb.accessory,android.software.backup,android.hardware.touchscreen,android.hardware.touchscreen.multitouch,android.software.print,android.software.activities_on_secondary_displays,android.software.voice_recognizers,android.software.picture_in_picture,android.hardware.audio.low_latency,android.software.vulkan.deqp.level,android.software.cant_save_state,android.hardware.security.model.compatible,android.hardware.opengles.aep,com.google.android.googlequicksearchbox.OEM_SMARTSPACE_WIDGET,android.hardware.bluetooth,android.hardware.camera.autofocus,android.hardware.telephony.gsm,android.hardware.telephony.ims,android.software.incremental_delivery,android.hardware.usb.host,android.hardware.audio.output,android.software.verified_boot,android.hardware.camera.flash,android.hardware.camera.front,android.hardware.se.omapi.uicc,android.hardware.screen.portrait,android.software.home_screen,android.hardware.microphone,android.software.autofill,android.software.securely_removes_users,android.hardware.bluetooth_le,android.hardware.sensor.compass,android.hardware.touchscreen.multitouch.jazzhand,android.software.app_widgets,android.software.input_methods,android.hardware.sensor.light,android.hardware.vulkan.version,android.software.companion_device_setup,android.software.device_admin,com.google.android.feature.WELLBEING,android.hardware.wifi.passpoint,android.hardware.camera,android.hardware.screen.landscape,android.hardware.ram.normal,android.software.managed_users,android.software.webview,android.hardware.camera.capability.manual_post_processing,com.google.android.contacts.feature.SIM_WRITE,android.hardware.camera.any,android.hardware.camera.capability.raw,android.hardware.vulkan.compute,android.software.connectionservice,android.hardware.touchscreen.multitouch.distinct,android.hardware.location.network,com.google.android.feature.GMSEXPRESS_PLUS_BUILD,android.software.cts,android.hardware.camera.capability.manual_sensor,android.software.app_enumeration,android.hardware.camera.level.full,android.hardware.wifi.direct,android.software.live_wallpaper,android.software.ipsec_tunnels,android.hardware.location.gps,android.software.midi,android.hardware.hardware_keystore,android.hardware.wifi,android.hardware.location,android.hardware.vulkan.level,android.software.secure_lock_screen,android.hardware.telephony,android.software.file_based_encryption\",\n    \"GSF.version\": \"203019037\",\n    \"UserReadableName\": \"alps MECHEN X56\",\n    \"Build.RADIO\": \"unknown\",\n    \"Client\": \"android-google\",\n    \"Locales\": \"af,am,ar,ar_EG,ar_XB,as,ast,az,be,bg,bg_BG,bn,bs,ca,ca_ES,cs,cs_CZ,da,da_DK,de,de_AT,de_DE,el,el_GR,en,en_AU,en_CA,en_GB,en_IN,en_US,en_XA,en_XC,eo,es,es_419,es_ES,es_US,et,et_EE,eu,fa,fa_IR,fi,fi_FI,fil,fil_PH,fr,fr_CA,fr_FR,gl,gu,hi,hi_IN,hr,hr_HR,hu,hu_HU,hy,hy_AM,in,in_ID,is,it,it_IT,iw,iw_IL,ja,ja_JP,ka,kab,kk,km,km_KH,kn,ko,ko_KR,kw,ky,lo,lt,lt_LT,lv,lv_LV,mk,ml,mn,mr,ms,ms_MY,my,my_MM,nb,nb_NO,ne,nl,nl_NL,or,pa,pl,pl_PL,pt,pt_BR,pt_PT,ro,ro_RO,ru,ru_RU,si,sk,sk_SK,sl,sl_SI,sq,sr,sr_Latn,sr_RS,sv,sv_SE,sw,ta,te,th,th_TH,tr,tr_TR,uk,uk_UA,ur,uz,vi,vi_VN,yue,zh_CN,zh_HK,zh_TW,zu\",\n    \"Screen.Density\": \"408\",\n    \"Platforms\": \"arm64-v8a,armeabi-v7a,armeabi\",\n    \"Build.PRODUCT\": \"MECHEN X56\",\n    \"Navigation\": \"1\",\n    \"Keyboard\": \"1\",\n    \"Build.ID\": \"MECHEN X56\",\n    \"TimeZone\": \"UTC-10\",\n    \"Screen.Width\": \"1080\",\n    \"GL.Extensions\": \"GL_ANDROID_extension_pack_es31a,GL_APPLE_texture_2D_limited_npot,GL_EXT_EGL_image_array,GL_EXT_YUV_target,GL_EXT_blend_minmax,GL_EXT_buffer_storage,GL_EXT_clear_texture,GL_EXT_clip_control,GL_EXT_color_buffer_float,GL_EXT_color_buffer_half_float,GL_EXT_conservative_depth,GL_EXT_copy_image,GL_EXT_debug_marker,GL_EXT_discard_framebuffer,GL_EXT_draw_buffers,GL_EXT_draw_buffers_indexed,GL_EXT_draw_elements_base_vertex,GL_EXT_external_buffer,GL_EXT_float_blend,GL_EXT_geometry_point_size,GL_EXT_geometry_shader,GL_EXT_gpu_shader5,GL_EXT_memory_object,GL_EXT_memory_object_fd,GL_EXT_multi_draw_arrays,GL_EXT_multisampled_render_to_texture,GL_EXT_multisampled_render_to_texture2,GL_EXT_occlusion_query_boolean,GL_EXT_polygon_offset_clamp,GL_EXT_primitive_bounding_box,GL_EXT_pvrtc_sRGB,GL_EXT_read_format_bgra,GL_EXT_robustness,GL_EXT_sRGB_write_control,GL_EXT_separate_shader_objects,GL_EXT_shader_framebuffer_fetch,GL_EXT_shader_group_vote,GL_EXT_shader_implicit_conversions,GL_EXT_shader_io_blocks,GL_EXT_shader_non_constant_global_initializers,GL_EXT_shader_pixel_local_storage,GL_EXT_shader_pixel_local_storage2,GL_EXT_shader_texture_lod,GL_EXT_sparse_texture,GL_EXT_tessellation_shader,GL_EXT_texture_border_clamp,GL_EXT_texture_buffer,GL_EXT_texture_cube_map_array,GL_EXT_texture_format_BGRA8888,GL_EXT_texture_format_sRGB_override,GL_EXT_texture_rg,GL_EXT_texture_sRGB_R8,GL_EXT_texture_sRGB_RG8,GL_EXT_texture_sRGB_decode,GL_EXT_texture_shadow_lod,GL_IMG_framebuffer_downsample,GL_IMG_multisampled_render_to_texture,GL_IMG_program_binary,GL_IMG_read_format,GL_IMG_shader_binary,GL_IMG_texture_compression_pvrtc,GL_IMG_texture_compression_pvrtc2,GL_IMG_texture_format_BGRA8888,GL_IMG_texture_npot,GL_IMG_vertex_array_object,GL_KHR_blend_equation_advanced,GL_KHR_blend_equation_advanced_coherent,GL_KHR_debug,GL_KHR_robustness,GL_KHR_texture_compression_astc_ldr,GL_OES_EGL_image,GL_OES_EGL_image_external,GL_OES_EGL_image_external_essl3,GL_OES_EGL_sync,GL_OES_blend_equation_separate,GL_OES_blend_func_separate,GL_OES_blend_subtract,GL_OES_byte_coordinates,GL_OES_compressed_ETC1_RGB8_texture,GL_OES_compressed_paletted_texture,GL_OES_depth24,GL_OES_depth_texture,GL_OES_draw_buffers_indexed,GL_OES_draw_elements_base_vertex,GL_OES_draw_texture,GL_OES_egl_sync,GL_OES_element_index_uint,GL_OES_extended_matrix_palette,GL_OES_fixed_point,GL_OES_fragment_precision_high,GL_OES_framebuffer_object,GL_OES_geometry_point_size,GL_OES_geometry_shader,GL_OES_get_program_binary,GL_OES_gpu_shader5,GL_OES_mapbuffer,GL_OES_matrix_get,GL_OES_matrix_palette,GL_OES_packed_depth_stencil,GL_OES_point_size_array,GL_OES_point_sprite,GL_OES_query_matrix,GL_OES_read_format,GL_OES_required_internalformat,GL_OES_rgb8_rgba8,GL_OES_sample_shading,GL_OES_sample_variables,GL_OES_shader_image_atomic,GL_OES_shader_io_blocks,GL_OES_shader_multisample_interpolation,GL_OES_single_precision,GL_OES_standard_derivatives,GL_OES_stencil8,GL_OES_stencil_wrap,GL_OES_surfaceless_context,GL_OES_tessellation_point_size,GL_OES_tessellation_shader,GL_OES_texture_border_clamp,GL_OES_texture_buffer,GL_OES_texture_cube_map,GL_OES_texture_cube_map_array,GL_OES_texture_env_crossbar,GL_OES_texture_float,GL_OES_texture_half_float,GL_OES_texture_mirrored_repeat,GL_OES_texture_npot,GL_OES_texture_stencil8,GL_OES_texture_storage_multisample_2d_array,GL_OES_vertex_array_object,GL_OES_vertex_half_float\",\n    \"HasFiveWayNavigation\": \"false\",\n    \"TouchScreen\": \"3\",\n    \"GL.Version\": \"196610\",\n    \"Build.VERSION.RELEASE\": \"13\",\n    \"Build.BOOTLOADER\": \"unknown\",\n    \"Build.MANUFACTURER\": \"alps\"\n}";
                    //SerializationStrategy
                    //& dont forget to logtofile the body string
                    //properties.putAll(params);
                    //Map<String, String> headers = new HashMap<String, String>();
                    //headers.put("User-Agent", "com.aurora.store.nightly-4.8.0-9d80b1dd5-72");
                    //"${BuildConfig.APPLICATION_ID}-${BuildConfig.VERSION_NAME}-${BuildConfig.VERSION_CODE}"
                    //LogUtil.logToFile(g);
                    //LogUtil.logToFile(ft);
                    String response =new NativeHttpClientAdapter().makeNetworkRequestnew(
                        /* "https://auroraoss.com/api/auth",*/ json.toString(4).getBytes()//replace to properties...
                    //,headers
                    //new Properties().values()
                    //json.encodeToString(spoofProvider.deviceProperties).toByteArray()
                    );
                    authres=response;
                    LogUtil.logToFile(authres);

                } catch (Throwable e) {LogUtil.logToFile(e);}
            //}}.start();
        return authres;
    }
    /*
    void gauth(){
        startActivityForResult(
            AccountManager.newChooseAccountIntent(
                null,
                null,
                new String[]{"com.google"},
                null,
                null,
                null,
                null
            ),444);
    }
    String accountName="";
    String token="";
    String authtoken(Intent data){
        accountName=data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
        LogUtil.logToFile(accountName);
        String GOOGLE_PLAY_CERT =
            "MIIEQzCCAyugAwIBAgIJAMLgh0ZkSjCNMA0GCSqGSIb3DQEBBAUAMHQxCzAJBgNVBAYTAlVTMRMwEQYDVQQIEwpDYWxpZm9ybmlhMRYwFAYDVQQHEw1Nb3VudGFpbiBWaWV3MRQwEgYDVQQKEwtHb29nbGUgSW5jLjEQMA4GA1UECxMHQW5kcm9pZDEQMA4GA1UEAxMHQW5kcm9pZDAeFw0wODA4MjEyMzEzMzRaFw0zNjAxMDcyMzEzMzRaMHQxCzAJBgNVBAYTAlVTMRMwEQYDVQQIEwpDYWxpZm9ybmlhMRYwFAYDVQQHEw1Nb3VudGFpbiBWaWV3MRQwEgYDVQQKEwtHb29nbGUgSW5jLjEQMA4GA1UECxMHQW5kcm9pZDEQMA4GA1UEAxMHQW5kcm9pZDCCASAwDQYJKoZIhvcNAQEBBQADggENADCCAQgCggEBAKtWLgDYO6IIrgqWbxJOKdoR8qtW0I9Y4sypEwPpt1TTcvZApxsdyxMJZ2JORland2qSGT2y5b+3JKkedxiLDmpHpDsz2WCbdxgxRczfey5YZnTJ4VZbH0xqWVW/8lGmPav5xVwnIiJS6HXk+BVKZF+JcWjAsb/GEuq/eFdpuzSqeYTcfi6idkyugwfYwXFU1+5fZKUaRKYCwkkFQVfcAs1fXA5V+++FGfvjJ/CxURaSxaBvGdGDhfXE28LWuT9ozCl5xw4Yq5OGazvV24mZVSoOO0yZ31j7kYvtwYK6NeADwbSxDdJEqO4k//0zOHKrUiGYXtqw/A0LFFtqoZKFjnkCAQOjgdkwgdYwHQYDVR0OBBYEFMd9jMIhF1Ylmn/Tgt9r45jk14alMIGmBgNVHSMEgZ4wgZuAFMd9jMIhF1Ylmn/Tgt9r45jk14aloXikdjB0MQswCQYDVQQGEwJVUzETMBEGA1UECBMKQ2FsaWZvcm5pYTEWMBQGA1UEBxMNTW91bnRhaW4gVmlldzEUMBIGA1UEChMLR29vZ2xlIEluYy4xEDAOBgNVBAsTB0FuZHJvaWQxEDAOBgNVBAMTB0FuZHJvaWSCCQDC4IdGZEowjTAMBgNVHRMEBTADAQH/MA0GCSqGSIb3DQEBBAUAA4IBAQBt0lLO74UwLDYKqs6Tm8/yzKkEu116FmH4rkaymUIE0P9KaMftGlMexFlaYjzmB2OxZyl6euNXEsQH8gjwyxCUKRJNexBiGcCEyj6z+a1fuHHvkiaai+KL8W1EyNmgjmyy8AW7P+LLlkR+ho5zEHatRbM/YAnqGcFh5iZBqpknHf1SKMXFh4dd239FJ1jWYfbMDMy3NS5CTMQ2XFI1MvcyUTdZPErjQfTbQe3aDQsQcafEQPD+nqActifKZ0Np0IS9L9kR/wbNvyz6ENwPiTrjV2KRkEjH78ZMcUQXg0L3BYHJ3lc69Vs5Ddf9uUGGMYldX3WfMBEmh/9iFBDAaTCK";

        Bundle bundleOf=new Bundle();
        bundleOf.putString("overridePackage","com.android.vending");
        bundleOf.putString("overrideCertificate",GOOGLE_PLAY_CERT);
        AccountManagerFuture<Bundle> acmf= AccountManager.get(this).getAuthToken(
            new Account(accountName, "com.google"),
            "oauth2:https://www.googleapis.com/auth/googleplay",
            bundleOf,
            this, new AccountManagerCallback<Bundle>(){
                @Override
                public void run(AccountManagerFuture<Bundle> p1) {
                    try {

                        if(p1.isDone()){
                            token= p1.getResult().getString(AccountManager.KEY_AUTHTOKEN);
                            LogUtil.logToFile(token);
                            //now save auth params & run successful proccess
                        }
                    } catch (Exception e) {LogUtil.logToFile(e);}
                }
            }
            ,
            new Handler(Looper.getMainLooper()));
        return token;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode==444){
            authtoken(data);
        }
    }
    */
    protected void purchase(GooglePlayAPI api,String packageName) {
        try {
            BuyResponse buyResponse = api.purchase(packageName, versionCode, offerType);
            if (buyResponse.hasPurchaseStatusResponse()
                && buyResponse.getPurchaseStatusResponse().hasAppDeliveryData()
                && buyResponse.getPurchaseStatusResponse().getAppDeliveryData().hasDownloadUrl()
                ) {
                deliveryData = buyResponse.getPurchaseStatusResponse().getAppDeliveryData();
            }
            if (buyResponse.hasDownloadToken()) {
                downloadToken = buyResponse.getDownloadToken();
            }
        } catch (IOException e) {
            LogUtil.logToFile(getClass().getSimpleName()+ "Purchase for " + packageName + " failed with " + e.getClass().getName() + ": " + e.getMessage());
        }
    }

    protected void delivery(GooglePlayAPI api,String packageName) throws IOException {
        DeliveryResponse deliveryResponse = api.delivery(
            packageName,
            0,
            versionCode,
            offerType,
            downloadToken
        );
        if (deliveryResponse.hasAppDeliveryData()
            && deliveryResponse.getAppDeliveryData().hasDownloadUrl()
            ) {
            deliveryData = deliveryResponse.getAppDeliveryData();
        } else {LogUtil.logToFile("not purchased");}/*if (!app.isFree() && !YalpStoreApplication.user.appProvidedEmail()) {
         throw new NotPurchasedException();
         }*/
    }
    class NativeDeviceInfoProvider {

        private void setProperty(String p0, String p1) {
            properties.setProperty(p0,p1);
        }
        Properties properties;
        Properties getNativeDeviceProperties(Context context,Boolean isExport) {
            isExport=false;
            properties =new Properties();
            //Build Props
            setProperty("UserReadableName", Build.MANUFACTURER+" "+Build.MODEL);
            setProperty("Build.HARDWARE", Build.HARDWARE);
            setProperty(
                "Build.RADIO",
                ( (Build.getRadioVersion() != null)?
                Build.getRadioVersion()
                :
                "unknown")
            );
            setProperty("Build.FINGERPRINT", Build.FINGERPRINT);
            setProperty("Build.BRAND", Build.BRAND);
            setProperty("Build.DEVICE", Build.DEVICE);
            setProperty("Build.VERSION.SDK_INT", Build.VERSION.SDK_INT+"");
            setProperty("Build.VERSION.RELEASE", Build.VERSION.RELEASE);
            setProperty("Build.MODEL", Build.MODEL);
            setProperty("Build.MANUFACTURER", Build.MANUFACTURER);
            setProperty("Build.PRODUCT", Build.PRODUCT);
            setProperty("Build.ID", Build.ID);
            setProperty("Build.BOOTLOADER", Build.BOOTLOADER);
            
            Configuration config = context.getResources().getConfiguration();
            setProperty("TouchScreen", config.touchscreen+"");
            setProperty("Keyboard", config.keyboard+"");
            setProperty("Navigation", config.navigation+"");
            setProperty("ScreenLayout", (config.screenLayout & 15)+"");
            setProperty("HasHardKeyboard", (config.keyboard == Configuration.KEYBOARD_QWERTY)+"");
            setProperty(
                "HasFiveWayNavigation",
                (config.navigation == Configuration.NAVIGATIONHIDDEN_YES)+""
            );

            //Display Metrics
            DisplayMetrics metrics = context.getResources().getDisplayMetrics();
            setProperty("Screen.Density", metrics.densityDpi+"");
            setProperty("Screen.Width", metrics.widthPixels+"");
            setProperty("Screen.Height", metrics.heightPixels+"");

            //Supported Platforms
            setProperty("Platforms",new String().join(",", Build.SUPPORTED_ABIS));
            //Supported Features
            setProperty("Features", new String().join(",", getFeatures(context)));
            //Shared Locales
            setProperty("Locales", new String().join(",", getLocales(context)));
            //Shared Libraries
            setProperty("SharedLibraries",new String().join(",", getSharedLibraries(context)));
            //GL Extensions
            ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            setProperty(
                "GL.Version",
                activityManager.getDeviceConfigurationInfo().reqGlEsVersion+""
            );
            /*setProperty(
             "GL.Extensions",
             EglExtensionProvider.eglExtensions.joinToString(separator = ",")
             )
             */
            setProperty(
                "GL.Extensions",
                new String().join(",",EglExtensionRetriever.getEglExtensions())
            );

            //Google Related Props
            setProperty("Client", "android-google");

            //NativeGsfVersionProvider gsfVersionProvider = NativeGsfVersionProvider(context, isExport);
            setProperty("GSF.version", 203019037L+"");
            setProperty("Vending.version", 82151710L+"");
            setProperty("Vending.versionString", "21.5.17-21 [0] [PR] 326734551");

            //MISC
            setProperty("Roaming", "mobile-notroaming");
            setProperty("TimeZone", "UTC-10");

            //Telephony (USA 3650 AT&T);
            setProperty("CellOperator", "310");
            setProperty("SimOperator", "38");



            return properties;
        }

        private String[] getFeatures(Context context){
            FeatureInfo[] fis= context.getPackageManager().getSystemAvailableFeatures();
            String[] strarr=new String[fis.length];
            int iii=0;
            if(fis.length>0)
                for(FeatureInfo jk:fis){
                    strarr[iii]=jk.name;
                    iii++;
                }
            return strarr;
            /*
             return context
             .getPackageManager()
             .getSystemAvailableFeatures()
             .mapNotNull { it.name }*/
        }

        private String[] getLocales(Context context) {
            String[] locs=context
                .getAssets()
                .getLocales();
            String[] strarr=new String[locs.length];
            int iii=0;
            for(String jk:locs){
                strarr[iii]=jk.replace("-", "_");
                iii++;
            }
            return strarr;
            /* return context
             .getAssets()
             .getLocales()
             .mapNotNull { it.replace("-", "_") }*/
        }

        private String[] getSharedLibraries(Context context) {
            return context
                .getPackageManager()
                .getSystemSharedLibraryNames();
            //?.toList() ?: emptyList()
        }
    }

    static class EglExtensionRetriever {

        public static List<String> getEglExtensions() {
            Set<String> glExtensions = new HashSet<>();
            EGL10 egl10 = (EGL10) EGLContext.getEGL();
            if (egl10 == null) {
                return new ArrayList<>();
            }
            EGLDisplay display = egl10.eglGetDisplay(EGL10.EGL_DEFAULT_DISPLAY);
            egl10.eglInitialize(display, new int[2]);
            int cf[] = new int[1];
            if (egl10.eglGetConfigs(display, null, 0, cf)) {
                EGLConfig[] configs = new EGLConfig[cf[0]];
                if (egl10.eglGetConfigs(display, configs, cf[0], cf)) {
                    int[] a1 = new int[] {EGL10.EGL_WIDTH, EGL10.EGL_PBUFFER_BIT, EGL10.EGL_HEIGHT, EGL10.EGL_PBUFFER_BIT, EGL10.EGL_NONE};
                    int[] a2 = new int[] {12440, EGL10.EGL_PIXMAP_BIT, EGL10.EGL_NONE};
                    int[] a3 = new int[1];
                    for (int i = 0; i < cf[0]; i++) {
                        egl10.eglGetConfigAttrib(display, configs[i], EGL10.EGL_CONFIG_CAVEAT, a3);
                        if (a3[0] != EGL10.EGL_SLOW_CONFIG) {
                            egl10.eglGetConfigAttrib(display, configs[i], EGL10.EGL_SURFACE_TYPE, a3);
                            if ((1 & a3[0]) != 0) {
                                egl10.eglGetConfigAttrib(display, configs[i], EGL10.EGL_RENDERABLE_TYPE, a3);
                                if ((1 & a3[0]) != 0) {
                                    addExtensionsForConfig(egl10, display, configs[i], a1, null, glExtensions);
                                }
                                if ((4 & a3[0]) != 0) {
                                    addExtensionsForConfig(egl10, display, configs[i], a1, a2, glExtensions);
                                }
                            }
                        }
                    }
                }
            }
            egl10.eglTerminate(display);
            List<String> sorted = new ArrayList<>(glExtensions);
            Collections.sort(sorted);
            return sorted;
        }

        private static void addExtensionsForConfig(EGL10 egl10, EGLDisplay egldisplay, EGLConfig eglconfig, int ai[], int ai1[], Set<String> set) {
            EGLContext eglContext = egl10.eglCreateContext(egldisplay, eglconfig, EGL10.EGL_NO_CONTEXT, ai1);
            if (eglContext == EGL10.EGL_NO_CONTEXT) {
                return;
            }
            javax.microedition.khronos.egl.EGLSurface eglSurface = egl10.eglCreatePbufferSurface(egldisplay, eglconfig, ai);
            if (eglSurface == EGL10.EGL_NO_SURFACE) {
                egl10.eglDestroyContext(egldisplay, eglContext);
            } else {
                egl10.eglMakeCurrent(egldisplay, eglSurface, eglSurface, eglContext);
                String s = GLES10.glGetString(7939);
                if (!TextUtils.isEmpty(s)) {
                    String as[] = s.split(" ");
                    int i = as.length;
                    for (int j = 0; j < i; j++) {
                        set.add(as[j]);
                    }
                }
                egl10.eglMakeCurrent(egldisplay, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_CONTEXT);
                egl10.eglDestroySurface(egldisplay, eglSurface);
                egl10.eglDestroyContext(egldisplay, eglContext);
            }
        }
    }
}
