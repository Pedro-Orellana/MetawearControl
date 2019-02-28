package com.example.metawearcontrol2;

import android.support.annotation.NonNull;
import android.support.design.button.MaterialButton;
import android.support.design.card.MaterialCardView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class RecyclerViewAdapter extends RecyclerView.Adapter<RecyclerViewAdapter.MaterialCardViewHolder> {
    private String deviceName;
    private String deviceAddress;
    private int deviceType;
    private MainActivity.onClickImplementation implementation;


    public RecyclerViewAdapter(String deviceName, String deviceAddress, int deviceType, MainActivity.onClickImplementation implementation){
       this.deviceName = deviceName;
       this.deviceAddress = deviceAddress;
       this.deviceType = deviceType;
       this.implementation = implementation;
    }

    @NonNull
    @Override
    public RecyclerViewAdapter.MaterialCardViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.metawear_card,viewGroup,false);
        return new MaterialCardViewHolder(view);
    }


    @Override
    public void onBindViewHolder(@NonNull RecyclerViewAdapter.MaterialCardViewHolder holder, int i) {
        MaterialCardView cardView = holder.cardView;
        TextView deviceNameTextView = cardView.findViewById(R.id.text_view_device_name);
        TextView deviceAddressTextView = cardView.findViewById(R.id.text_view_battery);
        TextView deviceTypeTextView = cardView.findViewById(R.id.text_view_rssi);
        MaterialButton button = cardView.findViewById(R.id.button_connect_ring);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                implementation.myOnClick();
            }
        });

        deviceNameTextView.setText(deviceName);
        deviceAddressTextView.setText(deviceAddress);
        deviceTypeTextView.setText(Integer.toString(deviceType));

    }

    @Override
    public int getItemCount() {
        return 1;
    }


     class MaterialCardViewHolder extends RecyclerView.ViewHolder{
        private MaterialCardView cardView;

         MaterialCardViewHolder(View view){
            super(view);
            cardView = (MaterialCardView) view;
        }
    }
}
