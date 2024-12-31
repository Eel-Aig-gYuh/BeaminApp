package com.example.ubercus.Customer.ui.home;

import static com.firebase.ui.auth.AuthUI.getApplicationContext;

import android.Manifest;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.directions.route.Route;
import com.directions.route.RouteException;
import com.directions.route.RoutingListener;
import com.example.ubercus.Callback.IFirebaseDriverInfoListener;
import com.example.ubercus.Callback.IFirebaseFailedListener;
import com.example.ubercus.Common;
import com.example.ubercus.Model.AnimationModel;
import com.example.ubercus.Model.DriverGeoModel;
import com.example.ubercus.Model.DriverInfoModel;
import com.example.ubercus.Model.GeoQueryModel;
import com.example.ubercus.R;
import com.example.ubercus.Remote.IGoogleAPI;
import com.example.ubercus.Remote.RetrofitClient;
import com.example.ubercus.databinding.FragmentCustomerHomeBinding;
import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.widget.Autocomplete;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.PermissionListener;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import io.reactivex.schedulers.Schedulers;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.Observable;
import io.reactivex.disposables.CompositeDisposable;

public class CustomerHomeFragment extends Fragment implements OnMapReadyCallback, LocationListener, GoogleApiClient.OnConnectionFailedListener, GoogleApiClient.ConnectionCallbacks, RoutingListener, IFirebaseDriverInfoListener, IFirebaseFailedListener {

    // map update period
    private GoogleMap mMap;
    Location mLastLocation;
    LocationRequest locationRequest;
    private FusedLocationProviderClient fusedLocationProviderClient;
    GoogleApiClient mGoogleApiClient;




    private FragmentCustomerHomeBinding binding;

    private SlidingUpPanelLayout slidingUpPanelLayout;

    // location


    private LocationCallback locationCallback;
    
    SupportMapFragment mapFragment;

    private Button mRequest;
    private RadioGroup mRadioGroup;

    // load driver
    private double distance = 1.0; // default in km
    private static final double LIMIT_RANGE = 10.0; // km
    private Location previousLocation, currentLocation; // Use to calculate distance.

    private LatLng destinationLatLng;

    // listener
    IFirebaseDriverInfoListener iFirebaseDriverInfoListener;
    IFirebaseFailedListener iFirebaseFailedListener;

    // online System
    DatabaseReference onlineRef, currentUserRef, customersLocationRef;
    GeoFire geofire;

    private boolean firstTime=true;
    private String cityName;

    private LinearLayout mDriverInfo;
    private ImageView mDriverProfileImage;
    private TextView mDriverName, mDriverPhone, mDriverCar;
    private String requestService;

    //
    private CompositeDisposable compositeDisposable = new CompositeDisposable();
    private IGoogleAPI iGoogleAPI;
    private AutocompleteSupportFragment autocompleteSupportFragment;
    private LatLng pickupLocation;
    private boolean requestBol = false;
    private Marker pickupMarker;

    private Autocomplete autocomplete;


    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        HomeViewModel homeViewModel = new ViewModelProvider(this).get(HomeViewModel.class);

        binding = FragmentCustomerHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        destinationLatLng = new LatLng(0.0, 0.0);

        mDriverInfo = (LinearLayout) root.findViewById(R.id.DriverInfo);
        mDriverProfileImage = (ImageView) root.findViewById(R.id.driverProfileImage);
        mDriverName = (TextView) root.findViewById(R.id.driverName);
        mDriverPhone = (TextView) root.findViewById(R.id.driverPhone);
        mDriverCar = (TextView) root.findViewById(R.id.driverCar);

        mRadioGroup = (RadioGroup) root.findViewById(R.id.radioGroup);
        mRadioGroup.check(R.id.type_car);

        mRequest = root.findViewById(R.id.request);

