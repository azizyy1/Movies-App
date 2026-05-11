package com.example.moviesapp_azizy;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private RecyclerView recyclerView;
    private MyMovieAdapter myMovieAdapter;
    private EditText searchEditText;
    private ProgressBar progressBar;
    private SwipeRefreshLayout swipeRefreshLayout;
    private View emptyStateLayout;
    private TextView emptyStateText;
    private RequestQueue requestQueue;
    private Handler searchHandler = new Handler(Looper.getMainLooper());
    private Runnable searchRunnable;

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    private FusedLocationProviderClient fusedLocationClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        requestQueue = Volley.newRequestQueue(this);

        searchEditText = findViewById(R.id.editTextSearch);
        progressBar = findViewById(R.id.progressBar);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        recyclerView = findViewById(R.id.recyclerView);
        emptyStateLayout = findViewById(R.id.emptyStateLayout);
        emptyStateText = findViewById(R.id.emptyStateText);
        FloatingActionButton fabCinema = findViewById(R.id.fabCinema);
        FloatingActionButton fabProfile = findViewById(R.id.fabProfile);

        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 2));

        fabCinema.setOnClickListener(v -> checkLocationPermissionAndFindCinema());
        fabProfile.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, ProfileActivity.class);
            startActivity(intent);
        });

        swipeRefreshLayout.setOnRefreshListener(() -> {
            searchEditText.setText("");
            fetchMovies(TmdbConfig.BASE_URL + "movie/popular?api_key=" + TmdbConfig.API_KEY);
        });

        if (searchEditText != null) {
            searchEditText.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if (searchRunnable != null) {
                        searchHandler.removeCallbacks(searchRunnable);
                    }
                }

                @Override
                public void afterTextChanged(Editable s) {
                    searchRunnable = () -> {
                        String query = s.toString().trim();
                        if (query.isEmpty()) {
                            fetchMovies(TmdbConfig.BASE_URL + "movie/popular?api_key=" + TmdbConfig.API_KEY);
                        } else {
                            searchMovies(query);
                        }
                    };
                    searchHandler.postDelayed(searchRunnable, 500);
                }
            });
        }

        fetchMovies(TmdbConfig.BASE_URL + "movie/popular?api_key=" + TmdbConfig.API_KEY);
    }

    private void searchMovies(String query) {
        String url = TmdbConfig.BASE_URL + "search/movie?api_key=" + TmdbConfig.API_KEY + "&query=" + query;
        fetchMovies(url);
    }

    private void fetchMovies(String url) {
        if (!swipeRefreshLayout.isRefreshing()) {
            progressBar.setVisibility(View.VISIBLE);
        }

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(
                Request.Method.GET, url, null,
                response -> {
                    progressBar.setVisibility(View.GONE);
                    swipeRefreshLayout.setRefreshing(false);
                    try {
                        JSONArray results = response.getJSONArray("results");
                        if (results.length() == 0) {
                            recyclerView.setVisibility(View.GONE);
                            emptyStateLayout.setVisibility(View.VISIBLE);
                            emptyStateText.setText("No results for your search");
                        } else {
                            recyclerView.setVisibility(View.VISIBLE);
                            emptyStateLayout.setVisibility(View.GONE);
                            MyMovieData[] movies = new MyMovieData[results.length()];
                            for (int i = 0; i < results.length(); i++) {
                                JSONObject movieObject = results.getJSONObject(i);
                                int id = movieObject.getInt("id");
                                String title = movieObject.getString("title");
                                String releaseDate = movieObject.optString("release_date", "N/A");
                                String imageUrl = movieObject.optString("poster_path", "");
                                movies[i] = new MyMovieData(id, title, releaseDate, imageUrl);
                            }
                            myMovieAdapter = new MyMovieAdapter(movies, MainActivity.this);
                            recyclerView.setAdapter(myMovieAdapter);
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "JSON Exception: " + e.getMessage());
                    }
                },
                error -> {
                    progressBar.setVisibility(View.GONE);
                    swipeRefreshLayout.setRefreshing(false);
                    recyclerView.setVisibility(View.GONE);
                    emptyStateLayout.setVisibility(View.VISIBLE);
                    
                    Log.e(TAG, "Volley Error: " + (error.getMessage() != null ? error.getMessage() : "Unknown error"));
                    String message = "Failed to fetch movies. Please check your connection.";
                    if (error.networkResponse != null && error.networkResponse.statusCode == 401) {
                        message = "Invalid TMDB API Key (401 Error)";
                    }
                    emptyStateText.setText(message);
                }
        );
        requestQueue.add(jsonObjectRequest);
    }

    private void checkLocationPermissionAndFindCinema() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            findNearbyCinemas();
        }
    }

    private void findNearbyCinemas() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
            if (location != null) {
                openGoogleMaps(location.getLatitude(), location.getLongitude());
            } else {
                // Request a fresh location if last location is null
                LocationRequest locationRequest = new LocationRequest();
                locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
                locationRequest.setInterval(1000);
                locationRequest.setFastestInterval(500);
                locationRequest.setNumUpdates(1);

                fusedLocationClient.requestLocationUpdates(locationRequest, new LocationCallback() {
                    @Override
                    public void onLocationResult(@NonNull LocationResult locationResult) {
                        Location loc = locationResult.getLastLocation();
                        if (loc != null) {
                            openGoogleMaps(loc.getLatitude(), loc.getLongitude());
                        } else {
                            Toast.makeText(MainActivity.this, "Unable to get location. Make sure GPS is on.", Toast.LENGTH_SHORT).show();
                        }
                    }
                }, Looper.getMainLooper());
            }
        });
    }

    private void openGoogleMaps(double latitude, double longitude) {
        Uri gmmIntentUri = Uri.parse("geo:" + latitude + "," + longitude + "?q=cinema");
        Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
        mapIntent.setPackage("com.google.android.apps.maps");

        if (mapIntent.resolveActivity(getPackageManager()) != null) {
            startActivity(mapIntent);
        } else {
            Uri webUri = Uri.parse("https://www.google.com/maps/search/cinema/@" + latitude + "," + longitude + ",15z");
            startActivity(new Intent(Intent.ACTION_VIEW, webUri));
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                findNearbyCinemas();
            } else {
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
