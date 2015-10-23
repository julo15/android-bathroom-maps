package com.apolloyang.bathroommaps.model;

import com.apolloyang.deuter.Web.JSONHelper;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
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
import java.util.Date;
import java.util.List;

/**
 * Created by julianlo on 9/28/15.
 */
public class BathroomMapsAPI {

    private static final String baseUrlString = "http://ec2-54-200-75-151.us-west-2.compute.amazonaws.com:8080";

    public static class Review {
        private JSONObject mJson;

        public Review(JSONObject json) {
            mJson = json;
        }

        public int getRating() {
            return mJson.optInt("rating", 0);
        }

        public String getText() {
            return mJson.optString("text", "");
        }

        public Date getDate() {
            return null;
        }
    }

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
                    .title(getName())
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)));
            return mMarker;
        }

        public void clearMarker() {
            mMarker.remove();
        }

        // endregion

        // region Properties

        public String getId() {
            return mJson.optString("_id", null);
        }

        public LatLng getLocation() {
            return new LatLng(mJson.optDouble("lat", 0), mJson.optDouble("lon", 0));
        }

        public String getName() {
            return mJson.optString("name", "Nameless");
        }

        public double getRatingAverage() {
            JSONObject rating = mJson.optJSONObject("rating");
            if (rating == null) {
                return 0;
            }
            return rating.optDouble("avg", 0);
        }

        public int getRatingCount() {
            return mJson.optJSONObject("rating").optInt("count", 0);
        }

        public List<Review> getReviews() {
            int count = getRatingCount();
            ArrayList<Review> reviews = new ArrayList<>(count);
            JSONArray reviewsJson = mJson.optJSONObject("rating").optJSONArray("reviews");
            for (int i = 0; i < count; i++) {
                reviews.add(new Review(reviewsJson.optJSONObject(i)));
            }
            return reviews;
        }

        // endregion
    }

    public List<Bathroom> getBathrooms(LatLng here) throws Exception {
        String urlString = String.format("%s/bathrooms?lat=%f&lon=%f",
                baseUrlString,
                here.latitude,
                here.longitude);
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
        String urlString = String.format("%s/addbathroom?admin&lat=%f&lon=%f&name=%s&cat=%s",
                baseUrlString,
                position.latitude,
                position.longitude,
                URLEncoder.encode(name, "utf-8"),
                URLEncoder.encode(category, "utf-8"));

        String response = CharStreams.toString(new InputStreamReader((new URL(urlString)).openConnection().getInputStream()));
        return response;
    }

    public String removeBathroom(String id) throws Exception {
        String urlString = String.format("%s/removebathroom?id=%s",
                baseUrlString,
                id);

        String response = CharStreams.toString(new InputStreamReader((new URL(urlString)).openConnection().getInputStream()));
        return response;
    }

    public String addReview(String id, int rating, String text) throws Exception {
        String urlString = String.format("%s/addreview?id=%s&rating=%d&text=%s",
                baseUrlString,
                id,
                rating,
                URLEncoder.encode(text, "utf-8"));

        String response = CharStreams.toString(new InputStreamReader((new URL(urlString)).openConnection().getInputStream()));
        return response;
    }
}
