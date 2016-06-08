package com.compscieddy.meetinthemiddle;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.compscieddy.eddie_utils.Etils;
import com.compscieddy.eddie_utils.Lawg;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.VisibleRegion;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, LocationListener,
    GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, View.OnClickListener, GoogleMap.OnMapClickListener {

  private static final Lawg lawg = Lawg.newInstance(MapsActivity.class.getSimpleName());

  private GoogleMap mMap;
  private final int LOCATION_REQUEST_CODE = 1;
  private Handler mHandler;

  private Coordinate mLastKnownCoord = new Coordinate();
  private boolean mIsLocationPermissionEnabled = false;

  private final int ANIMATE_CAMERA_REPEAT = 2000;

  private LocationManager mLocationManager;
  private GoogleApiClient mGoogleApiClient;
  private Marker mCurrentMarker;

  private Runnable mAnimateCameraRunnable = new Runnable() {
    @Override
    public void run() {
      if (false) lawg.d("mAnimateCameraRunnable");

      if (!mIsLocationPermissionEnabled) {
        return;
      }

      float zoom = mMap.getCameraPosition().zoom; if (false) lawg.d(" zoom: " + zoom);

      LatLng latLng = mLastKnownCoord.getLatLng();
      if (latLng.latitude != -1 && latLng.longitude != -1 && zoom < 13) {
        mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
        mMap.animateCamera(CameraUpdateFactory.zoomIn());
      }

      mHandler.postDelayed(mAnimateCameraRunnable, ANIMATE_CAMERA_REPEAT);
    }
  };
  private Location mLastLocation;

  EditText groupEditText;
  ForadayTextView groupTextView;
  Button setButton;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_maps);
    // Obtain the SupportMapFragment and get notified when the map is ready to be used.
    SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
        .findFragmentById(R.id.map);
    mapFragment.getMapAsync(this);

    mHandler = new Handler(Looper.getMainLooper());
    mHandler.postDelayed(mAnimateCameraRunnable, ANIMATE_CAMERA_REPEAT);

    if (mGoogleApiClient == null) {
      mGoogleApiClient = new GoogleApiClient.Builder(MapsActivity.this)
          .addConnectionCallbacks(MapsActivity.this)
          .addOnConnectionFailedListener(MapsActivity.this)
          .addApi(LocationServices.API)
          .build();
    }

    mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
    int locationPermissionCheck = ContextCompat.checkSelfPermission(MapsActivity.this, Manifest.permission.ACCESS_FINE_LOCATION);
    if (locationPermissionCheck == PackageManager.PERMISSION_GRANTED) {
      initLocationPermissionGranted();
    } else {
      requestLocationPermission();
    }

    groupEditText = (EditText) findViewById(R.id.group_edit_text);
    groupTextView = (ForadayTextView) findViewById(R.id.group_text_view);
    setButton = (Button) findViewById(R.id.group_set_button);

    groupTextView.setOnClickListener(this);
    setButton.setOnClickListener(this);
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

  private void initLocationPermissionGranted() {
    try {
      mIsLocationPermissionEnabled = true;
      mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 500, 10, MapsActivity.this);
    } catch (SecurityException se) {
      lawg.e("se: " + se);
    }
  }

  private void requestLocationPermission() {
    ActivityCompat.requestPermissions(MapsActivity.this, new String[]{
        Manifest.permission.ACCESS_FINE_LOCATION
    }, LOCATION_REQUEST_CODE);
  }

  @Override
  public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
    if (requestCode == LOCATION_REQUEST_CODE) {
      if (permissions.length == 1 && permissions[0] == Manifest.permission.ACCESS_FINE_LOCATION
          && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
        initLocationPermissionGranted();
      }
    }
  }

  @Override
  public void onLocationChanged(Location location) {
    lawg.e("onLocationChanged");
    /*
    double latitude = location.getLatitude();
    double longitude = location.getLongitude();
    mLastKnownCoord.set(latitude, longitude);
    LatLng currentLatLng = new LatLng(latitude, longitude);
    mMap.addMarker(new MarkerOptions().position(currentLatLng).title("Current Location"));
    mMap.moveCamera(CameraUpdateFactory.newLatLng(currentLatLng));
    mMap.animateCamera(CameraUpdateFactory.zoomIn());
    */
  }

  @Override
  public void onProviderEnabled(String provider) {

  }

  @Override
  public void onProviderDisabled(String provider) {

  }

  @Override
  public void onStatusChanged(String provider, int status, Bundle extras) {

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
      // "MyLocation" is the "blue dot" feature for showing the current location and jumping to the location
//      mMap.setMyLocationEnabled(true);
    } catch (SecurityException se) {
      lawg.e("se: " + se);
    }
    mMap.setOnMapClickListener(this);
  }

  @Override
  public void onConnected(@Nullable Bundle bundle) {
    try {
      mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
      if (mLastLocation != null) {
        double latitude = mLastLocation.getLatitude();
        double longitude = mLastLocation.getLongitude();
        LatLng latLng = new LatLng(latitude, longitude);
        mLastKnownCoord.set(latitude, longitude);
        if (mCurrentMarker != null) mCurrentMarker.remove();

        Bitmap icon = BitmapFactory.decodeResource(getResources(),
                R.drawable.ic_darren);

        Bitmap croppedIcon = getCroppedBitmap(icon);

        mCurrentMarker = mMap.addMarker(new MarkerOptions().position(latLng).title("Current Location").icon(BitmapDescriptorFactory.fromBitmap(croppedIcon)));
      }
    } catch (SecurityException se) {
      lawg.e("se: " + se);
    }
  }

  @Override
  public void onConnectionSuspended(int i) {

  }

  @Override
  public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

  }

  public Bitmap getCroppedBitmap(Bitmap bitmap) {
    Bitmap output = Bitmap.createBitmap(bitmap.getWidth(),
            bitmap.getHeight(), Bitmap.Config.ARGB_8888);
    Canvas canvas = new Canvas(output);

    final int color = getResources().getColor(R.color.white);
    final Paint paint = new Paint();
    final Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());

    paint.setAntiAlias(true);
    canvas.drawARGB(0, 0, 0, 0);
    paint.setColor(color);

    canvas.drawCircle(bitmap.getWidth() / 2, bitmap.getHeight() / 2,
        bitmap.getWidth() / 2, paint);
    paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
    canvas.drawBitmap(bitmap, rect, rect, paint);

    return output;
  }

  //Toasts the tapped points coords
  @Override
  public void onMapClick(LatLng point) {
    Etils.showToast(this, "Tapped point is: " + point);

    VisibleRegion vRegion = mMap.getProjection().getVisibleRegion();
    LatLng upperLeft = vRegion.farLeft;
    LatLng lowerRight = vRegion.nearRight;
    //Logs the visible area of the map
    lawg.d("Top left = " + upperLeft + " and Bottom right = " + lowerRight);

  }

  @Override
  public void onClick(View v) {
    switch (v.getId()) {
      case R.id.group_text_view:
        groupEditText.setVisibility(View.VISIBLE);
        groupTextView.setVisibility(View.INVISIBLE);
        setButton.setVisibility(View.VISIBLE);
        groupEditText.requestFocus();

        break;

      case R.id.group_set_button:

        groupEditText.setVisibility(View.INVISIBLE);
        groupTextView.setVisibility(View.VISIBLE);
        setButton.setVisibility(View.INVISIBLE);

        //name will need to be saved as a shared preference or in database
        groupTextView.setText(groupEditText.getText());

        break;

    }

  }
}
