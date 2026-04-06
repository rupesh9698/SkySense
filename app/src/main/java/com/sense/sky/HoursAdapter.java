package com.sense.sky;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.recyclerview.widget.RecyclerView;

import com.squareup.picasso.Picasso;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class HoursAdapter extends RecyclerView.Adapter<HoursAdapter.ViewHolder> {

    private final Context context;
    private final ArrayList<HoursModel> hoursModelArrayList;

    public HoursAdapter(Context context, ArrayList<HoursModel> hoursModelArrayList) {
        this.context = context;
        this.hoursModelArrayList = hoursModelArrayList;
    }

    @NonNull
    @Override
    public HoursAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.hours_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HoursAdapter.ViewHolder holder, int position) {
        HoursModel model = hoursModelArrayList.get(position);

        // Parse "yyyy-MM-dd HH:mm" (WeatherAPI uses 24-hr format)
        SimpleDateFormat input  = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
        SimpleDateFormat output = new SimpleDateFormat("h:mm a", Locale.getDefault());
        try {
            Date t = input.parse(model.getTime());
            if (t != null) holder.idACTVTime.setText(output.format(t));
        } catch (ParseException ignored) {}

        if (model.getCurrentTemperatureUnit().equals("fahrenheit")) {
            holder.idACTVTemperature.setText(
                    String.format(Locale.getDefault(), "%.0f °F",
                            parseDouble(model.getFahrenheit())));
        } else {
            holder.idACTVTemperature.setText(
                    String.format(Locale.getDefault(), "%.0f °C",
                            parseDouble(model.getTemperature())));
        }

        Picasso.get().load("https:".concat(model.getIcon())).into(holder.idACIVCondition);
        holder.idACTVHumidity.setText(String.format(Locale.getDefault(), "Humidity: %s%%", model.getHumidity()));
        holder.idACTVWindSpeed.setText(String.format(Locale.getDefault(), "WS: %s km/h", model.getWindSpeed()));
    }

    private double parseDouble(String s) {
        try { return Double.parseDouble(s); } catch (NumberFormatException e) { return 0; }
    }

    @Override
    public int getItemCount() { return hoursModelArrayList.size(); }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final AppCompatTextView  idACTVTime, idACTVTemperature, idACTVHumidity, idACTVWindSpeed;
        private final AppCompatImageView idACIVCondition;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            idACTVTime        = itemView.findViewById(R.id.idACTVTime);
            idACTVTemperature = itemView.findViewById(R.id.idACTVTemperature);
            idACTVHumidity    = itemView.findViewById(R.id.idACTVHumidity);
            idACTVWindSpeed   = itemView.findViewById(R.id.idACTVWindSpeed);
            idACIVCondition   = itemView.findViewById(R.id.idACIVCondition);
        }
    }
}
