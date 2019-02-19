package com.example.metawearcontrol2;

import android.support.annotation.NonNull;
import android.support.design.card.MaterialCardView;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;

public class RecyclerViewAdapter extends RecyclerView.Adapter<RecyclerViewAdapter.MaterialCardViewHolder> {
    @NonNull
    @Override
    public RecyclerViewAdapter.MaterialCardViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        return null;
    }


    @Override
    public void onBindViewHolder(@NonNull RecyclerViewAdapter.MaterialCardViewHolder materialCardViewHolder, int i) {

    }

    @Override
    public int getItemCount() {
        return 0;
    }


    public class MaterialCardViewHolder extends RecyclerView.ViewHolder{
        private MaterialCardView cardView;

        public MaterialCardViewHolder(View view){
            super(view);
            cardView = (MaterialCardView) view;
        }
    }
}
