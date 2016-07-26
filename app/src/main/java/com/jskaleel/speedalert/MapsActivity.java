package com.jskaleel.speedalert;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Vibrator;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.SphericalUtil;
import com.jskaleel.speedalert.utils.AlertUtils;
import com.jskaleel.speedalert.utils.CurrentLocationClient;
import com.jskaleel.speedalert.utils.GpsUtils;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, CurrentLocationClient.OnLocationReceivedListener, UpdateMaxSpeed {

    public double maxSpeed = 0.0;
    public static boolean isWebServiceRunnig = false;
    private boolean showAlertDialog = true;
    private GoogleMap mMap;
    private CurrentLocationClient mCurrentLocationClient;
    private static String[] PERMISSIONS = {Manifest.permission.ACCESS_FINE_LOCATION};
    private LatLng mCurrentLatLng;
    private static final float ZOOM_LEVEL = 18.0f;
    private Vibrator viberate;
    private TextView txtMaxSpeed;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        init();
    }

    private void init() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                    && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                checkGPSPermission();
            } else {
                mCurrentLocationClient = new CurrentLocationClient(MapsActivity.this, CurrentLocationClient.SCOPE.LOCATION, this);
            }
        } else {
            mCurrentLocationClient = new CurrentLocationClient(MapsActivity.this, CurrentLocationClient.SCOPE.LOCATION, this);
        }

        txtMaxSpeed = (TextView) findViewById(R.id.txt_max_speed);
//        viberate = (Vibrator)getSystemService(Context.VIBRATOR_SERVICE);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        initMapService();
    }

    private void initMapService() {
        if (mMap != null) {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                        && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    mMap.setMyLocationEnabled(true);
                }
            } else {
                mMap.setMyLocationEnabled(true);
            }
            mMap.getUiSettings().setMyLocationButtonEnabled(true);
            mMap.getUiSettings().setCompassEnabled(true);
            mMap.getUiSettings().setMapToolbarEnabled(false);

            mMap.setOnMyLocationButtonClickListener(new GoogleMap.OnMyLocationButtonClickListener() {
                @Override
                public boolean onMyLocationButtonClick() {
                    if (!GpsUtils.isGpsEnabled(MapsActivity.this)) {
                        GpsUtils.showGpsAlert(MapsActivity.this);
                    }
                    return false;
                }
            });
        }
    }

    @Override
    public void onLocationReceived(Location location) {
        if (location == null) {
            return;
        }

        double currentLatitude = location.getLatitude();
        double currentLongitude = location.getLongitude();

        mCurrentLatLng = new LatLng(currentLatitude, currentLongitude);
        moveToCurrentLocation();

        double car_speed = Math.round(location.getSpeed()) * 3.6;

        double radiusDegrees = 30.0;
        LatLng center = new LatLng(currentLatitude, currentLongitude);

        LatLng southwest = SphericalUtil.computeOffset(center, radiusDegrees * Math.sqrt(2.0), 225);
        LatLng northeast = SphericalUtil.computeOffset(center, radiusDegrees * Math.sqrt(2.0), 45);
        Log.e("ss ","values " +maxSpeed +"---> " +car_speed);
        if (maxSpeed == 0) {
            checkSpeedLimit(southwest, northeast);
        } else if (car_speed >= maxSpeed) {
            if(showAlertDialog){
                showAlertDialog = false;

                if(MapsActivity.this.isFinishing()){
                    return;
                }

                AlertUtils.showAlert(MapsActivity.this, getString(R.string.speed_reached_alert), new DialogInterface.OnClickListener(){
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        showAlertDialog = true;
                    }
                });
            }else{
//                viberate.vibrate(200);
            }
            checkSpeedLimit(southwest, northeast);
        }
    }

    private void moveToCurrentLocation() {
        if (mCurrentLatLng != null && mMap != null) {
            CameraUpdate update = CameraUpdateFactory.newLatLngZoom(mCurrentLatLng, ZOOM_LEVEL);
            mMap.animateCamera(update);
        }
    }

    private void checkSpeedLimit(LatLng southwest, LatLng northeast) {
        if (!isWebServiceRunnig) {
            isWebServiceRunnig = true;
            new TaskFetchRoadSpeedLimit(MapsActivity.this, southwest, northeast).execute();
        }
    }

    @Override
    protected void onDestroy() {
        mCurrentLocationClient.reset();
        super.onDestroy();
    }

    private void checkGPSPermission() {
        for (String PERMISSION : PERMISSIONS) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if ((checkSelfPermission(PERMISSION)) != 0) {
                    if (PERMISSION.equalsIgnoreCase(android.Manifest.permission.ACCESS_FINE_LOCATION)) {
                        requestPermissions(PERMISSIONS, 1);
                    }
                }
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == 1) {
            if (grantResults.length > 0 && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                if (!ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                    promptSettings("Location");
                }
            } else {
                mCurrentLocationClient = new CurrentLocationClient(MapsActivity.this, CurrentLocationClient.SCOPE.LOCATION, this);
                if (mMap != null) {
                    mMap.setMyLocationEnabled(true);
                }
            }
        }
    }

    private void promptSettings(String type) {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle(String.format(getString(R.string.denied_never_ask_title), type));
        builder.setMessage(String.format(getString(R.string.denied_never_ask_msg), type));
        builder.setPositiveButton("go to Settings", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                goToSettings();
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.setCancelable(false);
        builder.show();
    }

    private void goToSettings() {
        Intent myAppSettings = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + this.getPackageName()));
        myAppSettings.addCategory(Intent.CATEGORY_DEFAULT);
        myAppSettings.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(myAppSettings);
    }


    @Override
    public void updateMaxSpeed(double maxSpeed) {
        this.maxSpeed = maxSpeed;
        txtMaxSpeed.setText("Max. Speed : " + maxSpeed + " kph");
    }
}
