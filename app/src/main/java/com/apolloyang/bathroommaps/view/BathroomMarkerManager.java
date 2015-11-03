package com.apolloyang.bathroommaps.view;

import com.apolloyang.bathroommaps.R;
import com.apolloyang.bathroommaps.model.BathroomMapsAPI;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.HashMap;
import java.util.List;

/**
 * Created by julianlo on 10/23/15.
 */
public class BathroomMarkerManager {

    private GoogleMap mGoogleMap;

    // Storage
    private HashMap<Marker, BathroomMapsAPI.Bathroom> mMarkerBathroomHashMap = new HashMap<>();
    private HashMap<String, BathroomMapsAPI.Bathroom> mStringBathroomHashMap = new HashMap<>();
    private HashMap<BathroomMapsAPI.Bathroom, Marker> mBathroomMarkerHashMap = new HashMap<>();

    // Bathroom icons
    public static final int POO_ICON = R.drawable.icon_poo_40x40_brown; // R.drawable.ic_wc_black_18dp, 24dp
    public static final int POO_ICON_BIG = R.drawable.icon_poo_80x80_brown;

    public BathroomMarkerManager(GoogleMap map) {
        mGoogleMap = map;
    }

    public void init(List<BathroomMapsAPI.Bathroom> bathrooms) {
        clearAll();

        for (BathroomMapsAPI.Bathroom bathroom : bathrooms) {
            add(bathroom);
        }
    }

    public void addOrUpdate(BathroomMapsAPI.Bathroom bathroom) {
        if (mStringBathroomHashMap.containsKey(bathroom.getId())) {
            remove(bathroom.getId());
        }
        // TODO: Pending bathroom should perhaps look different
        add(bathroom);
    }

    public void remove(String id) {
        BathroomMapsAPI.Bathroom bathroom = mStringBathroomHashMap.get(id);
        Marker marker = mBathroomMarkerHashMap.get(bathroom);
        marker.remove();

        mMarkerBathroomHashMap.remove(marker);
        mStringBathroomHashMap.remove(bathroom.getId());
        mBathroomMarkerHashMap.remove(bathroom);
    }

    private void add(BathroomMapsAPI.Bathroom bathroom) {
        // Add new
        Marker marker = mGoogleMap.addMarker(new MarkerOptions()
                        .position(bathroom.getLocation())
                        .title(bathroom.getName())
                        .icon(BitmapDescriptorFactory.fromResource(BathroomMarkerManager.POO_ICON)));
                mMarkerBathroomHashMap.put(marker, bathroom);
        mStringBathroomHashMap.put(bathroom.getId(), bathroom);
        mBathroomMarkerHashMap.put(bathroom, marker);
    }

    public BathroomMapsAPI.Bathroom getBathroom(Marker marker) {
        return mMarkerBathroomHashMap.get(marker);
    }

    private void clearAll() {
        for (Marker marker : mMarkerBathroomHashMap.keySet()) {
            marker.remove();
        }

        mMarkerBathroomHashMap.clear();
        mStringBathroomHashMap.clear();
        mBathroomMarkerHashMap.clear();
    }
}
