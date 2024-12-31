package com.example.ubercus.Services;


import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.ubercus.History.HistoryObject;
import com.example.ubercus.R;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.paypal.android.sdk.payments.PayPalConfiguration;
import com.paypal.android.sdk.payments.PayPalPayment;
import com.paypal.android.sdk.payments.PayPalService;
import com.paypal.android.sdk.payments.PaymentActivity;

import java.math.BigDecimal;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;
import java.util.Map;

public class HistorySingleActivity extends AppCompatActivity implements OnMapReadyCallback {
    private String  rideId, currentUserId, customerId, driverId, userDriverOrCustomer;

    private TextView loctionRide;
    private TextView distanceRide;
    private TextView dateRide;
    private TextView nameUser;
    private TextView phoneUser;
    private Button mPay;

    private LatLng destinationLatLng, pickupLatLng;

    private DatabaseReference historyRideInfoDB;

    private GoogleMap mMap;
    private SupportMapFragment mMapFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_history_single);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });


        Intent intent = new Intent(this, PayPalService.class);
        intent.putExtra(PayPalService.EXTRA_PAYPAL_CONFIGURATION, config);
        startService(intent);


        mMapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);

        mMapFragment.getMapAsync(this);

        mPay = (Button) findViewById(R.id.pay);

        loctionRide = findViewById(R.id.rideLocation);
        distanceRide = findViewById(R.id.rideDistance);
        dateRide = findViewById(R.id.rideDate);
        nameUser = findViewById(R.id.userName);
        phoneUser = findViewById(R.id.userPhone);


        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();


        historyRideInfoDB = FirebaseDatabase.getInstance().getReference().child("history").child(rideId);
        getRidingInfomation();

    }

    private void displayCustomerRelatedObjects(){
        mPay.setVisibility(View.VISIBLE);

        mPay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                payPalPayment();
            }
        });
    }

    private void payPalPayment() {
        double ridePrice = 0;
        PayPalPayment payPalPayment = new PayPalPayment(new BigDecimal(ridePrice), "VND", "Baemin Ride",
                PayPalPayment.PAYMENT_INTENT_SALE);

        Intent intent = new Intent(this, PaymentActivity.class);

        intent.putExtra(PayPalService.EXTRA_PAYPAL_CONFIGURATION, config);
        intent.putExtra(PaymentActivity.EXTRA_PAYMENT, payPalPayment);

        startActivityForResult(intent, PAYPAL_REQUEST_CODE);
    }


    @Override
    protected void onDestroy() {
        stopService(new Intent(this, PayPalService.class));
        super.onDestroy();
    }


    private int PAYPAL_REQUEST_CODE = 1;
    private static PayPalConfiguration config = new PayPalConfiguration()
            .environment(PayPalConfiguration.ENVIRONMENT_SANDBOX)
            .clientId(PayPalConfig.PAYPAL_CLIENT_ID);



    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PAYPAL_REQUEST_CODE){
            if (requestCode == Activity.RESULT_OK){

            }
            else{
                Toast.makeText(this, "Thanh toán thất bại rồi !", Toast.LENGTH_SHORT).show();
            }
        }

    }


    private void getRidingInfomation() {
        historyRideInfoDB.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot child: snapshot.getChildren()){
                    if(child.getKey().equals("customer")){
                        customerId = child.getValue().toString();

                        if (!customerId.equals(currentUserId)){
                            userDriverOrCustomer = "Drivers";

                            getIntent().putExtra("customerOrDriver", "Drivers");

                            getUserInformation("Customers", customerId);

                        }
                    }

                    if(child.getKey().equals("driver")){
                        driverId = child.getValue().toString();

                        if (!driverId.equals(currentUserId)){
                            userDriverOrCustomer = "Customers";
                            displayCustomerRelatedObjects();

                            getIntent().putExtra("customerOrDriver", "Customers");

                            getUserInformation("Drivers", driverId);
                        }
                    }

                    if(child.getKey().equals("timeStamp")){
                        dateRide.setText(getDate(Long.valueOf(child.getValue().toString())));

                    }

                    if(child.getKey().equals("destination")){
                        loctionRide.setText(getDate(Long.valueOf(child.getValue().toString())));

                    }

                    if(child.getKey().equals("location")){
                        pickupLatLng = new LatLng(Double.parseDouble(child.child("from").child("lat").getValue().toString()),
                                Double.parseDouble(child.child("from").child("lng").getValue().toString()));
                        destinationLatLng = new LatLng(Double.parseDouble(child.child("to").child("lat").getValue().toString()),
                                Double.parseDouble(child.child("to").child("lng").getValue().toString()));
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void getUserInformation(String ortherUserDriverOrCustomer, String otherUserId) {
        DatabaseReference mOtherUserDb = FirebaseDatabase.getInstance().getReference()
                .child("Users").child(ortherUserDriverOrCustomer).child(otherUserId);

        mOtherUserDb.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()){
                    Map<String, Object> map = (Map<String, Object>) snapshot.getValue();

                    if (map.get("firstName") != null){
                        nameUser.setText(map.get("firstName").toString());
                    }
                    if (map.get("phoneNumber") != null){
                        phoneUser.setText(map.get("phoneNumber").toString());
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

    }

    private String getDate(Long timeStamp) {
        Calendar cal = Calendar.getInstance(Locale.getDefault());
        cal.setTimeInMillis(timeStamp*1000);
        String date = DateFormat.getDateInstance().format("dd-MM-yyyy hh:mm");


        return date;
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {

    }
}