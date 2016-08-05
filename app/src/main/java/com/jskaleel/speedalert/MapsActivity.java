package com.jskaleel.speedalert;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Vibrator;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import com.afollestad.assent.Assent;
import com.afollestad.assent.AssentCallback;
import com.afollestad.assent.PermissionResultSet;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.maps.android.SphericalUtil;
import com.jskaleel.speedalert.maputils.MapStateListener;
import com.jskaleel.speedalert.maputils.TouchableMapFragment;
import com.jskaleel.speedalert.utils.AlertUtils;
import com.jskaleel.speedalert.utils.CurrentLocationClient;
import com.jskaleel.speedalert.utils.DeviceUtils;
import com.jskaleel.speedalert.utils.GpsUtils;

import java.util.ArrayList;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, CurrentLocationClient.OnLocationReceivedListener, UpdateMaxSpeed {

    public double maxSpeed = 0.0;
    public static boolean isWebServiceRunnig = false;
    private boolean showAlertDialog = true;
    private GoogleMap mMap;
    private CurrentLocationClient mCurrentLocationClient;
    private LatLng mCurrentLatLng;
    private static final float ZOOM_LEVEL = 14.0f;
    private TextView txtMaxSpeed, txtCurrentSpeed;
    private MyLocationListener myLocationListener;
    private LocationManager locationManager;
    private TouchableMapFragment mapFragment;
    private boolean isMapTouched = false;

    private ArrayList<LatLng> violatePoints;
    private PolylineOptions polyLine;

    @Override
    protected void onResume() {
        super.onResume();
        Assent.setActivity(this, this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (isFinishing())
            Assent.setActivity(this, null);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        Assent.setActivity(this, this);
        mapFragment = (TouchableMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        init();
    }

    private void init() {
        if (!Assent.isPermissionGranted(Assent.ACCESS_FINE_LOCATION)) {
            Assent.requestPermissions(new AssentCallback() {
                @Override
                public void onPermissionResult(PermissionResultSet result) {
                    if (result.isGranted(Assent.ACCESS_FINE_LOCATION)) {
                        initLocationClient();
                    }
                }
            }, 69, Assent.ACCESS_FINE_LOCATION);
        } else {
            initLocationClient();
        }

        txtMaxSpeed = (TextView) findViewById(R.id.txt_max_speed);
        txtCurrentSpeed = (TextView) findViewById(R.id.txt_current_speed);

        violatePoints = new ArrayList<>();
        polyLine = new PolylineOptions();
        polyLine.width(15);
        polyLine.clickable(false);
        polyLine.color(Color.RED);
    }

    private void initLocationClient() {
        mCurrentLocationClient = new CurrentLocationClient(MapsActivity.this, this);


        myLocationListener = new MyLocationListener();
        Criteria criteria = new Criteria();
        criteria.setSpeedRequired(true);
        criteria.setSpeedAccuracy(Criteria.ACCURACY_HIGH);

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        String bestProvider = locationManager.getBestProvider(criteria, true);
        locationManager.requestLocationUpdates(bestProvider, 2000, 0, myLocationListener);

        initMapService();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        if (Assent.isPermissionGranted(Assent.ACCESS_FINE_LOCATION)) {
            initMapService();
        }

        new MapStateListener(mMap, mapFragment, this) {

            @Override
            public void onMapTouched() {
                Log.d("MapsActivity", "onMapTouched");
                isMapTouched = true;
            }

            @Override
            public void onMapReleased() {
                Log.d("MapsActivity", "onMapReleased");
            }

            @Override
            public void onMapUnsettled() {
                Log.d("MapsActivity", "onMapUnsettled");
            }

            @Override
            public void onMapSettled() {
                Log.d("MapsActivity", "onMapSettled");
                /*if (mMap != null) {
                    isMapTouched = false;
                }*/
            }
        };
    }

    private void initMapService() {
        if (mMap != null) {

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                mMap.setMyLocationEnabled(true);
            } else {
                if (Assent.isPermissionGranted(Assent.ACCESS_FINE_LOCATION)) {
                    mMap.setMyLocationEnabled(true);
                }
            }

            mMap.getUiSettings().setMyLocationButtonEnabled(true);
            mMap.getUiSettings().setCompassEnabled(true);

            mMap.setOnMyLocationButtonClickListener(new GoogleMap.OnMyLocationButtonClickListener() {
                @Override
                public boolean onMyLocationButtonClick() {
                    if (!GpsUtils.isGpsEnabled(MapsActivity.this)) {
                        GpsUtils.showGpsAlert(MapsActivity.this);
                    } else {
                        isMapTouched = false;
                        animateToCurrentLocation();
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

        Vibrator viberate = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        viberate.vibrate(300);

        double currentLatitude = location.getLatitude();
        double currentLongitude = location.getLongitude();

        mCurrentLatLng = new LatLng(currentLatitude, currentLongitude);

        animateToCurrentLocation();

        double currentSpeed = Math.round(location.getSpeed()) * 3.6f;
        txtCurrentSpeed.setText(String.format(getString(R.string.your_speed), String.valueOf(currentSpeed)));
        if(currentSpeed > 0.0) {
            polyLine.add(mCurrentLatLng);
            if (mMap != null) {
                mMap.addPolyline(polyLine);
            }
        }

        double radiusDegrees = 30.0;
        LatLng center = new LatLng(currentLatitude, currentLongitude);

        LatLng southwest = SphericalUtil.computeOffset(center, radiusDegrees * Math.sqrt(2.0), 225);
        LatLng northeast = SphericalUtil.computeOffset(center, radiusDegrees * Math.sqrt(2.0), 45);
        Log.e("ss ", "values " + maxSpeed + "---> " + currentSpeed);

        if (maxSpeed == 0) {
            checkSpeedLimit(southwest, northeast);
        } else if (currentSpeed >= maxSpeed) {
            if (showAlertDialog) {
                showAlertDialog = false;
                if (!violatePoints.contains(center)) {
                    violatePoints.add(center);
                    mMap.addMarker(new MarkerOptions().position(center).icon(BitmapDescriptorFactory.defaultMarker()));
                }

                if (MapsActivity.this.isFinishing()) {
                    return;
                }

                AlertUtils.showAlert(MapsActivity.this, getString(R.string.speed_reached_alert), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        showAlertDialog = true;
                    }
                });
            }
            checkSpeedLimit(southwest, northeast);
        }
    }

    private void animateToCurrentLocation() {
        if (!isMapTouched) {
            if (mCurrentLatLng != null && mMap != null) {
                CameraUpdate update = CameraUpdateFactory.newLatLngZoom(mCurrentLatLng, ZOOM_LEVEL);
                mMap.animateCamera(update);
            }
        }
    }

    private void checkSpeedLimit(LatLng southwest, LatLng northeast) {
        if (!isWebServiceRunnig) {
            isWebServiceRunnig = true;
            if (DeviceUtils.isInternetConnected(MapsActivity.this)) {
                new TaskFetchRoadSpeedLimit(MapsActivity.this, southwest, northeast).execute();
            }
        }
    }

    @Override
    protected void onDestroy() {
        mCurrentLocationClient.reset();
        locationManager.removeUpdates(myLocationListener);
        super.onDestroy();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        Assent.handleResult(permissions, grantResults);
    }

    @Override
    public void updateMaxSpeed(double maxSpeed) {
        this.maxSpeed = maxSpeed;
        txtMaxSpeed.setText(String.format(getString(R.string.max_speed), String.valueOf(maxSpeed)));
    }

    class MyLocationListener implements LocationListener {

        @Override
        public void onLocationChanged(Location location) {
            onLocationReceived(location);

        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
        }

        @Override
        public void onProviderEnabled(String provider) {

        }

        @Override
        public void onProviderDisabled(String provider) {

        }
    }
}
