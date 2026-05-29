/*
 * Yalp Store
 * Copyright (C) 2018 Sergey Yeriomin <yeriomin@gmail.com>
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package com.mdm.store;

import android.net.Uri;
import android.os.Build;
import android.text.TextUtils;
import com.github.yeriomin.playstoreapi.AuthException;
import com.github.yeriomin.playstoreapi.GooglePlayAPI;
import com.github.yeriomin.playstoreapi.GooglePlayException;
import com.github.yeriomin.playstoreapi.HttpClientAdapter;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;
/*
import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.Request;
import okhttp3.OkHttpClient;
import okhttp3.Response;
import okio.BufferedSink;
*/
import java.nio.Buffer;
/*
import okio.Source;
import okio.Okio;
*/
import java.io.File;
import java.util.HashMap;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import com.emanuelef.remote_capture.activities.LogUtil;

public class NativeHttpClientAdapter extends HttpClientAdapter {

    static private final int TIMEOUT = 15000;
    static private final int BUFFER_SIZE = 8192;

    static {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.FROYO) {
            System.setProperty("http.keepAlive", "false");
        }
    }

    @Override
    public byte[] get(String url, Map<String, String> params, Map<String, String> headers) throws IOException {
        return request(getHttpURLConnection(buildUrl(url, params)), null, headers);
    }

    @Override
    public byte[] getEx(String url, Map<String, List<String>> params, Map<String, String> headers) throws IOException {
        return request(getHttpURLConnection(buildUrlEx(url, params)), null, headers);
    }

    @Override
    public byte[] postWithoutBody(String url, Map<String, String> urlParams, Map<String, String> headers) throws IOException {
        return post(buildUrl(url, urlParams), new byte[0], headers);
    }

    @Override
    public byte[] post(String url, Map<String, String> params, Map<String, String> headers) throws IOException {
        headers.put("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
        return post(url, buildFormBody(params).getBytes(), headers);
    }
    /*
    public String postauth(String url, byte[] b) throws IOException {
        
        RequestBody re= RequestBody.create(MediaType.parse("application/json"),b,0,b.length);
        /*BufferedSink ggb=Okio.buffer(Okio.appendingSink(new File("/storage/emulated/0/logg.txt")));
        re.writeTo(ggb);
        ggb.buffer().readAll(Okio.appendingSink(new File("/storage/emulated/0/logg.txt")));
        LogUtil.logToFile(ggb.buffer().readUtf8());*/
  /*      Response resp= new OkHttpClient().newCall(
        new Request.Builder().url("https://auroraoss.com/api/auth").header("User-Agent","com.aurora.store.nightly-4.8.0-9d80b1dd5-72").method("POST",re).build()).execute();
        LogUtil.logToFile( resp.isSuccessful()+""+new String(resp.body().bytes())+resp.message()+resp);
        if(resp.isSuccessful())
        return new String(resp.body().bytes());
        else return "not";
    }*/
    

    public String makeNetworkRequestnew(byte[] b) {
        HttpURLConnection urlConnection = null;
        try {
            URL url = new URL("https://auroraoss.com/api/auth");
            urlConnection = (HttpURLConnection) url.openConnection();

            // Configure Request
            urlConnection.setRequestMethod("POST");
            urlConnection.setRequestProperty("User-Agent", "com.aurora.store.nightly-4.8.0-9d80b1dd5-72");
            urlConnection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            // Explicitly set the content length
            urlConnection.setRequestProperty("Content-Length", String.valueOf(b.length));

            urlConnection.setDoOutput(true);
            urlConnection.setDoInput(true);

            // Write payload to the server
            try (OutputStream os = urlConnection.getOutputStream()) {
                os.write(b, 0, b.length);
                os.flush();
            }

            // Check Response Code
            int responseCode = urlConnection.getResponseCode();
            boolean isSuccessful = (responseCode >= 200 && responseCode < 300);

            // Switch to ErrorStream if response code is not 2xx to prevent FileNotFoundException
            InputStream is = isSuccessful ? urlConnection.getInputStream() : urlConnection.getErrorStream();
            byte[] responseBytes = new byte[0];

            if (is != null) {
                try {ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    byte[] buffer = new byte[1024];
                    int len;
                    while ((len = is.read(buffer)) != -1) {
                        bos.write(buffer, 0, len);
                    }
                    responseBytes = bos.toByteArray();
                } finally {
                    is.close();
                }
            }

            String responseString = new String(responseBytes, StandardCharsets.UTF_8);

            // Log details for debugging
            LogUtil.logToFile("Status Code: " + responseCode);
            LogUtil.logToFile("Response Body: " + responseString);

            if (isSuccessful) {
                return responseString;
            } else {
                return "not";
            }

        } catch (Exception e) {
            e.printStackTrace();
            LogUtil.logToFile(e);
            return "not";
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }
    }
    /*
    public String makeNetworkRequest(byte[] b) {
        HttpURLConnection urlConnection = null;
        try {
            // Create the URL object
            URL url = new URL("https://auroraoss.com/api/auth");
            urlConnection = (HttpURLConnection) url.openConnection();

            // Configure the request
            urlConnection.setRequestMethod("POST");
            urlConnection.setRequestProperty("User-Agent", "com.aurora.store.nightly-4.8.0-9d80b1dd5-72");
            urlConnection.setRequestProperty("Content-Type", "application/json");
            urlConnection.setDoOutput(true);
            urlConnection.setDoInput(true);

            // Write the request body
            try (OutputStream os = urlConnection.getOutputStream()) {
                os.write(b, 0, b.length);
                os.flush();
            }

            // Get the response code
            int responseCode = urlConnection.getResponseCode();
            boolean isSuccessful = (responseCode >= 200 && responseCode < 300);

            // Read the response body
            InputStream is = isSuccessful ? urlConnection.getInputStream() : urlConnection.getErrorStream();
            byte[] responseBytes = new byte[0];

            if (is != null) {
                try {ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    byte[] buffer = new byte[1024];
                    int len;
                    while ((len = is.read(buffer)) != -1) {
                        bos.write(buffer, 0, len);
                    }
                    responseBytes = bos.toByteArray();
                } finally {
                    is.close();
                }
            }

            String responseString = new String(responseBytes);
            String responseMessage = urlConnection.getResponseMessage();

            // Log the results similarly to the original code
            // Note: Replace LogUtil.logToFile with your actual logging mechanism
            LogUtil.logToFile(isSuccessful + "" + responseString + responseMessage + urlConnection.toString());

            if (isSuccessful) {
                return responseString;
            } else {
                return "not";
            }

        } catch (Exception e) {
            e.printStackTrace();
            LogUtil.logToFile(e);
            return "not";
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }
    }*/
    public String postauthgood(String url, byte[] body) throws IOException {
        Map<String, String> headers = new HashMap<String, String>();
        headers.put("User-Agent", "com.aurora.store.nightly-4.8.0-9d80b1dd5-72");
        HttpURLConnection connection = getHttpURLConnection(url);
        connection.setRequestMethod("POST");
        return new String(request(connection, body, headers));
        /*RequestBody re= RequestBody.create(MediaType.parse("application/json"),b,0,b.length);
        BufferedSink ggb=Okio.buffer(Okio.appendingSink(new File("/storage/emulated/0/logg.txt")));
        re.writeTo(ggb);
        ggb.buffer().readAll(Okio.appendingSink(new File("/storage/emulated/0/logg.txt")));
        LogUtil.logToFile(ggb.buffer().readUtf8());
        Response resp= new OkHttpClient().newCall(
            new Request.Builder().url("https://auroraoss.com/api/auth").header("User-Agent","com.aurora.store.nightly-4.8.0-9d80b1dd5-72").method("POST",re).build()).execute();
        LogUtil.logToFile( resp.isSuccessful()+""+new String(resp.body().bytes())+resp.message()+resp);
        if(resp.isSuccessful())
            return new String(resp.body().bytes());
        else return "not";*/
    }
    public byte[] postauthold(String url, Map<String, String> params, Map<String, String> headers) throws IOException {
        return post(url, params, headers);
    }
    @Override
    public byte[] post(String url, byte[] body, Map<String, String> headers) throws IOException {
        if (!headers.containsKey("Content-Type")) {
            headers.put("Content-Type", "application/x-protobuf");
        }
        HttpURLConnection connection = getHttpURLConnection(url);
        connection.setRequestMethod("POST");
        return request(connection, body, headers);
    }

    @Override
    public String buildUrl(String url, Map<String, String> params) {
        Uri.Builder builder = Uri.parse(url).buildUpon();
        for (String key: params.keySet()) {
            builder.appendQueryParameter(key, params.get(key));
        }
        return builder.build().toString();
    }

    @Override
    public String buildUrlEx(String url, Map<String, List<String>> params) {
        Uri.Builder builder = Uri.parse(url).buildUpon();
        for (String key: params.keySet()) {
            for (String value: params.get(key)) {
                builder.appendQueryParameter(key, value);
            }
        }
        return builder.build().toString();
    }

    protected HttpURLConnection getHttpURLConnection(String url) throws IOException {
        return NetworkUtil.getHttpURLConnection(url);
    }

    protected byte[] request(HttpURLConnection connection, byte[] body, Map<String, String> headers) throws IOException {
        connection.setConnectTimeout(TIMEOUT);
        connection.setReadTimeout(TIMEOUT);
        connection.setRequestProperty("Connection", "close");
        connection.setRequestProperty("Accept-Encoding", "gzip");
        connection.addRequestProperty("Cache-Control", "max-age=300");
        for (String headerName: headers.keySet()) {
            connection.addRequestProperty(headerName, headers.get(headerName));
        }
        addBody(connection, body);

        byte[] content = new byte[0];
        LogUtil.i(getClass().getSimpleName(), "Requesting " + connection.getURL().toString());
        try {
            connection.connect();
        } catch (NullPointerException e) {
            // This is a known bug in Android 7.0; it was fixed by this change which went into Android 7.1:
            // https://android-review.googlesource.com/#/c/271775/
            // https://github.com/square/okhttp/issues/3245
            throw new IOException("This is a known bug in Android 7.0; it was fixed by this change which went into Android 7.1: " + e.getMessage());
        }

        int code = 0;
        boolean isGzip;
        try {
            isGzip = null != connection.getContentEncoding() && connection.getContentEncoding().contains("gzip");
        } catch (NullPointerException e) {
            // Happens on api<=8 only, see https://issuetracker.google.com/issues/36926705
            // The solution is stop using HttpURLConnection entirely...
            // Luckily, it seems to happen when the token gets stale,
            // which means it can be fixed by redoing the request with a new token
            LogUtil.e(getClass().getSimpleName(), "Buggy HttpURLConnection implementation detected");
            throw new AuthException("Actually this is a NullPointerException thrown by a buggy implementation of HttpURLConnection", 401);
        }
        try {
            code = connection.getResponseCode();
            LogUtil.i(getClass().getSimpleName(), "HTTP result code " + code + " Cache " + connection.getHeaderField("X-Android-Response-Source")+" "+connection.getResponseMessage());
            content = readFully(connection.getInputStream(), isGzip);
        } catch (IOException e) {
            content = readFully(connection.getErrorStream(), isGzip);
            LogUtil.e(getClass().getSimpleName(), "IOException " + e.getClass().getName() + " " + e.getMessage()+" "+new String(content));
            if (code < 400) {
                throw e;
            }
        } catch (Throwable e) {
            LogUtil.e(getClass().getSimpleName(), "Unknown exception " + e.getClass().getName() + " " + e.getMessage());
        } finally {
            connection.disconnect();
        }
        processHttpErrorCode(code, content);
        return content;
    }

    static public String urlEncode(String input) {
        try {
            return URLEncoder.encode(input, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            // Unlikely
        }
        return null;
    }

    static private void addBody(HttpURLConnection connection, byte[] body) throws IOException {
        if (null == body) {
            body = new byte[0];
        }
        connection.addRequestProperty("Content-Length", Integer.toString(body.length));
        if (body.length > 0) {
            connection.setDoOutput(true);
            OutputStream outputStream = connection.getOutputStream();
            outputStream.write(body);
            outputStream.close();
        } else {
            connection.setDoOutput(false);
        }
    }

    static private void processHttpErrorCode(int code, byte[] content) throws GooglePlayException {
        if (code < 400) {
            return;
        }
        GooglePlayException e = new GooglePlayException("Client error " + code, code);
        if (code == 401 || code == 403) {
            e = new AuthException("Auth error", code);
            Map<String, String> authResponse = GooglePlayAPI.parseResponse(new String(content));
            if (authResponse.containsKey("Error") && authResponse.get("Error").equals("NeedsBrowser")) {
                ((AuthException) e).setTwoFactorUrl(authResponse.get("Url"));
            }
            LogUtil.logToFile(code+" "+new String(content));
        } else if (code == 429) {
            e = new GooglePlayException("You are making too many requests, try again later", code);
        } else if (code >= 500) {
            e = new GooglePlayException("Server error " + code, code);
        }
        e.setRawResponse(content);
        throw e;
    }

    static private byte[] readFully(InputStream inputStream, boolean gzipped) throws IOException {
        if (null == inputStream) {
            return new byte[0];
        }
        if (gzipped) {
            inputStream = new GZIPInputStream(inputStream);
        }
        InputStream bufferedInputStream = new BufferedInputStream(inputStream, BUFFER_SIZE);
        byte[] buffer = new byte[BUFFER_SIZE];
        int bytesRead;
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        while ((bytesRead = bufferedInputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, bytesRead);
        }
        byte[] result = outputStream.toByteArray();
        Util.closeSilently(bufferedInputStream);
        Util.closeSilently(outputStream);
        return result;
    }

    static private String buildFormBody(Map<String, String> params) {
        List<String> keyValuePairs = new ArrayList<>();
        for (String key: params.keySet()) {
            keyValuePairs.add(urlEncode(key) + "=" + urlEncode(params.get(key)));
        }
        return TextUtils.join("&", keyValuePairs);
    }
}
