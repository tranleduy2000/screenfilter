package com.duy.screenfilter.adapter;

import android.content.Context;
import android.support.annotation.ColorInt;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.duy.screenfilter.R;

/**
 * Created by Duy on 21-Aug-17.
 */

public class ColorAdapter extends RecyclerView.Adapter<ColorAdapter.ViewHolder> {
    @ColorInt
    private final int[] colors;
    private LayoutInflater inflater;
    private OnColorClickListener listener;

    public ColorAdapter(Context context, OnColorClickListener listener) {
        this.colors = context.getResources().getIntArray(R.array.filter_colors);
        this.inflater = LayoutInflater.from(context);
        this.listener = listener;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolder(inflater.inflate(R.layout.list_item_color, parent, false));
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        holder.color.setCardBackgroundColor(colors[position]);
        holder.color.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (listener != null) listener.onColorClicked(colors[holder.getAdapterPosition()]);
            }
        });
    }

    @Override
    public int getItemCount() {
        return colors.length;
    }

    public interface OnColorClickListener {
        void onColorClicked(int color);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private CardView color;

        public ViewHolder(View itemView) {
            super(itemView);
            color = itemView.findViewById(R.id.card_color);
        }
    }
}
