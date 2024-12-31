package com.example.ubercus.Services;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ubercus.History.HistoryObject;
import com.example.ubercus.R;
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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Calendar;
import java.util.Locale;

public class HistoryActivity extends AppCompatActivity {
    private String customerOrDriver;
    private String userId;

    private RecyclerView mHistoryRecycleView;
    private RecyclerView.Adapter mHistoryAdapter;
    private RecyclerView.LayoutManager mHistoryLayoutManager;
    private Button mPay;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_history);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        mHistoryRecycleView = findViewById(R.id.historyRecycleView);
        mHistoryRecycleView.setNestedScrollingEnabled(false);
        mHistoryRecycleView.setHasFixedSize(true);

        mHistoryLayoutManager = new LinearLayoutManager(this);
        mHistoryRecycleView.setLayoutManager(mHistoryLayoutManager);

        mHistoryAdapter = new HistoryAdapter(resultsHistory, this);
        mHistoryRecycleView.setAdapter(mHistoryAdapter);

        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        if (getIntent() != null && getIntent().getExtras() != null) {
            customerOrDriver = getIntent().getExtras().getString("customerOrDriver", "Customers");
        } else {
            customerOrDriver = "Customers";
        }

        getUserHistoryId();
    }

    private ArrayList<HistoryObject> resultsHistory = new ArrayList<HistoryObject>();
    private ArrayList<HistoryObject> getDatasetsHistory() {
        return resultsHistory;
    }

    private void getUserHistoryId() {
        if (getIntent() != null && getIntent().getExtras() != null) {
            customerOrDriver = getIntent().getExtras().getString("customerOrDriver");
            if (customerOrDriver == null) {
                customerOrDriver = "Customers";
            }
        } else {
            customerOrDriver = "Customers";
        }
        DatabaseReference userHistoryDatabase = FirebaseDatabase.getInstance().getReference().child("Users").child(customerOrDriver).child(userId).child("history");

        userHistoryDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()){
                    for (DataSnapshot history: snapshot.getChildren()){
                        FetchRideInformation(history.getKey());
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void FetchRideInformation(String rideKey) {
        DatabaseReference historyDatabase = FirebaseDatabase.getInstance().getReference().child("history").child(rideKey);

        historyDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()){
                    for (DataSnapshot history: snapshot.getChildren()){
                        String rideId = snapshot.getKey();
                        Long timeStamp = 0L;
                        Double prices = 0.0;

                        // resultsHistory.add(new HistoryObject(String.valueOf(1)));

                        for (DataSnapshot child: snapshot.getChildren()){
                            if  (child.getKey().equals("timeStamp")) {
                                timeStamp = Long.valueOf(child.getValue().toString());
                            }
                        }

                        HistoryObject obj = new HistoryObject(rideKey, getDate(timeStamp), prices.toString());
                        resultsHistory.add(obj);
                        mHistoryAdapter.notifyDataSetChanged();
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private String getDate(Long timeStamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault());

        // Chuyển đổi timestamp sang Date
        Date date = new Date(timeStamp * 1000);

        // Trả về chuỗi đã định dạng
        return sdf.format(date);
    }
}