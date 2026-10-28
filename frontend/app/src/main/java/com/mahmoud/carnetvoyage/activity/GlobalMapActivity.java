package com.mahmoud.carnetvoyage.activity;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.mahmoud.carnetvoyage.R;
import com.mahmoud.carnetvoyage.adapter.TripLegendAdapter;
import com.mahmoud.carnetvoyage.api.ApiClient;
import com.mahmoud.carnetvoyage.api.ApiService;
import com.mahmoud.carnetvoyage.model.Memory;
import com.mahmoud.carnetvoyage.model.Trip;
import com.mahmoud.carnetvoyage.util.AnalyticsHelper;
import com.mahmoud.carnetvoyage.util.ApiErrorHandler;
import com.mahmoud.carnetvoyage.util.MapColorUtils;
import com.mahmoud.carnetvoyage.util.NetworkUtils;
import com.mahmoud.carnetvoyage.util.SessionManager;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class GlobalMapActivity extends AppCompatActivity implements OnMapReadyCallback {

    // ── Vues ─────────────────────────────────────────────────────────────────
    private ProgressBar progressBar;
    private View emptyState;
    private View cardCounter;
    private TextView tvCounterMemories;
    private TextView tvCounterTrips;
    private ExtendedFloatingActionButton fabLegend;

    // ── État ──────────────────────────────────────────────────────────────────
    private ApiService apiService;
    private SessionManager sessionManager;
    private GoogleMap googleMap;
    private boolean mapReady = false;

    /**
     * Clé : Trip, Valeur : liste des souvenirs géolocalisés de ce voyage.
     * Remplie au fur et à mesure des appels getMemoriesByTrip().
     */
    private final Map<Trip, List<Memory>> tripMemoriesMap = new HashMap<>();

    /**
     * Indique que toutes les données ont été chargées et que la carte peut être dessinée.
     */
    private boolean dataReady = false;

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_global_map);
        AnalyticsHelper.logGlobalMapViewed();

        sessionManager = new SessionManager(this);
        apiService = ApiClient.getApiService(this);

        initViews();
        initMap();
        loadAllData();
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
    }

    // ── Initialisation ────────────────────────────────────────────────────────

    private void initViews() {
        progressBar      = findViewById(R.id.progressBar);
        emptyState       = findViewById(R.id.emptyState);
        cardCounter      = findViewById(R.id.cardCounter);
        tvCounterMemories = findViewById(R.id.tvCounterMemories);
        tvCounterTrips   = findViewById(R.id.tvCounterTrips);
        fabLegend        = findViewById(R.id.fabLegend);

        View btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());
        fabLegend.setOnClickListener(v -> showLegendBottomSheet());

        // ── Edge-to-edge (API 35+) ────────────────────────────────────────────────
        // La carte reste plein écran (immersif). On pousse uniquement les éléments
        // flottants en haut (btnBack, cardCounter) sous la status bar.
        // Listener sur la root FrameLayout : garantit la réception des insets
        // même sans CoordinatorLayout parent.
        int baseBtnBackMarginTop =
                ((ViewGroup.MarginLayoutParams) btnBack.getLayoutParams()).topMargin;
        int baseCardCounterMarginTop =
                ((ViewGroup.MarginLayoutParams) cardCounter.getLayoutParams()).topMargin;

        ViewCompat.setOnApplyWindowInsetsListener(
                findViewById(R.id.rootLayout), (v, windowInsets) -> {
                    int topInset = windowInsets.getInsets(
                            WindowInsetsCompat.Type.systemBars()).top;

                    ViewGroup.MarginLayoutParams btnBackLp =
                            (ViewGroup.MarginLayoutParams) btnBack.getLayoutParams();
                    btnBackLp.topMargin = baseBtnBackMarginTop + topInset;
                    btnBack.setLayoutParams(btnBackLp);

                    ViewGroup.MarginLayoutParams cardCounterLp =
                            (ViewGroup.MarginLayoutParams) cardCounter.getLayoutParams();
                    cardCounterLp.topMargin = baseCardCounterMarginTop + topInset;
                    cardCounter.setLayoutParams(cardCounterLp);

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

    // ── Chargement séquentiel des données ────────────────────────────────────

    private void loadAllData() {
        if (!NetworkUtils.isNetworkAvailable(this)) {
            NetworkUtils.showNoNetworkSnackbar(findViewById(android.R.id.content));
            return;
        }

        progressBar.setVisibility(View.VISIBLE);

        // Drapeaux pour attendre les deux listes en parallèle
        final boolean[] myTripsLoaded     = { false };
        final boolean[] sharedTripsLoaded = { false };
        final List<Trip> allTrips = new ArrayList<>();

        Runnable onBothLoaded = () -> {
            if (!myTripsLoaded[0] || !sharedTripsLoaded[0]) return;
            // Déduplique (un trip partagé peut apparaître dans les deux listes — en pratique
            // le backend les sépare, mais on garde la logique défensive)
            if (allTrips.isEmpty()) {
                progressBar.setVisibility(View.GONE);
                showEmptyState();
                return;
            }
            loadMemoriesForAllTrips(allTrips);
        };

        // ── Mes voyages ───────────────────────────────────────────────────────
        apiService.getMyTrips().enqueue(new Callback<List<Trip>>() {
            @Override
            public void onResponse(@NonNull Call<List<Trip>> call,
                                   @NonNull Response<List<Trip>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    allTrips.addAll(response.body());
                }
                myTripsLoaded[0] = true;
                onBothLoaded.run();
            }

            @Override
            public void onFailure(@NonNull Call<List<Trip>> call, @NonNull Throwable t) {
                myTripsLoaded[0] = true;
                onBothLoaded.run();
            }
        });

        // ── Voyages partagés ──────────────────────────────────────────────────
        apiService.getSharedTrips().enqueue(new Callback<List<Trip>>() {
            @Override
            public void onResponse(@NonNull Call<List<Trip>> call,
                                   @NonNull Response<List<Trip>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    allTrips.addAll(response.body());
                }
                sharedTripsLoaded[0] = true;
                onBothLoaded.run();
            }

            @Override
            public void onFailure(@NonNull Call<List<Trip>> call, @NonNull Throwable t) {
                sharedTripsLoaded[0] = true;
                onBothLoaded.run();
            }
        });
    }

    /**
     * Pour chaque voyage, charge ses souvenirs.
     * Déclenche displayOnMap() quand tous les appels sont terminés.
     */
    private void loadMemoriesForAllTrips(List<Trip> trips) {
        int total = trips.size();
        AtomicInteger completed = new AtomicInteger(0);

        for (Trip trip : trips) {
            apiService.getMemoriesByTrip(trip.getId()).enqueue(new Callback<List<Memory>>() {
                @Override
                public void onResponse(@NonNull Call<List<Memory>> call,
                                       @NonNull Response<List<Memory>> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        List<Memory> geoMemories = filterGeoMemories(response.body());
                        if (!geoMemories.isEmpty()) {
                            tripMemoriesMap.put(trip, geoMemories);
                        }
                    }
                    if (completed.incrementAndGet() == total) {
                        progressBar.setVisibility(View.GONE);
                        dataReady = true;
                        if (mapReady) displayOnMap();
                    }
                }

                @Override
                public void onFailure(@NonNull Call<List<Memory>> call, @NonNull Throwable t) {
                    if (completed.incrementAndGet() == total) {
                        progressBar.setVisibility(View.GONE);
                        dataReady = true;
                        if (mapReady) displayOnMap();
                    }
                }
            });
        }
    }

    /** Filtre les souvenirs ayant des coordonnées GPS valides */
    private List<Memory> filterGeoMemories(List<Memory> all) {
        List<Memory> result = new ArrayList<>();
        for (Memory m : all) {
            if (m.getLatitude() != null && m.getLongitude() != null
                    && !(m.getLatitude() == 0.0 && m.getLongitude() == 0.0)) {
                result.add(m);
            }
        }
        return result;
    }

    // ── Google Maps ───────────────────────────────────────────────────────────

    @Override
    public void onMapReady(@NonNull GoogleMap map) {
        googleMap = map;
        googleMap.getUiSettings().setZoomControlsEnabled(true);
        googleMap.getUiSettings().setMapToolbarEnabled(false);

        // InfoWindow personnalisée
        googleMap.setInfoWindowAdapter(new GlobalInfoWindowAdapter());

        mapReady = true;
        if (dataReady) displayOnMap();
    }

    private void displayOnMap() {
        if (googleMap == null) return;

        if (tripMemoriesMap.isEmpty()) {
            showEmptyState();
            return;
        }

        googleMap.clear();

        int totalMemories = 0;
        LatLngBounds.Builder globalBounds = new LatLngBounds.Builder();
        boolean hasAnyPoint = false;

        for (Map.Entry<Trip, List<Memory>> entry : tripMemoriesMap.entrySet()) {
            Trip trip = entry.getKey();
            List<Memory> memories = entry.getValue();
            float hue = MapColorUtils.getHueForTripId(trip.getId());

            for (Memory memory : memories) {
                LatLng pos = new LatLng(memory.getLatitude(), memory.getLongitude());
                globalBounds.include(pos);
                hasAnyPoint = true;

                // Tag = tableau {Memory, Trip} pour l'InfoWindow
                Marker marker = googleMap.addMarker(new MarkerOptions()
                        .position(pos)
                        .title(memory.getTitle())
                        .icon(BitmapDescriptorFactory.defaultMarker(hue)));

                if (marker != null) {
                    marker.setTag(new Object[]{ memory, trip });
                }

                totalMemories++;
            }
        }

        // Compteur
        int tripCount = tripMemoriesMap.size();
        tvCounterMemories.setText("📊 " + totalMemories
                + (totalMemories <= 1 ? " souvenir" : " souvenirs"));
        tvCounterTrips.setText("dans " + tripCount
                + (tripCount <= 1 ? " voyage" : " voyages"));
        cardCounter.setVisibility(View.VISIBLE);
        fabLegend.setVisibility(View.VISIBLE);

        // Centrer la caméra
        if (hasAnyPoint) {
            try {
                final LatLngBounds bounds = globalBounds.build();
                googleMap.setOnMapLoadedCallback(() ->
                        googleMap.animateCamera(
                                CameraUpdateFactory.newLatLngBounds(bounds, 120)));
            } catch (Exception e) {
                // Fallback si un seul point
                LatLng single = new LatLng(
                        tripMemoriesMap.values().iterator().next().get(0).getLatitude(),
                        tripMemoriesMap.values().iterator().next().get(0).getLongitude());
                googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(single, 10f));
            }
        }
    }

    private void showEmptyState() {
        runOnUiThread(() -> emptyState.setVisibility(View.VISIBLE));
    }

    // ── InfoWindow personnalisée ──────────────────────────────────────────────

    private class GlobalInfoWindowAdapter implements GoogleMap.InfoWindowAdapter {

        private final View windowView;

        GlobalInfoWindowAdapter() {
            windowView = LayoutInflater.from(GlobalMapActivity.this)
                    .inflate(R.layout.info_window_global, null);
        }

        @Nullable
        @Override
        public View getInfoWindow(@NonNull Marker marker) {
            bindMarkerData(marker, windowView);
            return windowView;
        }

        @Nullable
        @Override
        public View getInfoContents(@NonNull Marker marker) {
            return null; // on utilise getInfoWindow
        }

        private void bindMarkerData(Marker marker, View view) {
            Object tag = marker.getTag();
            if (!(tag instanceof Object[])) return;
            Object[] data = (Object[]) tag;
            if (data.length < 2) return;

            Memory memory = (Memory) data[0];
            Trip trip     = (Trip) data[1];

            TextView tvMemoryTitle = view.findViewById(R.id.tvMemoryTitle);
            TextView tvTripName    = view.findViewById(R.id.tvTripName);
            TextView tvVisitedAt   = view.findViewById(R.id.tvVisitedAt);

            tvMemoryTitle.setText(memory.getTitle() != null ? memory.getTitle() : "");
            tvTripName.setText("✈️ " + (trip.getTitle() != null ? trip.getTitle() : ""));
            tvVisitedAt.setText(formatVisitedAt(memory.getVisitedAt()));
        }

        private String formatVisitedAt(String visitedAt) {
            if (visitedAt == null) return "";
            try {
                LocalDateTime dt = LocalDateTime.parse(visitedAt);
                return dt.format(DateTimeFormatter.ofPattern(
                        "d MMM yyyy 'à' HH'h'mm", Locale.FRENCH));
            } catch (Exception e) {
                return visitedAt;
            }
        }
    }

    // ── BottomSheet Légende ───────────────────────────────────────────────────

    private void showLegendBottomSheet() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View sheetView = LayoutInflater.from(this)
                .inflate(R.layout.bottom_sheet_trips_legend, null);

        RecyclerView recycler = sheetView.findViewById(R.id.recyclerLegend);
        recycler.setLayoutManager(new LinearLayoutManager(this));

        // Construire la liste des entrées pour l'adapter
        List<TripLegendAdapter.TripEntry> entries = new ArrayList<>();
        for (Map.Entry<Trip, List<Memory>> e : tripMemoriesMap.entrySet()) {
            entries.add(new TripLegendAdapter.TripEntry(e.getKey(), e.getValue()));
        }
        // Trier par titre de voyage
        entries.sort((a, b) -> {
            String ta = a.trip.getTitle() != null ? a.trip.getTitle() : "";
            String tb = b.trip.getTitle() != null ? b.trip.getTitle() : "";
            return ta.compareToIgnoreCase(tb);
        });

        TripLegendAdapter adapter = new TripLegendAdapter(entries, entry -> {
            dialog.dismiss();
            zoomToTrip(entry);
        });
        recycler.setAdapter(adapter);

        dialog.setContentView(sheetView);
        dialog.show();
    }

    /** Anime la caméra sur les marqueurs du voyage sélectionné */
    private void zoomToTrip(TripLegendAdapter.TripEntry entry) {
        if (googleMap == null || entry.memories.isEmpty()) return;

        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        for (Memory m : entry.memories) {
            builder.include(new LatLng(m.getLatitude(), m.getLongitude()));
        }
        try {
            LatLngBounds bounds = builder.build();
            googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 150));
        } catch (Exception e) {
            Memory first = entry.memories.get(0);
            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(
                    new LatLng(first.getLatitude(), first.getLongitude()), 12f));
        }
    }

    // ── Session ───────────────────────────────────────────────────────────────

    private void handleUnauthorized() {
        sessionManager.logout();
        Toast.makeText(this, "Session expirée. Veuillez vous reconnecter.",
                Toast.LENGTH_LONG).show();
        finish();
    }
}
