package com.sense.sky.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Root response model for WeatherAPI /v1/forecast.json
 * Mapped automatically by Gson / Retrofit converter.
 */
public class WeatherResponse {

    @SerializedName("location")
    public Location location;

    @SerializedName("current")
    public Current current;

    @SerializedName("forecast")
    public Forecast forecast;

    // ── Location ─────────────────────────────────────────────────────────────
    public static class Location {

        @SerializedName("name")
        public String name;
    }

    // ── Current conditions ────────────────────────────────────────────────────
    public static class Current {

        @SerializedName("temp_c")
        public double tempC;

        @SerializedName("temp_f")
        public double tempF;

        @SerializedName("feelslike_c")
        public double feelslikeC;

        @SerializedName("feelslike_f")
        public double feelslikeF;

        @SerializedName("last_updated")
        public String lastUpdated;

        @SerializedName("condition")
        public Condition condition;

        @SerializedName("pressure_mb")
        public double pressureMb;

        @SerializedName("wind_kph")
        public double windKph;

        @SerializedName("humidity")
        public int humidity;

        @SerializedName("uv")
        public double uv;

        @SerializedName("vis_km")
        public double visKm;
    }

    // ── Shared condition ─────────────────────────────────────────────────────
    public static class Condition {

        @SerializedName("text")
        public String text;

        @SerializedName("icon")
        public String icon;
    }

    // ── Forecast ─────────────────────────────────────────────────────────────
    public static class Forecast {

        @SerializedName("forecastday")
        public List<ForecastDay> forecastday;
    }

    public static class ForecastDay {

        @SerializedName("date")
        public String date;

        @SerializedName("hour")
        public List<Hour> hour;

        @SerializedName("day")
        public Day day;
    }

    // ── Hourly slot ──────────────────────────────────────────────────────────
    public static class Hour {

        @SerializedName("time")
        public String time;

        @SerializedName("temp_c")
        public double tempC;

        @SerializedName("temp_f")
        public double tempF;

        @SerializedName("condition")
        public Condition condition;

        @SerializedName("humidity")
        public int humidity;

        @SerializedName("wind_kph")
        public double windKph;
    }

    // ── Daily summary ────────────────────────────────────────────────────────
    public static class Day {

        @SerializedName("mintemp_c")
        public double mintempC;

        @SerializedName("mintemp_f")
        public double mintempF;

        @SerializedName("maxtemp_c")
        public double maxtempC;

        @SerializedName("maxtemp_f")
        public double maxtempF;

        @SerializedName("avghumidity")
        public double avghumidity;

        @SerializedName("maxwind_kph")
        public double maxwindKph;

        @SerializedName("condition")
        public Condition condition;
    }
}
