package com.viasofts.mygcs;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;

public class SimpleTextAdapter extends RecyclerView.Adapter<SimpleTextAdapter.ViewHolder> {

    private ArrayList<String> mData = null;

    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView textView1;

        ViewHolder(@NonNull View itemView) {
            super(itemView);

            textView1 = itemView.findViewById(R.id.recycler_view_item);
        }
    }

    SimpleTextAdapter(ArrayList<String> list) {
        mData = list;
    }

    @NonNull
    @Override
    public SimpleTextAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        View view = inflater.inflate(R.layout.main_item_list, parent, false);
        SimpleTextAdapter.ViewHolder vh = new SimpleTextAdapter.ViewHolder(view);

        return vh;
    }

    @Override
    public void onBindViewHolder(@NonNull SimpleTextAdapter.ViewHolder holder, int position){
        String text = mData.get(position);
        holder.textView1.setText(text);
        Log.d("test_tag", mData.get(position));
    }

    @Override
    public int getItemCount() {
        return mData.size();
    }

}
