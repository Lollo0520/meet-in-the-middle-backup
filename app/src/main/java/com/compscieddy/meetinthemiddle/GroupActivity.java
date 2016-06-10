package com.compscieddy.meetinthemiddle;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.compscieddy.eddie_utils.Etils;
import com.compscieddy.eddie_utils.Lawg;
import com.compscieddy.meetinthemiddle.adapter.ChatsAdapter;
import com.compscieddy.meetinthemiddle.model.UserMarker;
import com.fondesa.recyclerviewdivider.RecyclerViewDivider;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.VisibleRegion;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import butterknife.Bind;
import butterknife.ButterKnife;

public class GroupActivity extends FragmentActivity implements OnMapReadyCallback, LocationListener,
    GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, View.OnClickListener, GoogleMap.OnMapClickListener {

  private static final Lawg lawg = Lawg.newInstance(GroupActivity.class.getSimpleName());

  private GoogleMap mMap;
  private final int LOCATION_REQUEST_CODE = 1;
  private Handler mHandler;

  private Coordinate mLastKnownCoord = new Coordinate();
  private boolean mIsLocationPermissionEnabled = false;

  private final int ANIMATE_CAMERA_REPEAT = 2000;

  private LocationManager mLocationManager;
  private GoogleApiClient mGoogleApiClient;
  private Map<String, Marker> mMarkers = new HashMap<>();

  private final String UUID_KEY = "UUID_KEY"; // Temporary way to identify different users or different installations
  private Location mLastLocation;
  private String mUUID;

  @Bind(R.id.group_edit_text) EditText mGroupEditText;
  @Bind(R.id.group_text_view) TextView mGroupTextView;
  @Bind(R.id.group_set_button) TextView mSetButton;
  @Bind(R.id.chats_recycler_view) RecyclerView mChatsRecyclerView;
  @Bind(R.id.invite_button) FontTextView mInviteButton;

  private ChatsAdapter mChatsAdapter;

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

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_group);
    ButterKnife.bind(GroupActivity.this);

    // Obtain the SupportMapFragment and get notified when the map is ready to be used.
    SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
        .findFragmentById(R.id.map);
    mapFragment.getMapAsync(this);

    SharedPreferences sharedPreferences = GroupActivity.this.getSharedPreferences(
        getString(R.string.preference_file_key), Context.MODE_PRIVATE);
    mUUID = sharedPreferences.getString(UUID_KEY, null);
    if (mUUID == null) {
      SharedPreferences.Editor editor = sharedPreferences.edit();
      mUUID = UUID.randomUUID().toString();
      editor.putString(UUID_KEY, mUUID);
      editor.apply();
    }

    mHandler = new Handler(Looper.getMainLooper());
    // TODO: let's turn off this zooming animation for now
    // mHandler.postDelayed(mAnimateCameraRunnable, ANIMATE_CAMERA_REPEAT);

    if (mGoogleApiClient == null) {
      mGoogleApiClient = new GoogleApiClient.Builder(GroupActivity.this)
          .addConnectionCallbacks(GroupActivity.this)
          .addOnConnectionFailedListener(GroupActivity.this)
          .addApi(LocationServices.API)
          .build();
    }

    mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
    int locationPermissionCheck = ContextCompat.checkSelfPermission(GroupActivity.this, Manifest.permission.ACCESS_FINE_LOCATION);
    if (locationPermissionCheck == PackageManager.PERMISSION_GRANTED) {
      initLocationPermissionGranted();
    } else {
      requestLocationPermission();
    }

    setListeners();
    setupRecyclerView();
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
      mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 500, 10, GroupActivity.this);
    } catch (SecurityException se) {
      lawg.e("se: " + se);
    }
  }

  private void requestLocationPermission() {
    ActivityCompat.requestPermissions(GroupActivity.this, new String[]{
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

    LatLng sydney = new LatLng(-34, 151);
    Marker sydneyMarker = mMap.addMarker(new MarkerOptions().position(sydney).title("Marker in Sydney"));;
    mMarkers.put(UUID.randomUUID().toString(), sydneyMarker);
    mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney));

    initMarkers();
  }

  private void initMarkers() {
    FirebaseDatabase database = FirebaseDatabase.getInstance();
    DatabaseReference markerReference = database.getReference("markers");
    markerReference.addChildEventListener(new ChildEventListener() {
      @Override
      public void onChildAdded(DataSnapshot dataSnapshot, String s) {
        UserMarker userMarker = dataSnapshot.getValue(UserMarker.class);
        lawg.d("initMarkers() onChildAdded() " + " dataSnapshot: " + dataSnapshot + " userMarker: " + userMarker);
        String userUUID = userMarker.userUUID;
        LatLng latLng = userMarker.getLatLng();
        if (mMarkers.containsKey(userUUID)) {
          Marker existingMarker = mMarkers.get(userUUID);
          existingMarker.setPosition(latLng);
        } else {
          Marker marker = mMap.addMarker(new MarkerOptions().position(latLng).title(userUUID));
          mMarkers.put(userUUID, marker);
        }

        // http://stackoverflow.com/a/14828739/4326052
        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        for (Marker m : mMarkers.values()) {
          builder.include(m.getPosition());
        }
        LatLngBounds bounds = builder.build();
        int padding = Etils.dpToPx(50);
        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngBounds(bounds, padding);
        mMap.animateCamera(cameraUpdate);

        // TODO: Google maps bounds need to be extended here
      }

      @Override
      public void onChildChanged(DataSnapshot dataSnapshot, String s) {
        // Implement so that if marker location is changed, the appropriate marker gets updated
      }

      @Override
      public void onChildRemoved(DataSnapshot dataSnapshot) {}

      @Override
      public void onChildMoved(DataSnapshot dataSnapshot, String s) {}

      @Override
      public void onCancelled(DatabaseError databaseError) {
        lawg.e("Firebase Error onCancelled() [" + databaseError.getCode() + "] " + databaseError.getMessage() + databaseError.getDetails());
      }
    });

    mMap.setOnMapClickListener(this);

  }

  private void setListeners() {
    mGroupTextView.setOnClickListener(this);
    mSetButton.setOnClickListener(this);
    mInviteButton.setOnClickListener(this);
  }

  private void setupRecyclerView() {
    mChatsAdapter = new ChatsAdapter();
    lawg.e("_______________" + mChatsAdapter);
    mChatsRecyclerView.setAdapter(mChatsAdapter);
    mChatsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
    RecyclerViewDivider.with(this).addTo(mChatsRecyclerView).marginSize(Etils.dpToPx(5)).build();
  }

  @Override
  public void onConnected(@Nullable Bundle bundle) {
    lawg.d("GoogleApiClient onConnected()");
    try {
      mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
      if (mLastLocation != null) {
        double latitude = mLastLocation.getLatitude();
        double longitude = mLastLocation.getLongitude();
        mLastKnownCoord.set(latitude, longitude);

        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference markerReference = database.getReference("markers");
        UserMarker userMarker = new UserMarker(mUUID, latitude, longitude);
        markerReference.child(mUUID).setValue(userMarker);

//        if (mCurrentMarker != null) mCurrentMarker.remove();

        Bitmap icon = BitmapFactory.decodeResource(getResources(),
                R.drawable.ic_darren);
        Bitmap croppedIcon = Util.getCroppedBitmap(GroupActivity.this, icon);

        // TODO: Don't add current marker, just update Firebase to make it do it for you
        // mCurrentMarker = mMap.addMarker(new MarkerOptions().position(latLng).title("Current Location").icon(BitmapDescriptorFactory.fromBitmap(croppedIcon)));
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
        mGroupEditText.setVisibility(View.VISIBLE);
        mGroupTextView.setVisibility(View.INVISIBLE);
        mSetButton.setVisibility(View.VISIBLE);
        mGroupEditText.requestFocus();
        break;

      case R.id.group_set_button:
        mGroupEditText.setVisibility(View.INVISIBLE);
        mGroupTextView.setVisibility(View.VISIBLE);
        mSetButton.setVisibility(View.INVISIBLE);

        //name will need to be saved as a shared preference or in database
        mGroupTextView.setText(mGroupEditText.getText());
        break;
      case R.id.invite_button:
        //do something
        Etils.showToast(GroupActivity.this, "Invite Button Clicked");
        break;

    }

  }
}
