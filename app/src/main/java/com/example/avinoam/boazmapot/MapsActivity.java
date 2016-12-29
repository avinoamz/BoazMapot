package com.example.avinoam.boazmapot;

import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;


import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.List;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback,
        LocationListener, GoogleApiClient.ConnectionCallbacks {

    private GoogleMap mMap;
    private GoogleApiClient mGoogleApiClient;
    private Location currentLocation, lastLocation;
    private long currentTime, lastTime;
    private LocationManager locationManager;
    private Firebase ref;
    protected LocationRequest mLocationRequest;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addApi(LocationServices.API)
                .build();

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        createLocationRequest();
        Firebase.setAndroidContext(this);

        ref = new Firebase(Config.FIREBASE_URL);

        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                mMap.clear();
                for (DataSnapshot postSnapshot : snapshot.getChildren()) {

                    if (postSnapshot.child("Latitude").exists() && postSnapshot.child("Longitude").exists()) {
                        double lat = postSnapshot.child("Latitude").getValue(Double.class);
                        double lon = postSnapshot.child("Longitude").getValue(Double.class);
                        String name = postSnapshot.child("Nickname").getValue(String.class);
                        mMap.addMarker(new MarkerOptions()
                                .position(new LatLng(lat, lon))
                                .title(name));
                    }
                }
            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {
                System.out.println("The read failed: " + firebaseError.getMessage());
            }
        });


        currentTime = System.currentTimeMillis();
        lastTime = System.currentTimeMillis();

    }

    protected void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(10000);
        mLocationRequest.setFastestInterval(5000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(mLocationRequest);
        builder.build();
    }


    @Override
    protected void onStart() {
        mGoogleApiClient.connect();
        super.onStart();
    }

    @Override
    protected void onStop() {
        mGoogleApiClient.disconnect();
        super.onStop();
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        try {
            LocationServices.FusedLocationApi.requestLocationUpdates(
                    mGoogleApiClient, mLocationRequest, this);
        } catch (SecurityException e) {

        }
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {

        mMap = googleMap;

        try {
            mMap.setMyLocationEnabled(true);
        } catch (SecurityException e) {
            //TODO
        }

        Location location = getLastKnownLocation();
        if (location != null) {
            if (Config.isBLeLoc) {
                Double lat = Double.parseDouble(Config.bleLoc.getLatitude());
                Double lng = Double.parseDouble(Config.bleLoc.getLongitude());
                LatLng point = new LatLng(lat, lng);
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(point, 15));
                Config.isBLeLoc = false;
            } else {
                LatLng point = new LatLng(location.getLatitude(), location.getLongitude());
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(point, 15));
            }
        } else {
            Toast.makeText(this, "Location not Found. Activate GPS, or try again", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        lastTime = currentTime;
        currentTime = System.currentTimeMillis();
        lastLocation = currentLocation;
        currentLocation = location;

        SaveGPSLocation(null);

        TextView speedTextView = (TextView) findViewById(R.id.textViewSpeed);
        speedTextView.setClickable(false);
        speedTextView.setCursorVisible(false);

        if (lastTime != currentTime) {
            long dist = calculateDistance(lastLocation.getLatitude(), lastLocation.getLongitude(), currentLocation.getLatitude(), currentLocation.getLatitude());
            long timePassed = (currentTime - lastTime);
            long mSpeed = dist / (timePassed * 10);
            speedTextView.setText("Speed: " + mSpeed);
        }

    }

    private static long calculateDistance(double lat1, double lng1, double lat2, double lng2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1))
                * Math.cos(Math.toRadians(lat2)) * Math.sin(dLon / 2)
                * Math.sin(dLon / 2);
        double c = 2 * Math.asin(Math.sqrt(a));
        long distanceInMeters = Math.round(6371000 * c);
        return distanceInMeters;
    }

    private Location getLastKnownLocation() {
        try {
            locationManager = (LocationManager) getApplicationContext().getSystemService(LOCATION_SERVICE);
            List<String> providers = locationManager.getProviders(true);
            Location bestLocation = null;
            for (String provider : providers) {
                Location l = locationManager.getLastKnownLocation(provider);
                if (l == null) {
                    continue;
                }
                if (bestLocation == null || l.getAccuracy() < bestLocation.getAccuracy()) {
                    // Found best last known location: %s", l);
                    bestLocation = l;
                }
            }
            lastLocation = currentLocation;
            currentLocation = bestLocation;
            Config.myLoc = bestLocation;

            return bestLocation;
        } catch (SecurityException e) {

        }
        return null;
    }


    public void SaveGPSLocation(View v) {

        Location location = getLastKnownLocation();
        if (Config.getUser() != null) {
            String name = Config.getUser().getDisplayName();
            if (location != null) {
                ref.child(name).removeValue();

                LatLng point = new LatLng(location.getLatitude(), location.getLongitude());
                ref.child(name).child("Latitude").setValue(location.getLatitude());
                ref.child(name).child("Longitude").setValue(location.getLongitude());
                ref.child(name).child("Nickname").setValue(name);


                // Toast.makeText(this, "Saving Location", Toast.LENGTH_SHORT).show();
            } else {
                //Location is null
            }

        } else {
            //   Toast.makeText(this, "User Not Found, return to login page", Toast.LENGTH_SHORT).show();
        }

    }

    public void LoadQR(View v) {
        IntentIntegrator scanIntegrator = new IntentIntegrator(this);
        scanIntegrator.initiateScan();
    }

    public void LoadBLE(View v) {

        Intent i = new Intent(this, BLEActivity.class);
        startActivityForResult(i, 1);
    }

    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        //retrieve scan result
        if (requestCode == 980) {
        } else if (resultCode == 979) {
            String stredittext = intent.getStringExtra("BLE_Status");
            if (stredittext.equals("newBLE")) {
                Double lat = Double.parseDouble(Config.bleLoc.getLatitude());
                Double lng = Double.parseDouble(Config.bleLoc.getLongitude());
                LatLng point = new LatLng(lat, lng);
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(point, 15));
                Config.isBLeLoc = false;
            }
        } else {

            try {
                IntentResult scanningResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, intent);
                if (scanningResult != null) {
                    List<String> data = scanningResult.getLocation();
                    if (data == null)
                        return;
                    Double lat = Double.parseDouble(data.get(0));
                    Double lon = Double.parseDouble(data.get(1));
                    String madeBy = data.get(2);
                    LatLng point = new LatLng(lat, lon);
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(point, 15));
                    Toast.makeText(this, "Created By " + madeBy, Toast.LENGTH_SHORT).show();
                } else {
                    Toast toast = Toast.makeText(getApplicationContext(),
                            "No scan data received!", Toast.LENGTH_SHORT);
                    toast.show();
                }
            } catch (Exception e) {
            }
        }
    }

    public void SaveQR(View v) {
        Location location = getLastKnownLocation();
        if (location != null) {
            String name = Config.getUser().getDisplayName();
            Double lat = location.getLatitude();
            Double lon = location.getLongitude();
            final String msg = lat + "," + lon + "," + name;

            AlertDialog alertDialog = new AlertDialog.Builder(this).create();
            alertDialog.setTitle("Create QR Code with the following info");

            alertDialog.setMessage(msg);
            alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "Copy data",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            ClipboardManager manager =
                                    (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                            manager.setText(msg);
                            dialog.dismiss();
                        }
                    });
            alertDialog.show();

        } else {
            Toast.makeText(this, "Can't find current location, maybe GPS is closed", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onBackPressed() {
        Intent a = new Intent(Intent.ACTION_MAIN);
        a.addCategory(Intent.CATEGORY_HOME);
        a.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(a);
    }
}
