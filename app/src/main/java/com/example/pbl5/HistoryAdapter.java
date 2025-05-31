package com.example.pbl5;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import java.util.ArrayList;

public class HistoryAdapter extends BaseAdapter {
    private Context context;
    private ArrayList<String> historyList;
    private LayoutInflater inflater;

    public HistoryAdapter(Context context, ArrayList<String> historyList) {
        this.context = context;
        this.historyList = historyList;
        this.inflater = LayoutInflater.from(context);
    }

    @Override
    public int getCount() {
        return historyList.size();
    }

    @Override
    public Object getItem(int position) {
        return historyList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;

        if (convertView == null) {
            convertView = inflater.inflate(R.layout.history_item, parent, false);
            holder = new ViewHolder();
            holder.tvMessage = convertView.findViewById(R.id.tv_message);
            holder.tvTimestamp = convertView.findViewById(R.id.tv_timestamp);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        String fullMessage = historyList.get(position);

        // Parse timestamp và message
        if (fullMessage.contains(": 🔸 ")) {
            String[] parts = fullMessage.split(": 🔸 ", 2);
            if (parts.length == 2) {
                holder.tvTimestamp.setText(parts[0]);
                holder.tvMessage.setText(parts[1]);
            } else {
                holder.tvTimestamp.setText("--:--:--");
                holder.tvMessage.setText(fullMessage);
            }
        } else {
            holder.tvTimestamp.setText("--:--:--");
            holder.tvMessage.setText(fullMessage);
        }

        return convertView;
    }

    private static class ViewHolder {
        TextView tvMessage;
        TextView tvTimestamp;
    }
}