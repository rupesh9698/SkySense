package com.sense.sky.api;

import com.sense.sky.model.WeatherResponse;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

/** Retrofit interface for WeatherAPI endpoints. */
public interface WeatherApiService {

    /**
     * Fetch current + forecast data.
     *
     * @param apiKey  Your WeatherAPI key.
     * @param query   "lat,lon" string, city name, or zip code.
     * @param days    Number of forecast days (1–10).
     * @param aqi     "yes" or "no" — air quality data.
     * @param alerts  "yes" or "no" — weather alerts.
     */
    @GET("v1/forecast.json")
    Call<WeatherResponse> getForecast(
            @Query("key")    String apiKey,
            @Query("q")      String query,
            @Query("days")   int    days,
            @Query("aqi")    String aqi,
            @Query("alerts") String alerts
    );
}
