package com.mahmoud.carnetvoyage.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.mahmoud.carnetvoyage.R;
import com.mahmoud.carnetvoyage.adapter.ShareAdapter;
import com.mahmoud.carnetvoyage.api.ApiClient;
import com.mahmoud.carnetvoyage.api.ApiService;
import com.mahmoud.carnetvoyage.model.ShareRequest;
import com.mahmoud.carnetvoyage.model.ShareResponse;
import com.mahmoud.carnetvoyage.util.AnalyticsHelper;
import com.mahmoud.carnetvoyage.util.ApiErrorHandler;
import com.mahmoud.carnetvoyage.util.NetworkUtils;
import com.mahmoud.carnetvoyage.util.SessionManager;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ShareTripActivity extends AppCompatActivity
        implements ShareAdapter.OnUnshareClickListener {

    public static final String EXTRA_TRIP_ID    = "extra_trip_id";
    public static final String EXTRA_TRIP_TITLE = "extra_trip_title";

    // Vues
    private TextView tvActivityTitle;
    private TextInputEditText etEmail;
    private MaterialButton btnShare;
    private SwipeRefreshLayout swipeRefresh;
    private TextView tvSharesHeader;
    private RecyclerView recyclerShares;
    private TextView tvEmptyShares;

    // État
    private ApiService apiService;
    private SessionManager sessionManager;
    private Long tripId;
    private final List<ShareResponse> shares = new ArrayList<>();
    private ShareAdapter shareAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        tripId = getIntent().getLongExtra(EXTRA_TRIP_ID, -1L);
        if (tripId == -1L) { finish(); return; }

        String tripTitle = getIntent().getStringExtra(EXTRA_TRIP_TITLE);

        setContentView(R.layout.activity_share_trip);

        apiService = ApiClient.getApiService(this);
        sessionManager = new SessionManager(this);

        initViews(tripTitle);
        setupRecyclerView();
        setupListeners();
        loadShares();
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
    }

    private void initViews(String tripTitle) {
        tvActivityTitle = findViewById(R.id.tvActivityTitle);
        etEmail         = findViewById(R.id.etEmail);
        btnShare        = findViewById(R.id.btnShare);
        swipeRefresh    = findViewById(R.id.swipeRefresh);
        tvSharesHeader  = findViewById(R.id.tvSharesHeader);
        recyclerShares  = findViewById(R.id.recyclerShares);
        tvEmptyShares   = findViewById(R.id.tvEmptyShares);

        tvActivityTitle.setText("Partager : " + (tripTitle != null ? tripTitle : ""));
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
    }

    private void setupRecyclerView() {
        shareAdapter = new ShareAdapter(shares, this);
        recyclerShares.setLayoutManager(new LinearLayoutManager(this));
        recyclerShares.setAdapter(shareAdapter);
    }

    private void setupListeners() {
        btnShare.setOnClickListener(v -> onShareClicked());

        swipeRefresh.setOnRefreshListener(() -> {
            if (!NetworkUtils.isNetworkAvailable(this)) {
                swipeRefresh.setRefreshing(false);
                NetworkUtils.showNoNetworkSnackbar(findViewById(android.R.id.content));
                return;
            }
            loadShares();
        });
    }

    // ── Chargement de la liste des partages ───────────────────────────────────

    private void loadShares() {
        if (!NetworkUtils.isNetworkAvailable(this)) {
            swipeRefresh.setRefreshing(false);
            NetworkUtils.showNoNetworkSnackbar(findViewById(android.R.id.content));
            return;
        }

        apiService.getSharesOfTrip(tripId).enqueue(new Callback<List<ShareResponse>>() {
            @Override
            public void onResponse(@NonNull Call<List<ShareResponse>> call,
                                   @NonNull Response<List<ShareResponse>> response) {
                swipeRefresh.setRefreshing(false);

                if (response.isSuccessful() && response.body() != null) {
                    shares.clear();
                    shares.addAll(response.body());
                    shareAdapter.notifyDataSetChanged();
                    updateSharesHeader();
                    tvEmptyShares.setVisibility(shares.isEmpty() ? View.VISIBLE : View.GONE);
                } else if (ApiErrorHandler.isUnauthorized(response)) {
                    handleUnauthorized();
                } else {
                    Toast.makeText(ShareTripActivity.this,
                            ApiErrorHandler.parseError(response), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<ShareResponse>> call, @NonNull Throwable t) {
                swipeRefresh.setRefreshing(false);
                Toast.makeText(ShareTripActivity.this,
                        "Erreur réseau : " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void updateSharesHeader() {
        int n = shares.size();
        tvSharesHeader.setText("🤝 Partagé avec (" + n + (n <= 1 ? " personne)" : " personnes)"));
    }

    // ── Partager avec un utilisateur ──────────────────────────────────────────

    private void onShareClicked() {
        String email = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";

        if (email.isEmpty()) {
            etEmail.setError("Entrez une adresse email");
            etEmail.requestFocus();
            return;
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Format d'email invalide");
            etEmail.requestFocus();
            return;
        }

        if (!NetworkUtils.isNetworkAvailable(this)) {
            NetworkUtils.showNoNetworkSnackbar(findViewById(android.R.id.content));
            return;
        }

        hideKeyboard();
        setLoading(true);

        apiService.shareTrip(tripId, new ShareRequest(email)).enqueue(new Callback<ShareResponse>() {
            @Override
            public void onResponse(@NonNull Call<ShareResponse> call,
                                   @NonNull Response<ShareResponse> response) {
                setLoading(false);

                if (response.isSuccessful()) {
                    AnalyticsHelper.logTripShared();
                    etEmail.setText("");
                    Toast.makeText(ShareTripActivity.this,
                            "Voyage partagé avec succès ✅", Toast.LENGTH_SHORT).show();
                    loadShares();
                } else if (ApiErrorHandler.isUnauthorized(response)) {
                    handleUnauthorized();
                } else {
                    showShareError(response.code());
                }
            }

            @Override
            public void onFailure(@NonNull Call<ShareResponse> call, @NonNull Throwable t) {
                setLoading(false);
                Toast.makeText(ShareTripActivity.this,
                        "Erreur réseau : " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void showShareError(int code) {
        String message;
        switch (code) {
            case 400: message = "Vous ne pouvez pas partager un voyage avec vous-même."; break;
            case 403: message = "Vous n'êtes pas le propriétaire de ce voyage."; break;
            case 404: message = "Aucun utilisateur trouvé avec cet email."; break;
            case 409: message = "Ce voyage est déjà partagé avec cet utilisateur."; break;
            default:  message = "Erreur inattendue (code " + code + ").";
        }
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    // ── Retirer un partage ────────────────────────────────────────────────────

    @Override
    public void onUnshare(ShareResponse share) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Retirer l'accès ?")
                .setMessage("Retirer l'accès à " + share.getSharedWithUsername()
                        + " (" + share.getSharedWithEmail() + ") ?\n\n"
                        + "Cette personne ne pourra plus voir ce voyage.")
                .setNegativeButton("Annuler", null)
                .setPositiveButton("Retirer", (dialog, which) -> unshareUser(share))
                .show();
    }

    private void unshareUser(ShareResponse share) {
        if (!NetworkUtils.isNetworkAvailable(this)) {
            NetworkUtils.showNoNetworkSnackbar(findViewById(android.R.id.content));
            return;
        }
        setLoading(true);

        apiService.unshareTrip(tripId, share.getSharedWithUserId()).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                setLoading(false);
                if (response.isSuccessful()) {
                    Toast.makeText(ShareTripActivity.this,
                            "Accès retiré à " + share.getSharedWithUsername(),
                            Toast.LENGTH_SHORT).show();
                    loadShares();
                } else if (ApiErrorHandler.isUnauthorized(response)) {
                    handleUnauthorized();
                } else {
                    Toast.makeText(ShareTripActivity.this,
                            ApiErrorHandler.parseError(response), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                setLoading(false);
                Toast.makeText(ShareTripActivity.this,
                        "Erreur réseau : " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    // ── Utilitaires ───────────────────────────────────────────────────────────

    private void setLoading(boolean loading) {
        btnShare.setEnabled(!loading);
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        View focus = getCurrentFocus();
        if (imm != null && focus != null) {
            imm.hideSoftInputFromWindow(focus.getWindowToken(), 0);
        }
    }

    private void handleUnauthorized() {
        sessionManager.logout();
        Toast.makeText(this, "Session expirée. Veuillez vous reconnecter.", Toast.LENGTH_LONG).show();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
        finish();
    }
}
