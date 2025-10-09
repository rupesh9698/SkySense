package com.sense.sky;

import android.content.Context;
import android.content.SharedPreferences;

public class WeatherCacheManager {

    private static final String PREF_NAME = "WeatherCache";
    private static final String KEY_WEATHER_DATA = "weatherData";
    private static final String KEY_TIMESTAMP = "timestamp";
    private final SharedPreferences preferences;

    public WeatherCacheManager(Context context) {
        preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    // key is typically "lat,lon" rounded to 4 decimals
    public void saveWeatherData(String key, String weatherData, long timestamp) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(KEY_WEATHER_DATA + "_" + key, weatherData);
        editor.putLong(KEY_TIMESTAMP + "_" + key, timestamp);
        editor.apply();
    }

    public String getWeatherData(String key) {
        return preferences.getString(KEY_WEATHER_DATA + "_" + key, null);
    }

    public long getWeatherDataTimestamp(String key) {
        return preferences.getLong(KEY_TIMESTAMP + "_" + key, 0);
    }
}