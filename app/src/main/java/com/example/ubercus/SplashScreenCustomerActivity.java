package com.example.ubercus;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.ubercus.Customer.CustomerHomeActivity;
import com.example.ubercus.Model.CustomerInfoModel;
import com.example.ubercus.Utils.UserUtils;
import com.example.ubercus.databinding.ActivitySplashScreenBinding;
import com.example.ubercus.databinding.LayoutRegisterBinding;
import com.firebase.geofire.GeoFire;
import com.firebase.ui.auth.AuthMethodPickerLayout;
import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.IdpResponse;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Completable;

public class SplashScreenCustomerActivity extends AppCompatActivity {
    private static final int LOGIN_REQUEST_CODE = 1234;
    private List<AuthUI.IdpConfig> providers;
    private FirebaseAuth firebaseAuth;
    private FirebaseAuth.AuthStateListener listener;
    private FirebaseDatabase database;
    private DatabaseReference customerInfoRef;

    private ActivitySplashScreenBinding binding;
    private GeoFire geoFire;

    @Override
    protected void onStart() {
        super.onStart();
        delaySplashScreen();
    }

    @Override
    protected void onStop() {
        if (firebaseAuth != null && listener != null)
            firebaseAuth.removeAuthStateListener(listener);
        super.onStop();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        binding = ActivitySplashScreenBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        init();
    }

    private void init() {
        database = FirebaseDatabase.getInstance();
        customerInfoRef = database.getReference("Users").child(Common.CUSTOMER_INFO_REFERENCE);

        providers = Arrays.asList(new AuthUI.IdpConfig.PhoneBuilder().build(),
                new AuthUI.IdpConfig.GoogleBuilder().build());

        firebaseAuth = FirebaseAuth.getInstance();
        listener = myFirebaseAuth -> {
            FirebaseUser user = myFirebaseAuth.getCurrentUser();
            if (user != null) {
                // update token
                FirebaseMessaging.getInstance().getToken()
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Toast.makeText(SplashScreenCustomerActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        }).addOnSuccessListener(new OnSuccessListener<String>() {
                            @Override
                            public void onSuccess(String s) {
                                Log.d("TOKEN", s.toString());
                                UserUtils.updateToken(SplashScreenCustomerActivity.this, s.toString());
                            }
                        });


                checkUserFromFirebase();
                binding.progressBar.setVisibility(View.VISIBLE);
                Toast.makeText(SplashScreenCustomerActivity.this, "Chào mừng quay trở lại !" + user.getUid(), Toast.LENGTH_SHORT).show();
            } else {
                showLoginLayout();
            }
        };
    }

    private void checkUserFromFirebase() {
        customerInfoRef.child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            // Toast.makeText(SplashScreenCustomerActivity.this, "User already register", Toast.LENGTH_SHORT).show();
                            CustomerInfoModel customerInfoModel=snapshot.getValue(CustomerInfoModel.class);
                            goToHomeActivity(customerInfoModel);
                        } else {
                            showRegisterLayout();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(SplashScreenCustomerActivity.this, "" + error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void goToHomeActivity(CustomerInfoModel customerInfoModel){
        Common.currenCustomer=customerInfoModel;
        startActivity(new Intent(SplashScreenCustomerActivity.this, CustomerHomeActivity.class));
        finish();
    }
    private void showRegisterLayout() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.DialogTheme);
        LayoutRegisterBinding registerBinding = LayoutRegisterBinding.inflate(LayoutInflater.from(this));

        builder.setView(registerBinding.getRoot());
        AlertDialog dialog = builder.create();
        dialog.show();

        if (FirebaseAuth.getInstance().getCurrentUser().getPhoneNumber() != null &&
                !TextUtils.isEmpty(FirebaseAuth.getInstance().getCurrentUser().getPhoneNumber()))
            registerBinding.edtPhoneNumber.setText(FirebaseAuth.getInstance().getCurrentUser().getPhoneNumber());

        registerBinding.btnRegister.setOnClickListener(view -> {
            if (TextUtils.isEmpty(registerBinding.edtFirstName.getText().toString())) {
                Toast.makeText(this, "Vui lòng nhập tên của bạn!", Toast.LENGTH_SHORT).show();
                return;
            } else if (TextUtils.isEmpty(registerBinding.edtLastName.getText().toString())) {
                Toast.makeText(this, "Vui lòng nhập họ và chữ lót của bạn!", Toast.LENGTH_SHORT).show();
                return;
            } else if (TextUtils.isEmpty(registerBinding.edtPhoneNumber.getText().toString())) {
                Toast.makeText(this, "Vui lòng nhập số điện thoại của bạn!", Toast.LENGTH_SHORT).show();
                return;
            } else {
                CustomerInfoModel model = new CustomerInfoModel();
                model.setFirstName(registerBinding.edtFirstName.getText().toString());
                model.setLastName(registerBinding.edtLastName.getText().toString());
                model.setPhoneNumber(registerBinding.edtPhoneNumber.getText().toString());
                model.setRating(0.0);

                customerInfoRef.child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                        .setValue(model)
                        .addOnFailureListener(e ->
                        {
                            dialog.dismiss();
                            Toast.makeText(SplashScreenCustomerActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();

                        })
                        .addOnSuccessListener(unused -> {
                            Toast.makeText(this, "Đăng ký thành công rồi!", Toast.LENGTH_SHORT).show();
                            dialog.dismiss();
                            goToHomeActivity(model);
                        });
            }
        });
    }

    private void showLoginLayout() {
        AuthMethodPickerLayout authMethodPickerLayout = new AuthMethodPickerLayout
                .Builder(R.layout.layout_sign_in)
                .setPhoneButtonId(R.id.btn_phone_sign_in)
                .setGoogleButtonId(R.id.btn_google_sign_in)
                .build();

        startActivityForResult(AuthUI.getInstance()
                .createSignInIntentBuilder()
                .setAuthMethodPickerLayout(authMethodPickerLayout)
                .setTheme(R.style.LoginTheme)
                .setIsSmartLockEnabled(false)
                .setAvailableProviders(providers)
                .build(), LOGIN_REQUEST_CODE);
    }

    @SuppressLint("CheckResult")
    private void delaySplashScreen() {
        binding.progressBar.setVisibility(View.VISIBLE);

        Completable.timer(5, TimeUnit.SECONDS,
                        AndroidSchedulers.mainThread())
                .subscribe(() -> firebaseAuth.addAuthStateListener(listener));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == LOGIN_REQUEST_CODE) {
            IdpResponse response = IdpResponse.fromResultIntent(data);
            if (resultCode == RESULT_OK) {
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            } else {
                Toast.makeText(this, "[ERROR]" + response.getError().getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }
}