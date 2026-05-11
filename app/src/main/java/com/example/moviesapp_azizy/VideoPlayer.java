package com.example.moviesapp_azizy;

import android.content.res.Configuration;
import android.os.Bundle;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

public class VideoPlayer extends AppCompatActivity {
    private WebView webView;
    private String videoUrl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_video_player);

        videoUrl = getIntent().getStringExtra("videoUrl");
        webView = findViewById(R.id.webView);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.getSettings().setMediaPlaybackRequiresUserGesture(false);
        webView.setWebViewClient(new WebViewClient());
        webView.setWebChromeClient(new WebChromeClient());
        
        if (videoUrl != null) {
            webView.loadUrl(videoUrl);
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (webView != null) {
            webView.loadUrl(videoUrl);
        }
    }
}
