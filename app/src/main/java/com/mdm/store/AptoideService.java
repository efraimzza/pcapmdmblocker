package com.mdm.store;

//package io.github.kdroidfilter.storekit.aptoide.api.services;
/*
import io.github.kdroidfilter.storekit.aptoide.core.model.AptoideApplicationInfo;
import io.github.kdroidfilter.storekit.utils.HttpService;
import io.github.kdroidfilter.storekit.utils.JsonParserUtils;
*/

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import org.json.JSONObject;
import org.json.JSONException;
import com.emanuelef.remote_capture.activities.LogUtil;
import java.util.ArrayList;

public class AptoideService {

    // סימולציה של Logger
    private void logInfo(String message) {
        System.out.println("AptoideService: " + message);
    }

    // Helper method to build the URL with parameters
    private static String buildUrl(String basePath, Map<String, String> parameters) {
        StringBuilder url = new StringBuilder(basePath);
        if (!parameters.isEmpty()) {
            url.append("?");
            /*for (Map.Entry<String, String> entry : parameters.entrySet()) {
                url.append(entry.getKey()).append("=").append(entry.getValue()).append("&");
            }*/
            for (String key : parameters.keySet()) {
                url.append(key).append("=").append(parameters.get(key)).append("&");
            }
            url.deleteCharAt(url.length() - 1); // Remove trailing &
        }
        return url.toString();
    }

    /**
     * Fetches application metadata from the Aptoide API using the package name.
     */
    public static AptoideApplicationInfo getAppMetaByPackageName(String packageName, String language) throws Exception {
        if (language == null) language = "en";
        LogUtil.logToFile("Fetching app metadata for package name: " + packageName);

        String url = AptoideConstants.BASE_APTOIDE_API_URL + AptoideConstants.APP_GET_META_PATH;

        Map<String, String> params = new HashMap<>();
        params.put("package_name", packageName);
        params.put("language", language);

        String fullUrl = buildUrl(url, params);

        HttpService.HttpResponse response = HttpService.executeRequest(fullUrl, "GET", null);

        if (!response.isSuccess()) {
            throw new IllegalArgumentException("Application with package name: " + packageName +
                                               " does not exist or is not accessible. HTTP status: " + response.status);
        }

        String responseText = response.body;

        // ** שימו לב: כאן נדרשת לוגיקת המרת JSON שלכם **
        AptoideResponse aptoideResponse;
        try {
            aptoideResponse = decodeAptoideResponse(responseText);
        } catch (Exception e) {
            throw new Exception("Error decoding Aptoide JSON response.", e);
        }

        LogUtil.logToFile("Successfully fetched app metadata for package name: " + packageName);

        return aptoideResponse.data;
    }

    /**
     * Fetches application metadata from the Aptoide API using the app ID.
     */
    public AptoideApplicationInfo getAppMetaById(long appId, String language) throws Exception {
        if (language == null) language = "en";
        logInfo("Fetching app metadata for app ID: " + appId);

        String url = AptoideConstants.BASE_APTOIDE_API_URL + AptoideConstants.APP_GET_META_PATH;

        Map<String, String> params = new HashMap<>();
        params.put("app_id", String.valueOf(appId));
        params.put("language", language);

        String fullUrl = buildUrl(url, params);

        HttpService.HttpResponse response = HttpService.executeRequest(fullUrl, "GET", null);

        if (!response.isSuccess()) {
            throw new IllegalArgumentException("Application with app ID: " + appId +
                                               " does not exist or is not accessible. HTTP status: " + response.status);
        }

        String responseText = response.body;
        AptoideResponse aptoideResponse;
        try {
            aptoideResponse = decodeAptoideResponse(responseText);
        } catch (Exception e) {
            throw new Exception("Error decoding Aptoide JSON response.", e);
        }

        logInfo("Successfully fetched app metadata for app ID: " + appId);

        return aptoideResponse.data;
    }

