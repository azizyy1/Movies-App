package com.example.moviesapp_azizy;

import static android.content.ContentValues.TAG;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.bumptech.glide.Glide;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class MovieDetailActivity extends AppCompatActivity implements OnMapReadyCallback {
    private SupportMapFragment mapFragment;
    private TextView descriptionTextView;
    private TextView nameTextView;
    private TextView ratingTextView;
    private TextView releaseDateTextView;
    private ImageView img;
    private ProgressBar progressBar;
    private String trailerKey;
    private RequestQueue requestQueue;
    private Button playButton;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    private GoogleMap mMap;
    private List<LatLng> cinemaLocations = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_movie_detail);

        descriptionTextView = findViewById(R.id.Details);
        img = findViewById(R.id.imageview);
        nameTextView = findViewById(R.id.textName);
        ratingTextView = findViewById(R.id.textRating);
        releaseDateTextView = findViewById(R.id.textReleaseDate);
        progressBar = findViewById(R.id.progressBar);
        requestQueue = Volley.newRequestQueue(this);

        int movieId = getIntent().getIntExtra("movieId", -1);
        if (movieId != -1) {
            fetchMovieDetails(movieId);
        } else {
            descriptionTextView.setText("No movie ID provided");
        }

        playButton = findViewById(R.id.playButton);
        playButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playTrailer();
            }
        });

        // Mock cinema locations
        cinemaLocations.add(new LatLng(33.596460, -7.615480));
        cinemaLocations.add(new LatLng(33.588, -7.611));

        mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    private void fetchMovieDetails(int movieId) {
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);

        String movieDetailsUrl = TmdbConfig.BASE_URL + "movie/" + movieId + "?api_key=" + TmdbConfig.API_KEY;
        String movieVideosUrl = TmdbConfig.BASE_URL + "movie/" + movieId + "/videos?api_key=" + TmdbConfig.API_KEY;

        JsonObjectRequest movieDetailsRequest = new JsonObjectRequest(
                Request.Method.GET, movieDetailsUrl, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        if (progressBar != null) progressBar.setVisibility(View.GONE);
                        try {
                            String movieName = response.getString("title");
                            String movieDescription = response.getString("overview");
                            String posterPath = response.getString("poster_path");
                            double rating = response.optDouble("vote_average", 0.0);
                            String releaseDate = response.optString("release_date", "N/A");
                            String imageUrl = TmdbConfig.IMAGE_BASE_URL + posterPath;

                            nameTextView.setText(movieName);
                            descriptionTextView.setText(movieDescription);
                            ratingTextView.setText(String.format("★ %.1f", rating));
                            releaseDateTextView.setText("Release Date: " + releaseDate);

                            Glide.with(MovieDetailActivity.this)
                                    .load(imageUrl)
                                    .placeholder(R.drawable.ic_launcher_foreground)
                                    .into(img);
                        } catch (JSONException e) {
                            Log.e(TAG, "JSON Parsing error: " + e.getMessage());
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        if (progressBar != null) progressBar.setVisibility(View.GONE);
                        Log.e(TAG, "Error fetching movie details: " + error.getMessage());
                        descriptionTextView.setText("Failed to fetch movie details. Please check your connection.");
                    }
                }
        );

        JsonObjectRequest movieVideosRequest = new JsonObjectRequest(
                Request.Method.GET, movieVideosUrl, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            if (response.has("results")) {
                                JSONArray results = response.getJSONArray("results");
                                for (int i = 0; i < results.length(); i++) {
                                    JSONObject video = results.getJSONObject(i);
                                    if (video.getString("type").equals("Trailer") && video.getString("site").equals("YouTube")) {
                                        trailerKey = video.getString("key");
                                        break;
                                    }
                                }
                            }
                        } catch (JSONException e) {
                            Log.e(TAG, "JSON Parsing error (videos): " + e.getMessage());
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e(TAG, "Error fetching movie videos: " + error.getMessage());
                    }
                }
        );

        requestQueue.add(movieDetailsRequest);
        requestQueue.add(movieVideosRequest);
    }

    private void playTrailer() {
        if (trailerKey != null && !trailerKey.isEmpty()) {
            String trailerUrl = "https://www.youtube.com/embed/" + trailerKey;
            Intent intent = new Intent(MovieDetailActivity.this, VideoPlayer.class);
            intent.putExtra("videoUrl", trailerUrl);
            startActivity(intent);
        } else {
            Toast.makeText(this, "Trailer not available for this movie", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.getUiSettings().setZoomControlsEnabled(true);
        
        for (LatLng location : cinemaLocations) {
            mMap.addMarker(new MarkerOptions().position(location).title("Cinema"));
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
            moveToCurrentLocation();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    private void moveToCurrentLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            Location location = null;
            try {
                location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                if (location == null) {
                    location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                }
            } catch (SecurityException e) {
                e.printStackTrace();
            }

            if (location != null) {
                LatLng currentLocation = new LatLng(location.getLatitude(), location.getLongitude());
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 12));
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (mMap != null && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    mMap.setMyLocationEnabled(true);
                    moveToCurrentLocation();
                }
            }
        }
    }
}
