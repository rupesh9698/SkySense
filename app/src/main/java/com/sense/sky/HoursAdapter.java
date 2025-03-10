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

        SimpleDateFormat input = new SimpleDateFormat("yyyy-MM-dd hh:mm", Locale.getDefault());
        SimpleDateFormat output = new SimpleDateFormat("hh:mm aa", Locale.getDefault());

        try {
            Date t = input.parse(model.getTime());
            if (t != null) {
                holder.idACTVTime.setText(output.format(t));
            }
        } catch (ParseException ignored) {
        }

        if (model.getCurrentTemperatureUnit().equals("fahrenheit")) {
            holder.idACTVTemperature.setText(String.format("%s °F", model.getFahrenheit()));
        }
        else {
            holder.idACTVTemperature.setText(String.format("%s °C", model.getTemperature()));
        }

        Picasso.get().load("https:".concat(model.getIcon())).into(holder.idACIVCondition);
        holder.idACTVHumidity.setText(String.format("Humidity: %s%%", model.getHumidity()));
        holder.idACTVWindSpeed.setText(String.format("WS: %s km/h", model.getWindSpeed()));
    }

    @Override
    public int getItemCount() {
        return hoursModelArrayList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        private final AppCompatTextView idACTVTime, idACTVTemperature, idACTVHumidity, idACTVWindSpeed;
        private final AppCompatImageView idACIVCondition;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            idACTVTime = itemView.findViewById(R.id.idACTVTime);
            idACTVTemperature = itemView.findViewById(R.id.idACTVTemperature);
            idACTVHumidity = itemView.findViewById(R.id.idACTVHumidity);
            idACTVWindSpeed = itemView.findViewById(R.id.idACTVWindSpeed);
            idACIVCondition = itemView.findViewById(R.id.idACIVCondition);
        }
    }
}
