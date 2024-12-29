package com.example.ubercus.Utils;

import android.content.Context;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.drawerlayout.widget.DrawerLayout;

import com.example.ubercus.Common;
import com.example.ubercus.Model.TokenModel;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;

import java.util.Map;
import java.util.Objects;


public class DriverUtils {
    public static void updateUser(View view, Map<String, Object> updateData){
        FirebaseDatabase.getInstance()
                .getReference(Common.DRIVERS_INFO_REFERENCES)
                .child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                .updateChildren(updateData)
                .addOnFailureListener(e -> Snackbar.make(view, e.getMessage(), Snackbar.LENGTH_SHORT).show())
                .addOnSuccessListener(unused -> Snackbar.make(view, "Đã cập nhật thành công !", Snackbar.LENGTH_SHORT).show());
    }

    public static void updateToken(Context context, String token){
        TokenModel tokenModel = new TokenModel();

        FirebaseDatabase.getInstance()
                .getReference(Common.TOKEN_REFERENCE)
                .child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                .setValue(tokenModel)
                .addOnFailureListener(e -> Toast.makeText(context, e.getMessage(), Toast.LENGTH_SHORT))
                .addOnSuccessListener(unused -> {

                });
    }
}