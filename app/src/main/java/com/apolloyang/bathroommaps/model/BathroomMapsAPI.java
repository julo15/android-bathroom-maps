package com.apolloyang.bathroommaps.model;

import com.apolloyang.deuter.Web.JSONHelper;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.common.io.CharStreams;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by julianlo on 9/28/15.
 */
public class BathroomMapsAPI {

    private static final String baseUrlString = "http://ec2-54-200-75-151.us-west-2.compute.amazonaws.com:8080";

    public static class Bathroom {

        private JSONObject mJson;
        private Marker mMarker;

        public Bathroom(JSONObject json) {
            mJson = json;
        }

        // region Methods

        public Marker addMarker(GoogleMap map) {
            mMarker = map.addMarker(new MarkerOptions()
                    .position(getLocation())
                    .title(getName()));
            return mMarker;
        }

        public void clearMarker() {
            mMarker.remove();
        }

        // endregion

        // region Properties

        public LatLng getLocation() {
            return new LatLng(JSONHelper.getDoubleWithDefaultFromJSONObject(mJson, "lat", 0),
                    JSONHelper.getDoubleWithDefaultFromJSONObject(mJson, "lon", 0));
        }

        public String getName() {
            return JSONHelper.getStringWithDefaultFromJSONObject(mJson, "name", "Nameless");
        }

        public double getRatingAverage() {
            JSONObject rating = JSONHelper.getJSONObjectFromJSONObject(mJson, "rating");
            return JSONHelper.getDoubleWithDefaultFromJSONObject(rating, "avg", 0);
        }

        public int getRatingCount() {
            JSONObject rating = JSONHelper.getJSONObjectFromJSONObject(mJson, "rating");
            return (int)JSONHelper.getDoubleWithDefaultFromJSONObject(rating, "avg", 0);
        }

        // endregion
    }

    public List<Bathroom> getBathrooms() throws Exception {
        String urlString = baseUrlString + "/bathrooms";
        URL url = new URL(urlString);
        HttpURLConnection connection = (HttpURLConnection)url.openConnection();
        JSONObject object = JSONHelper.getJSONObjectFromInputStream(connection.getInputStream());

        JSONArray array = object.getJSONArray("bathrooms");
        ArrayList<Bathroom> bathrooms = new ArrayList<Bathroom>(array.length());
        for (int i = 0; i < array.length(); i++) {
            bathrooms.add(new Bathroom(array.getJSONObject(i)));
        }
        return bathrooms;
    }

    public String addBathroom(LatLng position, String name, String category) throws Exception {
        String urlString = String.format("%s/addbathroom?lat=%f&lon=%f&name=%s&cat=%s",
                baseUrlString,
                position.latitude,
                position.longitude,
                URLEncoder.encode(name, "utf-8"),
                URLEncoder.encode(category, "utf-8"));

        String response = CharStreams.toString(new InputStreamReader((new URL(urlString)).openConnection().getInputStream()));
        return response;
    }

    public String removeBathroom(LatLng position, String name) throws Exception {
        String urlString = String.format("%s/removebathroom?lat=%f&lon=%f&name=%s",
                baseUrlString,
                position.latitude,
                position.longitude,
                URLEncoder.encode(name, "utf-8"));

        String response = CharStreams.toString(new InputStreamReader((new URL(urlString)).openConnection().getInputStream()));
        return response;
    }
}
