package com.apolloyang.bathroommaps.view;

import android.animation.Animator;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Location;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.SpannableString;
import android.text.style.UnderlineSpan;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import com.apolloyang.bathroommaps.R;
import com.apolloyang.bathroommaps.model.BathroomMapsAPI;
import com.apolloyang.bathroommaps.model.GoogleDirectionsAPI;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.HashMap;
import java.util.List;

public class MainActivity extends AppCompatActivity
    implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener,
        AddBathroomDialogFragment.Listener {

    private static final String DIALOG_ADDBATHROOM = "addbathroom";
    private static final String DIALOG_REVIEWS = "reviews";

    private GoogleApiClient mGoogleApiClient;
    private GoogleMap mGoogleMap;
    private boolean mGoogleApiConnected;
    private Marker mCurrentMarker;
    private Toolbar mToolbar;
    private View mWalkButton;
    private TextView mWalkingTimeTextView;
    private TextView mRatingTextView;
    private View mAddButton;
    private Marker mAddMarker;
    private BathroomMarkerManager mManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mToolbar = (Toolbar)findViewById(R.id.my_toolbar_bathroom);
        //setSupportActionBar(mToolbar);
        //getSupportActionBar().hide();

        mWalkingTimeTextView = (TextView)findViewById(R.id.time_textview);

        View.OnClickListener onClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ReviewsDialogFragment dialogFragment = new ReviewsDialogFragment(mManager.getBathroom(mCurrentMarker), MainActivity.this);
                dialogFragment.show(getSupportFragmentManager(), DIALOG_REVIEWS);
            }
        };

        mRatingTextView = (TextView)findViewById(R.id.rating_textview);
        mRatingTextView.setOnClickListener(onClickListener);

        findViewById(R.id.rating_ratingbar).setOnClickListener(onClickListener);

        mWalkButton = findViewById(R.id.walk_button);
        mWalkButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String uriString = String.format("google.navigation:q=%s&mode=w&z=%d",
                        Uri.encode(String.format("%f,%f",
                                mCurrentMarker.getPosition().latitude,
                                mCurrentMarker.getPosition().longitude)),
                        15);

                Uri uri = Uri.parse(uriString);
                Intent mapIntent = new Intent(Intent.ACTION_VIEW, uri);
                mapIntent.setPackage("com.google.android.apps.maps");
                if (mapIntent.resolveActivity(getPackageManager()) != null) {
                    startActivity(mapIntent);
                }
            }
        });

        findViewById(R.id.locate_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ensureMapCentered(null, true /* resetZoom */);
                animateToDefaultView();
            }
        });

        mAddButton = findViewById(R.id.add_button);
        mAddButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                animateToolbar(false);

                if (mAddMarker != null) {
                    mAddMarker.remove();
                }

                mAddMarker = mGoogleMap.addMarker(new MarkerOptions()
                        .position(mGoogleMap.getCameraPosition().target)
                        .draggable(true));
            }
        });

        /*
        int code = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(this);
        if (code != ConnectionResult.SUCCESS) {
            int i = 2;
            i++;
        }
        */

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
    }

    @Override
    protected void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
    }

    @Override
    protected void onStop() {
        mGoogleApiClient.disconnect();
        super.onStop();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        //getMenuInflater().inflate(R.menu.menu_main, menu);
        return false;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onMapReady(GoogleMap map) {
        mGoogleMap = map;
        map.setMyLocationEnabled(true);
        map.getUiSettings().setMapToolbarEnabled(false);
        map.getUiSettings().setMyLocationButtonEnabled(false);

        map.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {

                if (mCurrentMarker != null) {
                    mCurrentMarker.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.ic_wc_black_18dp));
                }
                mCurrentMarker = marker;
                if ((mAddMarker != null) && (mCurrentMarker.getId().equals(mAddMarker.getId()))) {
                    AddBathroomDialogFragment dialogFragment = new AddBathroomDialogFragment(AddBathroomDialogFragment.Mode.ADDBATHROOM, mAddMarker.getPosition(), null, MainActivity.this);
                    dialogFragment.show(getSupportFragmentManager(), DIALOG_ADDBATHROOM);
                    mAddMarker.remove();
                    mAddMarker = null;
                    mCurrentMarker = null;
                } else {
                    mCurrentMarker.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.ic_wc_black_24dp));
                    BathroomMapsAPI.Bathroom bathroom = mManager.getBathroom(marker);
                    setToolbarBathroom(bathroom);

                    animateToolbar(true);
                    ensureMapCentered(marker.getPosition(), false /* resetZoom */);

                    Location hereLoc = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
                    if (hereLoc != null) {
                        GetWalkingTimeTask task = new GetWalkingTimeTask(mWalkingTimeTextView);
                        task.execute(
                                new LatLng(hereLoc.getLatitude(), hereLoc.getLongitude()),
                                marker.getPosition()
                        );
                    }
                }

                return true;
            }
        });

        map.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener() {
            @Override
            public void onMapLongClick(LatLng latLng) {
                if (mCurrentMarker != null) {
                    final BathroomMapsAPI.Bathroom bathroom = mManager.getBathroom(mCurrentMarker);
                    new AlertDialog.Builder(MainActivity.this)
                            .setTitle(mCurrentMarker.getTitle())
                            .setMessage("Remove this bathroom?")
                            .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    RemoveBathroomTask task = new RemoveBathroomTask(MainActivity.this,
                                            bathroom.getId());
                                    task.execute();
                                }
                            })
                            .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                }
                            })
                            .show();
                }
            }
        });

        map.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                animateToDefaultView();
            }
        });

        // Need this here for the marker to actually update
        map.setOnMarkerDragListener(new GoogleMap.OnMarkerDragListener() {
            @Override
            public void onMarkerDragStart(Marker marker) {

            }

            @Override
            public void onMarkerDrag(Marker marker) {

            }

            @Override
            public void onMarkerDragEnd(Marker marker) {
            }
        });

        /*
        map.setOnCameraChangeListener(new GoogleMap.OnCameraChangeListener() {
            @Override
            public void onCameraChange(CameraPosition cameraPosition) {
                if ((mCurrentMarker != null) &&
                        ((cameraPosition.target.latitude != mCurrentMarker.getPosition().latitude) ||
                         (cameraPosition.target.longitude != mCurrentMarker.getPosition().longitude))) {
                    LatLng x = mCurrentMarker.getPosition();
                    animateToolbar(false);
                }
            }
        });
        */

        mManager = new BathroomMarkerManager(mGoogleMap);
        AddMarkersTask task = new AddMarkersTask(mGoogleApiClient, map, mManager);
        task.execute();
        ensureMapCentered(null /* use current location */, true /* resetZoom */);
    }

    @Override
    public void onBathroomAddedOrUpdated(BathroomMapsAPI.Bathroom bathroom, Exception exception) {
        Toast toast = Toast.makeText(this, (exception == null) ? "Success!" : exception.toString(), Toast.LENGTH_LONG);
        toast.show();
        if (bathroom != null) {
            mManager.addOrUpdate(bathroom);
            mCurrentMarker = null;
            setToolbarBathroom(bathroom);
        }
    }

    private void setToolbarBathroom(BathroomMapsAPI.Bathroom bathroom) {
        ((TextView) findViewById(R.id.name_textview)).setText(bathroom.getName());

        int count = bathroom.getRatingCount();

        RatingBar ratingBar = (RatingBar)findViewById(R.id.rating_ratingbar);
        ratingBar.setVisibility((count > 0) ? View.VISIBLE : View.GONE);
        ratingBar.setRating((float)bathroom.getRatingAverage());

        String text =
                (count > 0) ? String.format("(%d reviews)",
                        bathroom.getRatingCount()) : "No reviews";
        SpannableString span = new SpannableString(text);
        span.setSpan(new UnderlineSpan(), 0, span.length(), 0);
        mRatingTextView.setText(span);
    }

    private void animateToDefaultView() {
        animateToolbar(false);
        if (mCurrentMarker != null) {
            mCurrentMarker.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.ic_wc_black_18dp));
            mCurrentMarker = null;
        }
    }

    private void animateToolbar(boolean show) {
        final long ANIMATION_DURATION = 200;
        if (show) {
            mWalkButton.setVisibility(View.VISIBLE);
            mWalkButton.animate()
                    .alpha(1)
                    .setDuration(ANIMATION_DURATION);

            mToolbar.setVisibility(View.VISIBLE);
            //getSupportActionBar().show();
            mToolbar.animate()
                    .alpha(1)
                    .setDuration(ANIMATION_DURATION)
                    .setListener(null);
        } else {
            mWalkButton.animate()
                    .alpha(0)
                    .setDuration(ANIMATION_DURATION);
            mToolbar.animate()
                    .alpha(0)
                    .setDuration(ANIMATION_DURATION)
                    .setListener(new Animator.AnimatorListener() {
                        @Override
                        public void onAnimationStart(Animator animation) {

                        }

                        @Override
                        public void onAnimationEnd(Animator animation) {
                            mToolbar.setVisibility(View.GONE);
                            //getSupportActionBar().hide();
                            mWalkButton.setVisibility(View.GONE);
                        }

                        @Override
                        public void onAnimationCancel(Animator animation) {

                        }

                        @Override
                        public void onAnimationRepeat(Animator animation) {

                        }
                    });
        }
    }

    private void ensureMapCentered(LatLng latLng, boolean resetZoom) {
        // handle race between retrieving googlemap (onMapReady) and connecting to google api (onConnected)
        if (mGoogleApiConnected && (mGoogleMap != null)) {

            // If no location, use current location
            if (latLng == null) {
                Location hereLoc = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
                if (hereLoc != null) {
                    latLng = new LatLng(hereLoc.getLatitude(), hereLoc.getLongitude());
                }
            }

            if (latLng != null) {

                CameraPosition.Builder builder = CameraPosition.builder()
                        .target(latLng)
                        .zoom(resetZoom ? 16 : mGoogleMap.getCameraPosition().zoom);
                mGoogleMap.animateCamera(CameraUpdateFactory.newCameraPosition(builder.build()), 200, null);
            }
        }
    }

    // GoogleApiClient.ConnectionCallbacks
    @Override
    public void onConnected(Bundle var1)
    {
        onDoneConnecting("connected");
        mGoogleApiConnected = true;

        SupportMapFragment mapFragment = (SupportMapFragment)getSupportFragmentManager().findFragmentById(R.id.fragment);
        mapFragment.getMapAsync(this);
    }

    @Override
    public void onConnectionSuspended(int var1)
    {
        onDoneConnecting("suspended");
    }

    // GoogleApiClient.OnConnectionFailedListener
    @Override
    public void onConnectionFailed(ConnectionResult var1)
    {
        onDoneConnecting("failed");
    }

    private void onDoneConnecting(String message) {
        Toast toast = Toast.makeText(this, message, Toast.LENGTH_SHORT);
        toast.show();
    }

    // Parameters
    // Progress
    // Result
    private static class AddMarkersTask extends AsyncTask<Void, Void, List<BathroomMapsAPI.Bathroom>> {

        private GoogleApiClient mGoogleApiClient;
        private GoogleMap mMap;
        private BathroomMarkerManager mManager;

        public AddMarkersTask(GoogleApiClient googleApiClient, GoogleMap map, BathroomMarkerManager manager) {
            mGoogleApiClient = googleApiClient;
            mMap = map;
            mManager = manager;
        }

        @Override
        protected List<BathroomMapsAPI.Bathroom> doInBackground(Void... params) {
            BathroomMapsAPI api = new BathroomMapsAPI();
            List<BathroomMapsAPI.Bathroom> bathrooms = null;
            try {
                // TODO: Check if this will return null
                Location hereLoc = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
                bathrooms = api.getBathrooms(new LatLng(hereLoc.getLatitude(), hereLoc.getLongitude()));
            } catch (Exception e) {
                System.err.println(e);
            }
            return bathrooms;
        }

        @Override
        protected void onPostExecute(List<BathroomMapsAPI.Bathroom> result) {
            if (result != null) {
                mManager.init(result);
            }
        }
    }

    private static class GetWalkingTimeTask extends AsyncTask<LatLng, Void, String> {
        private TextView mTimeTextView;

        public GetWalkingTimeTask(TextView timeTextView) {
            mTimeTextView = timeTextView;
        }

        @Override
        protected void onPreExecute() {
            mTimeTextView.setText(null);
        }

        @Override
        protected String doInBackground(LatLng... params) {
            try {
                return GoogleDirectionsAPI.getWalkingTime(params[0], params[1]);
            } catch (Exception e) {
                Log.e("BathroomMaps", "GetWalkingTimeTask exception: " + e.toString());
            }
            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            mTimeTextView.setText(result);
        }
    }

    // Parameters
    // Progress
    // Result
    private class RemoveBathroomTask extends AsyncTask<Void, Void, Boolean> {

        private Context mContext;
        private String mId;

        public RemoveBathroomTask(Context context, String id) {
            mContext = context;
            mId = id;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            BathroomMapsAPI api = new BathroomMapsAPI();
            try {
                return api.removeBathroom(mId);
            } catch (Exception e) {
                System.err.println(e);
            }
            return false;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            Toast toast = Toast.makeText(mContext, result ? "Bathroom removed" : "Something went wrong", Toast.LENGTH_LONG);
            toast.show();

            mManager.remove(mId);
            mCurrentMarker = null;
        }
    }
}
