package com.apolloyang.bathroommaps.model;

import com.apolloyang.deuter.Web.JSONHelper;
import com.google.android.gms.maps.model.LatLng;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Created by julianlo on 10/9/15.
 */
public class GoogleDirectionsAPI {
    private static final String BASE_URL = "https://maps.googleapis.com/maps/api";
    private static final String API_KEY = "AIzaSyBCgkWIlFfCl2t-0bLvU_58A-Z70lVszw0";

    public static String getWalkingTime(LatLng from, LatLng to) throws MalformedURLException, IOException, JSONException {
        String urlString = String.format("%s/directions/json?origin=%s&destination=%s&mode=walking&key=%s",
                BASE_URL,
                convertLatLngToString(from),
                convertLatLngToString(to),
                API_KEY);
        URL url = new URL(urlString);
        JSONObject obj = JSONHelper.getJSONObjectFromInputStream(url.openConnection().getInputStream());
        String text = obj.getJSONArray("routes").getJSONObject(0).getJSONArray("legs").getJSONObject(0).getJSONObject("duration").getString("text");
        return text;
    }

    private static String convertLatLngToString(LatLng loc) {
        return String.format("%f,%f", loc.latitude, loc.longitude);
    }
}
