package com.mahmoud.carnetvoyage;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
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
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;
import com.mahmoud.carnetvoyage.activity.CreateTripActivity;
import com.mahmoud.carnetvoyage.activity.GlobalMapActivity;
import com.mahmoud.carnetvoyage.activity.LoginActivity;
import com.mahmoud.carnetvoyage.activity.TripDetailActivity;
import com.mahmoud.carnetvoyage.adapter.TripAdapter;
import com.mahmoud.carnetvoyage.api.ApiClient;
import com.mahmoud.carnetvoyage.api.ApiService;
import com.mahmoud.carnetvoyage.model.Trip;
import com.mahmoud.carnetvoyage.util.AnalyticsHelper;
import com.mahmoud.carnetvoyage.util.ApiErrorHandler;
import com.mahmoud.carnetvoyage.util.NetworkUtils;
import com.mahmoud.carnetvoyage.util.SessionManager;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity implements TripAdapter.OnTripClickListener {

    // ── Vues ─────────────────────────────────────────────────────────────────
    private TextView tvWelcome;
    private ImageView btnGlobalMap;
    private TabLayout tabLayout;
    private RecyclerView recyclerTrips;
    private SwipeRefreshLayout swipeRefresh;
    private ShimmerFrameLayout shimmerLayout;
    private View emptyState;
    private TextView tvEmptyTitle;
    private TextView tvEmptySubtitle;
    private FloatingActionButton fabAddTrip;

    // ── État ──────────────────────────────────────────────────────────────────
    private SessionManager sessionManager;
    private ApiService apiService;
    private TripAdapter adapter;
    private final List<Trip> trips = new ArrayList<>();

    /** 0 = Mes voyages, 1 = Partagés avec moi */
    private int currentTab = 0;

    /** Rafraîchit la liste si un voyage a été créé avec succès */
    private final ActivityResultLauncher<Intent> createTripLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    loadCurrentTab();
                }
            }
    );

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sessionManager = new SessionManager(this);
        if (!sessionManager.isLoggedIn()) {
            redirectToLogin();
            return;
        }

        setContentView(R.layout.activity_main);
        AnalyticsHelper.init(this);
        apiService = ApiClient.getApiService(this);

        initViews();
        setupRecyclerView();
        setupListeners();
        loadCurrentTab();
    }

    // ── Initialisation ────────────────────────────────────────────────────────

    private void initViews() {
        tvWelcome     = findViewById(R.id.tvWelcome);
        btnGlobalMap  = findViewById(R.id.btnGlobalMap);
        tabLayout     = findViewById(R.id.tabLayout);
        recyclerTrips = findViewById(R.id.recyclerTrips);
        swipeRefresh  = findViewById(R.id.swipeRefresh);
        shimmerLayout = findViewById(R.id.shimmerLayout);
        emptyState    = findViewById(R.id.emptyState);
        tvEmptyTitle  = findViewById(R.id.tvEmptyTitle);
        tvEmptySubtitle = findViewById(R.id.tvEmptySubtitle);
        fabAddTrip    = findViewById(R.id.fabAddTrip);

        tvWelcome.setText(getString(R.string.welcome_user, sessionManager.getUsername()));

        // ── Edge-to-edge (API 35+) ────────────────────────────────────────────────
        // Décale le contenu de la top bar sous la status bar sans modifier le fond violet.
        // basePaddingTop est capturé UNE FOIS depuis le XML (16dp en px) : immunisé contre
        // les rotations, car l'Activity recrée la vue et relit la valeur XML à chaque fois.
        View topBar = findViewById(R.id.topBar);
        int basePaddingTop = topBar.getPaddingTop();
        ViewCompat.setOnApplyWindowInsetsListener(topBar, (v, windowInsets) -> {
            int topInset = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars()).top;
            v.setPadding(v.getPaddingLeft(), basePaddingTop + topInset,
                         v.getPaddingRight(), v.getPaddingBottom());
            return WindowInsetsCompat.CONSUMED;
        });
    }

    private void setupRecyclerView() {
        adapter = new TripAdapter(trips, this);
        recyclerTrips.setLayoutManager(new LinearLayoutManager(this));
        recyclerTrips.setAdapter(adapter);
    }

    private void setupListeners() {
        // Bouton déconnexion : demande confirmation avant de déconnecter
        findViewById(R.id.btnLogout).setOnClickListener(v -> showLogoutDialog());

        // Bouton carte globale
        btnGlobalMap.setOnClickListener(v -> {
            Intent intent = new Intent(this, GlobalMapActivity.class);
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        });

        swipeRefresh.setOnRefreshListener(() -> {
            if (!NetworkUtils.isNetworkAvailable(this)) {
                swipeRefresh.setRefreshing(false);
                NetworkUtils.showNoNetworkSnackbar(findViewById(android.R.id.content));
                return;
            }
            loadCurrentTab();
        });

        fabAddTrip.setOnClickListener(v -> {
            Intent intent = new Intent(this, CreateTripActivity.class);
            createTripLauncher.launch(intent);
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        });

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                currentTab = tab.getPosition();

                // Vider la liste immédiatement pour éviter l'affichage résiduel
                trips.clear();
                adapter.notifyDataSetChanged();
                emptyState.setVisibility(View.GONE);

                if (currentTab == 0) {
                    fabAddTrip.setVisibility(View.VISIBLE);
                    tvEmptyTitle.setText(R.string.no_trips_title);
                    tvEmptySubtitle.setText(R.string.no_trips_subtitle);
                } else {
                    fabAddTrip.setVisibility(View.GONE);
                    tvEmptyTitle.setText(R.string.no_shared_trips_title);
                    tvEmptySubtitle.setText(R.string.no_shared_trips_subtitle);
                }

                loadCurrentTab();
            }

            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    // ── Chargement des données ────────────────────────────────────────────────

    private void loadCurrentTab() {
        if (!NetworkUtils.isNetworkAvailable(this)) {
            swipeRefresh.setRefreshing(false);
            NetworkUtils.showNoNetworkSnackbar(findViewById(android.R.id.content));
            return;
        }

        // Shimmer : affiché seulement au premier chargement (liste vide)
        if (trips.isEmpty()) {
            shimmerLayout.setVisibility(View.VISIBLE);
            shimmerLayout.startShimmer();
        }

        Call<List<Trip>> call = (currentTab == 0)
                ? apiService.getMyTrips()
                : apiService.getSharedTrips();

        call.enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<List<Trip>> call,
                                   @NonNull Response<List<Trip>> response) {
                stopShimmer();
                swipeRefresh.setRefreshing(false);

                if (response.isSuccessful() && response.body() != null) {
                    trips.clear();
                    trips.addAll(response.body());
                    adapter.notifyDataSetChanged();
                    emptyState.setVisibility(trips.isEmpty() ? View.VISIBLE : View.GONE);
                } else if (ApiErrorHandler.isUnauthorized(response)) {
                    handleUnauthorized();
                } else {
                    Toast.makeText(MainActivity.this,
                            ApiErrorHandler.parseError(response), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<Trip>> call, @NonNull Throwable t) {
                stopShimmer();
                swipeRefresh.setRefreshing(false);
                Toast.makeText(MainActivity.this,
                        getString(R.string.network_error_msg, t.getMessage()), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void stopShimmer() {
        shimmerLayout.stopShimmer();
        shimmerLayout.setVisibility(View.GONE);
    }

    // ── Interactions avec les voyages ─────────────────────────────────────────

    @Override
    public void onTripClick(Trip trip) {
        Intent intent = new Intent(this, TripDetailActivity.class);
        intent.putExtra(TripDetailActivity.EXTRA_TRIP_ID, trip.getId());
        startActivity(intent);
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
    }

    @Override
    public void onTripLongClick(Trip trip) {
        if (!trip.isOwner()) {
            Toast.makeText(this, R.string.not_owner_msg, Toast.LENGTH_SHORT).show();
            return;
        }
        showDeleteDialog(trip);
    }

    private void showDeleteDialog(Trip trip) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.delete_trip_confirm_title)
                .setMessage(getString(R.string.delete_trip_confirm_message, trip.getTitle()))
                .setNegativeButton(R.string.cancel_button, null)
                .setPositiveButton(R.string.delete_button, (dialog, which) -> deleteTrip(trip))
                .show();
    }

    private void deleteTrip(Trip trip) {
        if (!NetworkUtils.isNetworkAvailable(this)) {
            NetworkUtils.showNoNetworkSnackbar(findViewById(android.R.id.content));
            return;
        }
        apiService.deleteTrip(trip.getId()).enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(MainActivity.this,
                            R.string.trip_deleted_success, Toast.LENGTH_SHORT).show();
                    loadCurrentTab();
                } else if (ApiErrorHandler.isUnauthorized(response)) {
                    handleUnauthorized();
                } else {
                    Toast.makeText(MainActivity.this,
                            ApiErrorHandler.parseError(response), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                Toast.makeText(MainActivity.this,
                        getString(R.string.network_error_msg, t.getMessage()), Toast.LENGTH_LONG).show();
            }
        });
    }

    // ── Navigation & Session ──────────────────────────────────────────────────

    /** Dialogue de confirmation avant déconnexion */
    private void showLogoutDialog() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.logout_confirm_title)
                .setMessage(R.string.logout_confirm_message)
                .setNegativeButton(R.string.cancel_button, null)
                .setPositiveButton(R.string.logout_button, (dialog, which) -> {
                    sessionManager.logout();
                    redirectToLogin();
                })
                .show();
    }

    /** Déconnexion automatique sur 401 (token expiré) */
    private void handleUnauthorized() {
        sessionManager.logout();
        Toast.makeText(this, R.string.session_expired_msg, Toast.LENGTH_LONG).show();
        redirectToLogin();
    }

    private void redirectToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
        finish();
    }
}
