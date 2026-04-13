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

public class DaysAdapter extends RecyclerView.Adapter<DaysAdapter.ViewHolder> {

    private final Context context;
    private final ArrayList<DaysModel> daysModelArrayList;

    public DaysAdapter(Context context, ArrayList<DaysModel> daysModelArrayList) {
        this.context = context;
        this.daysModelArrayList = daysModelArrayList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.days_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DaysModel model = daysModelArrayList.get(position);

        SimpleDateFormat input  = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        SimpleDateFormat output = new SimpleDateFormat("EEE, MMM dd", Locale.getDefault());
        try {
            Date t = input.parse(model.date());
            if (t != null) holder.idACTVDay.setText(output.format(t));
        } catch (ParseException ignored) {}

        if (model.currentTemperatureUnit().equals("fahrenheit")) {
            holder.idACTVTemperature.setText(
                    String.format(Locale.getDefault(), "%.0f–%.0f °F",
                            parseDouble(model.minFahrenheit()),
                            parseDouble(model.maxFahrenheit())));
        } else {
            holder.idACTVTemperature.setText(
                    String.format(Locale.getDefault(), "%.0f–%.0f °C",
                            parseDouble(model.min_temperature()),
                            parseDouble(model.max_temperature())));
        }

        Picasso.get().load("https:".concat(model.icon())).into(holder.idACIVCondition);
        holder.idACTVHumidity.setText(String.format(Locale.getDefault(), "Humidity: %s%%", model.humidity()));
        holder.idACTVWindSpeed.setText(String.format(Locale.getDefault(), "WS: %s km/h", model.windSpeed()));
    }

    private double parseDouble(String s) {
        try { return Double.parseDouble(s); } catch (NumberFormatException e) { return 0; }
    }

    @Override
    public int getItemCount() { return daysModelArrayList.size(); }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final AppCompatTextView idACTVDay, idACTVTemperature, idACTVHumidity, idACTVWindSpeed;
        private final AppCompatImageView idACIVCondition;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            idACTVDay         = itemView.findViewById(R.id.idACTVDay);
            idACTVTemperature = itemView.findViewById(R.id.idACTVTemperature);
            idACTVHumidity    = itemView.findViewById(R.id.idACTVHumidity);
            idACTVWindSpeed   = itemView.findViewById(R.id.idACTVWindSpeed);
            idACIVCondition   = itemView.findViewById(R.id.idACIVCondition);
        }
    }
}
