package com.example.ubercus.Customer;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.Menu;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.example.ubercus.ChooseRole;
import com.example.ubercus.Common;
import com.example.ubercus.Driver.DriverHomeActivity;
import com.example.ubercus.R;
import com.example.ubercus.Services.HistoryActivity;
import com.example.ubercus.SplashScreenCustomerActivity;
import com.example.ubercus.Utils.UserUtils;
import com.example.ubercus.databinding.ActivityCustomerHomeBinding;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.navigation.NavigationView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.HashMap;
import java.util.Map;

public class CustomerHomeActivity extends AppCompatActivity {

    private static final int PICK_IMAGE_REQUEST = 7173;
    private AppBarConfiguration mAppBarConfiguration;
    private ActivityCustomerHomeBinding binding;
    private DrawerLayout drawer;
    private NavigationView navigationView;
    private NavController navController;

    private AlertDialog waitingDialog;
    private StorageReference storageReference;
    private Uri imgeUri;
    private ImageView img_avatar;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityCustomerHomeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.appBarCustomerHome.customerToolbar);

        drawer = binding.customerDrawerLayout;
        navigationView = binding.customerNavView;
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_home)
                .setOpenableLayout(drawer)
                .build();
        navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_customer_home);
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);

        init();

    }

    private void showDialogUpload() {
        AlertDialog.Builder builder = new AlertDialog.Builder(CustomerHomeActivity.this);
        builder.setTitle("Change avatar")
                .setMessage("Bạn thực sự muốn thay đổi ảnh đại diện ư?")
                .setNegativeButton("CANCEL", (dialog, which) -> {
                    dialog.dismiss();
                })
                .setPositiveButton("UPLOAD", (dialog, which) -> {
                    if (imgeUri != null) {
                        waitingDialog.setMessage("Uploading ...");
                        waitingDialog.show();

                        String unique_name = FirebaseAuth.getInstance().getCurrentUser().getUid();
                        StorageReference avatarFolder = storageReference.child("avatar/" + unique_name);

                        avatarFolder.putFile(imgeUri)
                                .addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        waitingDialog.dismiss();
                                        // Toast.makeText(CustomerHomeActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                                        Snackbar.make(drawer, e.getMessage(), Snackbar.LENGTH_SHORT).show();
                                    }
                                })
                                .addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                                    @Override
                                    public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                                        if (task.isSuccessful()) {
                                            avatarFolder.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                                @Override
                                                public void onSuccess(Uri uri) {
                                                    Map<String, Object> updateData = new HashMap<>();
                                                    updateData.put("avatar", uri.toString());

                                                    UserUtils.updateUser(drawer, updateData);
                                                }
                                            });
                                        }
                                    }
                                })
                                .addOnCompleteListener(task -> {
                                    if (task.isSuccessful()) {
                                        avatarFolder.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                            @Override
                                            public void onSuccess(Uri uri) {
                                                Map<String, Object> updateData = new HashMap<>();
                                                updateData.put("avatar", uri.toString());

                                                UserUtils.updateUser(drawer, updateData);
                                            }
                                        });
                                    }
                                    waitingDialog.dismiss();
                                })
                                .addOnProgressListener(snapshot -> {
                                    double progress = (100.0 * snapshot.getBytesTransferred() / snapshot.getTotalByteCount());
                                    waitingDialog.setMessage(new StringBuilder("Đang tải: ").append(progress).append("%"));
                                });
                    }
                })
                .setCancelable(false);
        AlertDialog dialog = builder.create();
        dialog.setOnShowListener(dialog1 -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                    .setTextColor(getResources().getColor(android.R.color.holo_red_dark));
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
                    .setTextColor(getResources().getColor(android.R.color.holo_green_dark));

        });
        dialog.show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK) {
            if (data != null && data.getData() != null) {
                imgeUri = data.getData();
                img_avatar.setImageURI(imgeUri);

                showDialogUpload();
            }
        }
    }

    private void init() {

        waitingDialog = new AlertDialog.Builder(this)
                .setCancelable(false)
                .setMessage("Đang chờ ...")
                .create();
        storageReference = FirebaseStorage.getInstance().getReference();

        navigationView.setNavigationItemSelectedListener(item -> {
            if (item.getItemId() == R.id.nav_sign_out) {
                AlertDialog.Builder builder = new AlertDialog.Builder(CustomerHomeActivity.this);
                builder.setTitle("sign_out")
                        .setMessage("Bạn thực sự muốn đăng xuất ư?")
                        .setNegativeButton("CANCEL", (dialog, which) -> {
                            dialog.dismiss();
                        })
                        .setPositiveButton("SIGN OUT", (dialog, which) -> {
                            FirebaseAuth.getInstance().signOut();
                            Intent intent = new Intent(CustomerHomeActivity.this, ChooseRole.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                            finish();
                        })
                        .setCancelable(false);
                AlertDialog dialog = builder.create();
                dialog.setOnShowListener(dialog1 -> {
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                            .setTextColor(getResources().getColor(android.R.color.holo_red_dark));
                    dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
                            .setTextColor(getResources().getColor(android.R.color.holo_green_dark));

                });
                dialog.show();
            }
            // nut lich su
            if (item.getItemId() == R.id.nav_history){
                Intent intent = new Intent(CustomerHomeActivity.this, HistoryActivity.class);
                startActivity(intent);
            }
            // cai dat thong tin
            if (item.getItemId() == R.id.nav_setting){
                Intent intent = new Intent(CustomerHomeActivity.this, CustomerSettingActivity.class);
                startActivity(intent);

            }
            return true;
        });

        // set data for user
        View headerView = navigationView.getHeaderView(0);
        TextView txt_name = (TextView) headerView.findViewById(R.id.txt_name);
        TextView txt_phone = (TextView) headerView.findViewById(R.id.txt_phone);
        TextView txt_start = (TextView) headerView.findViewById(R.id.txt_start);
        img_avatar = (ImageView) headerView.findViewById(R.id.img_avatar);

        txt_name.setText(Common.buildWelcomeMessage());
        txt_phone.setText(Common.currenCustomer != null ? Common.currenCustomer.getPhoneNumber() : "");
        txt_start.setText(Common.currenCustomer != null ? String.valueOf(Common.currenCustomer.getRating()) : "0.0");

        img_avatar.setOnClickListener(v -> {
            Intent intent = new Intent();
            intent.setType("image/*");
            intent.setAction(Intent.ACTION_GET_CONTENT);
            startActivityForResult(intent, PICK_IMAGE_REQUEST);
        });

        if (Common.currenCustomer != null && Common.currenCustomer.getAvatar() != null &&
                !TextUtils.isEmpty(Common.currenCustomer.getAvatar())) {
            Glide.with(this)
                    .load(Common.currenCustomer.getAvatar())
                    .into(img_avatar);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.driver_home, menu);
        return true;
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_customer_home);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration)
                || super.onSupportNavigateUp();
    }
}