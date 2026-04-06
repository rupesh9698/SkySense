package com.sense.sky.api;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Singleton Retrofit client for WeatherAPI.
 * Includes OkHttp with configurable timeouts and optional debug logging.
 */
public class WeatherApiClient {

    private static final String BASE_URL = "https://api.weatherapi.com/";

    private static volatile WeatherApiService instance;

    private WeatherApiClient() {}

    /**
     * Returns the shared {@link WeatherApiService} instance.
     *
     * @param debug When {@code true}, attaches an HTTP logging interceptor
     *              (logs request method, URL, and response code to Logcat).
     */
    public static WeatherApiService getInstance(boolean debug) {
        if (instance == null) {
            synchronized (WeatherApiClient.class) {
                if (instance == null) {
                    OkHttpClient.Builder httpBuilder = new OkHttpClient.Builder()
                            .connectTimeout(15, TimeUnit.SECONDS)
                            .readTimeout(20, TimeUnit.SECONDS)
                            .writeTimeout(15, TimeUnit.SECONDS);

                    if (debug) {
                        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
                        logging.setLevel(HttpLoggingInterceptor.Level.BASIC);
                        httpBuilder.addInterceptor(logging);
                    }

                    instance = new Retrofit.Builder()
                            .baseUrl(BASE_URL)
                            .client(httpBuilder.build())
                            .addConverterFactory(GsonConverterFactory.create())
                            .build()
                            .create(WeatherApiService.class);
                }
            }
        }
        return instance;
    }

    /** Clears the cached instance (useful for tests or re-initialisation). */
    public static void reset() {
        instance = null;
    }
}
