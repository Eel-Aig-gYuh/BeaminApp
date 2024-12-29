package com.example.ubercus.Driver;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.example.ubercus.R;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class DriverSettingActivity extends AppCompatActivity {

    private EditText mFirstNameField, mLastNameField, mPhoneField;
    private RadioGroup mRadioGroup;
    private String service;
    private EditText mCarField;

    private Button mBack, mConfirm;

    private FirebaseAuth mAuth;

    private DatabaseReference mDriverDatabase;

    private String userId;
    private String mFirstName, mLastName;
    private String mPhone;
    private String mCar;


    private String mProfileImage;
    private Uri resultUri;

    private ImageView mImageView;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_driver_setting);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        mImageView = (ImageView) findViewById(R.id.driverProfileImg);
        mRadioGroup = (RadioGroup) findViewById(R.id.rdo_type_car);

        mFirstNameField = (EditText) findViewById(R.id.edt_first_name);
        mLastNameField = (EditText) findViewById(R.id.edt_last_name);
        mPhoneField = (EditText) findViewById(R.id.edt_phone_number);
        mCarField = (EditText) findViewById(R.id.edt_car);

        mConfirm = (Button) findViewById(R.id.driverConfirm);
        mBack = (Button) findViewById(R.id.driverBack);

        mAuth = FirebaseAuth.getInstance();
        userId = mAuth.getCurrentUser().getUid();
        mDriverDatabase = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(userId);

        getUserInformation();

        mImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_PICK);
                intent.setType("image/*");

                startActivityForResult(intent, 1);
            }
        });

        mConfirm.setOnClickListener(v -> saveUserInformation());
        mBack.setOnClickListener(v -> finish());

    }

    private void saveUserInformation() {
        mFirstName = mFirstNameField.getText().toString();
        mLastName = mLastNameField.getText().toString();
        mPhone = mPhoneField.getText().toString();

        int selectedId = mRadioGroup.getCheckedRadioButtonId();

        final RadioButton radioButton = (RadioButton) findViewById(selectedId);

        if(radioButton.getText() == null){
            return;
        }

        service = radioButton.getText().toString();



        Map userInfo = new HashMap();
        userInfo.put("firstName", mFirstName);
        userInfo.put("firstName", mLastName);
        userInfo.put("phoneNumber", mPhone);
        userInfo.put("car", mCar);
        userInfo.put("service", service);

        mDriverDatabase.updateChildren(userInfo);

        if (resultUri != null){


            StorageReference filePath = FirebaseStorage.getInstance().getReference().child("profile_images").child(userId);
            Bitmap bitmap = null;

            try {
                bitmap = MediaStore.Images.Media.getBitmap(getApplication().getContentResolver(), resultUri );
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            ByteArrayOutputStream boas = new ByteArrayOutputStream();

            bitmap.compress(Bitmap.CompressFormat.JPEG, 20, boas);

            byte[] data = boas.toByteArray();
            UploadTask uploadTask = filePath.putBytes(data);

            uploadTask.addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    finish();
                }
            });

            uploadTask.addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    Uri downloadUrl = taskSnapshot.getUploadSessionUri();

                    Map newImage = new HashMap();
                    newImage.put("profileImagUrl", downloadUrl);

                    mDriverDatabase.updateChildren(newImage);

                    finish();
                }
            });

        }
        else
            finish();

        finish();
    }

    private void getUserInformation() {
        mDriverDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists() && snapshot.getChildrenCount()>0){
                    Map<String, Object> map = (Map<String, Object>) snapshot.getValue();

                    if(map.get("firstName") != null){
                        mFirstName = map.get("firstName").toString();
                        mFirstNameField.setText(mFirstName);
                    }
                    if(map.get("firstName") != null){
                        mLastName = map.get("lastName").toString();
                        mLastNameField.setText(mLastName);
                    }
                    if(map.get("phoneNumber") != null){
                        mPhone = map.get("phoneNumber").toString();
                        mPhoneField.setText(mPhone);
                    }
                    if(map.get("car") != null){
                        mCar = map.get("car").toString();
                        mCarField.setText(mCar);
                    }
                    if(map.get("service") != null){
                        service = map.get("service").toString();
                        switch (service){
                            case "type_motorbike":
                                mRadioGroup.check(R.id.type_motorbike);
                                break;

                            case "type_car":
                                mRadioGroup.check(R.id.type_car);
                                break;

                            default:
                                mRadioGroup.check(R.id.type_motorbike);
                                break;
                        }
                    }
                    if(map.get("profileImageUrl") != null){
                        mProfileImage = map.get("profileImageUrl").toString();
                        Glide.with(getApplication()).load(mProfileImage).into(mImageView);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 1 && resultCode == RESULT_OK){
            final Uri imagUri = data.getData();

            resultUri = imagUri;
            mImageView.setImageURI(resultUri);
        }
    }
}