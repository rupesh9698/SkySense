package com.sense.sky;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.drawable.ColorDrawable;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.text.HtmlCompat;
import androidx.core.widget.NestedScrollView;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener;
import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    public static String WEATHER_API_KEY = "Enter your weather API key";
    public static String MAPS_API_KEY = "Enter your google maps API key";
    ArrayAdapter<String> adapterItems;
    private ProgressBar idProgressBar;
    private NestedScrollView idNSVHome;
    private AppCompatImageView idACIVIcon;
    private AppCompatTextView idACTVCityName, idACTVTemperature, idACTVUpdatedAt, idACTVCondition, idACTVPressure, idACTVWindSpeed, idACTVHumidity;
    private ArrayList<HoursModel> hoursModelArrayList;
    private ArrayList<DaysModel> daysModelArrayList;
    private HoursAdapter hoursAdapter;
    private DaysAdapter daysAdapter;
    private FusedLocationProviderClient fusedLocationClient;
    private String city, currentTemperatureUnit = "celsius";
    private AutoCompleteTextView idACACTVDropdown;
    private String[] items = {};

    //Check if the network is available or not
    public static boolean isNetworkAvailable(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager != null) {
            NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
            return activeNetworkInfo != null && activeNetworkInfo.isAvailable();
        }
        return false;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Initializing the views
        idProgressBar = findViewById(R.id.idProgressBar);
        idNSVHome = findViewById(R.id.idNSVHome);
        idACIVIcon = findViewById(R.id.idACIVIcon);
        idACACTVDropdown = findViewById(R.id.idACACTVDropdown);
        idACTVCityName = findViewById(R.id.idACTVCityName);
        idACTVTemperature = findViewById(R.id.idACTVTemperature);
        idACTVUpdatedAt = findViewById(R.id.idACTVUpdatedAt);
        idACTVCondition = findViewById(R.id.idACTVCondition);
        idACTVPressure = findViewById(R.id.idACTVPressure);
        idACTVWindSpeed = findViewById(R.id.idACTVWindSpeed);
        idACTVHumidity = findViewById(R.id.idACTVHumidity);
        RecyclerView idRVWeatherHours = findViewById(R.id.idRVWeatherHours);
        RecyclerView idRVWeatherDays = findViewById(R.id.idRVWeatherDays);
        hoursModelArrayList = new ArrayList<>();
        daysModelArrayList = new ArrayList<>();
        hoursAdapter = new HoursAdapter(this, hoursModelArrayList);
        daysAdapter = new DaysAdapter(this, daysModelArrayList);
        idRVWeatherHours.setAdapter(hoursAdapter);
        idRVWeatherDays.setAdapter(daysAdapter);

        //Creating Action Bar
        ActionBar actionBar = getSupportActionBar();
        assert actionBar != null;
        actionBar.setTitle(R.string.app_name);
        actionBar.setDisplayShowHomeEnabled(true);
        actionBar.setTitle(HtmlCompat.fromHtml("<font color='#000000'>SkySense</font>", HtmlCompat.FROM_HTML_MODE_LEGACY));
        actionBar.setBackgroundDrawable(new ColorDrawable(ContextCompat.getColor(MainActivity.this, R.color.colorAccent)));

        //Checking if the network is available or not
        if (!isNetworkAvailable(this)) {
            showInternetErrorPopup();
        }

        //Location service
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        LocationCallback locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                Location location = locationResult.getLastLocation();
                if (location != null) {
                    getCityFromLocation(location.getLatitude(), location.getLongitude());
                }
            }
        };

        // Checking for location permissions
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Permission is not granted, request it
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            // Permission already granted, fetch location
            fetchLocation();
        }

        //Implementation of Google Places
        Places.initialize(getApplicationContext(), MAPS_API_KEY);
        AutocompleteSupportFragment autocompleteFragment = (AutocompleteSupportFragment) getSupportFragmentManager().findFragmentById(R.id.autocomplete_fragment);
        assert autocompleteFragment != null;
        autocompleteFragment.setPlaceFields(Arrays.asList(Place.Field.ID, Place.Field.DISPLAY_NAME));

        //Google place on select listener
        autocompleteFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(@NonNull Place place) {
                city = place.getDisplayName();
                getWeatherInfo(city);
            }

            @Override
            public void onError(@NonNull Status status) {
                Toast.makeText(MainActivity.this, status.getStatusMessage(), Toast.LENGTH_SHORT).show();
            }
        });


        //getting saved cities from shared preferences if present
        SharedPreferences sharedPreferences = getSharedPreferences("Select Favourite", MODE_PRIVATE);
        int itemCount = sharedPreferences.getInt("itemCount", 0);
        if (itemCount > 0) {
            items = new String[itemCount];
            for (int i = 0; i < itemCount; i++) {
                items[i] = sharedPreferences.getString("item_" + i, "");
            }
        }

        //setting favourite cities in items array
        adapterItems = new ArrayAdapter<>(this, R.layout.list_items, items);
        idACACTVDropdown.setAdapter(adapterItems);
        idACACTVDropdown.setFocusable(false);
        idACACTVDropdown.setCursorVisible(false);

        //Click on listener for dropdown
        idACACTVDropdown.setOnItemClickListener((parent, view, position, id) -> {
            String selectedCity = ((AppCompatTextView) view).getText().toString();
            getWeatherInfo(selectedCity);
        });
    }

    //Get Weather Information
    private void getWeatherInfo(String cityName) {

        WeatherCacheManager cacheManager = new WeatherCacheManager(this);
        String cachedData = cacheManager.getWeatherData(cityName);

        //Check if the last hour data for current city is already exists or not
        if (cachedData != null) {
            long currentTimeMillis = System.currentTimeMillis();
            long cachedTimeMillis = cacheManager.getWeatherDataTimestamp(cityName);
            long timeDifferenceMillis = currentTimeMillis - cachedTimeMillis;
            long oneHourInMillis = 60 * 60 * 1000;

            if (timeDifferenceMillis <= oneHourInMillis) {
                parseAndDisplayWeatherData(cachedData);
                return;
            }
        }

        //If not then fetch the data by calling API
        String url = "https://api.weatherapi.com/v1/forecast.json?key=" + WEATHER_API_KEY + "&q=" + cityName + "&days=7";
        RequestQueue requestQueue = Volley.newRequestQueue(MainActivity.this);
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url, null, new Response.Listener<>() {
            @SuppressLint("NotifyDataSetChanged")
            @Override
            public void onResponse(JSONObject response) {
                idProgressBar.setVisibility(View.GONE);
                idNSVHome.setVisibility(View.VISIBLE);
                hoursModelArrayList.clear();
                daysModelArrayList.clear();

                try {
                    idACTVCityName.setText(response.getJSONObject("location").getString("name"));
                    String temperature1 = response.getJSONObject("current").getString("temp_c");
                    String fahrenheit1 = response.getJSONObject("current").getString("temp_f");
                    String updatedAt = response.getJSONObject("current").getString("last_updated");
                    String condition = response.getJSONObject("current").getJSONObject("condition").getString("text");
                    String conditionIcon = response.getJSONObject("current").getJSONObject("condition").getString("icon");
                    String pressure = response.getJSONObject("current").getString("pressure_mb");
                    String windSpeed = response.getJSONObject("current").getString("wind_kph");
                    String humidity1 = response.getJSONObject("current").getString("humidity");

                    SimpleDateFormat input = new SimpleDateFormat("yyyy-MM-dd hh:mm", Locale.getDefault());
                    SimpleDateFormat output = new SimpleDateFormat("EEEE hh:mm aa", Locale.getDefault());

                    try {
                        Date t = input.parse(updatedAt);
                        if (t != null) {
                            idACTVUpdatedAt.setText(output.format(t));
                        }
                    } catch (ParseException ignored) {
                    }

                    if (currentTemperatureUnit.equals("fahrenheit")) {
                        idACTVTemperature.setText(String.format("%s °F", fahrenheit1));
                    } else {
                        idACTVTemperature.setText(String.format("%s °C", temperature1));
                    }
                    Picasso.get().load("https:".concat(conditionIcon)).into(idACIVIcon);
                    idACTVCondition.setText(condition);
                    idACTVPressure.setText(String.format("%s mb", pressure));
                    idACTVWindSpeed.setText(String.format("%s km/h", windSpeed));
                    idACTVHumidity.setText(String.format("%s%%", humidity1));

                    JSONObject forecastObj1 = response.getJSONObject("forecast");
                    JSONObject forecastObj2 = forecastObj1.getJSONArray("forecastday").getJSONObject(0);
                    JSONArray hourArray = forecastObj2.getJSONArray("hour");

                    for (int i = 0; i < hourArray.length(); i++) {
                        JSONObject hourObject = hourArray.getJSONObject(i);
                        String time = hourObject.getString("time");
                        String temperature2 = hourObject.getString("temp_c");
                        String fahrenheit2 = hourObject.getString("temp_f");
                        String image1 = hourObject.getJSONObject("condition").getString("icon");
                        String humidity2 = hourObject.getString("humidity");
                        String windSpeed2 = hourObject.getString("wind_kph");
                        hoursModelArrayList.add(new HoursModel(time, temperature2, fahrenheit2, currentTemperatureUnit, image1, humidity2, windSpeed2));
                    }
                    hoursAdapter.notifyDataSetChanged();

                    JSONArray dayArray = forecastObj1.getJSONArray("forecastday");

                    for (int i = 0; i < dayArray.length(); i++) {

                        JSONObject dayObject = dayArray.getJSONObject(i).getJSONObject("day");
                        String date = dayArray.getJSONObject(i).getString("date");
                        String minTemperature = dayObject.getString("mintemp_c");
                        String minFahrenheit = dayObject.getString("mintemp_f");
                        String maxTemperature = dayObject.getString("maxtemp_c");
                        String maxFahrenheit = dayObject.getString("maxtemp_f");
                        String image2 = dayObject.getJSONObject("condition").getString("icon");
                        String humidity3 = dayObject.getString("avghumidity");
                        String windSpeed3 = dayObject.getString("maxwind_kph");
                        daysModelArrayList.add(new DaysModel(date, minTemperature, minFahrenheit, maxTemperature, maxFahrenheit, currentTemperatureUnit, image2, humidity3, windSpeed3));
                    }
                    daysAdapter.notifyDataSetChanged();
                    long currentTimestamp = System.currentTimeMillis();
                    cacheManager.saveWeatherData(cityName, response.toString(), currentTimestamp);

                } catch (JSONException e) {
                    throw new RuntimeException(e);
                }
            }
        }, error -> Toast.makeText(MainActivity.this, "Please Enter Valid City Name", Toast.LENGTH_SHORT).show());
        requestQueue.add(jsonObjectRequest);

    }

    // Callback for permission request result
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, fetch location
                fetchLocation();
            } else {
                Toast.makeText(this, "Location permission is required to fetch city.", Toast.LENGTH_SHORT).show();
                System.exit(0);
            }
        }
    }

    // Fetch the location and city
    private void fetchLocation() {
        // Check again for location permissions
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            // Fetching the location
            fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
                if (location != null) {
                    getCityFromLocation(location.getLatitude(), location.getLongitude());
                } else {
                    Toast.makeText(MainActivity.this, "Location not available.", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            Toast.makeText(this, "Location permission is required to fetch city.", Toast.LENGTH_SHORT).show();
        }
    }

    //Get City Location after giving location permission and fetch data
    private void getCityFromLocation(double latitude, double longitude) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);
            if (addresses != null && !addresses.isEmpty()) {
                city = addresses.get(0).getLocality();
                getWeatherInfo(city);
            }
        } catch (IOException ignored) {
        }
    }

    //Give popup if network is not available
    private void showInternetErrorPopup() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Internet Connection Error");
        builder.setMessage("Please check your internet connection and try again.");
        builder.setPositiveButton("OK", (dialog, which) -> {
            dialog.dismiss();
            MainActivity.this.finish();
        });
        builder.setCancelable(false);
        builder.show();
    }

    //Fetch data from shared preference if user open app within 1 hour
    @SuppressLint("NotifyDataSetChanged")
    private void parseAndDisplayWeatherData(String data) {
        try {
            JSONObject response = new JSONObject(data);
            idProgressBar.setVisibility(View.GONE);
            idNSVHome.setVisibility(View.VISIBLE);
            hoursModelArrayList.clear();
            daysModelArrayList.clear();

            idACTVCityName.setText(response.getJSONObject("location").getString("name"));
            String temperature1 = response.getJSONObject("current").getString("temp_c");
            String fahrenheit1 = response.getJSONObject("current").getString("temp_f");
            String updatedAt = response.getJSONObject("current").getString("last_updated");
            String condition = response.getJSONObject("current").getJSONObject("condition").getString("text");
            String conditionIcon = response.getJSONObject("current").getJSONObject("condition").getString("icon");
            String pressure = response.getJSONObject("current").getString("pressure_mb");
            String windSpeed = response.getJSONObject("current").getString("wind_kph");
            String humidity1 = response.getJSONObject("current").getString("humidity");

            SimpleDateFormat input = new SimpleDateFormat("yyyy-MM-dd hh:mm", Locale.getDefault());
            SimpleDateFormat output = new SimpleDateFormat("EEEE hh:mm aa", Locale.getDefault());

            try {
                Date t = input.parse(updatedAt);
                if (t != null) {
                    idACTVUpdatedAt.setText(output.format(t));
                }
            } catch (ParseException ignored) {
            }

            if (currentTemperatureUnit.equals("fahrenheit")) {
                idACTVTemperature.setText(String.format("%s °F", fahrenheit1));
            } else {
                idACTVTemperature.setText(String.format("%s °C", temperature1));
            }
            Picasso.get().load("https:".concat(conditionIcon)).into(idACIVIcon);
            idACTVCondition.setText(condition);
            idACTVPressure.setText(String.format("%s mb", pressure));
            idACTVWindSpeed.setText(String.format("%s km/h", windSpeed));
            idACTVHumidity.setText(String.format("%s%%", humidity1));

            JSONObject forecastObj1 = response.getJSONObject("forecast");
            JSONObject forecastObj2 = forecastObj1.getJSONArray("forecastday").getJSONObject(0);
            JSONArray hourArray = forecastObj2.getJSONArray("hour");

            for (int i = 0; i < hourArray.length(); i++) {
                JSONObject hourObject = hourArray.getJSONObject(i);
                String time = hourObject.getString("time");
                String temperature2 = hourObject.getString("temp_c");
                String fahrenheit2 = hourObject.getString("temp_f");
                String image1 = hourObject.getJSONObject("condition").getString("icon");
                String humidity2 = hourObject.getString("humidity");
                String windSpeed2 = hourObject.getString("wind_kph");
                hoursModelArrayList.add(new HoursModel(time, temperature2, fahrenheit2, currentTemperatureUnit, image1, humidity2, windSpeed2));
            }
            hoursAdapter.notifyDataSetChanged();

            JSONArray dayArray = forecastObj1.getJSONArray("forecastday");

            for (int i = 0; i < dayArray.length(); i++) {

                JSONObject dayObject = dayArray.getJSONObject(i).getJSONObject("day");
                String date = dayArray.getJSONObject(i).getString("date");
                String minTemperature = dayObject.getString("mintemp_c");
                String minFahrenheit = dayObject.getString("mintemp_f");
                String maxTemperature = dayObject.getString("maxtemp_c");
                String maxFahrenheit = dayObject.getString("maxtemp_f");
                String image2 = dayObject.getJSONObject("condition").getString("icon");
                String humidity3 = dayObject.getString("avghumidity");
                String windSpeed3 = dayObject.getString("maxwind_kph");
                daysModelArrayList.add(new DaysModel(date, minTemperature, minFahrenheit, maxTemperature, maxFahrenheit, currentTemperatureUnit, image2, humidity3, windSpeed3));
            }
            daysAdapter.notifyDataSetChanged();
        } catch (JSONException ignored) {
        }
    }

    //create and show Action bar buttons
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_actionbar, menu);

        menu.findItem(R.id.addToFavourite).setVisible(true);
        menu.findItem(R.id.temperature_change).setVisible(true);
        return super.onCreateOptionsMenu(menu);
    }

    //On click listener for action bar buttons
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

        int id = item.getItemId();

        //Change the temperature unit
        if (id == R.id.temperature_change) {
            if (currentTemperatureUnit.equals("celsius")) {
                currentTemperatureUnit = "fahrenheit";
                item.setIcon(R.drawable.celsius);
            } else {
                currentTemperatureUnit = "celsius";
                item.setIcon(R.drawable.fahrenheit);
            }
            getWeatherInfo(city);
            Toast.makeText(this, "Changed to " + currentTemperatureUnit, Toast.LENGTH_SHORT).show();
        }

        //Add city to favourite if not present in list
        if (id == R.id.addToFavourite) {

            if (isItemPresent(city)) {
                Toast.makeText(this, "Already Present", Toast.LENGTH_SHORT).show();
            } else {
                String[] addItem = new String[items.length + 1];
                System.arraycopy(items, 0, addItem, 0, items.length);
                addItem[items.length] = city;
                items = addItem;

                SharedPreferences sharedPreferences = getSharedPreferences("Select Favourite", MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putInt("itemCount", items.length);
                for (int i = 0; i < items.length; i++) {
                    editor.putString("item_" + i, items[i]);
                }
                editor.apply();

                adapterItems = new ArrayAdapter<>(this, R.layout.list_items, items);
                idACACTVDropdown.setAdapter(adapterItems);

                Toast.makeText(this, "Added to Favourites", Toast.LENGTH_SHORT).show();
            }
        }

        return super.onOptionsItemSelected(item);
    }

    //If item is already present in favourite list
    private boolean isItemPresent(String targetItem) {
        for (String item : items) {
            if (item.equals(targetItem)) {
                return true;
            }
        }
        return false;
    }
}