package com.example.ubercus.Customer.ui.setting;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.ubercus.databinding.FragmentCustomerSettingBinding;

public class CustomerSettingFragment extends Fragment {

    private FragmentCustomerSettingBinding binding;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        CustomerSettingViewModel customerSettingViewModel =
                new ViewModelProvider(this).get(CustomerSettingViewModel.class);

        binding = FragmentCustomerSettingBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        final TextView textView = binding.customerTextGallery;
        customerSettingViewModel.getText().observe(getViewLifecycleOwner(), textView::setText);
        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}