package com.mahmoud.carnetvoyage.activity;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.mahmoud.carnetvoyage.R;
import com.mahmoud.carnetvoyage.api.ApiClient;
import com.mahmoud.carnetvoyage.api.ApiService;
import com.mahmoud.carnetvoyage.util.AnalyticsHelper;
import com.mahmoud.carnetvoyage.model.Trip;
import com.mahmoud.carnetvoyage.model.TripRequest;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CreateTripActivity extends AppCompatActivity {

    private TextInputLayout tilTitle;
    private TextInputEditText etTitle, etDescription, etStartDate, etEndDate;
    private MaterialButton btnSave;
    private ProgressBar progressBar;
    private ImageView ivBack;

    private ApiService apiService;
    private String startDateIso = null;   // "2026-07-15"
    private String endDateIso = null;

    private final DateTimeFormatter isoFmt = DateTimeFormatter.ISO_LOCAL_DATE;
    private final DateTimeFormatter displayFmt =
            DateTimeFormatter.ofPattern("d MMM yyyy", Locale.FRENCH);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_trip);

        apiService = ApiClient.getApiService(this);

        initViews();
        setupListeners();
    }

    private void initViews() {
        tilTitle = findViewById(R.id.tilTitle);
        etTitle = findViewById(R.id.etTitle);
        etDescription = findViewById(R.id.etDescription);
        etStartDate = findViewById(R.id.etStartDate);
        etEndDate = findViewById(R.id.etEndDate);
        btnSave = findViewById(R.id.btnSave);
        progressBar = findViewById(R.id.progressBar);
        ivBack = findViewById(R.id.ivBack);

        View topBar = findViewById(R.id.topBar);
        int basePaddingTop = topBar.getPaddingTop();
        ViewCompat.setOnApplyWindowInsetsListener(
                findViewById(R.id.rootLayout), (v, windowInsets) -> {
                    int topInset = windowInsets.getInsets(
                            WindowInsetsCompat.Type.systemBars()).top;
                    topBar.setPadding(topBar.getPaddingLeft(), basePaddingTop + topInset,
                                      topBar.getPaddingRight(), topBar.getPaddingBottom());
                    return WindowInsetsCompat.CONSUMED;
                });
    }

    private void setupListeners() {
        ivBack.setOnClickListener(v -> finish());

        etStartDate.setOnClickListener(v -> showDatePicker(true));
        etEndDate.setOnClickListener(v -> showDatePicker(false));

        btnSave.setOnClickListener(v -> attemptCreate());
    }

    private void showDatePicker(boolean isStart) {
        MaterialDatePicker<Long> picker = MaterialDatePicker.Builder.datePicker()
                .setTitleText(isStart ? "Date de début" : "Date de fin")
                .build();

        picker.addOnPositiveButtonClickListener(selection -> {
            // selection est en ms UTC midnight
            LocalDate date = Instant.ofEpochMilli(selection)
                    .atZone(ZoneOffset.UTC)
                    .toLocalDate();
            String iso = date.format(isoFmt);
            String pretty = date.format(displayFmt);

            if (isStart) {
                startDateIso = iso;
                etStartDate.setText(pretty);
            } else {
                endDateIso = iso;
                etEndDate.setText(pretty);
            }
        });

        picker.show(getSupportFragmentManager(), "DATE_PICKER");
    }

    private void attemptCreate() {
        tilTitle.setError(null);

        String title = etTitle.getText() == null ? "" : etTitle.getText().toString().trim();
        String description = etDescription.getText() == null ? "" : etDescription.getText().toString().trim();

        if (TextUtils.isEmpty(title)) {
            tilTitle.setError("Le titre est obligatoire");
            return;
        }

        // Vérif simple : si les 2 dates sont remplies, la fin >= début
        if (startDateIso != null && endDateIso != null) {
            if (LocalDate.parse(endDateIso, isoFmt).isBefore(LocalDate.parse(startDateIso, isoFmt))) {
                Toast.makeText(this, "La date de fin doit être après la date de début",
                        Toast.LENGTH_LONG).show();
                return;
            }
        }

        setLoading(true);

        TripRequest request = new TripRequest(
                title,
                description.isEmpty() ? null : description,
                startDateIso,
                endDateIso
        );

        apiService.createTrip(request).enqueue(new Callback<Trip>() {
            @Override
            public void onResponse(@NonNull Call<Trip> call, @NonNull Response<Trip> response) {
                setLoading(false);
                if (response.isSuccessful() && response.body() != null) {
                    AnalyticsHelper.logTripCreated(title);
                    Toast.makeText(CreateTripActivity.this,
                            "Voyage créé 🎉", Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK);
                    finish();
                } else {
                    Toast.makeText(CreateTripActivity.this,
                            "Erreur : code " + response.code(),
                            Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<Trip> call, @NonNull Throwable t) {
                setLoading(false);
                Toast.makeText(CreateTripActivity.this,
                        "Erreur réseau : " + t.getMessage(),
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnSave.setEnabled(!loading);
    }
}