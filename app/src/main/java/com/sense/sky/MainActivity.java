package com.sense.sky;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.CurrentLocationRequest;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.AutocompletePrediction;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.net.FetchPlaceRequest;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.libraries.places.widget.PlaceAutocomplete;
import com.google.android.libraries.places.widget.PlaceAutocompleteActivity;
import com.google.android.libraries.places.widget.model.AutocompleteUiCustomization;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.sense.sky.api.WeatherApiClient;
import com.sense.sky.api.WeatherApiService;
import com.sense.sky.databinding.ActivityMainBinding;
import com.sense.sky.model.WeatherResponse;
import com.squareup.picasso.Picasso;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    public static final String WEATHER_API_KEY = BuildConfig.WEATHER_API_KEY;
    public static final String MAPS_API_KEY = BuildConfig.MAPS_API_KEY;
    // ── Constants ─────────────────────────────────────────────────────────────
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private static final long CACHE_TTL_MILLIS = 60 * 60 * 1000L; // 1 hour
    // ── View binding ──────────────────────────────────────────────────────────
    private ActivityMainBinding binding;

    // ── Adapters / lists ──────────────────────────────────────────────────────
    private java.util.ArrayList<HoursModel> hoursModelArrayList;
    private java.util.ArrayList<DaysModel> daysModelArrayList;
    private HoursAdapter hoursAdapter;
    private DaysAdapter daysAdapter;

    // ── Location ──────────────────────────────────────────────────────────────
    private FusedLocationProviderClient fusedLocationClient;

    // ── Favourites ────────────────────────────────────────────────────────────
    private String[] favouriteNames = {};
    private ArrayAdapter<String> adapterItems;

    // ── State ─────────────────────────────────────────────────────────────────
    private String currentTemperatureUnit = "celsius";
    private Double lastLat = null;
    private Double lastLon = null;
    private String lastResponseJson = null;
    private String lastDisplayName = "";

    // ── Places ────────────────────────────────────────────────────────────────
    private PlacesClient placesClient;
    private ActivityResultLauncher<Intent> placeAutocompleteLauncher;

    // ── Retrofit ──────────────────────────────────────────────────────────────
    private WeatherApiService weatherApiService;
    private Call<WeatherResponse> activeCall; // tracked so we can cancel on destroy

    // ─────────────────────────────────────────────────────────────────────────
    //  Lifecycle
    // ─────────────────────────────────────────────────────────────────────────

    public static boolean isOffline(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return true;
        Network active = cm.getActiveNetwork();
        if (active == null) return true;
        NetworkCapabilities caps = cm.getNetworkCapabilities(active);
        return caps == null || !caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Material3 Toolbar replaces ActionBar
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(R.string.app_name);
        }

        // RecyclerViews
        hoursModelArrayList = new java.util.ArrayList<>();
        daysModelArrayList = new java.util.ArrayList<>();
        hoursAdapter = new HoursAdapter(this, hoursModelArrayList);
        daysAdapter = new DaysAdapter(this, daysModelArrayList);
        binding.idRVWeatherHours.setAdapter(hoursAdapter);
        binding.idRVWeatherDays.setAdapter(daysAdapter);

        // SwipeRefreshLayout
        binding.swipeRefresh.setOnRefreshListener(refreshLayout -> {
            if (lastLat != null && lastLon != null) {
                fetchFromNetwork(lastLat, lastLon, lastDisplayName);
            } else {
                refreshLayout.finishRefresh(false);
            }
        });

        if (isOffline(this)) {
            showNoInternetDialog();
        }

        // Retrofit service
        weatherApiService = WeatherApiClient.getInstance(true);

        // Location
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Places SDK
        if (!Places.isInitialized()) {
            Places.initializeWithNewPlacesApiEnabled(getApplicationContext(), MAPS_API_KEY);
        }
        placesClient = Places.createClient(this);

        placeAutocompleteLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            Intent data = result.getData();
            if (result.getResultCode() == PlaceAutocompleteActivity.RESULT_OK && data != null) {
                AutocompletePrediction prediction = PlaceAutocomplete.getPredictionFromIntent(data);
                if (prediction == null) {
                    toast("Could not resolve selected place.");
                    return;
                } else {
                    prediction.getPlaceId();
                }
                fetchPlaceAndLoadWeather(prediction.getPlaceId());
            }
        });

        // Search bar click
        binding.autocompleteFragment.setOnClickListener(v -> launchPlaceAutocomplete());

        // Load saved favourites
        loadFavourites();

        binding.idACACTVDropdown.setOnItemClickListener((parent, view, position, id) -> {
            String name = favouriteNames[position];
            SharedPreferences coordinates = getSharedPreferences("Fav_Locations", MODE_PRIVATE);
            String saved = coordinates.getString("saved_coordinates" + name, null);
            if (saved != null) {
                String[] parts = saved.split(",");
                if (parts.length == 2) {
                    try {
                        double lat = Double.parseDouble(parts[0]);
                        double lon = Double.parseDouble(parts[1]);
                        lastDisplayName = name;
                        getWeatherInfo(lat, lon, name);
                    } catch (NumberFormatException e) {
                        toast("Invalid saved coordinates.");
                    }
                }
            } else {
                toast("Coordinates not found for favourite.");
            }
        });

        requestLocationAndFetch();
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Places helpers
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Cancel any in-flight Retrofit call to avoid memory leaks
        if (activeCall != null && !activeCall.isCanceled()) {
            activeCall.cancel();
        }
        binding = null;
    }

    private void launchPlaceAutocomplete() {
        Intent intent = new PlaceAutocomplete.IntentBuilder().setAutocompleteUiCustomization(AutocompleteUiCustomization.builder().build()).build(this);
        placeAutocompleteLauncher.launch(intent);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Location
    // ─────────────────────────────────────────────────────────────────────────

    private void fetchPlaceAndLoadWeather(String placeId) {
        List<Place.Field> fields = Arrays.asList(Place.Field.ID, Place.Field.DISPLAY_NAME, Place.Field.LOCATION);
        placesClient.fetchPlace(FetchPlaceRequest.newInstance(placeId, fields)).addOnSuccessListener(response -> {
            Place p = response.getPlace();
            LatLng ll = p.getLocation();
            CharSequence name = p.getDisplayName();
            if (ll != null) {
                lastDisplayName = name != null ? name.toString() : "";
                getWeatherInfo(ll.latitude, ll.longitude, lastDisplayName);
            } else {
                toast("No coordinates for selected place.");
            }
        }).addOnFailureListener(e -> toast("Place fetch failed."));
    }

    private void requestLocationAndFetch() {
        boolean fine = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        boolean coarse = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        if (!fine && !coarse) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            fetchLocation();
        }
    }

    @SuppressLint("MissingPermission")
    private void fetchLocation() {
        boolean fine = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        boolean coarse = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        if (!fine && !coarse) {
            toast("Location permission is required.");
            return;
        }

        CurrentLocationRequest req = new CurrentLocationRequest.Builder().setPriority(Priority.PRIORITY_BALANCED_POWER_ACCURACY).setMaxUpdateAgeMillis(0).build();

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
                        toast("Location not available.");
                    }
                });
            }
        }).addOnFailureListener(e -> toast("Failed to get location."));
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Weather fetching  (Retrofit replaces Volley)
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            boolean granted = false;
            for (int r : grantResults)
                if (r == PackageManager.PERMISSION_GRANTED) {
                    granted = true;
                    break;
                }
            if (granted) {
                fetchLocation();
            } else {
                toast("Location permission is required.");
                System.exit(0);
            }
        }
    }

    private String buildCacheKey(double lat, double lon) {
        return String.format(Locale.US, "%.4f,%.4f", lat, lon);
    }

    /**
     * Entry point — checks cache first, then hits network.
     */
    private void getWeatherInfo(double lat, double lon, String displayName) {
        lastLat = lat;
        lastLon = lon;

        showLoading(true);

        String cacheKey = buildCacheKey(lat, lon);
        WeatherCacheManager cache = new WeatherCacheManager(this);
        String cached = cache.getWeatherData(cacheKey);
        long ts = cache.getWeatherDataTimestamp(cacheKey);
        boolean fresh = (System.currentTimeMillis() - ts) <= CACHE_TTL_MILLIS;

        if (isOffline(this) && cached != null) {
            parseAndDisplayJson(cached);
            return;
        }
        if (cached != null && fresh) {
            parseAndDisplayJson(cached);
            return;
        }

        fetchFromNetwork(lat, lon, displayName);
    }

    /**
     * Always fires a network request — used by SwipeRefreshLayout and forced reload.
     */
    private void fetchFromNetwork(double lat, double lon, String displayName) {
        if (activeCall != null && !activeCall.isCanceled()) activeCall.cancel();

        String cacheKey = buildCacheKey(lat, lon);
        WeatherCacheManager cache = new WeatherCacheManager(this);
        String cached = cache.getWeatherData(cacheKey);

        activeCall = weatherApiService.getForecast(WEATHER_API_KEY, lat + "," + lon, 7, "no", "no");

        activeCall.enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<WeatherResponse> call, @NonNull Response<WeatherResponse> response) {
                binding.swipeRefresh.finishRefresh(true);
                if (response.isSuccessful() && response.body() != null) {
                    WeatherResponse wr = response.body();

                    // Determine display name from API response
                    if (wr.location != null && wr.location.name != null && !wr.location.name.isEmpty()) {
                        lastDisplayName = wr.location.name;
                    } else {
                        lastDisplayName = displayName;
                    }

                    // Cache raw JSON via Gson
                    com.google.gson.Gson gson = new com.google.gson.Gson();
                    String json = gson.toJson(wr);
                    cache.saveWeatherData(cacheKey, json, System.currentTimeMillis());
                    lastResponseJson = json;

                    bindWeatherResponse(wr);
                } else {
                    handleNetworkError(cached, "Server error: " + response.code());
                }
            }

            @Override
            public void onFailure(@NonNull Call<WeatherResponse> call, @NonNull Throwable t) {
                if (call.isCanceled()) return;
                binding.swipeRefresh.finishRefresh(false);
                handleNetworkError(cached, "Network error.");
            }
        });
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Binding helpers
    // ─────────────────────────────────────────────────────────────────────────

    private void handleNetworkError(String cached, String message) {
        showLoading(false);
        if (cached != null) {
            parseAndDisplayJson(cached);
            toast("Showing cached data.");
        } else {
            toast(message + " Unable to load weather.");
        }
    }

    /**
     * De-serialize cached JSON and re-bind.
     */
    private void parseAndDisplayJson(String json) {
        try {
            com.google.gson.Gson gson = new com.google.gson.Gson();
            WeatherResponse wr = gson.fromJson(json, WeatherResponse.class);
            lastResponseJson = json;
            bindWeatherResponse(wr);
        } catch (Exception ignored) {
            toast("Failed to parse cached data.");
            showLoading(false);
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private void bindWeatherResponse(WeatherResponse wr) {
        showLoading(false);
        hoursModelArrayList.clear();
        daysModelArrayList.clear();

        // ── Location ──
        if (wr.location != null) {
            binding.idACTVCityName.setText((wr.location.name != null ? wr.location.name : lastDisplayName).toUpperCase(Locale.getDefault()));
        }

        // ── Current ──
        if (wr.current != null) {
            bindCurrent(wr.current);
        }

        // ── Hourly ──
        if (wr.forecast != null && wr.forecast.forecastday != null && !wr.forecast.forecastday.isEmpty()) {
            WeatherResponse.ForecastDay today = wr.forecast.forecastday.get(0);
            if (today.hour != null) {
                for (WeatherResponse.Hour h : today.hour) {
                    String icon = h.condition != null ? h.condition.icon : "";
                    hoursModelArrayList.add(new HoursModel(h.time, String.valueOf(h.tempC), String.valueOf(h.tempF), currentTemperatureUnit, icon, String.valueOf(h.humidity), String.valueOf(h.windKph)));
                }
            }
        }
        hoursAdapter.notifyDataSetChanged();

        // ── Daily ──
        if (wr.forecast != null && wr.forecast.forecastday != null) {
            for (WeatherResponse.ForecastDay fd : wr.forecast.forecastday) {
                WeatherResponse.Day d = fd.day;
                if (d == null) continue;
                String icon = d.condition != null ? d.condition.icon : "";
                daysModelArrayList.add(new DaysModel(fd.date, String.valueOf(d.mintempC), String.valueOf(d.mintempF), String.valueOf(d.maxtempC), String.valueOf(d.maxtempF), currentTemperatureUnit, icon, String.valueOf(d.avghumidity), String.valueOf(d.maxwindKph)));
            }
        }
        daysAdapter.notifyDataSetChanged();
        invalidateOptionsMenu();
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Menu
    // ─────────────────────────────────────────────────────────────────────────

    private void bindCurrent(WeatherResponse.Current c) {
        // Temperature
        if (currentTemperatureUnit.equals("fahrenheit")) {
            binding.idACTVTemperature.setText(String.format(Locale.getDefault(), "%.0f °F", c.tempF));
            binding.idACTVFeelsLike.setText(String.format(Locale.getDefault(), "%.0f °F", c.feelslikeF));
        } else {
            binding.idACTVTemperature.setText(String.format(Locale.getDefault(), "%.0f °C", c.tempC));
            binding.idACTVFeelsLike.setText(String.format(Locale.getDefault(), "%.0f °C", c.feelslikeC));
        }

        // Updated-at timestamp
        SimpleDateFormat inputFmt = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
        SimpleDateFormat outputFmt = new SimpleDateFormat("EEEE h:mm a", Locale.getDefault());
        try {
            Date t = inputFmt.parse(c.lastUpdated);
            if (t != null)
                binding.idACTVUpdatedAt.setText(outputFmt.format(t).toUpperCase(Locale.getDefault()));
        } catch (ParseException ignored) {
        }

        // Condition text + icon
        if (c.condition != null) {
            binding.idACTVCondition.setText(c.condition.text);
            if (c.condition.icon != null && !c.condition.icon.isEmpty()) {
                Picasso.get().load("https:".concat(c.condition.icon)).into(binding.idACIVIcon);
            }
        }

        // Stats
        binding.idACTVPressure.setText(String.format(Locale.getDefault(), "%.0f mb", c.pressureMb));
        binding.idACTVWindSpeed.setText(String.format(Locale.getDefault(), "%.1f km/h", c.windKph));
        binding.idACTVHumidity.setText(String.format(Locale.getDefault(), "%d%%", c.humidity));

        // New fields
        binding.idACTVUvIndex.setText(String.format(Locale.getDefault(), "%.0f", c.uv));
        binding.idACTVVisibility.setText(String.format(Locale.getDefault(), "%.0f km", c.visKm));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_actionbar, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem starItem = menu.findItem(R.id.addToFavourite);
        if (starItem != null) {
            starItem.setIcon(isFavouritePresent(lastDisplayName)
                    ? R.drawable.star_filled
                    : R.drawable.star_stroke);
        }

        MenuItem tempItem = menu.findItem(R.id.temperature_change);
        if (tempItem != null) {
            tempItem.setIcon(currentTemperatureUnit.equals("celsius")
                    ? R.drawable.fahrenheit   // shows what you'll switch TO
                    : R.drawable.celsius);
        }

        return super.onPrepareOptionsMenu(menu);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Favourites
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.temperature_change) {
            if (currentTemperatureUnit.equals("celsius")) {
                currentTemperatureUnit = "fahrenheit";
            } else {
                currentTemperatureUnit = "celsius";
            }
            if (lastResponseJson != null) {
                parseAndDisplayJson(lastResponseJson);
            } else if (lastLat != null && lastLon != null) {
                getWeatherInfo(lastLat, lastLon, lastDisplayName);
            }
            toast("Switched to " + (currentTemperatureUnit.equals("celsius") ? "℃" : "°F"));
        }

        if (id == R.id.addToFavourite) {
            if (lastDisplayName == null || lastDisplayName.isEmpty()
                    || lastLat == null || lastLon == null) {
                Toast.makeText(this, "Nothing to add to favourites.", Toast.LENGTH_SHORT).show();
                return super.onOptionsItemSelected(item);
            }
            if (isFavouritePresent(lastDisplayName)) {
                removeCurrentFromFavourites();
                item.setIcon(R.drawable.star_stroke);
                Toast.makeText(this, "Removed from favourites.", Toast.LENGTH_SHORT).show();
            } else {
                addCurrentToFavourites();
                item.setIcon(R.drawable.star_filled);
                Toast.makeText(this, "Added to favourites.", Toast.LENGTH_SHORT).show();
            }
        }

        return super.onOptionsItemSelected(item);
    }

    private void loadFavourites() {
        SharedPreferences prefs = getSharedPreferences("Select Favourite", MODE_PRIVATE);
        int count = prefs.getInt("itemCount", 0);
        if (count > 0) {
            favouriteNames = new String[count];
            for (int i = 0; i < count; i++) {
                favouriteNames[i] = prefs.getString("item_" + i, "");
            }
        }
        adapterItems = new ArrayAdapter<>(this, R.layout.list_items, favouriteNames);
        binding.idACACTVDropdown.setAdapter(adapterItems);
        binding.idACACTVDropdown.setFocusable(false);
        binding.idACACTVDropdown.setCursorVisible(false);
    }

    private void addCurrentToFavourites() {
        if (lastDisplayName == null || lastDisplayName.isEmpty() || lastLat == null || lastLon == null) {
            toast("Nothing to add to favourites.");
            return;
        }
        if (isFavouritePresent(lastDisplayName)) {
            toast("Already in favourites.");
            return;
        }
        String[] updated = Arrays.copyOf(favouriteNames, favouriteNames.length + 1);
        updated[favouriteNames.length] = lastDisplayName;
        favouriteNames = updated;

        SharedPreferences namePrefs = getSharedPreferences("Select Favourite", MODE_PRIVATE);
        SharedPreferences.Editor editor = namePrefs.edit();
        editor.putInt("itemCount", favouriteNames.length);
        for (int i = 0; i < favouriteNames.length; i++)
            editor.putString("item_" + i, favouriteNames[i]);
        editor.apply();

        getSharedPreferences("Fav_Locations", MODE_PRIVATE).edit().putString("saved_coordinates" + lastDisplayName, String.format(Locale.US, "%.6f,%.6f", lastLat, lastLon)).apply();

        adapterItems = new ArrayAdapter<>(this, R.layout.list_items, favouriteNames);
        binding.idACACTVDropdown.setAdapter(adapterItems);
        toast("Added to favourites.");
    }

    private void removeCurrentFromFavourites() {
        List<String> list = new ArrayList<>(Arrays.asList(favouriteNames));
        list.remove(lastDisplayName);
        favouriteNames = list.toArray(new String[0]);

        SharedPreferences namesPrefs = getSharedPreferences("Select Favourite", MODE_PRIVATE);
        SharedPreferences.Editor editor = namesPrefs.edit();
        editor.putInt("itemCount", favouriteNames.length);
        for (int i = 0; i < favouriteNames.length; i++) {
            editor.putString("item_" + i, favouriteNames[i]);
        }
        editor.apply();

        getSharedPreferences("Fav_Locations", MODE_PRIVATE)
                .edit()
                .remove("saved_coordinates" + lastDisplayName)
                .apply();

        adapterItems = new ArrayAdapter<>(this, R.layout.list_items, favouriteNames);
        binding.idACACTVDropdown.setAdapter(adapterItems);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  UI helpers
    // ─────────────────────────────────────────────────────────────────────────

    private boolean isFavouritePresent(String name) {
        for (String f : favouriteNames) if (f.equals(name)) return true;
        return false;
    }

    private void showLoading(boolean loading) {
        if (binding == null) return;
        if (loading) {
            binding.shimmerViewContainer.setVisibility(android.view.View.VISIBLE);
            binding.shimmerViewContainer.startShimmer();
            binding.swipeRefresh.setVisibility(android.view.View.GONE);
        } else {
            binding.shimmerViewContainer.stopShimmer();
            binding.shimmerViewContainer.setVisibility(android.view.View.GONE);
            binding.swipeRefresh.setVisibility(android.view.View.VISIBLE);
        }
    }

    private void showNoInternetDialog() {
        new MaterialAlertDialogBuilder(this).setTitle("No Internet Connection").setMessage("Please check your internet connection. The app will show cached data if available.").setPositiveButton("OK", (dialog, which) -> dialog.dismiss()).setCancelable(false).show();
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Connectivity
    // ─────────────────────────────────────────────────────────────────────────

    private void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }
}