    /**
     * Fetches application metadata from the Aptoide API using the APK MD5 sum.
     */
    public AptoideApplicationInfo getAppMetaByMd5sum(String md5sum, String language) throws Exception {
        if (language == null) language = "en";
        logInfo("Fetching app metadata for APK MD5 sum: " + md5sum);

        String url = AptoideConstants.BASE_APTOIDE_API_URL + AptoideConstants.APP_GET_META_PATH;

        Map<String, String> params = new HashMap<>();
        params.put("apk_md5sum", md5sum);
        params.put("language", language);

        String fullUrl = buildUrl(url, params);

        HttpService.HttpResponse response = HttpService.executeRequest(fullUrl, "GET", null);

        if (!response.isSuccess()) {
            throw new IllegalArgumentException("Application with APK MD5 sum: " + md5sum +
                                               " does not exist or is not accessible. HTTP status: " + response.status);
        }

        String responseText = response.body;
        AptoideResponse aptoideResponse;
        try {
            aptoideResponse = decodeAptoideResponse(responseText);
        } catch (Exception e) {
            throw new Exception("Error decoding Aptoide JSON response.", e);
        }

        logInfo("Successfully fetched app metadata for APK MD5 sum: " + md5sum);

        return aptoideResponse.data;
    }
    
// מחלקות מודל - השתמשתי בשמות המדויקים של שדות ה-JSON עבור קלות המיפוי

    public static class AptoideResponse {
        public AptoideInfo info = new AptoideInfo();
        public AptoideApplicationInfo data = new AptoideApplicationInfo();
    }

    public static class AptoideInfo {
        public String status = "";
        public AptoideTime time = new AptoideTime();
    }

    public static class AptoideTime {
        public double seconds = 0.0;
        public String human = "";
    }

    public static class AptoideApplicationInfo {
        public long id = 0;
        public String name = "";

        // Kotlin source used both 'package_' and '@SerialName("package") val packageName'.
        // We keep both fields to capture the data if it exists in the JSON.
        public String package_ = "";
        public String packageName = ""; // Maps to "package" in JSON

        public String uname = "";
        public long size = 0;
        public String icon = "";
        public String graphic = "";
        public String added = "";
        public String modified = "";
        public String updated = "";
        public String main_package = null;
        public AptoideAge age = new AptoideAge();
        public AptoideDeveloper developer = new AptoideDeveloper();
        public AptoideStore store = new AptoideStore();
        public AptoideFile file = new AptoideFile(); // שדה זה מכיל את ה-path
        public AptoideMedia media = new AptoideMedia();
        public AptoideUrls urls = new AptoideUrls();
        public AptoideStats stats = new AptoideStats();
        public String aab = null;
        public String obb = null;
        public String pay = null;
        public AptoideAppcoins appcoins = new AptoideAppcoins();
        public List<String> soft_locks = new ArrayList<String>();
    }

    public static class AptoideAge {
        public String name = "";
        public String title = "";
        public String pegi = "";
        public int rating = 0;
    }

    public static class AptoideDeveloper {
        public long id = 0;
        public String name = "";
        public String website = "";
        public String email = "";
        public String privacy = null;
    }

    public static class AptoideStore {
        public long id = 0;
        public String name = "";
        public String avatar = "";
        public AptoideAppearance appearance = new AptoideAppearance();
        public AptoideStoreStats stats = new AptoideStoreStats();
    }

    public static class AptoideAppearance {
        public String theme = "";
        public String description = "";
    }

    public static class AptoideStoreStats {
        public int apps = 0;
        public int subscribers = 0;
        public int downloads = 0;
    }

