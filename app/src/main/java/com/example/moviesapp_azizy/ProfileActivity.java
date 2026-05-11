package com.example.moviesapp_azizy;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.SparseArray;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;

public class ProfileActivity extends AppCompatActivity {

    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final int CAMERA_PERMISSION_CODE = 101;
    
    private ImageView ivProfilePicture;
    private TextView tvUsername;
    private Button btnChangePicture, btnGoToMovies;
    private FaceDetector detector;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        ivProfilePicture = findViewById(R.id.ivProfilePicture);
        tvUsername = findViewById(R.id.tvUsername);
        btnChangePicture = findViewById(R.id.btnChangePicture);
        btnGoToMovies = findViewById(R.id.btnGoToMovies);

        loadUserData();
        loadProfilePicture();

        detector = new FaceDetector.Builder(getApplicationContext())
                .setTrackingEnabled(false)
                .setLandmarkType(FaceDetector.ALL_LANDMARKS)
                .build();

        btnChangePicture.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_CODE);
            } else {
                openCamera();
            }
        });

        btnGoToMovies.setOnClickListener(v -> {
            Intent intent = new Intent(ProfileActivity.this, MainActivity.class);
            startActivity(intent);
        });
    }

    private void openCamera() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        try {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        } catch (Exception e) {
            Toast.makeText(this, "Error: No camera app found.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK && data != null) {
            Bundle extras = data.getExtras();
            if (extras != null && extras.get("data") != null) {
                Bitmap imageBitmap = (Bitmap) extras.get("data");
                
                if (detectFace(imageBitmap)) {
                    ivProfilePicture.setImageBitmap(imageBitmap);
                    saveProfilePicture(imageBitmap);
                    Toast.makeText(this, "Face detected! Profile updated.", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "No face detected. Try a clearer selfie.", Toast.LENGTH_LONG).show();
                }
            } else {
                Toast.makeText(this, "Failed to capture image.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private boolean detectFace(Bitmap bitmap) {
        if (!detector.isOperational()) {
            Toast.makeText(this, "Face detector not ready yet. Please wait...", Toast.LENGTH_SHORT).show();
            return false;
        }

        Frame frame = new Frame.Builder().setBitmap(bitmap).build();
        SparseArray<Face> faces = detector.detect(frame);
        return faces.size() > 0;
    }

    private void saveProfilePicture(Bitmap bitmap) {
        File file = new File(getFilesDir(), "profile_picture.png");
        try (FileOutputStream out = new FileOutputStream(file)) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadProfilePicture() {
        File file = new File(getFilesDir(), "profile_picture.png");
        if (file.exists()) {
            try (FileInputStream in = new FileInputStream(file)) {
                Bitmap bitmap = BitmapFactory.decodeStream(in);
                ivProfilePicture.setImageBitmap(bitmap);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void loadUserData() {
        SharedPreferences preferences = getSharedPreferences("user_prefs", MODE_PRIVATE);
        String username = preferences.getString("username", "User");
        tvUsername.setText(getString(R.string.username_format, username));
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCamera();
            } else {
                Toast.makeText(this, "Camera permission is required to take a profile picture", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (detector != null) {
            detector.release();
        }
    }
}