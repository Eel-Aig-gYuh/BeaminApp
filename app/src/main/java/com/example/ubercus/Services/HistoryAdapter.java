package com.example.ubercus.Services;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ubercus.History.HistoryObject;
import com.example.ubercus.R;

import java.util.ArrayList;
import java.util.List;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder> {
    private ArrayList<HistoryObject> historyList;
    private Context context;

    public HistoryAdapter(ArrayList<HistoryObject> historyList, Context context) {
        this.historyList = historyList;
        this.context = context;
    }

    @NonNull
    @Override
    public HistoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_history, parent, false);
        return new HistoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HistoryViewHolder holder, int position) {
        HistoryObject history = historyList.get(position);
        holder.rideId.setText(history.getRideId());
        holder.date.setText(history.getTime());
        holder.price.setText(history.getPrices());
    }

    @Override
    public int getItemCount() {
        return historyList.size();
    }

    public static class HistoryViewHolder extends RecyclerView.ViewHolder {
        public TextView rideId, date, price;

        public HistoryViewHolder(@NonNull View itemView) {
            super(itemView);
            rideId = itemView.findViewById(R.id.rideId);
            date = itemView.findViewById(R.id.rideDate);
            price = itemView.findViewById(R.id.prices);
        }
    }
}