        mRequest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (requestBol){
                    geoQuery.removeAllListeners();
                    driverLocationRef.removeEventListener(driverLocationRefListener);

                    if (driverFoundId != null){
                        DatabaseReference driverRef = FirebaseDatabase.getInstance().getReference("Users").child("Drivers").child(driverFoundId);

                        driverRef.setValue(true);
                        driverRef = null;
                    }

                    radius = 1;
                    isDriverFound = false;

                    String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
                    DatabaseReference ref = FirebaseDatabase.getInstance().getReference("customerRequest");
                    GeoFire geoFire = new GeoFire(ref);
                    geoFire.removeLocation(userId);

                    if (pickupMarker!=null){
                        pickupMarker.remove();
                    }
                    if (mDriverMarker!=null){
                        mDriverMarker.remove();
                    }

                    mRequest.setText("Đặt xe ở đây nè ...");

                    mDriverInfo.setVisibility(View.GONE);
                    mDriverName.setText("");
                    mDriverPhone.setText("");
                    mDriverCar.setText("");
                    mDriverProfileImage.setImageResource(R.mipmap.ic_laucher);

                    // endRide();
                }
                else{
                    requestBol = true;

                    int selectedId = mRadioGroup.getCheckedRadioButtonId();

                    final RadioButton radioButton = (RadioButton) root.findViewById(selectedId);

                    if(radioButton.getText() == null){
                        return;
                    }

                    requestService = radioButton.getText().toString();

                    String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

                    DatabaseReference ref = FirebaseDatabase.getInstance().getReference("customerRequest");
                    GeoFire geoFire = new GeoFire(ref);
                    geoFire.setLocation(userId, new GeoLocation(mLastLocation.getLatitude(), mLastLocation.getLongitude()));

                    pickupLocation = new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude());
                    pickupMarker = mMap.addMarker(new MarkerOptions().position(pickupLocation).title("Đón tôi ở đây !")
                    );
                    //.icon(BitmapDescriptorFactory.fromResource(R.drawable.baseline_directions_car_24))

                    mRequest.setText("Chờ chút nhé, tài xế ơi...");

