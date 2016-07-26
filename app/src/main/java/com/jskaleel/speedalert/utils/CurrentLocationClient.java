package com.jskaleel.speedalert.utils;

import android.content.Context;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;

public class CurrentLocationClient implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener {

    static final String TAG = "CurrentLocationClient";

    private GoogleApiClient mClient;
    private Location mCurrentLocation;
    private Context mContext;
    private SCOPE mScope;
    private OnLocationReceivedListener mOnLocationReceivedCallback;

    public CurrentLocationClient(Context context, SCOPE scope, OnLocationReceivedListener listener) {
        mOnLocationReceivedCallback = listener;
        init(context, scope);
    }

    public CurrentLocationClient(Context context, SCOPE scope) {
        init(context, scope);
    }

    private void init(Context context, SCOPE scope) {
        mScope = scope;
        mContext = context;
        mClient = new GoogleApiClient.Builder(context)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        mClient.connect();
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        Location location = LocationServices.FusedLocationApi.getLastLocation(mClient);

        if (location != null) {
            onLocationReceived(location);
        } else {
            LocationRequest request = new LocationRequest();
            request.setNumUpdates(1);
            request.setPriority(LocationRequest.PRIORITY_LOW_POWER);
            LocationServices.FusedLocationApi.requestLocationUpdates(mClient, request, this);
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.e(TAG, "Connection suspended" + i);
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        Log.e(TAG, "Connection Failed" + result.toString());
    }

    @Override
    public void onLocationChanged(Location location) {
        if (location != null) {
            onLocationReceived(location);
        }
    }

    private void onLocationReceived(Location location) {
        mCurrentLocation = location;
        reset();
        if (mOnLocationReceivedCallback != null) {
            mOnLocationReceivedCallback.onLocationReceived(location);
        }
    }

    public void reset() {
        Log.e(TAG, "Location client reset");
        if (mClient.isConnected()) {
            LocationServices.FusedLocationApi.removeLocationUpdates(mClient, this);
        }
        mClient.disconnect();
    }

    public enum SCOPE {
        LOCATION, LOCATION_AND_ADDRESS
    }

    public interface OnLocationReceivedListener {
        void onLocationReceived(Location location);
    }
}