    public static class AptoideFile {
        public String vername = "";
        public int vercode = 0;
        public String md5sum = "";
        public long filesize = 0;
        public AptoideSignature signature = new AptoideSignature();
        public String added = "";
        public String path = ""; // זה נתיב הקובץ שהיה חסר
        public String path_alt = "";
        public AptoideHardware hardware = new AptoideHardware();
        public AptoideMalware malware = new AptoideMalware();
        public AptoideFlags flags = new AptoideFlags();
        public List<String> used_features = new ArrayList<String>();
        public List<String> used_permissions = new ArrayList<String>();
        public List<String> tags =new ArrayList<String>();
    }

    public static class AptoideSignature {
        public String sha1 = "";
        public String owner = "";
    }

    public static class AptoideHardware {
        public int sdk = 0;
        public String screen = "";
        public int gles = 0;
        public List<String> cpus = new ArrayList<String>();
        public List<List<Integer>> densities = new ArrayList<List<Integer>>();
        public List<AptoideDependency> dependencies = new ArrayList<AptoideDependency>();
    }

    public static class AptoideDependency {
        public String type = "";
        public String level = "";
    }

    public static class AptoideMalware {
        public String rank = "";
        public AptoideMalwareReason reason = new AptoideMalwareReason();
        public String added = "";
        public String modified = "";
    }

    public static class AptoideMalwareReason {
        public AptoideSignatureValidated signature_validated = new AptoideSignatureValidated();
    }

    public static class AptoideSignatureValidated {
        public String date = "";
        public String status = "";
        public String signature_from = "";
    }

    public static class AptoideFlags {
        public List<AptoideVote> votes = new ArrayList<AptoideVote>();
    }

    public static class AptoideVote {
        public String type = "";
        public int count = 0;
    }

    public static class AptoideMedia {
        public List<String> keywords = new ArrayList<String>();
        public String description = "";
        public String summary = "";
        public String news = "";
        public List<AptoideVideo> videos = new ArrayList<AptoideVideo>();
        public List<AptoideScreenshot> screenshots = new ArrayList<AptoideScreenshot>();
    }

    public static class AptoideVideo {
        public String type = "";
        public String url = "";
    }

    public static class AptoideScreenshot {
        public String url = "";
        public int height = 0;
        public int width = 0;
    }

    public static class AptoideUrls {
        public String w = "";
        public String m = "";
    }

    public static class AptoideStats {
        public AptoideRating rating = new AptoideRating();
        public AptoideRating prating = new AptoideRating();
        public int downloads = 0;
        public int pdownloads = 0;
    }

    public static class AptoideRating {
        public double avg = 0.0;
        public int total = 0;
        public List<AptoideRatingVote> votes = new ArrayList<AptoideRatingVote>();
    }

    public static class AptoideRatingVote {
        public int value = 0;
        public int count = 0;
    }

    public static class AptoideAppcoins {
        public boolean advertising = false;
        public boolean billing = false;
        public List<String> flags = new ArrayList<String>();
    }
  
        // עזרי בטיחות לקריאת נתונים מ-JSONObject
        private static String safeGetString(JSONObject json, String key) {
            return json.has(key) && !json.isNull(key) ? json.optString(key, "") : "";
        }

        private static long safeGetLong(JSONObject json, String key) {
            return json.has(key) && !json.isNull(key) ? json.optLong(key, 0) : 0;
        }

        private static int safeGetInt(JSONObject json, String key) {
            return json.has(key) && !json.isNull(key) ? json.optInt(key, 0) : 0;
        }

        // --- שיטות לניתוח מודלים מקוננים (חלק נבחר) ---

        private static AptoideTime parseTime(JSONObject json) {
            AptoideTime model = new AptoideTime();
            model.seconds = json.optDouble("seconds", 0.0);
            model.human = safeGetString(json, "human");
            return model;
        }

        private static AptoideInfo parseInfo(JSONObject json) {
            AptoideInfo model = new AptoideInfo();
            model.status = safeGetString(json, "status");
            if (json.has("time") && !json.isNull("time")) {
                model.time = parseTime(json.optJSONObject("time"));
            }
            return model;
        }

