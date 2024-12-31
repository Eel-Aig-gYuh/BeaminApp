package com.example.ubercus.Services;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ubercus.R;

public class HistoryViewHolders extends RecyclerView.ViewHolder implements View.OnClickListener {

    public TextView rideId;
    public TextView timeStamp;
    public TextView prices;

    public HistoryViewHolders(@NonNull View itemView) {
        super(itemView);
        itemView.setOnClickListener(this);

        rideId = (TextView) itemView.findViewById(R.id.rideId);
        timeStamp = (TextView) itemView.findViewById(R.id.timeStamp);
        prices = (TextView) itemView.findViewById(R.id.prices);
    }

    @Override
    public void onClick(View v) {
        Intent intent = new Intent(v.getContext(), HistorySingleActivity.class);
        Bundle bundle = new Bundle();
        bundle.putString("rideId", rideId.getContext().toString());
        v.getContext().startActivity(intent);

    }
}