                    getClosestDriver();
                }
            }
        });


        init();
        initViews(root);
        return root;
    }


    @Override
    public void onStop() {
        compositeDisposable.clear();
        super.onStop();
    }


    ValueEventListener onlineValueEventListener = new ValueEventListener() {
        @Override
        public void onDataChange(@NonNull DataSnapshot snapshot) {
            if(snapshot.exists() && currentUserRef != null)
                currentUserRef.onDisconnect().removeValue();
        }

        @Override
        public void onCancelled(@NonNull DatabaseError error) {
            Snackbar.make(mapFragment.getView(), error.getMessage(), Snackbar.LENGTH_LONG)
                    .show();
        }
    };


    @Override
    public void onDestroy() {
        fusedLocationProviderClient.removeLocationUpdates(locationCallback);
        geofire.removeLocation(FirebaseAuth.getInstance().getCurrentUser().getUid());
        onlineRef.removeEventListener(onlineValueEventListener);
        super.onDestroy();
    }

    @Override
    public void onResume() {
        super.onResume();
        registerOnlineSystem();
    }
    private void registerOnlineSystem(){
        onlineRef.addValueEventListener(onlineValueEventListener);
    }


    private int radius = 1;
    private boolean isDriverFound = false;
    private String driverFoundId;
    GeoQuery geoQuery;
    private String destination;

    private void getClosestDriver() {
        DatabaseReference driverLocation = FirebaseDatabase.getInstance().getReference().child("availableDriver");
        GeoFire geoFire = new GeoFire(driverLocation);

        geoQuery = geoFire.queryAtLocation(new GeoLocation(pickupLocation.latitude, pickupLocation.longitude), radius);

        geoQuery.removeAllListeners();

        geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
            @Override
            public void onKeyEntered(String key, GeoLocation location) {
                if (!isDriverFound && requestBol){
                    DatabaseReference mCustomerDatabase = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(key);

                    mCustomerDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            if (snapshot.exists() && snapshot.getChildrenCount()>0){
                                Map<String, Object> driverMap = (Map<String, Object>) snapshot.getValue();
                                // neu da co driver thi khong lam
                                if (isDriverFound){
                                    return;
                                }

                                if (driverMap.get("service").toString() != null){
                                    if (driverMap.get("service").equals(requestService.toString())){
                                        if (!isDriverFound){
                                            isDriverFound = true;
                                            driverFoundId = snapshot.getKey();

                                            DatabaseReference driverRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(driverFoundId).child("customerRequest");
                                            String customerId = FirebaseAuth.getInstance().getCurrentUser().getUid();
                                            HashMap map = new HashMap();
                                            map.put("customerRideId", customerId);

                                            String cityName = "";
                                            Geocoder geocoder = new Geocoder(getContext(), Locale.getDefault());
                                            List<Address> addressList ;
                                            try {
                                                // lay thanh pho hien tai cua current user
                                                addressList = geocoder.getFromLocation(mLastLocation.getLatitude(),
                                                        mLastLocation.getLongitude(), 1);
                                                cityName = addressList.get(0).getLocality();
                                                if (cityName == null)
                                                    cityName = addressList.get(0).getSubLocality();
                                            } catch (IOException e){

                                            }
                                            map.put("customerRideId", customerId);
                                            map.put("destination", cityName);
                                            map.put("destinationLat", mLastLocation.getLatitude());
                                            map.put("destinationLng", mLastLocation.getLongitude());

                                            driverRef.updateChildren(map);

                                            // add marker.
                                            // Marker pickupDriverMarker = mMap.addMarker(new MarkerOptions().position().title("Tài xế của bạn ở đây !"));

                                            getDriverLocation();
                                            getDriverInfo();
                                            // getHasRideEnded();
                                            mRequest.setText("Đã tìm thấy tài xế, đang tìm vị trí...");
                                        }
                                    }
                                }
                            }
                        }
                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                        }
                    });
                }
            }

            @Override
            public void onKeyExited(String key) {}

            @Override
            public void onKeyMoved(String key, GeoLocation location) {}

            @Override
            public void onGeoQueryReady() {
                if (!isDriverFound){
                    if (radius < 10000){
                        radius++;
                        getClosestDriver();
                    }
                    else {
                        Toast.makeText(getContext(), "Không có xe ở gần rồi!", Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onGeoQueryError(DatabaseError error) {}
        });
    }

    private void getDriverInfo() {
        mDriverInfo.setVisibility(View.VISIBLE);
        DatabaseReference mCustomerDatabase = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(driverFoundId);
        mCustomerDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists() && snapshot.getChildrenCount() > 0){
                    Map<String, Object> map = (Map<String, Object>) snapshot.getValue();
                    if (map.get("firstName") != null){
                        mDriverName.setText(map.get("firstName").toString());
                    }
                    if (map.get("phoneNumber") != null){
                        mDriverPhone.setText(map.get("phoneNumber").toString());
                    }
                    if (map.get("profileImageUri") != null){
                        Glide.with(getContext()).load(map.get("profileImageUri").toString()).into(mDriverProfileImage);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private Marker mDriverMarker;
    private DatabaseReference driverLocationRef;
    private ValueEventListener driverLocationRefListener;
    private void getDriverLocation() {
        driverLocationRef = FirebaseDatabase.getInstance().getReference().child("driversWorking").child(driverFoundId).child("l");
        driverLocationRefListener = driverLocationRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists() && requestBol){
                    List<Object> map = (List<Object>) snapshot.getValue();
                    double locationLat = 0;
                    double locationLng = 0;
                    mRequest.setText("Thấy tài xế rồi !");

                    if (map.get(0) != null){
                        locationLat = Double.parseDouble(map.get(0).toString());
                    }
                    if (map.get(1) != null){
                        locationLng = Double.parseDouble(map.get(1).toString());
                    }

                    // locationLat = 37.3028333;
                    // locationLng = -121.90466;
                    LatLng driverLatLng = new LatLng(locationLat, locationLng);

                    if (mDriverMarker != null){
                        mDriverMarker.remove();
                    }

                    Location loc1 = new Location("");
                    loc1.setLatitude(pickupLocation.latitude);
                    loc1.setLongitude(pickupLocation.longitude);

                    Location loc2 = new Location("");
                    loc2.setLatitude(driverLatLng.latitude);
                    loc2.setLongitude(driverLatLng.longitude);

                    float distance = loc1.distanceTo(loc2);

                    if (distance<50){
                        mRequest.setText("Tài xế tới rồi nè !");
                    } else {
                        mRequest.setText("Tìm thấy tài xế rồi nè, khoảng cách: " + String.valueOf(distance));
                    }
                    mDriverMarker = mMap.addMarker(new MarkerOptions().position(driverLatLng).title("Tài xế của bạn nè"));
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }


    private DatabaseReference driveHasEnded;
    private ValueEventListener driveHasEndedListener;

    private void getHasRideEnded() {
        driveHasEnded = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(driverFoundId).child("customerRequest").child("CustomerRideId");
        driveHasEndedListener = driveHasEnded.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()){

                }
                else {
                    endRide();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void endRide() {
        driveHasEnded.removeEventListener(driveHasEndedListener);
        requestBol = false;

        geoQuery.removeAllListeners();
        driverLocationRef.removeEventListener(driverLocationRefListener);

        if (driverFoundId != null){
            DatabaseReference driverRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(driverFoundId).child("customerRequest");;
            driverRef.setValue(true);
            driverFoundId=null;
        }
        isDriverFound = false;
        radius = 10;
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("customerRequest");
        GeoFire geoFire = new GeoFire(ref);
        geoFire.removeLocation(userId);

//        if(pickupMarker != null){
//            pickupMarker.remove();
//        }
        mRequest.setText("Đặt xe ở đây nè ...");
    }

    private void initViews(View root) {
        TextView txt_welcome = root.findViewById(R.id.txt_welcome);
        Common.setWelcomeMessage(txt_welcome);
    }

    private void init(){

        Places.initialize(getContext(), getString(R.string.myApiKeys));
        autocompleteSupportFragment = (AutocompleteSupportFragment) getChildFragmentManager()
                .findFragmentById(R.id.autocomplete_fragment);
        autocompleteSupportFragment.setPlaceFields(Arrays.asList
                (Place.Field.ID, Place.Field.ADDRESS, Place.Field.NAME, Place.Field.LAT_LNG));
        autocompleteSupportFragment.setHint(getString(R.string.where_to));
        autocompleteSupportFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(@NonNull Place place) {
                Toast.makeText(getContext(), "" + place.getLocation(), Toast.LENGTH_SHORT).show();

            }

            @Override
            public void onError(@NonNull Status status) {
                Toast.makeText(getContext(), "" + status.getStatusMessage(), Toast.LENGTH_SHORT).show();
            }
        });


        iGoogleAPI = RetrofitClient.getInstance().create(IGoogleAPI.class);


        iFirebaseDriverInfoListener=this;
        iFirebaseFailedListener=this;

        onlineRef = FirebaseDatabase.getInstance().getReference().child(".info/connected");

        if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(getContext(), getString(R.string.permission_require), Toast.LENGTH_SHORT).show();
            return;
        }


        locationRequest = new com.google.android.gms.location.LocationRequest();
        locationRequest.setSmallestDisplacement(10f);
        locationRequest.setInterval(1000);
        locationRequest.setFastestInterval(1000);
        locationRequest.setPriority(com.google.android.gms.location.LocationRequest.PRIORITY_HIGH_ACCURACY);

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                super.onLocationResult(locationResult);
                LatLng newPosition = new LatLng(locationResult.getLastLocation().getLatitude(),
                        locationResult.getLastLocation().getLongitude());
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(newPosition, 18f));


                // neu khach thay doi vi tri thi load lai driver
                if (firstTime){
                    previousLocation = locationResult.getLastLocation();
                    currentLocation = locationResult.getLastLocation();

                    firstTime=false;

                    setRestrictPlacesInCountry(locationResult.getLastLocation());
                }
                else{
                    previousLocation = currentLocation;
                    currentLocation = locationResult.getLastLocation();
                }

                if (previousLocation.distanceTo(currentLocation)/1000 <= LIMIT_RANGE) // display driver
                {
                    loadAvailableDrivers();
                    Log.d("Routing", "vi tri hien tai" + previousLocation);
                }
                else
                {
                    // do nothing
                }
            }
        };

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(getContext());
        if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(getActivity(),
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    // Define a request code for later identification
                    100);
        }
        fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper());



        loadAvailableDrivers();
    }

    private void setRestrictPlacesInCountry(Location location) {
        try{
            Geocoder geocoder = new Geocoder(getContext(), Locale.getDefault());
            List<Address> addressList = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
            assert addressList != null;
            if (addressList.size() > 0)
                autocompleteSupportFragment.setCountry(addressList.get(0).getCountryCode());

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void loadAvailableDrivers(){
        if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission
                (getContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Snackbar.make(getView(), getString(R.string.permission_require), Snackbar.LENGTH_SHORT).show();
            return;
        }
        fusedLocationProviderClient.getLastLocation()
                .addOnFailureListener(e -> Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_SHORT).show())
                .addOnSuccessListener(new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        // load driver
                        Geocoder geocoder = new Geocoder(getContext(), Locale.getDefault());
                        List<Address> addressList;
                        try{
                            addressList=geocoder.getFromLocation((location.getLatitude()),location.getLongitude(), 1);
                            if (addressList.size() > 0)
                                cityName = addressList.get(0).getLocality();
                            if (!TextUtils.isEmpty(cityName)){
                                if (cityName == null) cityName = addressList.get(0).getSubLocality();
                                // query
                                DatabaseReference driver_location_ref = FirebaseDatabase.getInstance()
                                        .getReference(Common.DRIVERS_LOCATION_REFERENCES);

                                GeoFire geoFire = new GeoFire(driver_location_ref);
                                GeoQuery geoQuery = geoFire.queryAtLocation(
                                        new GeoLocation(location.getLatitude(), location.getLongitude()), distance);
                                geoQuery.removeAllListeners();

                                geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
                                    @Override
                                    public void onKeyEntered(String key, GeoLocation location) {
                                        Common.driversFound.add(new DriverGeoModel(key, location));
                                    }

                                    @Override
                                    public void onKeyExited(String key) {

                                    }

                                    @Override
                                    public void onKeyMoved(String key, GeoLocation location) {

                                    }

                                    @Override
                                    public void onGeoQueryReady() {
                                        if (distance <= LIMIT_RANGE){
                                            distance++;
                                            loadAvailableDrivers();
                                        }
                                        else{
                                            distance=1.0;
                                            // addDriverMarker();
                                        }
                                    }

                                    @Override
                                    public void onGeoQueryError(DatabaseError error) {
                                        Snackbar.make(getView(), error.getMessage(), Snackbar.LENGTH_SHORT).show();
                                    }
                                });

                                // bat driver moi trong thanh pho va pham vi xung quanh
                                driver_location_ref.addChildEventListener(new ChildEventListener() {
                                    @Override
                                    public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

                                        // have new driver
                                        GeoQueryModel geoQueryModel = snapshot.getValue(GeoQueryModel.class);
                                        GeoLocation geoLocation = new GeoLocation(geoQueryModel.getL().get(0),
                                                geoQueryModel.getL().get(1));
                                        DriverGeoModel driverGeoModel = new DriverGeoModel(snapshot.getKey(), geoLocation);
                                        Location newDriverLocation = new Location("");
                                        newDriverLocation.setLatitude(geoLocation.latitude);
                                        newDriverLocation.setLongitude(geoLocation.longitude);
                                        float newdistance = location.distanceTo(newDriverLocation) / 1000; // in km
                                        // if (newdistance <= LIMIT_RANGE)
                                            // findDriverByKey(driverGeoModel); // if driver in range add to map
                                    }

                                    @Override
                                    public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

                                    }

                                    @Override
                                    public void onChildRemoved(@NonNull DataSnapshot snapshot) {

                                    }

                                    @Override
                                    public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError error) {

                                    }
                                });
                            }
                            else
                                Toast.makeText(getContext(), getString(R.string.city_name_empty), Toast.LENGTH_SHORT).show();;

                        } catch (IOException e) {
                            // Snackbar.make(getView(), e.getMessage(), Snackbar.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    @SuppressLint("CheckResult")
    private void addDriverMarker(){
        if(Common.driversFound.size() > 0 ){
            Observable.fromIterable(Common.driversFound)
                    .subscribeOn(Schedulers.newThread())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(driverGeoModel -> {
                        // On next
                        findDriverByKey(driverGeoModel);
                    }, throwable -> {
                        Snackbar.make(getView(), throwable.getMessage(), Snackbar.LENGTH_SHORT).show();
                    }, ()->{

                    });
        }
        else{
            // Toast.makeText(getContext(), getString(R.string.drivers_not_found), Toast.LENGTH_SHORT).show();
            // Snackbar.make(getView(), getString(R.string.drivers_not_found), Snackbar.LENGTH_SHORT).show();
        }
    }

    private void findDriverByKey(DriverGeoModel driverGeoModel) {
        FirebaseDatabase.getInstance()
                .getReference("Users").child(Common.DRIVERS_INFO_REFERENCES)
                .child(driverGeoModel.getKey())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if(snapshot.hasChildren()){
                            driverGeoModel.setDriverInfoModel(snapshot.getValue(DriverInfoModel.class));
                            iFirebaseDriverInfoListener.onDriverInfoLoadSuccess(driverGeoModel);
                        }
                        else{
                            iFirebaseFailedListener.onFirebaseFailedListener(getString(R.string.not_found_key)+driverGeoModel.getKey());
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        iFirebaseFailedListener.onFirebaseFailedListener(error.getMessage());
                    }
                });

    }

    protected synchronized void buildGoogleApiClient(){
        mGoogleApiClient = new GoogleApiClient.Builder(getContext())
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();

        mGoogleApiClient.connect();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;

        if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(getContext(), getString(R.string.permission_require), Toast.LENGTH_SHORT).show();
            return;
        }
        buildGoogleApiClient();
        mMap.setMyLocationEnabled(true);
        fusedLocationProviderClient.getLastLocation().addOnSuccessListener(location ->{
            mLastLocation = location;
        });


        // set giao dien
        mMap.getUiSettings().setZoomControlsEnabled(true);
        // check permission
        Dexter.withContext(getContext())
                .withPermission(android.Manifest.permission.ACCESS_FINE_LOCATION)
                .withListener(new PermissionListener() {
                    @Override
                    public void onPermissionGranted(PermissionGrantedResponse permissionGrantedResponse) {
                        if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                            ActivityCompat.requestPermissions(getActivity(),
                                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                                    // Define a request code for later identification
                                    100);
                        }
                        mMap.setMyLocationEnabled(true);
                        mMap.getUiSettings().setMyLocationButtonEnabled(true);
                        mMap.setOnMyLocationButtonClickListener(new GoogleMap.OnMyLocationButtonClickListener() {
                            @Override
                            public boolean onMyLocationButtonClick() {
                                if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                                    ActivityCompat.requestPermissions(getActivity(),
                                            new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                                            // Define a request code for later identification
                                            100);

                                    // You can optionally display a rationale to the user explaining why you need these permissions
                                    // before making the request.
                                }
                                fusedLocationProviderClient.getLastLocation()
                                        .addOnFailureListener(e -> Toast.makeText(getContext(), " " + e.getMessage(), Toast.LENGTH_SHORT).show())
                                        .addOnSuccessListener(location -> {
                                            LatLng userLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                                            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(userLatLng, 18f));
                                        });
                                return true;
                            }
                        });

                        // Find the location button using a more robust approach
                        View locationButton = ((View) mapFragment.getView().findViewById(Integer.parseInt("1"))
                                .getParent())
                                .findViewById(Integer.parseInt("2"));
                        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) locationButton.getLayoutParams();

                        params.addRule(RelativeLayout.ALIGN_PARENT_TOP, 0);
                        params.addRule(RelativeLayout.ALIGN_PARENT_TOP, RelativeLayout.TRUE);
                        params.setMargins(0, 0, 0, 250); // move view to see zoom control
                    }

                    @Override
                    public void onPermissionDenied(PermissionDeniedResponse permissionDeniedResponse) {
                        Toast.makeText(getContext(), "Permission " + permissionDeniedResponse.getPermissionName() + " " +
                                " bị từ chối rồi !", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(PermissionRequest permissionRequest, PermissionToken permissionToken) {
                        // Handle rationale for the user
                        permissionToken.continuePermissionRequest();
                    }
                })
                .check();

        mMap.getUiSettings().setZoomControlsEnabled(true);

        // Request permissions if not already granted
        if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(getActivity(),
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    // Define a request code for later identification
                    100);
        }
        try{
            boolean success = googleMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(getContext(), R.raw.uber_maps_style));
            if(!success)
                Log.e("EDMIT_ERRORR", "Định dạng bị lỗi rồi!");
        } catch (Resources.NotFoundException e){
            Log.e("EDMIT_ERRORR", e.getMessage());
        }
    }

    @Override
    public void onDriverInfoLoadSuccess(DriverGeoModel driverGeoModel) {
        // neu nhu da duoc danh dau roi thi khong danh dau nua
        if (!Common.makerList.containsKey(driverGeoModel.getKey())){
            Common.makerList.put(driverGeoModel.getKey(),
                    mMap.addMarker(new MarkerOptions()
                            .position(new LatLng(driverGeoModel.getGeoLocation().latitude,
                                    driverGeoModel.getGeoLocation().longitude))
                            .flat(true)
                            .title(Common.buildName(driverGeoModel.getDriverInfoModel().getFirstName(),
                                    driverGeoModel.getDriverInfoModel().getLastName()))
                            .snippet(driverGeoModel.getDriverInfoModel().getPhoneNumber())));
                            //.icon(descriptor)));

            if(!TextUtils.isEmpty(cityName)){
                DatabaseReference driverLocation = FirebaseDatabase.getInstance()
                        .getReference("Users").child("Drivers").child(Common.DRIVERS_LOCATION_REFERENCES)
                        .child(cityName)
                        .child(driverGeoModel.getKey());
                driverLocation.addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if(!snapshot.hasChildren()){
                            if(Common.makerList.get(driverGeoModel.getKey()) != null)
                                Common.makerList.get(driverGeoModel.getKey()).remove(); // remove marker

                            Common.makerList.remove(driverLocation.getKey()); // remove marker info from hash map
                            Common.driverLocationSubscribe.remove(driverGeoModel.getKey()); // remove thong tin driver
                            driverLocation.removeEventListener(this);

                        }
                        else {
                            if(Common.makerList.get(driverGeoModel.getKey()) != null){
                                GeoQueryModel geoQueryModel = snapshot.getValue(GeoQueryModel.class);
                                AnimationModel animationModel = new AnimationModel(false, geoQueryModel);

                                if(Common.driverLocationSubscribe.get(driverGeoModel.getKey()) != null){
                                    Marker currentMarker = Common.makerList.get(driverGeoModel.getKey());
                                    AnimationModel oldPosition = Common.driverLocationSubscribe.get(driverGeoModel.getKey());

                                    String from = new StringBuilder()
                                            .append(oldPosition.getGeoQueryModel().getL().get(0))
                                            .append(",")
                                            .append(oldPosition.getGeoQueryModel().getL().get(1))
                                            .toString();

                                    String to = new StringBuilder()
                                            .append(animationModel.getGeoQueryModel().getL().get(0))
                                            .append(",")
                                            .append(animationModel.getGeoQueryModel().getL().get(1))
                                            .toString();

                                    moveMarkerAnimation(driverGeoModel.getKey(), animationModel, currentMarker, from, to);
                                }
                                else{
                                    // first location init
                                    Common.driverLocationSubscribe.put(driverGeoModel.getKey(), animationModel);
                                }
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Snackbar.make(getView(), error.getMessage(), Snackbar.LENGTH_SHORT).show();
                    }
                });
            }
        }
    }

    private void moveMarkerAnimation(String key, AnimationModel animationModel, Marker currentMarker, String from, String to) {
        if (!animationModel.isRun()){
            // request api
            compositeDisposable.add(iGoogleAPI.getDirections("driving",
                    "less_driving",
                    from, to,
                    getString(R.string.google_api_key))
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(returnResult -> {
                        Log.d("API_RETURN", returnResult);

                        try {
                            // parse Json
                            JSONObject jsonObject = new JSONObject(returnResult);
                            JSONArray jsonArray = jsonObject.getJSONArray("routes");
                            for (int i=0; i<jsonArray.length(); i++){
                                JSONObject route = jsonArray.getJSONObject(i);
                                JSONObject poly = route.getJSONObject("overview_polyline");
                                String polyline = poly.getString("points");
                                // polylineList = Common.decodePoly(polyline);
                                animationModel.setPolylineList(Common.decodePoly(polyline));

                            }

                            // Moving
                            // index = -1;
                            // next = 1;
                            animationModel.setIndex(-1);
                            animationModel.setNext(1);

                            Runnable runnable = new Runnable(){
                                @Override
                                public void run(){
                                    if(animationModel.getPolylineList() != null && animationModel.getPolylineList().size() > 1){
                                        if (animationModel.getIndex() < animationModel.getPolylineList().size() - 2){
                                            // index++;
                                            animationModel.setIndex(animationModel.getIndex() + 1);
                                            // next = index++;
                                            animationModel.setNext(animationModel.getIndex() + 1);
                                            // start = polylineList.get(index);
                                            animationModel.setStart(animationModel.getPolylineList().get(animationModel.getIndex()));
                                            // end = polylineList.get(next);
                                            animationModel.setEnd(animationModel.getPolylineList().get(animationModel.getNext()));
                                        }

                                        ValueAnimator valueAnimator = ValueAnimator.ofInt(0, 1);
                                        valueAnimator.setDuration(3000);
                                        valueAnimator.setInterpolator(new LinearInterpolator());
                                        valueAnimator.addUpdateListener(value -> {
                                            // v = value.getAnimatedFraction();
                                            animationModel.setV(value.getAnimatedFraction());
                                            // lat = v*end.latitude + (1-v) * start.latitude;
                                            animationModel.setLat(animationModel.getV()*animationModel.getEnd().latitude
                                                    + (1 - animationModel.getV())*animationModel.getStart().latitude);
                                            // lng = v*end.longitude + (1-v) * start.longitude;
                                            animationModel.setLng(animationModel.getV()*animationModel.getEnd().longitude
                                                    + (1 - animationModel.getV())*animationModel.getStart().longitude);
                                            LatLng newPos = new LatLng(animationModel.getLat(), animationModel.getLng());
                                            currentMarker.setPosition(newPos);
                                            currentMarker.setAnchor(0.5f, 0.5f);
                                            currentMarker.setRotation(Common.getBearing(animationModel.getStart(), newPos));
                                        });

                                        valueAnimator.start();
                                        if(animationModel.getPolylineList() != null && animationModel.getIndex() < animationModel.getPolylineList().size() - 2){ // reach destination
                                            animationModel.getHandler().postDelayed(this, 1500);
                                        }
                                        else if(animationModel.getPolylineList() != null && animationModel.getIndex() < animationModel.getPolylineList().size() - 1) // done
                                        {
                                            animationModel.setRun(false);
                                            Common.driverLocationSubscribe.put(key, animationModel); // Cap nhan
                                        }
                                    }
                                }
                            };

                            // run handler
                            animationModel.getHandler().postDelayed(runnable, 1500);

                        } catch (Exception e){
                            Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_SHORT).show();;
                        }
                    })
            );
        }
    }

    @Override
    public void onFirebaseFailedListener(String message) {
        // Snackbar.make(getView(), message, Snackbar.LENGTH_SHORT).show();
        Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();;
    }


    @SuppressLint("RestrictedApi")
    @Override
    public void onLocationChanged(@NonNull Location location) {
        mLastLocation = location;
        currentLocation = location;

        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());

        mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
        mMap.animateCamera(CameraUpdateFactory.zoomTo(11));

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {

        locationRequest = new com.google.android.gms.location.LocationRequest();
        locationRequest.setSmallestDisplacement(50f); // 50m
        locationRequest.setInterval(1000); // 1s
        locationRequest.setFastestInterval(1000); // 1s
        locationRequest.setPriority(com.google.android.gms.location.LocationRequest.PRIORITY_HIGH_ACCURACY);

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(getContext());
        if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(getContext(), getString(R.string.permission_require), Toast.LENGTH_SHORT).show();
            return;
        }
        fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper());
    }


    // ve duong di
    @Override
    public void onConnectionSuspended(int i) {}

    @Override
    public void onRoutingFailure(RouteException e) {}

    @Override
    public void onRoutingStart() {}

    @Override
    public void onRoutingSuccess(ArrayList<Route> arrayList, int i) {

    }

    @Override
    public void onRoutingCancelled() {

    }


    // autoComplete

}