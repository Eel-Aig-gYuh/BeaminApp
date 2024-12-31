package com.example.ubercus.Driver.ui.home;

import static com.firebase.ui.auth.AuthUI.getApplicationContext;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.directions.route.AbstractRouting;
import com.directions.route.Route;
import com.directions.route.RouteException;
import com.directions.route.Routing;
import com.directions.route.RoutingListener;
import com.example.ubercus.Common;
import com.example.ubercus.R;
import com.example.ubercus.databinding.FragmentDriverHomeBinding;
import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.GeofencingEvent;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
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
import com.example.ubercus.Driver.ui.home.HomeViewModel;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class DriverHomeFragment extends Fragment implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, RoutingListener, com.google.android.gms.location.LocationListener {
    // update map period
    private GoogleMap mMap;
    Location mLastLocation;
    LocationRequest locationRequest;




    private FragmentDriverHomeBinding binding;
    // location
    private FusedLocationProviderClient fusedLocationProviderClient;

    private LocationCallback locationCallback;
    private boolean isFirstTime = true;
    SupportMapFragment mapFragment;
    private LinearLayout mCustomerInfo;
    private ImageView mCustomerProfileImage;
    private TextView mCustomerName, mCustomerPhone, mCustomerDestination;
    private Button mRideStatus;
    GoogleApiClient mGoogleApiClient;

    private int status = 0;

    // online System
    DatabaseReference onlineRef, currentUserRef, driversLocationRef;
    LatLng newPosition;
    private LatLng destinationLatLng;
    GeoFire geofire;
    private String customerId = " ", destination;

    ValueEventListener onlineValueEventListener = new ValueEventListener() {
        @Override
        public void onDataChange(@NonNull DataSnapshot snapshot) {
            if(snapshot.exists() && currentUserRef != null){
                currentUserRef.onDisconnect().removeValue();
                isFirstTime = true;
            }
        }

        @Override
        public void onCancelled(@NonNull DatabaseError error) {
            Toast.makeText(getContext(), error.getMessage(), Toast.LENGTH_SHORT).show();
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

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        HomeViewModel homeViewModel = new ViewModelProvider(this).get(HomeViewModel.class);

        binding = FragmentDriverHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();
        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        polylines = new ArrayList<>();


        mCustomerInfo = (LinearLayout) root.findViewById(R.id.customerInfo);
        mCustomerProfileImage = (ImageView) root.findViewById(R.id.customerProfileImage);
        mCustomerName = (TextView) root.findViewById(R.id.customerName);
        mCustomerPhone = (TextView) root.findViewById(R.id.customerPhone);
        mCustomerDestination = (TextView) root.findViewById(R.id.customerDestination);

        mRideStatus = (Button) root.findViewById(R.id.rideStatus);

        mRideStatus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (status) {
                    case 1:
                        status = 2;
                        erasePolylines();
                        if (destinationLatLng.latitude!=0.0 && destinationLatLng.longitude!=0.0){
                            getRouteToMaker(destinationLatLng);
                        }


                        mRideStatus.setText("Hoàn thành rồi!");

                        break;
                    case 2:
                        recordRide();
                        endRide();
                        break;
                }
            }
        });
        // Toast.makeText(getContext(), "Debug đang chạy giao diện trên driver!", Toast.LENGTH_SHORT).show();
        init();
        getAssignedCustomer();

        return root;
    }

    public class SharedViewModel extends ViewModel {
        private final MutableLiveData<String> sharedData = new MutableLiveData<>();

        public void setSharedData(String data) {
            sharedData.setValue(data);
        }

        public LiveData<String> getSharedData() {
            return sharedData;
        }
    }

    private void endRide() {
        mRideStatus.setText("Kết thúc chuyến đi.");
        erasePolylines();

        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference driverRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(userId).child("customerRequest");
        driverRef.setValue(true);

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("customerRequest");
        GeoFire geoFire = new GeoFire(ref);
        geoFire.removeLocation(customerId);
        customerId=" ";

        if(pickupMarker != null){
            pickupMarker.remove();
        }

        if (assignedCustomerPickupLocationRefListener != null){
            assignedCustomerPickupLocationRef.removeEventListener(assignedCustomerPickupLocationRefListener);
        }

        mCustomerInfo.setVisibility(View.GONE);
        mCustomerName.setText("");
        mCustomerPhone.setText("");
        mCustomerDestination.setText("Vị trí: --");
        mCustomerProfileImage.setImageResource(R.mipmap.ic_laucher);
    }


    double price_per_call;
    private void recordRide(){
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference driverRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(userId).child("history");
        DatabaseReference customerRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Customers").child(customerId).child("history");
        DatabaseReference historyRef = FirebaseDatabase.getInstance().getReference().child("history");

        String requestId = historyRef.push().getKey();

        driverRef.child(requestId).setValue(true);
        customerRef.child(requestId).setValue(true);

        HashMap map = new HashMap();
        map.put("driver", userId);
        map.put("customer", customerId);
        map.put("destination", cityName);
        map.put("location/from/lat", newPosition.latitude);
        map.put("location/from/lng", newPosition.longitude);

        map.put("location/to/lat", pickupLatLng.latitude);
        map.put("location/to/lng", pickupLatLng.longitude);

        map.put("rating", 0);
        map.put("time", getCurrentTimeStamp());
        map.put("prices", price_per_call);
        historyRef.child(requestId).updateChildren(map);
    }

    private Long getCurrentTimeStamp() {
        Long timeStamp = System.currentTimeMillis()/1000;
        return timeStamp;
    }


    private void getAssignedCustomer() {
        String driverId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference assignedCustomerRef = FirebaseDatabase.getInstance().getReference()
                .child("Users")
                .child("Drivers")
                .child(driverId)
                .child("customerRequest")
                .child("customerRideId");

        assignedCustomerRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    // Kiểm tra nếu dữ liệu là kiểu String
                    customerId = snapshot.getValue(String.class);

                    if (customerId != null) {

                        status = 1;

                        Log.d("CustomerID", "Customer ID: " + customerId);

                        // Gọi các hàm xử lý khác
                        getAssignedCustomerPickupLocation();
                        getAssignedCustomerDestination();
                        getAssignedCustomerInfo();
                    } else {
                        Log.e("CustomerID", "customerRideId is null or not found");
                    }
                }
                else {
                    Log.d("CustomerID", "Không tìm thấy customerRequest");
                    // Xử lý nếu không tìm thấy customerRequest
                    endRide();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void getAssignedCustomerDestination() {
        String driverId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference assignedCustomerRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(driverId).child("customerRequest");
        assignedCustomerRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Map<String, Object> map = (Map<String, Object>) snapshot.getValue();

                    if (map.get("destination")!=null){
                        destination = map.get("destination").toString();
                        mCustomerDestination.setText("Vị trí:: " + destination);

                    }
                    else{
                        mCustomerDestination.setText("Vị trí: --");
                    }

                    Double destinationLat = 0.0;
                    Double destinationLng = 0.0;

                    if (map.get("destinationLat") != null){
                        destinationLat = Double.valueOf(map.get("destinationLat").toString());
                    }

                    if (map.get("destinationLng") != null){
                        destinationLat = Double.valueOf(map.get("destinationLng").toString());
                        destinationLatLng = new LatLng(destinationLat, destinationLng);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void getAssignedCustomerInfo() {

        mCustomerInfo.setVisibility(View.VISIBLE);
        DatabaseReference mCustomerDatabase = FirebaseDatabase.getInstance().getReference().child("Users").child("Customers").child(customerId);
        mCustomerDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists() && snapshot.getChildrenCount() > 0){
                    Map<String, Object> map = (Map<String, Object>) snapshot.getValue();
                    if (map.get("firstName") != null){
                        mCustomerName.setText(map.get("firstName").toString());
                    }
                    if (map.get("phoneNumber") != null){
                        mCustomerPhone.setText(map.get("phoneNumber").toString());
                    }
                    if (map.get("profileImageUri") != null){
                        Glide.with(getContext()).load(map.get("profileImageUri").toString()).into(mCustomerProfileImage);
                    }

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }


    private void getDriverCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(getContext(), "Unable to get current location.", Toast.LENGTH_SHORT).show();

        }

        fusedLocationProviderClient.getLastLocation()
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        mLastLocation = location;
                        // mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15));

                        // Hiển thị marker trên bản đồ
                        // mMap.addMarker(new MarkerOptions().position(currentLatLng).title("Driver Location"));
                    } else {
                        Toast.makeText(getContext(), "Unable to get current location.", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    Marker pickupMarker;
    LatLng pickupLatLng;
    private DatabaseReference assignedCustomerPickupLocationRef;
    private ValueEventListener assignedCustomerPickupLocationRefListener;

    private void getAssignedCustomerPickupLocation() {
        assignedCustomerPickupLocationRef = FirebaseDatabase.getInstance().getReference().child("customerRequest").child(customerId).child("l");
        assignedCustomerPickupLocationRefListener = assignedCustomerPickupLocationRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists() && !customerId.equals("")){
                    List<Object> map = (List<Object>) snapshot.getValue();
                    double locationLat = 0;
                    double locationLng = 0;
                    if (map.get(0) != null){
                        locationLat = Double.parseDouble(map.get(0).toString());
                    }
                    if (map.get(1) != null){
                        locationLng = Double.parseDouble(map.get(1).toString());
                    }

                    pickupLatLng = new LatLng(locationLat, locationLng);

                    // tinh tien cua mot chuyen di
                    Location loc1 = new Location("");
                    loc1.setLatitude(locationLat);
                    loc1.setLongitude(locationLng);

                    Location loc2 = new Location("");
                    loc2.setLatitude(newPosition.latitude);
                    loc2.setLongitude(newPosition.longitude);

                    float distance = loc1.distanceTo(loc2);
                    price_per_call = 0.0;
                    if (currentService.equals("Xe hơi"))
                        price_per_call = price_per_call * 10000;
                    else
                        price_per_call = price_per_call * 5000;

                    pickupMarker = mMap.addMarker(new MarkerOptions().position(pickupLatLng).title("Vị trí đón khách đây nè"));

                    // getDriverCurrentLocation();
//                    Log.d("Routing: ", "dia diem pick up routing" + pickupLatLng);
//                    Log.d("Routing", "dia diem hien tai driver" + newPosition.latitude + "," + newPosition.longitude);

                    getRouteToMaker(pickupLatLng);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private String currentService = "";
    private void getDriverService() {
        String driverId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        DatabaseReference serviceRef = FirebaseDatabase.getInstance()
                .getReference("Users")
                .child("Drivers")
                .child(driverId)
                .child("service");

        // Lắng nghe dữ liệu từ Firebase
        serviceRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String service = snapshot.getValue(String.class);
                } else {
                    Toast.makeText(getContext(), "Không lấy được service rồi", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    // khong phai billing account thi khong ve duoc
    private void getRouteToMaker(LatLng pickupLatLng) {
        if (pickupLatLng == null || newPosition == null) {
            Log.e("Routing", "pickupLatLng hoặc mLastLocation bị null");
            return;
        }

        Routing routing = new Routing.Builder()
                .travelMode(AbstractRouting.TravelMode.DRIVING)
                .withListener(new RoutingListener() {
                    @Override
                    public void onRoutingFailure(RouteException e) {
                        if (e != null) {
                            Log.e("Routing", "Routing thất bại: " + e.getMessage());
                        } else {
                            Log.e("Routing", "Routing thất bại: Không rõ nguyên nhân");
                        }
                    }

                    @Override
                    public void onRoutingStart() {
                        Log.d("Routing", "Bắt đầu tạo tuyến đường...");
                    }

                    @Override
                    public void onRoutingSuccess(ArrayList<Route> route, int shortestRouteIndex) {
                        Log.d("Routing", "Tuyến đường thành công!");

                        // Xóa tuyến đường cũ (nếu có)
                        if (polylines != null) {
                            for (Polyline polyline : polylines) {
                                polyline.remove();
                            }
                        }
                        polylines = new ArrayList<>();

                        // Hiển thị tuyến đường lên bản đồ
                        for (int i = 0; i < route.size(); i++) {
                            int colorIndex = i % COLORS.length;
                            PolylineOptions polylineOptions = new PolylineOptions();
                            polylineOptions.color(getResources().getColor(COLORS[colorIndex]));
                            polylineOptions.width(10 + i * 3);
                            polylineOptions.addAll(route.get(i).getPoints());
                            Polyline polyline = mMap.addPolyline(polylineOptions);
                            polylines.add(polyline);

                            Log.d("Routing", "Tuyến đường " + (i + 1) + ": Khoảng cách - " + route.get(i).getDistanceValue() + "m");
                        }
                    }

                    @Override
                    public void onRoutingCancelled() {
                        Log.d("Routing", "Routing bị hủy.");
                    }
                })
                .alternativeRoutes(false)
                .waypoints(new LatLng(newPosition.latitude, newPosition.longitude), pickupLatLng)
                .key(String.valueOf(R.string.myApiKeys))
                .build();

        routing.execute();
    }

    String cityName;

    private void init(){
        onlineRef = FirebaseDatabase.getInstance().getReference().child(".info/connected");

        if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(getContext(), getString(R.string.permission_require), Toast.LENGTH_SHORT).show();
            return;
        }


        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                super.onLocationResult(locationResult);

                newPosition = new LatLng(locationResult.getLastLocation().getLatitude(),
                        locationResult.getLastLocation().getLongitude());
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(newPosition, 18f));

                String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
                DatabaseReference refAvailable = FirebaseDatabase.getInstance().getReference(Common.DRIVERS_LOCATION_REFERENCES);
                DatabaseReference refWorking = FirebaseDatabase.getInstance().getReference("driversWorking");
                GeoFire geoFireAvailable = new GeoFire(refAvailable);
                GeoFire geoFireWorking  = new GeoFire(refWorking);

                switch (customerId){
                    case "":
                        geoFireWorking.removeLocation(userId);
                        geoFireAvailable.setLocation(userId, new GeoLocation(locationResult.getLastLocation().getLatitude(),
                                locationResult.getLastLocation().getLongitude()));

                        break;

                    default:
                        geoFireAvailable.removeLocation(userId);
                        geoFireWorking.setLocation(userId, new GeoLocation(locationResult.getLastLocation().getLatitude(),
                                locationResult.getLastLocation().getLongitude()));
                        break;
                }

                // mLastLocation = locationResult.getLocations().get(0);

                // sau khi co duoc vi tri thi se lay dia chi
                Geocoder geocoder = new Geocoder(getContext(), Locale.getDefault());
                List<Address> addressList ;
                try{
                    // lay thanh pho hien tai cua current user
                    addressList = geocoder.getFromLocation(locationResult.getLastLocation().getLatitude(),
                            locationResult.getLastLocation().getLongitude(), 1);
                    cityName = addressList.get(0).getLocality();
                    if (cityName==null) cityName = addressList.get(0).getSubLocality();


                    // them vao db
                    driversLocationRef = FirebaseDatabase.getInstance().getReference(Common.DRIVERS_LOCATION_REFERENCES);

                    currentUserRef = driversLocationRef.child(FirebaseAuth.getInstance().getCurrentUser().getUid());

                    geofire = new GeoFire(driversLocationRef);


                    // update location
                    geofire.setLocation(FirebaseAuth.getInstance().getCurrentUser().getUid(),
                            new GeoLocation(locationResult.getLastLocation().getLatitude(),
                                    locationResult.getLastLocation().getLongitude()),
                            (key, error) -> {
                                if(error != null)
                                    Toast.makeText(getContext(), error.getMessage(), Toast.LENGTH_LONG)
                                            .show();
                                else
                                {
                                    if (isFirstTime) {
                                        Toast.makeText(getContext(), "Bắt đầu thôi", Toast.LENGTH_LONG)
                                                .show();
                                        isFirstTime = false;
                                    }
                                }
                            });

                    // chi khi dang nhap thi moi co trong db
                    registerOnlineSystem();
                } catch (IOException e) {
                    Snackbar.make(getView(), e.getMessage(), Snackbar.LENGTH_SHORT).show();
                }
            }
        };


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
//        fusedLocationProviderClient.getLastLocation().addOnSuccessListener(location ->{
//            mLastLocation = location;
//        });


        // set giao dien cho map
        mMap.getUiSettings().setZoomControlsEnabled(true);
        // check permission
        Dexter.withContext(getContext())
                .withPermission(android.Manifest.permission.ACCESS_FINE_LOCATION)
                .withListener(new PermissionListener() {
                    @Override
                    public void onPermissionGranted(PermissionGrantedResponse permissionGrantedResponse) {
                        if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                            Toast.makeText(getContext(), getString(R.string.permission_require), Toast.LENGTH_SHORT).show();
                            return;
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
                                        .addOnFailureListener(e -> Toast.makeText(getContext(), "" + e.getMessage(), Toast.LENGTH_SHORT).show())
                                        .addOnSuccessListener(location -> {
                                            LatLng userLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                                            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(userLatLng, 18f));
                                        });
                                return true;
                            }
                        });
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

    final int LOCATION_REQUEST_CODE = 1;
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode){
            case LOCATION_REQUEST_CODE:
                if (grantResults.length>0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    mapFragment.getMapAsync(this);
                }
                else{
                    Toast.makeText(getContext(), "Không có quyền rồi, cấp quyền đi mà.", Toast.LENGTH_SHORT).show();
                }

                break;
        }
    }

    // Routing ==================================
    private List<Polyline> polylines;
    private static final int[] COLORS = new int[]{R.color.colorPrimary};

    @Override
    public void onRoutingFailure(RouteException e) {
        if(e != null) {
            Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }else {
            Toast.makeText(getContext(), "Something went wrong, Try again", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRoutingStart() {

    }

    @SuppressLint("RestrictedApi")
    @Override
    public void onRoutingSuccess(ArrayList<Route> route, int shortestRouteIndex) {
        if(polylines.size()>0) {
            for (Polyline poly : polylines) {
                poly.remove();
            }
        }

        polylines = new ArrayList<>();
        //add route(s) to the map.
        for (int i = 0; i <route.size(); i++) {

            //In case of more than 5 alternative routes
            int colorIndex = i % COLORS.length;

            PolylineOptions polyOptions = new PolylineOptions();
            polyOptions.color(getResources().getColor(COLORS[colorIndex]));
            polyOptions.width(10 + i * 3);
            polyOptions.addAll(route.get(i).getPoints());
            Polyline polyline = mMap.addPolyline(polyOptions);
            polylines.add(polyline);

            Toast.makeText(getApplicationContext(),"Route "+ (i+1) +": distance - "+ route.get(i).getDistanceValue()+": duration - "+ route.get(i).getDurationValue(),Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRoutingCancelled() {

    }
    private void erasePolylines(){
        for (Polyline line : polylines){
            line.remove();
        }
        polylines.clear();
    }

    protected synchronized void buildGoogleApiClient(){
        mGoogleApiClient = new GoogleApiClient.Builder(getContext())
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();

        mGoogleApiClient.connect();
    }



    // map update period
    @Override
    public void onLocationChanged(@NonNull Location location) {
        mLastLocation = location;

        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());

        mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
        mMap.animateCamera(CameraUpdateFactory.zoomTo(11));


        // bat su kien driver available tren gg map
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference refAvailable = FirebaseDatabase.getInstance().getReference("availableDriver");
        DatabaseReference refWorking = FirebaseDatabase.getInstance().getReference("driversWorking");

        GeoFire geoFireAvailable = new GeoFire(refAvailable);
        GeoFire geoFireWorking = new GeoFire(refWorking);


        Log.d("CustomerID", "switch workings" + customerId);

        switch (customerId){
            case "":
                geoFireAvailable.removeLocation(userId);
                geoFireWorking.setLocation(userId, new GeoLocation(location.getLatitude(), location.getLongitude()));

                break;

            default:
                geoFireWorking.removeLocation(userId);
                geoFireAvailable.setLocation(userId, new GeoLocation(location.getLatitude(), location.getLongitude()));

                break;
        }

//        if (!customerId.equals(" ")) {
//            geoFireWorking.removeLocation(userId);
//            geoFireAvailable.setLocation(userId, new GeoLocation(location.getLatitude(), location.getLongitude()));
//
//        } else {
//            geoFireAvailable.removeLocation(userId);
//            geoFireWorking.setLocation(userId, new GeoLocation(location.getLatitude(), location.getLongitude()));
//        }
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

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onStop() {
        super.onStop();
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("availableDriver");

        GeoFire geoFire = new GeoFire(ref);
        geoFire.removeLocation(userId);
    }
}