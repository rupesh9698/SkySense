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

    public void saveWeatherData(String cityName, String weatherData, long timestamp) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(KEY_WEATHER_DATA + "_" + cityName, weatherData);
        editor.putLong(KEY_TIMESTAMP + "_" + cityName, timestamp);
        editor.apply();
    }

    public String getWeatherData(String cityName) {
        return preferences.getString(KEY_WEATHER_DATA + "_" + cityName, null);
    }

    public long getWeatherDataTimestamp(String cityName) {
        return preferences.getLong(KEY_TIMESTAMP + "_" + cityName, 0);
    }
}
