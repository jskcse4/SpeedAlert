package com.jskaleel.speedalert;

import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.XML;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;


public class TaskFetchRoadSpeedLimit extends AsyncTask<Void, Void, String> {
    private LatLng southwest, northeast;
    private String boundUrl;
    private UpdateMaxSpeed listener;

    private final static String BASEURL = "http://overpass-api.de/api/xapi?*[bbox=%s,%s,%s,%s][maxspeed=*]";

    public TaskFetchRoadSpeedLimit(UpdateMaxSpeed listener, LatLng southwest, LatLng northeast) {
        this.listener = listener;
        this.southwest = southwest;
        this.northeast = northeast;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        this.boundUrl = String.format(BASEURL, southwest.longitude, southwest.latitude, northeast.longitude, northeast.latitude);
        Log.d("BoundURL", "URL--->" + boundUrl);
    }

    @Override
    protected String doInBackground(Void... params) {
        Log.d("BoundURL", "do in background--->");
        InputStream is = null;

        try {
            URL url = new URL(boundUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            conn.setReadTimeout(60000);
            conn.setConnectTimeout(60000);
            conn.setRequestMethod("GET");

            conn.setDoInput(true);
            conn.connect();
            is = conn.getInputStream();
            return readIt(is);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (is != null) {
                    is.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return "";
    }

    @Override
    protected void onPostExecute(String response) {
        super.onPostExecute(response);
        Log.d("BoundURL", "on Post Execute ---> 1");

        if (!TextUtils.isEmpty(response)) {
            Log.d("BoundURL", "on Post Execute ---> 2"+response);

            JSONObject jsonObj = null;
            try {
                Log.d("BoundURL", "on Post Execute ---> 3");
                jsonObj = XML.toJSONObject(response);
                if(jsonObj.toString() != null) {
                    JSONObject osmObject = jsonObj.optJSONObject("osm");
                    if(osmObject != null) {
                        Object wayObject = osmObject.opt("way");
                        if(wayObject != null) {
                            JSONArray tagObjArray = null;
                            if (wayObject instanceof JSONObject) {
                                tagObjArray = ((JSONObject) wayObject).optJSONArray("tag");
                            } else if (wayObject instanceof JSONArray) {
                                tagObjArray = ((JSONArray) wayObject).optJSONObject(0).optJSONArray("tag");
                            }

                            if(tagObjArray != null && tagObjArray.length() > 0) {
                                for(int i = 0; i < tagObjArray.length(); i++) {
                                    String kObject = tagObjArray.optJSONObject(i).optString("k");
                                    if(kObject.equalsIgnoreCase("maxspeed")) {
                                        Object vObject = tagObjArray.optJSONObject(i).optString("v");
                                        double maxSpeed = Double.valueOf(vObject.toString());
                                        if (listener != null) {
                                            listener.updateMaxSpeed(maxSpeed);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (JSONException e) {
                Log.d("BoundURL", "JSON Exception --->");
                e.printStackTrace();
            }
        }

        MapsActivity.isWebServiceRunnig = false;
    }

    public String readIt(InputStream stream) throws IOException {
        BufferedReader r = new BufferedReader(new InputStreamReader(stream));
        StringBuilder total = new StringBuilder();
        String line;
        while ((line = r.readLine()) != null) {
            total.append(line).append('\n');
        }
        return total.toString();
    }
}
