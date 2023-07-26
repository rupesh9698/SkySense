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

public class DaysAdapter extends RecyclerView.Adapter<DaysAdapter.ViewHolder> {

    private Context context;
    private ArrayList<DaysModel> daysModelArrayList;

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

        SimpleDateFormat input = new SimpleDateFormat("yyyy-MM-dd");
        SimpleDateFormat output = new SimpleDateFormat("dd-MM-yyyy");

        try {
            Date t = input.parse(model.getDate());
            holder.idACTVDay.setText(output.format(t));
        }catch (ParseException e) {
            e.printStackTrace();
        }

        if (model.getCurrentTemperatureUnit().equals("fahrenheit")) {
            holder.idACTVTemperature.setText(model.getMinFahrenheit() + "–" + model.getMaxFahrenheit() + " °F");
        }
        else {
            holder.idACTVTemperature.setText(model.getMin_temperature() + "–" + model.getMax_temperature() + " °C");
        }

        Picasso.get().load("https:".concat(model.getIcon())).into(holder.idACIVCondition);
        holder.idACTVHumidity.setText("Humidity: " + model.getHumidity() + "%");
        holder.idACTVWindSpeed.setText("WS: " + model.getWindSpeed() + " km/h");
    }

    @Override
    public int getItemCount() {
        return daysModelArrayList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        private AppCompatTextView idACTVDay, idACTVTemperature, idACTVHumidity, idACTVWindSpeed;
        private AppCompatImageView idACIVCondition;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            idACTVDay = itemView.findViewById(R.id.idACTVDay);
            idACTVTemperature = itemView.findViewById(R.id.idACTVTemperature);
            idACTVHumidity = itemView.findViewById(R.id.idACTVHumidity);
            idACTVWindSpeed = itemView.findViewById(R.id.idACTVWindSpeed);
            idACIVCondition = itemView.findViewById(R.id.idACIVCondition);
        }
    }
}
