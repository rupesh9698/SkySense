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

public class HoursAdapter extends RecyclerView.Adapter<HoursAdapter.ViewHolder> {

    private Context context;
    private ArrayList<HoursModel> hoursModelArrayList;

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

        SimpleDateFormat input = new SimpleDateFormat("yyyy-MM-dd hh:mm");
        SimpleDateFormat output = new SimpleDateFormat("hh:mm aa");

        try {
            Date t = input.parse(model.getTime());
            holder.idACTVTime.setText(output.format(t));
        }catch (ParseException e) {
            e.printStackTrace();
        }

        if (model.getCurrentTemperatureUnit().equals("fahrenheit")) {
            holder.idACTVTemperature.setText(model.getFahrenheit() + " °F");
        }
        else {
            holder.idACTVTemperature.setText(model.getTemperature() + " °C");
        }

        Picasso.get().load("https:".concat(model.getIcon())).into(holder.idACIVCondition);
        holder.idACTVHumidity.setText("Humidity: " + model.getHumidity() + "%");
        holder.idACTVWindSpeed.setText("WS: " + model.getWindSpeed() + " km/h");
    }

    @Override
    public int getItemCount() {
        return hoursModelArrayList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        private AppCompatTextView idACTVTime, idACTVTemperature, idACTVHumidity, idACTVWindSpeed;
        private AppCompatImageView idACIVCondition;
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
