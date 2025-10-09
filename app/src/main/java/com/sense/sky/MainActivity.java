package com.sense.sky;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.drawable.ColorDrawable;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
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

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.location.CurrentLocationRequest;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.AutocompletePrediction;
import com.google.android.libraries.places.api.model.AutocompleteSessionToken;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.net.FetchPlaceRequest;
import com.google.android.libraries.places.api.net.FetchPlaceResponse;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.libraries.places.widget.PlaceAutocomplete;
import com.google.android.libraries.places.widget.PlaceAutocompleteActivity;
import com.google.android.libraries.places.widget.model.AutocompleteUiCustomization;
import com.squareup.picasso.Picasso;
import com.google.android.gms.tasks.Task;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    public static String WEATHER_API_KEY = "899d0c72c29848e6ba4180227232407";
    public static String MAPS_API_KEY = "AIzaSyC0A4XxYH_KPCZ_7XiPegM2I9sGUSMJ5wM";
    private static final long CACHE_TTL_MILLIS = 60 * 60 * 1000L;

    private ProgressBar idProgressBar;
    private NestedScrollView idNSVHome;
    private AppCompatImageView idACIVIcon;
    private AppCompatTextView idACTVCityName, idACTVTemperature, idACTVUpdatedAt, idACTVCondition, idACTVPressure, idACTVWindSpeed, idACTVHumidity;

    private ArrayList<HoursModel> hoursModelArrayList;
    private ArrayList<DaysModel> daysModelArrayList;
    private HoursAdapter hoursAdapter;
    private DaysAdapter daysAdapter;

    private FusedLocationProviderClient fusedLocationClient;
    private AutoCompleteTextView idACACTVDropdown;
    private ArrayAdapter<String> adapterItems;

    private String[] items = {};
    private String currentTemperatureUnit = "celsius";

    // Last selection / state
    private Double lastLat = null, lastLon = null;
    private String lastResponseJson = null; // for unit toggle re-render
    private String lastDisplayName = "";    // from Places or API response

    // Places
    private PlacesClient placesClient;
    private ActivityResultLauncher<Intent> placeAutocompleteLauncher;
    private AutocompleteSessionToken autocompleteSessionToken;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Views
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

        // Action bar
        ActionBar actionBar = getSupportActionBar();
        assert actionBar != null;
        actionBar.setTitle(R.string.app_name);
        actionBar.setDisplayShowHomeEnabled(true);
        actionBar.setTitle(HtmlCompat.fromHtml("<font color='#000000'>SkySense</font>", HtmlCompat.FROM_HTML_MODE_LEGACY));
        actionBar.setBackgroundDrawable(new ColorDrawable(ContextCompat.getColor(MainActivity.this, R.color.colorAccent)));

        // Show offline dialog if no internet
        if (!hasInternet(this)) {
            showInternetErrorPopup();
        }

        // Location client
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Places init (New API in v5)
        if (!Places.isInitialized()) {
            Places.initializeWithNewPlacesApiEnabled(getApplicationContext(), MAPS_API_KEY);
        }
        placesClient = Places.createClient(this);
        autocompleteSessionToken = AutocompleteSessionToken.newInstance();

        // Register result launcher for new Place Autocomplete widget
        placeAutocompleteLauncher =
                registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                        new ActivityResultCallback<ActivityResult>() {
                            @Override
                            public void onActivityResult(ActivityResult result) {
                                Intent intent = result.getData();
                                if (result.getResultCode() == PlaceAutocompleteActivity.RESULT_OK && intent != null) {
                                    AutocompletePrediction prediction = PlaceAutocomplete.getPredictionFromIntent(intent);
                                    AutocompleteSessionToken sessionTokenFromIntent = PlaceAutocomplete.getSessionTokenFromIntent(intent);
                                    if (sessionTokenFromIntent != null) {
                                        autocompleteSessionToken = sessionTokenFromIntent;
                                    }
                                    fetchPlaceAndLoadWeather(prediction.getPlaceId());
                                } else if (intent != null) {
                                    // Optional: read status for diagnostics
                                    // Status status = PlaceAutocomplete.getResultStatusFromIntent(intent);
                                }
                            }
                        });

        // Make the old container clickable to launch the new widget (update XML as below)
        View searchContainer = findViewById(R.id.autocomplete_fragment);
        if (searchContainer != null) {
            searchContainer.setOnClickListener(v -> launchPlaceAutocomplete());
        }

        // Load favourites (names)
        SharedPreferences sharedPreferences = getSharedPreferences("Select Favourite", MODE_PRIVATE);
        int itemCount = sharedPreferences.getInt("itemCount", 0);
        if (itemCount > 0) {
            items = new String[itemCount];
            for (int i = 0; i < itemCount; i++) {
                items[i] = sharedPreferences.getString("item_" + i, "");
            }
        }
        adapterItems = new ArrayAdapter<>(this, R.layout.list_items, items);
        idACACTVDropdown.setAdapter(adapterItems);
        idACACTVDropdown.setFocusable(false);
        idACACTVDropdown.setCursorVisible(false);

        // Favourite selection -> resolve coords from separate store
        idACACTVDropdown.setOnItemClickListener((parent, view, position, id) -> {
            String selectedName = ((AppCompatTextView) view).getText().toString();
            SharedPreferences favCoords = getSharedPreferences("Fav_Locations", MODE_PRIVATE);
            String coords = favCoords.getString("coords_" + selectedName, null);
            if (coords != null) {
                String[] parts = coords.split(",");
                if (parts.length == 2) {
                    try {
                        double lat = Double.parseDouble(parts[0]);
                        double lon = Double.parseDouble(parts[1]);
                        lastDisplayName = selectedName;
                        getWeatherInfo(lat, lon, selectedName);
                    } catch (NumberFormatException e) {
                        Toast.makeText(MainActivity.this, "Invalid saved coordinates.", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(MainActivity.this, "Saved coordinates missing.", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(MainActivity.this, "Coordinates not found for favourite.", Toast.LENGTH_SHORT).show();
            }
        });

        // Permissions and initial fetch
        requestLocationAndFetch();
    }

    // Launch the new Place Autocomplete widget via intent
    private void launchPlaceAutocomplete() {
        Intent autocompleteIntent = new PlaceAutocomplete.IntentBuilder()
                // Optional: set filters, origin, countries, theme, etc.
                // .setCountries(List.of("IN"))
                // .setOrigin(new LatLng(18.5204, 73.8567))
                // .setSessionToken(autocompleteSessionToken) // optional; widget will create one if not provided
                .setAutocompleteUiCustomization(
                        AutocompleteUiCustomization.builder().build()
                )
                .build(this);
        placeAutocompleteLauncher.launch(autocompleteIntent);
    }

    // After user picks a prediction, fetch full Place to get DISPLAY_NAME and LOCATION
    private void fetchPlaceAndLoadWeather(String placeId) {
        List<Place.Field> fields = Arrays.asList(
                Place.Field.ID,
                Place.Field.DISPLAY_NAME,
                Place.Field.LOCATION
        );
        FetchPlaceRequest request = FetchPlaceRequest.newInstance(placeId, fields);
        // If desired, associate the same session token
        // request = FetchPlaceRequest.builder(placeId, fields).setSessionToken(autocompleteSessionToken).build();
        Task<FetchPlaceResponse> task = placesClient.fetchPlace(request);
        task.addOnSuccessListener(response -> {
            Place p = response.getPlace();
            LatLng ll = p.getLocation();
            CharSequence name = p.getDisplayName();
            if (ll != null) {
                lastDisplayName = name != null ? name.toString() : "";
                getWeatherInfo(ll.latitude, ll.longitude, lastDisplayName);
            } else {
                Toast.makeText(this, "No coordinates for selected place.", Toast.LENGTH_SHORT).show();
            }
        }).addOnFailureListener(e ->
                Toast.makeText(this, "Place fetch failed.", Toast.LENGTH_SHORT).show()
        );
    }

    // Modern connectivity check with NetworkCapabilities
    public static boolean hasInternet(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;
        Network active = cm.getActiveNetwork();
        if (active == null) return false;
        NetworkCapabilities caps = cm.getNetworkCapabilities(active);
        return caps != null && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
    }

    private void requestLocationAndFetch() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            fetchLocation();
        }
    }

    // Use getCurrentLocation with fallback to getLastLocation
    @SuppressLint("MissingPermission")
    private void fetchLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

            CurrentLocationRequest req = new CurrentLocationRequest.Builder()
                    .setPriority(Priority.PRIORITY_BALANCED_POWER_ACCURACY)
                    .setMaxUpdateAgeMillis(0)
                    .build();

            fusedLocationClient.getCurrentLocation(req, null).addOnSuccessListener(this, location -> {
                if (location != null) {
                    lastDisplayName = "Current location";
                    getWeatherInfo(location.getLatitude(), location.getLongitude(), lastDisplayName);
                } else {
                    fusedLocationClient.getLastLocation().addOnSuccessListener(this, last -> {
                        if (last != null) {
                            lastDisplayName = "Current location";
                            getWeatherInfo(last.getLatitude(), last.getLongitude(), lastDisplayName);
                        } else {
                            Toast.makeText(MainActivity.this, "Location not available.", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }).addOnFailureListener(e ->
                    Toast.makeText(MainActivity.this, "Failed to get location.", Toast.LENGTH_SHORT).show()
            );
        } else {
            Toast.makeText(this, "Location permission is required.", Toast.LENGTH_SHORT).show();
        }
    }

    // Coordinate cache key rounded to 4 decimals (~11 m)
    private String buildCacheKey(double lat, double lon) {
        return String.format(Locale.US, "%.4f,%.4f", lat, lon);
    }

    private String buildWeatherUrl(double lat, double lon) {
        return "https://api.weatherapi.com/v1/forecast.json?key=" + WEATHER_API_KEY
                + "&q=" + lat + "," + lon
                + "&days=7&aqi=no&alerts=no";
    }

    private void getWeatherInfo(double lat, double lon, String displayName) {
        lastLat = lat;
        lastLon = lon;

        idProgressBar.setVisibility(View.VISIBLE);
        idNSVHome.setVisibility(View.GONE);

        String cacheKey = buildCacheKey(lat, lon);
        WeatherCacheManager cacheManager = new WeatherCacheManager(this);
        String cachedData = cacheManager.getWeatherData(cacheKey);
        long cachedTime = cacheManager.getWeatherDataTimestamp(cacheKey);
        long now = System.currentTimeMillis();
        boolean cacheFresh = (now - cachedTime) <= CACHE_TTL_MILLIS;

        // Offline: render cache if present
        if (!hasInternet(this) && cachedData != null) {
            lastResponseJson = cachedData;
            parseAndDisplayWeatherData(cachedData);
            return;
        }

        // Fresh cache: render and return
        if (cachedData != null && cacheFresh) {
            lastResponseJson = cachedData;
            parseAndDisplayWeatherData(cachedData);
            return;
        }

        // Fetch from WeatherAPI using lat,lon
        String url = buildWeatherUrl(lat, lon);
        RequestQueue requestQueue = Volley.newRequestQueue(MainActivity.this);
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    idProgressBar.setVisibility(View.GONE);
                    idNSVHome.setVisibility(View.VISIBLE);
                    hoursModelArrayList.clear();
                    daysModelArrayList.clear();
                    try {
                        String apiName = response.getJSONObject("location").optString("name", displayName);
                        lastDisplayName = apiName;
                        idACTVCityName.setText(apiName);

                        bindCurrentSection(response);
                        bindHourlySection(response);
                        bindDailySection(response);

                        long ts = System.currentTimeMillis();
                        cacheManager.saveWeatherData(cacheKey, response.toString(), ts);
                        lastResponseJson = response.toString();
                    } catch (JSONException e) {
                        Toast.makeText(MainActivity.this, "Parse error.", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    idProgressBar.setVisibility(View.GONE);
                    if (cachedData != null) {
                        lastResponseJson = cachedData;
                        parseAndDisplayWeatherData(cachedData);
                        Toast.makeText(MainActivity.this, "Showing cached data.", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(MainActivity.this, "Unable to load weather data.", Toast.LENGTH_SHORT).show();
                    }
                });

        jsonObjectRequest.setShouldCache(true);
        jsonObjectRequest.setRetryPolicy(new DefaultRetryPolicy(
                10_000,
                1,
                1.0f
        ));
        requestQueue.add(jsonObjectRequest);
    }

    @SuppressLint("NotifyDataSetChanged")
    private void bindCurrentSection(JSONObject response) throws JSONException {
        String temperatureC = response.getJSONObject("current").getString("temp_c");
        String temperatureF = response.getJSONObject("current").getString("temp_f");
        String updatedAt = response.getJSONObject("current").getString("last_updated");
        String condition = response.getJSONObject("current").getJSONObject("condition").getString("text");
        String conditionIcon = response.getJSONObject("current").getJSONObject("condition").getString("icon");
        String pressure = response.getJSONObject("current").getString("pressure_mb");
        String windSpeed = response.getJSONObject("current").getString("wind_kph");
        String humidity = response.getJSONObject("current").getString("humidity");

        SimpleDateFormat input = new SimpleDateFormat("yyyy-MM-dd hh:mm", Locale.getDefault());
        SimpleDateFormat output = new SimpleDateFormat("EEEE hh:mm aa", Locale.getDefault());
        try {
            Date t = input.parse(updatedAt);
            if (t != null) idACTVUpdatedAt.setText(output.format(t));
        } catch (ParseException ignored) { }

        if (currentTemperatureUnit.equals("fahrenheit")) {
            idACTVTemperature.setText(String.format("%s °F", temperatureF));
        } else {
            idACTVTemperature.setText(String.format("%s °C", temperatureC));
        }
        Picasso.get().load("https:".concat(conditionIcon)).into(idACIVIcon);
        idACTVCondition.setText(condition);
        idACTVPressure.setText(String.format("%s mb", pressure));
        idACTVWindSpeed.setText(String.format("%s km/h", windSpeed));
        idACTVHumidity.setText(String.format("%s%%", humidity));
    }

    @SuppressLint("NotifyDataSetChanged")
    private void bindHourlySection(JSONObject response) throws JSONException {
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
    }

    @SuppressLint("NotifyDataSetChanged")
    private void bindDailySection(JSONObject response) throws JSONException {
        JSONObject forecastObj1 = response.getJSONObject("forecast");
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
    }

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

    @SuppressLint("NotifyDataSetChanged")
    private void parseAndDisplayWeatherData(String data) {
        try {
            JSONObject response = new JSONObject(data);
            idProgressBar.setVisibility(View.GONE);
            idNSVHome.setVisibility(View.VISIBLE);
            hoursModelArrayList.clear();
            daysModelArrayList.clear();

            String apiName = response.getJSONObject("location").optString("name", lastDisplayName);
            idACTVCityName.setText(apiName);
            bindCurrentSection(response);
            bindHourlySection(response);
            bindDailySection(response);
        } catch (JSONException ignored) { }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_actionbar, menu);
        menu.findItem(R.id.addToFavourite).setVisible(true);
        menu.findItem(R.id.temperature_change).setVisible(true);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        // Temperature unit toggle without re-fetch; re-render last JSON
        if (id == R.id.temperature_change) {
            if (currentTemperatureUnit.equals("celsius")) {
                currentTemperatureUnit = "fahrenheit";
                item.setIcon(R.drawable.celsius);
            } else {
                currentTemperatureUnit = "celsius";
                item.setIcon(R.drawable.fahrenheit);
            }
            if (lastResponseJson != null) {
                parseAndDisplayWeatherData(lastResponseJson);
            } else if (lastLat != null && lastLon != null) {
                getWeatherInfo(lastLat, lastLon, lastDisplayName);
            }
            Toast.makeText(this, "Changed to " + currentTemperatureUnit, Toast.LENGTH_SHORT).show();
        }

        // Add current selection to favourites (name + coords)
        if (id == R.id.addToFavourite) {
            if (lastDisplayName == null || lastDisplayName.isEmpty() || lastLat == null || lastLon == null) {
                Toast.makeText(this, "Nothing to add to favourites.", Toast.LENGTH_SHORT).show();
                return super.onOptionsItemSelected(item);
            }
            if (isItemPresent(lastDisplayName)) {
                Toast.makeText(this, "Already Present", Toast.LENGTH_SHORT).show();
            } else {
                String[] addItem = new String[items.length + 1];
                System.arraycopy(items, 0, addItem, 0, items.length);
                addItem[items.length] = lastDisplayName;
                items = addItem;

                SharedPreferences namesPrefs = getSharedPreferences("Select Favourite", MODE_PRIVATE);
                SharedPreferences.Editor namesEditor = namesPrefs.edit();
                namesEditor.putInt("itemCount", items.length);
                for (int i = 0; i < items.length; i++) {
                    namesEditor.putString("item_" + i, items[i]);
                }
                namesEditor.apply();

                SharedPreferences favCoords = getSharedPreferences("Fav_Locations", MODE_PRIVATE);
                favCoords.edit()
                        .putString("coords_" + lastDisplayName, String.format(Locale.US, "%.6f,%.6f", lastLat, lastLon))
                        .apply();

                adapterItems = new ArrayAdapter<>(this, R.layout.list_items, items);
                idACACTVDropdown.setAdapter(adapterItems);

                Toast.makeText(this, "Added to Favourites", Toast.LENGTH_SHORT).show();
            }
        }

        return super.onOptionsItemSelected(item);
    }

    private boolean isItemPresent(String targetItem) {
        for (String item : items) {
            if (item.equals(targetItem)) {
                return true;
            }
        }
        return false;
    }

    // Permissions callback
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            boolean granted = false;
            for (int res : grantResults) {
                if (res == PackageManager.PERMISSION_GRANTED) {
                    granted = true;
                    break;
                }
            }
            if (granted) {
                fetchLocation();
            } else {
                Toast.makeText(this, "Location permission is required.", Toast.LENGTH_SHORT).show();
                System.exit(0);
            }
        }
    }
}
