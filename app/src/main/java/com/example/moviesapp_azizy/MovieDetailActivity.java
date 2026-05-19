package com.example.moviesapp_azizy;

import static android.content.ContentValues.TAG;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
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
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class MovieDetailActivity extends AppCompatActivity implements OnMapReadyCallback {
    private TextView descriptionTextView;
    private TextView nameTextView;
    private TextView ratingTextView;
    private TextView releaseDateTextView;
    private ImageView img;
    private ProgressBar progressBar;
    private String trailerKey;
    private RequestQueue requestQueue;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationClient;

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
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        int movieId = getIntent().getIntExtra("movieId", -1);
        if (movieId != -1) {
            fetchMovieDetails(movieId);
        } else {
            descriptionTextView.setText("No movie ID provided");
        }

        Button playButton = findViewById(R.id.playButton);
        playButton.setOnClickListener(v -> playTrailer());

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
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
                response -> {
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
                },
                error -> {
                    if (progressBar != null) progressBar.setVisibility(View.GONE);
                    descriptionTextView.setText("Failed to fetch movie details.");
                }
        );

        JsonObjectRequest movieVideosRequest = new JsonObjectRequest(
                Request.Method.GET, movieVideosUrl, null,
                response -> {
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
                },
                error -> Log.e(TAG, "Error fetching movie videos: " + error.getMessage())
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
            Toast.makeText(this, "Trailer not available", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.getUiSettings().setZoomControlsEnabled(true);
        
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
            getCurrentLocation();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        }

        mMap.setOnMapClickListener(latLng -> {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
                getCurrentLocation();
            }
        });
    }

    private void getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
            if (location != null) {
                LatLng userLocation = new LatLng(location.getLatitude(), location.getLongitude());

                mMap.clear();
                mMap.setMyLocationEnabled(true);

                mMap.addMarker(new MarkerOptions()
                        .position(userLocation)
                        .title("You are here")
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)));

                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 14));

                fetchNearbyCinemas(location.getLatitude(), location.getLongitude());
            } else {
                Toast.makeText(this, "Active le GPS puis réessaie", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (mMap != null && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    mMap.setMyLocationEnabled(true);
                    getCurrentLocation();
                }
            }
        }
    }
    private void fetchNearbyCinemas(double latitude, double longitude) {
        String url = "https://maps.googleapis.com/maps/api/place/nearbysearch/json?"
                + "location=" + latitude + "," + longitude
                + "&radius=5000"
                + "&type=movie_theater"
                + "&key=" + TmdbConfig.GOOGLE_MAPS_API_KEY;

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.GET,
                url,
                null,
                response -> {
                    try {
                        JSONArray results = response.getJSONArray("results");

                        for (int i = 0; i < results.length(); i++) {
                            JSONObject place = results.getJSONObject(i);

                            String name = place.getString("name");

                            JSONObject location = place
                                    .getJSONObject("geometry")
                                    .getJSONObject("location");

                            double lat = location.getDouble("lat");
                            double lng = location.getDouble("lng");

                            mMap.addMarker(new MarkerOptions()
                                    .position(new LatLng(lat, lng))
                                    .title(name)
                                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
                        }

                        if (results.length() == 0) {
                            Toast.makeText(this, "Aucun cinéma trouvé près de toi", Toast.LENGTH_SHORT).show();
                        }

                    } catch (JSONException e) {
                        Toast.makeText(this, "Erreur lecture cinémas", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> Toast.makeText(this, "Erreur chargement cinémas", Toast.LENGTH_SHORT).show()
        );

        requestQueue.add(request);
    }
}
