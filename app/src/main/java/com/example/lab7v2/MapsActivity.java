package com.example.lab7v2;

import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.nfc.Tag;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.animation.OvershootInterpolator;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
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
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback,
        GoogleMap.OnMapLoadedCallback,
        GoogleMap.OnMarkerClickListener,
        GoogleMap.OnMapLongClickListener, SensorEventListener {


    // sensor
    static public SensorManager mSensorManager;
    static List<Sensor> SensorList;
    static final public String SENSOR_TYPE = "sensortype";
    private Sensor mSensor;
    private boolean measurOn = false;
    private TextView txtView;

    // info is the point and sup is for suppressing the marker
    FloatingActionButton fabInfo, fabSup;
    Boolean isFabOpen = false;

    OvershootInterpolator interpolator = new OvershootInterpolator();

    // JSON file
    private final String TASKS_JSON_FILE = "tasks.json";





    private GoogleMap mMap;
    private static final int MY_PERMISSION_REQUEST_ACCESS_FINE_LOCATION = 101;

    private FusedLocationProviderClient fusedLocationClient;
    private LocationRequest mLocationRequest;
    private LocationCallback locationCallback;
    Marker gpsMarker = null;
    List<Marker> markerList ;



    public LocationCallback getLocationCallback() {
        return locationCallback;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        // Create an instance of FusedLocationProviderClient
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);


        markerList = new ArrayList<>();
        //below i link the button with my variable

        initFab();

        // sensor on create
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        //SensorList = mSensorManager.getSensorList(Sensor.TYPE_ACCELEROMETER);
        setContentView(R.layout.activity_main);
        txtView.findViewById(R.id.information);
        if (mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null){
            // Success!.
            mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        }
        else {
            // Failure! No accelerometer.
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
        mMap.setOnMapLoadedCallback(this);
        mMap.setOnMarkerClickListener(this);
        mMap.setOnMapLongClickListener(this);

        // Add a marker in Sydney and move the camera
        LatLng sydney = new LatLng(-34, 151);
        mMap.addMarker(new MarkerOptions().position(sydney).title("Marker in Sydney"));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney));
    }

    private void createLocationRequest(){
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(10000);
        mLocationRequest.setFastestInterval(5000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);


    }

    @SuppressLint("MissingPermission")
    private void startLocationUpdates(){
        fusedLocationClient.requestLocationUpdates(mLocationRequest, locationCallback, null);

    }



    public void zoomInClick(View view) {
        // Zoom in the map by 1
        mMap.moveCamera(CameraUpdateFactory.zoomIn());

    }

    public void zoomOutClick(View view) {
        // Dezoom in the map by 2
        mMap.moveCamera(CameraUpdateFactory.zoomOut());
    }

    @Override
    public void onMapLoaded() {
        Log.i(MapsActivity.class.getSimpleName(),"MapLoaded");
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            // Request the missing permission
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, MY_PERMISSION_REQUEST_ACCESS_FINE_LOCATION);
            return;
        }
        Task<Location> lastlocation = fusedLocationClient.getLastLocation();

        lastlocation.addOnSuccessListener(this, new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                // Add a marker on the last know location
                if (location != null && mMap != null){
                    mMap.addMarker(new MarkerOptions().position(new LatLng(location.getLatitude(), location.getLongitude()))
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
                            .title(getString(R.string.last_know_loc_msg)));
                }
            }
        });

        createLocationRequest();
        createLocationCallback();
        startLocationUpdates();
        // restore all the markers
        restoreFromJson();


    }

    private void createLocationCallback(){
        // create the location callback

        locationCallback = new LocationCallback(){

            public void onLocationResult(LocationResult locationResult){
                //Code execute when user's location changes
                if (locationResult != null){
                    //Remove the last reported location
                    if (gpsMarker != null)
                        gpsMarker.remove();
                    Location location = locationResult.getLastLocation();
                    gpsMarker = mMap.addMarker(new MarkerOptions()
                            .position(new LatLng(location.getLatitude(),location.getLongitude()))
                            .icon(BitmapDescriptorFactory.fromResource(R.drawable.marker))
                            .alpha(0.8f)
                            .title("Current location"));
                }

            }
        };
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopLocationUpdates();

        if (mSensor != null){
            MapsActivity.mSensorManager.unregisterListener(this,mSensor);
        }

    }

    @Override
    protected void onResume(){
        super.onResume();
        if (mSensor != null){
            MapsActivity.mSensorManager.registerListener(this,mSensor,1000);
        }
    }


    private void stopLocationUpdates(){
        if (locationCallback != null){
            fusedLocationClient.removeLocationUpdates(locationCallback);

        }
    }




    @Override
    public void onMapLongClick(LatLng latLng) {
        float distance = 0f;

        if (markerList.size()>0){
            Marker lastmarker = markerList.get(markerList.size()-1);
            float [] tmpDis = new float[3];
            // Calculate the distance between two point
            /*Location.distanceBetween(lastmarker.getPosition().latitude,lastmarker.getPosition().longitude,
                    latLng.latitude,latLng.longitude,tmpDis);

            distance = tmpDis[0];*/

/*
            PolylineOptions recOptions = new PolylineOptions()
                    .add(lastmarker.getPosition())
                    .add(latLng).width(10).color(Color.BLUE);
            mMap.addPolyline(recOptions);*/

        }

        // Add a custom marker on the long clikc event
        Marker marker = mMap.addMarker(new MarkerOptions()
                    .position(new LatLng(latLng.latitude,latLng.longitude))
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.marker2))
                    .alpha(0.8f));
                    //.title(String.format("Position :(%.2f, %.2f) Distance : %.2f", latLng.latitude, latLng.longitude,distance)));
        // Add marker to the list
        markerList.add(marker);


    }


    // init the fab animation
    private void initFab(){

        fabInfo = findViewById(R.id.action_2);
        fabSup = findViewById(R.id.action_1);

        fabInfo.setAlpha(0f);
        fabSup.setAlpha(0f);

        fabInfo.setTranslationX(-100f);
        fabSup.setTranslationX(-100f);

        fabInfo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // here put the display of information of the point
                if (!measurOn) {
                    measurOn = true;
                    txtView.setVisibility(View.VISIBLE);
                    if (mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null){
                        mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
                        onResume();
                    }else {

                    }

                }
                else {
                    measurOn = false;
                    onPause();
                    txtView.setVisibility(View.INVISIBLE);
                }




            }
        });
        fabSup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // here the button dissapear with animation
                fabInfo.animate().translationX(0f).alpha(0f).setInterpolator(interpolator).setDuration(500).start();
                fabSup.animate().translationX(0f).alpha(0f).setInterpolator(interpolator).setDuration(500).start();


            }
        });


    }

    /*private void openFab(){
        isFabOpen = !isFabOpen;

        fabInfo.animate().translationX(0f).alpha(1f).setInterpolator(interpolator).setDuration(300).start();
        fabSup.animate().translationX(0f).alpha(1f).setInterpolator(interpolator).setDuration(300).start();

    }

    private void closeFab(){
        isFabOpen = !isFabOpen;

        fabInfo.animate().translationX(-100f).alpha(0f).setInterpolator(interpolator).setDuration(300).start();
        fabSup.animate().translationX(-100f).alpha(0f).setInterpolator(interpolator).setDuration(300).start();



    }*/
    /*@Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.action_1:
                Log.i(TAG, "onClick: action 1");
                if (isFabOpen){
                    closeFab();
                }else {
                    openFab();
                }
                break;
            case R.id.action_2:
                Log.i(TAG, "onClick: action 2");
                break;
        }

    }*/


    // ci dessous la fonction qui réagit lorqu'on appuie sur un marker
    @Override
    public boolean onMarkerClick(Marker marker) {
        // Zoom the maps on the marker
        /*CameraPosition cameraPos = mMap.getCameraPosition();
        if (cameraPos.zoom <14f)
            mMap.moveCamera(CameraUpdateFactory.zoomTo(14f));*/

        fabInfo.animate().translationX(0f).alpha(1f).setInterpolator(interpolator).setDuration(500).start();
        fabSup.animate().translationX(0f).alpha(1f).setInterpolator(interpolator).setDuration(500).start();



        // i must put the simulation here normally with the two button




        return false;
    }


    public void clearMemory(View view) {
        // for clearing the memory
        markerList.clear();
        mMap.clear();
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(mSensor.getName());
        stringBuilder.append(String.format("nx %.4f", event.values[0]));
        stringBuilder.append(String.format(" y %.4f", event.values[1]));
        stringBuilder.append(String.format(" z %.4f", event.values[2]));

        txtView.setText(stringBuilder.toString());



    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // nothing

    }

    // JSON file

    @Override
    protected void onDestroy(){
        saveTasksToJson();
        super.onDestroy();
    }






    private void saveTasksToJson(){
        LatLng location;
        List<LatLng> positionList = new ArrayList<LatLng>();

        for (int i=0;i<markerList.size(); i++){
            location = markerList.get(i).getPosition();
            positionList.add(location);
        }

        Gson gson = new Gson();
        String listJson = gson.toJson(positionList);
        FileOutputStream outputStream;
        try {
            {
                outputStream = openFileOutput(TASKS_JSON_FILE,MODE_PRIVATE);
                FileWriter writer = new FileWriter(outputStream.getFD());
                writer.write(listJson);
                writer.close();

            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void restoreFromJson(){
        FileInputStream inputStream;
        int DEFAULT_BUFFER_SIZE = 1000;
        Gson gson = new Gson();
        String readJson;

        try {
            inputStream = openFileInput(TASKS_JSON_FILE);
            FileReader reader = new FileReader(inputStream.getFD());
            char[] buf = new char[DEFAULT_BUFFER_SIZE];
            int n ;
            StringBuilder builder = new StringBuilder();
            while ((n = reader.read(buf))>=0){
                String tmp = String.valueOf(buf);
                String substring = (n<DEFAULT_BUFFER_SIZE) ? tmp.substring(0,n) : tmp;
                builder.append(substring);

            }
            reader.close();
            readJson = builder.toString();
            Type collectionType = new TypeToken<List<LatLng>>(){

            }.getType();
            List<LatLng> o = gson.fromJson(readJson, collectionType);
            Marker marker;
            if (o!=null){
                markerList.clear();
                for (LatLng position : o){

                    marker = mMap.addMarker(new MarkerOptions()
                            .position(new LatLng(position.latitude,position.longitude))
                            .icon(BitmapDescriptorFactory.fromResource(R.drawable.marker2))
                            .alpha(0.8f));

                    // Add marker to the list
                    markerList.add(marker);

                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

/*
    public void displayInfo(View view) {
        if (!measurOn) {
            measurOn = true;
            txtView.setVisibility(View.VISIBLE);
            if (mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null){
                mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
                onResume();
            }else {
                Toast.makeText(this,"not available",Toast.LENGTH_SHORT).show();
            }

        }
        else {
            measurOn = false;
            onPause();
            txtView.setVisibility(View.INVISIBLE);
        }

    }
    */

}
