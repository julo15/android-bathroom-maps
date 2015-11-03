package com.apolloyang.bathroommaps.model;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.v4.net.ConnectivityManagerCompat;

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
import java.util.HashMap;
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
            setJson(json);
        }

        // region Methods

        public void setJson(JSONObject json) {
            mJson = json;
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

    // region Exceptions

    public static class NoInternetException extends Exception {

    }

    // endregion

    // region Helpers

    private void checkIfConnectedToInternet() throws NoInternetException {
        ConnectivityManager cm = (ConnectivityManager)mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        boolean isConnected = (activeNetwork != null) && activeNetwork.isConnectedOrConnecting();
        if (!isConnected) {
            throw new NoInternetException();
        }
    }

    // endregion

    private static BathroomMapsAPI sInstance;
    private Context mContext;

    public static void initialize(Context context) {
        sInstance = new BathroomMapsAPI(context);
    }

    private BathroomMapsAPI(Context context) {
        mContext = context;
    }

    public static BathroomMapsAPI getInstance() {
        return sInstance;
    }

    public List<Bathroom> getBathrooms(LatLng here, int distance) throws Exception {
        checkIfConnectedToInternet();

        String urlString = String.format("%s/bathrooms?lat=%f&lon=%f&distance=%d",
                baseUrlString,
                here.latitude,
                here.longitude,
                distance);
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

    public Bathroom addBathroom(LatLng position, String name, String category) throws Exception {
        checkIfConnectedToInternet();

        String urlString = String.format("%s/addbathroom?lat=%f&lon=%f&name=%s&cat=%s",
                baseUrlString,
                position.latitude,
                position.longitude,
                URLEncoder.encode(name, "utf-8"),
                URLEncoder.encode(category, "utf-8"));

        JSONObject object = JSONHelper.getJSONObjectFromInputStream((new URL(urlString)).openConnection().getInputStream());
        JSONObject result = object.getJSONObject("result");
        if (result.getInt("ok") == 1) {
            return new Bathroom(object.getJSONObject("bathroom"));
        }

        throw new Exception(result.getString("text"));
    }

    public boolean removeBathroom(String id) throws Exception {
        checkIfConnectedToInternet();

        String urlString = String.format("%s/removebathroom?id=%s",
                baseUrlString,
                id);
        JSONObject object = JSONHelper.getJSONObjectFromInputStream((new URL(urlString)).openConnection().getInputStream());
        JSONObject result = object.getJSONObject("result");
        return (result.getInt("ok") == 1);
    }

    public Bathroom addReview(String id, int rating, String text) throws Exception {
        checkIfConnectedToInternet();

        String urlString = String.format("%s/addreview?id=%s&rating=%d&text=%s",
                baseUrlString,
                id,
                rating,
                URLEncoder.encode(text, "utf-8"));

        JSONObject object = JSONHelper.getJSONObjectFromInputStream((new URL(urlString)).openConnection().getInputStream());
        JSONObject result = object.getJSONObject("result");
        if (result.getInt("ok") == 1) {
            return new Bathroom(object.getJSONObject("bathroom"));
        }

        throw new Exception(result.getString("text"));
    }
}
