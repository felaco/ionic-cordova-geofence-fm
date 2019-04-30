/**
 */
package com.example;

import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class JSONParser {
    private static final String TAG = "JSONParser";

    public void loadServiceFinmarkets(String area, String rut, String clave, String action) {
        Log.i(TAG, "area -> " + area + ", rut -> " + rut + ", clave -> " + clave + ", action -> " + action);
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                loginSession(area, rut, clave, action); //Realizar aquí tu proceso!
            }
        });
    }

    private void loginSession(String area, String rut, String clave, String action) {
        StringBuilder sb = new StringBuilder();

        String http = "http://www.finmarketsbackup.cl/jsepulveda/collahuasi-sos/api/login";


        HttpURLConnection urlConnection = null;
        try {
            URL url = new URL(http);
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setDoOutput(true);
            urlConnection.setRequestMethod("POST");
            urlConnection.setUseCaches(false);
            urlConnection.setConnectTimeout(10000);
            urlConnection.setReadTimeout(10000);
            urlConnection.setRequestProperty("Content-Type", "application/json");
            urlConnection.setRequestProperty("Accept", "application/json");

            urlConnection.connect();

            //Create JSONObject here
            JSONObject jsonParam = new JSONObject();
            jsonParam.put("rut", rut);
            jsonParam.put("clave", clave);
            OutputStreamWriter out = new OutputStreamWriter(urlConnection.getOutputStream());
            out.write(jsonParam.toString());
            out.close();

            int HttpResult = urlConnection.getResponseCode();
            if (HttpResult == HttpURLConnection.HTTP_OK) {
                BufferedReader br = new BufferedReader(new InputStreamReader(
                        urlConnection.getInputStream(), "utf-8"));
                String line = null;
                while ((line = br.readLine()) != null) {
                    sb.append(line + "\n");
                }
                br.close();

                JSONObject jsonObj = new JSONObject(sb.toString());
                JSONObject jsonObjData = jsonObj.getJSONObject("data");
                String token = jsonObjData.getString("token");

                Log.d(TAG, "1: " + sb.toString());
                proceso(token,area, action);

            } else {
                Log.d(TAG, "2: " + urlConnection.getResponseMessage());
            }
        } catch (MalformedURLException e) {
            Log.e(TAG, "3: " + e.getMessage());
            e.printStackTrace();
        } catch (IOException e) {
            Log.e(TAG, "4: " + e.getMessage());
            e.printStackTrace();
        } catch (JSONException e) {
            // TODO Auto-generated catch block
            Log.e(TAG, "5: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (urlConnection != null)
                urlConnection.disconnect();
        }
    }

    private void proceso(String token, String area, String action) {
        StringBuilder sb = new StringBuilder();

        String http = "http://www.finmarketsbackup.cl/jsepulveda/collahuasi-sos/api/user/updatearea";


        HttpURLConnection urlConnection = null;
        try {
            URL url = new URL(http);
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setDoOutput(true);
            urlConnection.setRequestMethod("POST");
            urlConnection.setUseCaches(false);
            urlConnection.setConnectTimeout(10000);
            urlConnection.setReadTimeout(10000);
            urlConnection.setRequestProperty("Content-Type", "application/json");
            urlConnection.setRequestProperty("Accept", "application/json");
            urlConnection.setRequestProperty("Authorization", "Bearer " + token);

            urlConnection.connect();

            //Create JSONObject here
            JSONObject jsonParam = new JSONObject();
            jsonParam.put("area", area);
            jsonParam.put("action", action);
            OutputStreamWriter out = new OutputStreamWriter(urlConnection.getOutputStream());
            out.write(jsonParam.toString());
            out.close();

            int HttpResult = urlConnection.getResponseCode();
            if (HttpResult == HttpURLConnection.HTTP_OK) {
                BufferedReader br = new BufferedReader(new InputStreamReader(
                        urlConnection.getInputStream(), "utf-8"));
                String line = null;
                while ((line = br.readLine()) != null) {
                    sb.append(line + "\n");
                }
                br.close();

                Log.d(TAG, "1: " + sb.toString());

            } else {
                Log.d(TAG, "2: " + urlConnection.getResponseMessage());
            }
        } catch (MalformedURLException e) {
            Log.e(TAG, "3: " + e.getMessage());
            e.printStackTrace();
        } catch (IOException e) {
            Log.e(TAG, "4: " + e.getMessage());
            e.printStackTrace();
        } catch (JSONException e) {
            // TODO Auto-generated catch block
            Log.e(TAG, "5: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (urlConnection != null)
                urlConnection.disconnect();
        }
    }
}
