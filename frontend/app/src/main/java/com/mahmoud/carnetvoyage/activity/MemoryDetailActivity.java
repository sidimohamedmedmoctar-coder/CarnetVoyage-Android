package com.mahmoud.carnetvoyage.activity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.mahmoud.carnetvoyage.R;
import com.mahmoud.carnetvoyage.util.Constants;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class MemoryDetailActivity extends AppCompatActivity {

    public static final String EXTRA_TITLE = "extra_title";
    public static final String EXTRA_DESCRIPTION = "extra_description";
    public static final String EXTRA_PHOTO_URL = "extra_photo_url";
    public static final String EXTRA_LATITUDE = "extra_latitude";
    public static final String EXTRA_LONGITUDE = "extra_longitude";
    public static final String EXTRA_VISITED_AT = "extra_visited_at";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_memory_detail);

        // Récupération des extras
        String title = getIntent().getStringExtra(EXTRA_TITLE);
        String description = getIntent().getStringExtra(EXTRA_DESCRIPTION);
        String photoUrl = getIntent().getStringExtra(EXTRA_PHOTO_URL);
        String visitedAt = getIntent().getStringExtra(EXTRA_VISITED_AT);
        double latitude = getIntent().getDoubleExtra(EXTRA_LATITUDE, Double.NaN);
        double longitude = getIntent().getDoubleExtra(EXTRA_LONGITUDE, Double.NaN);

        // Vues
        ImageView ivPhoto = findViewById(R.id.ivPhoto);
        TextView tvTitle = findViewById(R.id.tvTitle);
        TextView tvDate = findViewById(R.id.tvDate);
        TextView tvDescription = findViewById(R.id.tvDescription);
        TextView tvCoords = findViewById(R.id.tvCoords);
        MaterialButton btnOpenMaps = findViewById(R.id.btnOpenMaps);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        View topBar = findViewById(R.id.topBar);
        int baseTopBarHeight = topBar.getLayoutParams().height;
        int basePaddingTop   = topBar.getPaddingTop();
        ViewCompat.setOnApplyWindowInsetsListener(
                findViewById(R.id.rootLayout), (v, windowInsets) -> {
                    int topInset = windowInsets.getInsets(
                            WindowInsetsCompat.Type.systemBars()).top;
                    ViewGroup.LayoutParams lp = topBar.getLayoutParams();
                    lp.height = baseTopBarHeight + topInset;
                    topBar.setLayoutParams(lp);
                    topBar.setPadding(topBar.getPaddingLeft(), basePaddingTop + topInset,
                                      topBar.getPaddingRight(), topBar.getPaddingBottom());
                    return WindowInsetsCompat.CONSUMED;
                });

        // Titre (top bar + éventuellement en contenu)
        tvTitle.setText(title != null ? title : "");

        // Photo
        if (photoUrl != null && !photoUrl.isEmpty()) {
            String base = Constants.BASE_URL.endsWith("/")
                    ? Constants.BASE_URL.substring(0, Constants.BASE_URL.length() - 1)
                    : Constants.BASE_URL;
            String fullUrl = photoUrl.startsWith("/") ? base + photoUrl : base + "/" + photoUrl;
            Glide.with(this)
                    .load(fullUrl)
                    .placeholder(R.drawable.ic_launcher_background)
                    .centerCrop()
                    .into(ivPhoto);
        }

        // Date de visite
        if (visitedAt != null && !visitedAt.isEmpty()) {
            try {
                LocalDateTime dt = LocalDateTime.parse(visitedAt);
                DateTimeFormatter fmt = DateTimeFormatter.ofPattern(
                        "EEEE d MMMM yyyy 'à' HH'h'mm", Locale.FRENCH);
                tvDate.setText("🕒 " + fmt.format(dt));
            } catch (Exception e) {
                tvDate.setText("🕒 " + visitedAt);
            }
        } else {
            tvDate.setVisibility(View.GONE);
        }

        // Description
        if (description != null && !description.isEmpty()) {
            tvDescription.setText(description);
        } else {
            tvDescription.setVisibility(View.GONE);
        }

        // Coordonnées GPS
        boolean hasCoords = !Double.isNaN(latitude) && !Double.isNaN(longitude);
        if (hasCoords) {
            tvCoords.setText(String.format(Locale.US, "📍 %.4f, %.4f", latitude, longitude));
            btnOpenMaps.setVisibility(View.VISIBLE);
            btnOpenMaps.setOnClickListener(v -> openInMaps(latitude, longitude, title));
        } else {
            tvCoords.setVisibility(View.GONE);
        }
    }

    private void openInMaps(double lat, double lng, String label) {
        // URI geo: interprété par Google Maps et toute autre appli de cartographie
        String encodedLabel = label != null ? Uri.encode(label) : "Souvenir";
        Uri geoUri = Uri.parse(String.format(Locale.US,
                "geo:%f,%f?q=%f,%f(%s)", lat, lng, lat, lng, encodedLabel));
        Intent intent = new Intent(Intent.ACTION_VIEW, geoUri);
        startActivity(intent);
    }
}
