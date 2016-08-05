package com.jskaleel.speedalert.utils;

import android.content.Context;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;

public class CurrentLocationClient implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener {

    static final String TAG = "CurrentLocationClient";

    private GoogleApiClient mClient;
    private OnLocationReceivedListener mOnLocationReceivedCallback;

    public CurrentLocationClient(Context context, OnLocationReceivedListener listener) {
        mOnLocationReceivedCallback = listener;
        init(context);
    }

    public CurrentLocationClient(Context context) {
        init(context);
    }

    private void init(Context context) {
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
            request.setSmallestDisplacement(0f);
            request.setInterval(2000);
            request.setFastestInterval(1000);
            request.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
            LocationServices.FusedLocationApi.requestLocationUpdates(mClient, request, this);
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.e(TAG, "Connection suspended" + i);
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult result) {
        Log.e(TAG, "Connection Failed" + result.toString());
    }

    @Override
    public void onLocationChanged(Location location) {
        if (location != null) {
            onLocationReceived(location);
        }
    }

    private void onLocationReceived(Location location) {
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
        LOCATION
    }

    public interface OnLocationReceivedListener {
        void onLocationReceived(Location location);
    }
}