        private static AptoideSignature parseSignature(JSONObject json) {
            AptoideSignature model = new AptoideSignature();
            model.sha1 = safeGetString(json, "sha1");
            model.owner = safeGetString(json, "owner");
            return model;
        }

        private static AptoideMalware parseMalware(JSONObject json) {
            AptoideMalware model = new AptoideMalware();
            model.rank = safeGetString(json, "rank");
            // ... נתונים נוספים הושמטו לפשטות ...
            return model;
        }

        private static AptoideFile parseFile(JSONObject json) throws JSONException {
            AptoideFile model = new AptoideFile();
            model.vername = safeGetString(json, "vername");
            model.vercode = safeGetInt(json, "vercode");
            model.md5sum = safeGetString(json, "md5sum");
            model.filesize = safeGetLong(json, "filesize");
            model.added = safeGetString(json, "added");
            model.path = safeGetString(json, "path"); // ** שדה נתיב הקובץ הקריטי **
            model.path_alt = safeGetString(json, "path_alt");

            if (json.has("signature") && !json.isNull("signature")) {
                model.signature = parseSignature(json.optJSONObject("signature"));
            }
            if (json.has("malware") && !json.isNull("malware")) {
                model.malware = parseMalware(json.optJSONObject("malware"));
            }
            return model;
        }

        private static AptoideDeveloper parseDeveloper(JSONObject json) {
            AptoideDeveloper model = new AptoideDeveloper();
            model.id = safeGetLong(json, "id");
            model.name = safeGetString(json, "name");
            model.website = safeGetString(json, "website");
            model.email = safeGetString(json, "email");
            model.privacy = safeGetString(json, "privacy");
            return model;
        }

        private static AptoideMedia parseMedia(JSONObject json) {
            AptoideMedia model = new AptoideMedia();
            model.description = safeGetString(json, "description");
            model.summary = safeGetString(json, "summary");
            // ... נתונים נוספים הושמטו לפשטות ...
            return model;
        }

        // --- שיטה לניתוח נתוני האפליקציה הראשיים ---

        private static AptoideApplicationInfo parseApplicationInfo(JSONObject json) throws JSONException {
            AptoideApplicationInfo model = new AptoideApplicationInfo();
            model.id = safeGetLong(json, "id");
            model.name = safeGetString(json, "name");
            model.package_ = safeGetString(json, "package_");
            model.packageName = safeGetString(json, "package"); // ממופה לשדה "package"
            model.uname = safeGetString(json, "uname");
            model.size = safeGetLong(json, "size");
            model.icon = safeGetString(json, "icon");
            model.graphic = safeGetString(json, "graphic");

            if (json.has("developer") && !json.isNull("developer")) {
                model.developer = parseDeveloper(json.optJSONObject("developer"));
            }

            // ניתוח שדה ה-FILE החשוב
            if (json.has("file") && !json.isNull("file")) {
                model.file = parseFile(json.optJSONObject("file"));
            }

            if (json.has("media") && !json.isNull("media")) {
                model.media = parseMedia(json.optJSONObject("media"));
            }
            // ... מודלים נוספים ...

            return model;
        }

        /**
         * נקודת כניסה ראשית לניתוח תגובת Aptoide JSON
         */
        public static AptoideResponse decodeAptoideResponse(String jsonString) throws Exception {
            AptoideResponse response = new AptoideResponse();
            try {
                JSONObject rootJson = new JSONObject(jsonString);

                if (rootJson.has("info") && !rootJson.isNull("info")) {
                    response.info = parseInfo(rootJson.optJSONObject("info"));
                }

                if (rootJson.has("data") && !rootJson.isNull("data")) {
                    response.data = parseApplicationInfo(rootJson.optJSONObject("data"));
                }

            } catch (JSONException e) {
                throw new Exception("Error parsing Aptoide JSON response: " + e.getMessage(), e);
            }
            return response;
        }
    
}
