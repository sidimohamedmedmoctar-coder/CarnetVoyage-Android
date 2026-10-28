package com.mahmoud.carnetvoyage.activity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.facebook.shimmer.ShimmerFrameLayout;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;
import com.mahmoud.carnetvoyage.R;
import com.mahmoud.carnetvoyage.adapter.MemoryAdapter;
import com.mahmoud.carnetvoyage.api.ApiClient;
import com.mahmoud.carnetvoyage.api.ApiService;
import com.mahmoud.carnetvoyage.model.Memory;
import com.mahmoud.carnetvoyage.model.Trip;
import com.mahmoud.carnetvoyage.util.AnalyticsHelper;
import com.mahmoud.carnetvoyage.util.ApiErrorHandler;
import com.mahmoud.carnetvoyage.util.NetworkUtils;
import com.mahmoud.carnetvoyage.util.SessionManager;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TripDetailActivity extends AppCompatActivity
        implements MemoryAdapter.OnMemoryClickListener, OnMapReadyCallback {

    public static final String EXTRA_TRIP_ID = "extra_trip_id";

    // ── Vues ─────────────────────────────────────────────────────────────────
    private ImageView btnBack;
    private ImageView ivShare;
    private TextView tvTripTitle;
    private TextView tvDescription;
    private TextView tvDates;
    private TextView tvOwner;
    private TabLayout tabLayout;
    private SwipeRefreshLayout swipeRefresh;
    private ShimmerFrameLayout shimmerLayout;
    private RecyclerView recyclerMemories;
    private View emptyState;
    private View mapContainer;
    private TextView tvMapEmpty;
    private FloatingActionButton fabAddMemory;

    // ── État ──────────────────────────────────────────────────────────────────
    private ApiService apiService;
    private SessionManager sessionManager;
    private Long tripId;
    private String tripTitle = "";
    private boolean isOwner = false;
    private final List<Memory> memories = new ArrayList<>();
    private MemoryAdapter memoryAdapter;

    // ── Map ───────────────────────────────────────────────────────────────────
    private GoogleMap googleMap;
    private boolean mapReady = false;
    private boolean memoriesLoaded = false;

    /** Recharge la liste si un souvenir a été ajouté avec succès */
    private final ActivityResultLauncher<Intent> addMemoryLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    memoriesLoaded = false;
                    loadMemories();
                }
            }
    );

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        tripId = getIntent().getLongExtra(EXTRA_TRIP_ID, -1L);
        if (tripId == -1L) { finish(); return; }

        setContentView(R.layout.activity_trip_detail);

        apiService = ApiClient.getApiService(this);
        sessionManager = new SessionManager(this);

        initViews();
        initMap();
        setupListeners();
        loadData();
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
    }

    // ── Initialisation ────────────────────────────────────────────────────────

    private void initViews() {
        btnBack        = findViewById(R.id.btnBack);
        ivShare        = findViewById(R.id.ivShare);
        tvTripTitle    = findViewById(R.id.tvTripTitle);
        tvDescription  = findViewById(R.id.tvDescription);
        tvDates        = findViewById(R.id.tvDates);
        tvOwner        = findViewById(R.id.tvOwner);
        tabLayout      = findViewById(R.id.tabLayout);
        swipeRefresh   = findViewById(R.id.swipeRefresh);
        shimmerLayout  = findViewById(R.id.shimmerLayout);
        recyclerMemories = findViewById(R.id.recyclerMemories);
        emptyState     = findViewById(R.id.emptyState);
        mapContainer   = findViewById(R.id.mapContainer);
        tvMapEmpty     = findViewById(R.id.tvMapEmpty);
        fabAddMemory   = findViewById(R.id.fabAddMemory);

        recyclerMemories.setLayoutManager(new LinearLayoutManager(this));

        // ── Edge-to-edge (API 35+) ────────────────────────────────────────────────
        // La top bar est à hauteur fixe (56dp) et le contenu a un marginTop=56dp fixe.
        // On doit donc : (1) agrandir la top bar, (2) ajouter le padding en haut,
        // (3) décaler le contenu. Les trois valeurs de base sont capturées depuis le XML
        // avant toute modification → immunisé contre l'accumulation à la rotation.
        View topBar = findViewById(R.id.topBar);
        View contentLayout = findViewById(R.id.contentLayout);
        int baseTopBarHeight = topBar.getLayoutParams().height;
        int basePaddingTop   = topBar.getPaddingTop();
        int baseContentMarginTop =
                ((ViewGroup.MarginLayoutParams) contentLayout.getLayoutParams()).topMargin;

        // Listener sur la RACINE (CoordinatorLayout) et non sur topBar :
        // modifier les enfants depuis la racine ne redéclenche pas ce listener,
        // ce qui évite la boucle infinie layout → insets → layout → ANR.
        ViewCompat.setOnApplyWindowInsetsListener(
                findViewById(R.id.rootLayout), (v, windowInsets) -> {
                    int topInset = windowInsets.getInsets(
                            WindowInsetsCompat.Type.systemBars()).top;
                    ViewGroup.LayoutParams lp = topBar.getLayoutParams();
                    lp.height = baseTopBarHeight + topInset;
                    topBar.setLayoutParams(lp);
                    topBar.setPadding(topBar.getPaddingLeft(), basePaddingTop + topInset,
                                      topBar.getPaddingRight(), topBar.getPaddingBottom());
                    ViewGroup.MarginLayoutParams contentLp =
                            (ViewGroup.MarginLayoutParams) contentLayout.getLayoutParams();
                    contentLp.topMargin = baseContentMarginTop + topInset;
                    contentLayout.setLayoutParams(contentLp);
                    return WindowInsetsCompat.CONSUMED;
                });
    }

    private void initMap() {
        SupportMapFragment mapFragment = (SupportMapFragment)
                getSupportFragmentManager().findFragmentById(R.id.mapFragment);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());

        ivShare.setOnClickListener(v -> {
            Intent intent = new Intent(this, ShareTripActivity.class);
            intent.putExtra(ShareTripActivity.EXTRA_TRIP_ID, tripId);
            intent.putExtra(ShareTripActivity.EXTRA_TRIP_TITLE, tripTitle);
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        });

        swipeRefresh.setOnRefreshListener(() -> {
            if (!NetworkUtils.isNetworkAvailable(this)) {
                swipeRefresh.setRefreshing(false);
                NetworkUtils.showNoNetworkSnackbar(findViewById(android.R.id.content));
                return;
            }
            loadData();
        });

        fabAddMemory.setOnClickListener(v -> {
            Intent intent = new Intent(this, AddMemoryActivity.class);
            intent.putExtra(AddMemoryActivity.EXTRA_TRIP_ID, tripId);
            addMemoryLauncher.launch(intent);
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        });

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (tab.getPosition() == 0) {
                    swipeRefresh.setVisibility(View.VISIBLE);
                    mapContainer.setVisibility(View.GONE);
                    if (isOwner) fabAddMemory.setVisibility(View.VISIBLE);
                } else {
                    swipeRefresh.setVisibility(View.GONE);
                    mapContainer.setVisibility(View.VISIBLE);
                    fabAddMemory.setVisibility(View.GONE);
                    if (mapReady && memoriesLoaded) displayMemoriesOnMap();
                }
            }

            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    // ── Chargement des données ────────────────────────────────────────────────

    private void loadData() {
        if (!NetworkUtils.isNetworkAvailable(this)) {
            swipeRefresh.setRefreshing(false);
            NetworkUtils.showNoNetworkSnackbar(findViewById(android.R.id.content));
            return;
        }

        // Shimmer au premier chargement (liste vide)
        if (memories.isEmpty()) {
            shimmerLayout.setVisibility(View.VISIBLE);
            shimmerLayout.startShimmer();
        }

        loadTripInfo();
    }

    private void loadTripInfo() {
        apiService.getTripById(tripId).enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<Trip> call, @NonNull Response<Trip> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Trip trip = response.body();
                    populateTripInfo(trip);
                    isOwner = trip.isOwner();
                    ivShare.setVisibility(isOwner ? View.VISIBLE : View.GONE);
                    if (isOwner && tabLayout.getSelectedTabPosition() == 0) {
                        fabAddMemory.setVisibility(View.VISIBLE);
                    }
                    loadMemories();
                } else if (ApiErrorHandler.isUnauthorized(response)) {
                    stopShimmer();
                    handleUnauthorized();
                } else {
                    stopShimmer();
                    swipeRefresh.setRefreshing(false);
                    Toast.makeText(TripDetailActivity.this,
                            ApiErrorHandler.parseError(response), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<Trip> call, @NonNull Throwable t) {
                stopShimmer();
                swipeRefresh.setRefreshing(false);
                Toast.makeText(TripDetailActivity.this,
                        getString(R.string.network_error_msg, t.getMessage()), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void populateTripInfo(Trip trip) {
        tripTitle = trip.getTitle() != null ? trip.getTitle() : "";
        tvTripTitle.setText(tripTitle);
        AnalyticsHelper.logTripViewed(tripTitle);

        if (trip.getDescription() != null && !trip.getDescription().isEmpty()) {
            tvDescription.setText(trip.getDescription());
            tvDescription.setVisibility(View.VISIBLE);
        } else {
            tvDescription.setVisibility(View.GONE);
        }

        String datesText = formatDate(trip.getStartDate()) + " → " + formatDate(trip.getEndDate());
        tvDates.setText(getString(R.string.trip_dates_display, datesText));

        if (!trip.isOwner() && trip.getOwnerUsername() != null) {
            tvOwner.setText(getString(R.string.shared_by_display, trip.getOwnerUsername()));
            tvOwner.setVisibility(View.VISIBLE);
        } else {
            tvOwner.setVisibility(View.GONE);
        }
    }

    private String formatDate(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) return "";
        try {
            LocalDate date = LocalDate.parse(dateStr);
            return date.format(DateTimeFormatter.ofPattern("d MMM yyyy", Locale.FRENCH));
        } catch (Exception e) {
            return dateStr;
        }
    }

    private void loadMemories() {
        apiService.getMemoriesByTrip(tripId).enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<List<Memory>> call,
                                   @NonNull Response<List<Memory>> response) {
                stopShimmer();
                swipeRefresh.setRefreshing(false);

                if (response.isSuccessful() && response.body() != null) {
                    memories.clear();
                    memories.addAll(response.body());

                    if (memoryAdapter == null) {
                        memoryAdapter = new MemoryAdapter(memories, TripDetailActivity.this, isOwner);
                        recyclerMemories.setAdapter(memoryAdapter);
                    } else {
                        memoryAdapter.notifyDataSetChanged();
                    }

                    TabLayout.Tab tab0 = tabLayout.getTabAt(0);
                    if (tab0 != null) {
                        tab0.setText(getString(R.string.memories_tab_title, memories.size()));
                    }

                    emptyState.setVisibility(memories.isEmpty() ? View.VISIBLE : View.GONE);

                    memoriesLoaded = true;
                    if (mapReady) displayMemoriesOnMap();

                } else if (ApiErrorHandler.isUnauthorized(response)) {
                    handleUnauthorized();
                } else {
                    Toast.makeText(TripDetailActivity.this,
                            ApiErrorHandler.parseError(response), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<Memory>> call, @NonNull Throwable t) {
                stopShimmer();
                swipeRefresh.setRefreshing(false);
                Toast.makeText(TripDetailActivity.this,
                        getString(R.string.network_error_msg, t.getMessage()), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void stopShimmer() {
        shimmerLayout.stopShimmer();
        shimmerLayout.setVisibility(View.GONE);
    }

    // ── Interactions souvenirs (liste) ────────────────────────────────────────

    @Override
    public void onMemoryClick(Memory memory) {
        launchMemoryDetail(memory);
    }

    @Override
    public void onMemoryLongClick(Memory memory) {
        if (!isOwner) return;
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.delete_memory_confirm_title)
                .setMessage(getString(R.string.delete_memory_confirm_message, memory.getTitle()))
                .setNegativeButton(R.string.cancel_button, null)
                .setPositiveButton(R.string.delete_button, (dialog, which) -> deleteMemory(memory))
                .show();
    }

    private void deleteMemory(Memory memory) {
        if (!NetworkUtils.isNetworkAvailable(this)) {
            NetworkUtils.showNoNetworkSnackbar(findViewById(android.R.id.content));
            return;
        }
        apiService.deleteMemory(memory.getId()).enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(TripDetailActivity.this,
                            R.string.memory_deleted_success, Toast.LENGTH_SHORT).show();
                    memoriesLoaded = false;
                    loadMemories();
                } else if (ApiErrorHandler.isUnauthorized(response)) {
                    handleUnauthorized();
                } else {
                    Toast.makeText(TripDetailActivity.this,
                            ApiErrorHandler.parseError(response), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                Toast.makeText(TripDetailActivity.this,
                        getString(R.string.network_error_msg, t.getMessage()), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void launchMemoryDetail(Memory memory) {
        Intent intent = new Intent(this, MemoryDetailActivity.class);
        intent.putExtra(MemoryDetailActivity.EXTRA_TITLE, memory.getTitle());
        intent.putExtra(MemoryDetailActivity.EXTRA_DESCRIPTION, memory.getDescription());
        intent.putExtra(MemoryDetailActivity.EXTRA_PHOTO_URL, memory.getPhotoUrl());
        intent.putExtra(MemoryDetailActivity.EXTRA_VISITED_AT, memory.getVisitedAt());
        if (memory.getLatitude() != null)
            intent.putExtra(MemoryDetailActivity.EXTRA_LATITUDE, memory.getLatitude());
        if (memory.getLongitude() != null)
            intent.putExtra(MemoryDetailActivity.EXTRA_LONGITUDE, memory.getLongitude());
        startActivity(intent);
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
    }

    // ── Google Maps ───────────────────────────────────────────────────────────

    @Override
    public void onMapReady(@NonNull GoogleMap map) {
        googleMap = map;
        googleMap.getUiSettings().setZoomControlsEnabled(true);
        googleMap.getUiSettings().setMapToolbarEnabled(false);
        mapReady = true;
        if (memoriesLoaded) displayMemoriesOnMap();
    }

    private void displayMemoriesOnMap() {
        if (googleMap == null) return;
        googleMap.clear();

        List<Memory> sorted = new ArrayList<>(memories);
        sorted.sort((a, b) -> {
            try {
                return LocalDateTime.parse(a.getVisitedAt())
                        .compareTo(LocalDateTime.parse(b.getVisitedAt()));
            } catch (Exception e) { return 0; }
        });

        List<Memory> geoMemories = new ArrayList<>();
        for (Memory m : sorted) {
            if (hasValidCoords(m)) geoMemories.add(m);
        }

        if (geoMemories.isEmpty()) {
            tvMapEmpty.setVisibility(View.VISIBLE);
            return;
        }
        tvMapEmpty.setVisibility(View.GONE);

        int primaryColor = getPrimaryColor();
        LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();
        List<LatLng> polylinePoints = new ArrayList<>();

        for (int i = 0; i < geoMemories.size(); i++) {
            Memory m = geoMemories.get(i);
            LatLng position = new LatLng(m.getLatitude(), m.getLongitude());
            boundsBuilder.include(position);
            polylinePoints.add(position);

            Marker marker = googleMap.addMarker(new MarkerOptions()
                    .position(position)
                    .title(m.getTitle())
                    .snippet(buildSnippet(m))
                    .icon(createNumberedMarker(i + 1, primaryColor)));

            if (marker != null) marker.setTag(m);
        }

        float lineWidthPx = 5f * getResources().getDisplayMetrics().density;
        googleMap.addPolyline(new PolylineOptions()
                .addAll(polylinePoints)
                .width(lineWidthPx)
                .color(primaryColor)
                .geodesic(true));

        googleMap.setOnInfoWindowClickListener(marker -> {
            Object tag = marker.getTag();
            if (tag instanceof Memory) launchMemoryDetail((Memory) tag);
        });

        if (geoMemories.size() == 1) {
            googleMap.animateCamera(
                    CameraUpdateFactory.newLatLngZoom(polylinePoints.get(0), 14f));
        } else {
            try {
                googleMap.animateCamera(
                        CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), 150));
            } catch (Exception e) {
                googleMap.moveCamera(
                        CameraUpdateFactory.newLatLngZoom(polylinePoints.get(0), 10f));
            }
        }
    }

    private boolean hasValidCoords(Memory m) {
        return m.getLatitude() != null && m.getLongitude() != null
                && !(m.getLatitude() == 0.0 && m.getLongitude() == 0.0);
    }

    private String buildSnippet(Memory m) {
        StringBuilder sb = new StringBuilder();
        if (m.getVisitedAt() != null) {
            try {
                LocalDateTime dt = LocalDateTime.parse(m.getVisitedAt());
                sb.append(dt.format(
                        DateTimeFormatter.ofPattern(getString(R.string.map_date_format), Locale.FRENCH)));
            } catch (Exception e) { sb.append(m.getVisitedAt()); }
        }
        if (m.getDescription() != null && !m.getDescription().isEmpty()) {
            String desc = m.getDescription();
            sb.append("\n").append(desc.length() > 50 ? desc.substring(0, 47) + "…" : desc);
        }
        return sb.toString();
    }

    private BitmapDescriptor createNumberedMarker(int number, int color) {
        int sizePx = 80;
        Bitmap bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bmp);

        Paint circlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        circlePaint.setColor(color);
        canvas.drawCircle(sizePx / 2f, sizePx / 2f, sizePx / 2f, circlePaint);

        Paint strokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        strokePaint.setColor(Color.WHITE);
        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setStrokeWidth(5f);
        canvas.drawCircle(sizePx / 2f, sizePx / 2f, sizePx / 2f - 3f, strokePaint);

        Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(number >= 10 ? 28f : 34f);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTypeface(Typeface.DEFAULT_BOLD);
        Paint.FontMetrics fm = textPaint.getFontMetrics();
        float textY = sizePx / 2f - (fm.ascent + fm.descent) / 2f;
        canvas.drawText(String.valueOf(number), sizePx / 2f, textY, textPaint);

        return BitmapDescriptorFactory.fromBitmap(bmp);
    }

    private int getPrimaryColor() {
        TypedValue typedValue = new TypedValue();
        getTheme().resolveAttribute(com.google.android.material.R.attr.colorPrimary, typedValue, true);
        return typedValue.data;
    }

    // ── Session ───────────────────────────────────────────────────────────────

    private void handleUnauthorized() {
        sessionManager.logout();
        Toast.makeText(this, R.string.session_expired_msg, Toast.LENGTH_LONG).show();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
        finish();
    }
}
