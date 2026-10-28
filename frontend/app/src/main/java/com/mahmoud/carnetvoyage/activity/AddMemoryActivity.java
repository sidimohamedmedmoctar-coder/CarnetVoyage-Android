package com.mahmoud.carnetvoyage.activity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;
import com.mahmoud.carnetvoyage.R;
import com.mahmoud.carnetvoyage.api.ApiClient;
import com.mahmoud.carnetvoyage.api.ApiService;
import com.mahmoud.carnetvoyage.model.Memory;
import com.mahmoud.carnetvoyage.model.MemoryRequest;
import com.mahmoud.carnetvoyage.model.UploadResponse;
import com.mahmoud.carnetvoyage.util.AnalyticsHelper;
import com.mahmoud.carnetvoyage.util.ApiErrorHandler;
import com.mahmoud.carnetvoyage.util.NetworkUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AddMemoryActivity extends AppCompatActivity {

    public static final String EXTRA_TRIP_ID = "extra_trip_id";

    // Vues
    private ImageView ivPhotoPreview;
    private TextInputLayout tilDescription;
    private TextInputEditText etTitle;
    private TextInputEditText etDescription;
    private TextView tvSelectedDateTime;
    private TextView tvGpsCoords;
    private MaterialButton btnSave;
    private ProgressBar progressBar;

    // État
    private Long tripId;
    private Uri cameraPhotoUri;       // URI content:// du fichier créé pour la caméra
    private Uri selectedPhotoUri;     // URI de la photo sélectionnée (caméra ou galerie)
    private String uploadedPhotoUrl;  // URL retournée par le backend après upload

    private LocalDate selectedDate;
    private LocalTime selectedTime;

    private Double currentLatitude;
    private Double currentLongitude;

    // API & GPS
    private ApiService apiService;
    private FusedLocationProviderClient fusedLocationClient;

    // ── ActivityResult launchers (API modernes, pas onActivityResult) ──────────

    /** Demande la permission ACCESS_FINE_LOCATION */
    private final ActivityResultLauncher<String> locationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) {
                    fetchLocation();
                } else {
                    Toast.makeText(this,
                            "Permission GPS refusée. Entrez la position manuellement.",
                            Toast.LENGTH_LONG).show();
                }
            });

    /** Demande la permission RECORD_AUDIO puis lance la dictée vocale */
    private final ActivityResultLauncher<String> micPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) {
                    startSpeechRecognition();
                } else {
                    Toast.makeText(this,
                            "Permission microphone refusée.",
                            Toast.LENGTH_SHORT).show();
                }
            });

    /** Demande la permission CAMERA puis ouvre la caméra */
    private final ActivityResultLauncher<String> cameraPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) {
                    launchCamera();
                } else {
                    Toast.makeText(this,
                            "Permission caméra refusée.", Toast.LENGTH_SHORT).show();
                }
            });

    /** Ouvre la caméra et écrit la photo dans cameraPhotoUri */
    private final ActivityResultLauncher<Uri> takePictureLauncher =
            registerForActivityResult(new ActivityResultContracts.TakePicture(), success -> {
                if (success && cameraPhotoUri != null) {
                    selectedPhotoUri = cameraPhotoUri;
                    uploadedPhotoUrl = null; // reset : il faudra uploader la nouvelle photo
                    Glide.with(this).load(selectedPhotoUri).centerCrop().into(ivPhotoPreview);
                }
            });

    /** Ouvre la galerie (PickVisualMedia = API moderne, remplace ACTION_PICK) */
    private final ActivityResultLauncher<PickVisualMediaRequest> pickMediaLauncher =
            registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), uri -> {
                if (uri != null) {
                    selectedPhotoUri = uri;
                    uploadedPhotoUrl = null;
                    Glide.with(this).load(uri).centerCrop().into(ivPhotoPreview);
                }
            });

    /** Lance la reconnaissance vocale et insère le résultat dans la description */
    private final ActivityResultLauncher<Intent> speechLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    ArrayList<String> matches = result.getData()
                            .getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                    if (matches != null && !matches.isEmpty()) {
                        String recognized = matches.get(0);
                        String current = etDescription.getText() != null
                                ? etDescription.getText().toString().trim() : "";
                        etDescription.setText(
                                current.isEmpty() ? recognized : current + " " + recognized);
                        etDescription.setSelection(etDescription.getText().length());
                    }
                }
            });

    // ──────────────────────────────────────────────────────────────────────────

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        tripId = getIntent().getLongExtra(EXTRA_TRIP_ID, -1L);
        if (tripId == -1L) {
            finish();
            return;
        }

        setContentView(R.layout.activity_add_memory);
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);

        apiService = ApiClient.getApiService(this);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        initViews();
        initDefaultDateTime();
        setupListeners();

        // Récupérer la position GPS dès l'ouverture
        requestLocationPermissionAndFetch();
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
    }

    private void initViews() {
        ivPhotoPreview = findViewById(R.id.ivPhotoPreview);
        tilDescription = findViewById(R.id.tilDescription);
        etTitle = findViewById(R.id.etTitle);
        etDescription = findViewById(R.id.etDescription);
        tvSelectedDateTime = findViewById(R.id.tvSelectedDateTime);
        tvGpsCoords = findViewById(R.id.tvGpsCoords);
        btnSave = findViewById(R.id.btnSave);
        progressBar = findViewById(R.id.progressBar);

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

    /** Initialise date et heure à "maintenant" */
    private void initDefaultDateTime() {
        selectedDate = LocalDate.now();
        selectedTime = LocalTime.now().withSecond(0).withNano(0);
        updateDateTimeDisplay();
    }

    private void setupListeners() {
        findViewById(R.id.btnCamera).setOnClickListener(v -> requestCameraAndLaunch());
        findViewById(R.id.btnGallery).setOnClickListener(v -> openGallery());
        findViewById(R.id.btnPickDate).setOnClickListener(v -> showDatePicker());
        findViewById(R.id.btnPickTime).setOnClickListener(v -> showTimePicker());
        findViewById(R.id.btnRefreshGps).setOnClickListener(v -> requestLocationPermissionAndFetch());
        btnSave.setOnClickListener(v -> onSaveClicked());
        tilDescription.setEndIconOnClickListener(v -> requestMicAndSpeak());
    }

    // ── Photo : Caméra ────────────────────────────────────────────────────────

    private void requestCameraAndLaunch() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            launchCamera();
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void launchCamera() {
        try {
            // Crée un fichier temporaire dans le cache de l'app
            File photoFile = File.createTempFile("memory_", ".jpg", getCacheDir());
            // Génère un URI content:// via FileProvider (obligatoire depuis API 24)
            cameraPhotoUri = FileProvider.getUriForFile(
                    this,
                    getPackageName() + ".fileprovider",
                    photoFile
            );
            takePictureLauncher.launch(cameraPhotoUri);
        } catch (IOException e) {
            Toast.makeText(this, "Impossible de créer le fichier photo.", Toast.LENGTH_SHORT).show();
        }
    }

    // ── Photo : Galerie ───────────────────────────────────────────────────────

    private void openGallery() {
        pickMediaLauncher.launch(
                new PickVisualMediaRequest.Builder()
                        .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                        .build()
        );
    }

    // ── Date / Heure ──────────────────────────────────────────────────────────

    private void showDatePicker() {
        // Convertit la date sélectionnée en millisecondes UTC pour le picker
        long initialMs = selectedDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli();

        MaterialDatePicker<Long> picker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("Date de visite")
                .setSelection(initialMs)
                .build();

        picker.addOnPositiveButtonClickListener(selection -> {
            // selection est en ms UTC → on reconvertit en LocalDate
            selectedDate = java.time.Instant.ofEpochMilli(selection)
                    .atZone(ZoneOffset.UTC)
                    .toLocalDate();
            updateDateTimeDisplay();
        });

        picker.show(getSupportFragmentManager(), "date_picker");
    }

    private void showTimePicker() {
        MaterialTimePicker picker = new MaterialTimePicker.Builder()
                .setTimeFormat(TimeFormat.CLOCK_24H)
                .setHour(selectedTime.getHour())
                .setMinute(selectedTime.getMinute())
                .setTitleText("Heure de visite")
                .build();

        picker.addOnPositiveButtonClickListener(v -> {
            selectedTime = LocalTime.of(picker.getHour(), picker.getMinute());
            updateDateTimeDisplay();
        });

        picker.show(getSupportFragmentManager(), "time_picker");
    }

    private void updateDateTimeDisplay() {
        DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", Locale.FRENCH);
        String timeStr = String.format(Locale.FRENCH, "%02d:%02d",
                selectedTime.getHour(), selectedTime.getMinute());
        tvSelectedDateTime.setText(selectedDate.format(dateFmt) + " à " + timeStr);
    }

    // ── GPS ───────────────────────────────────────────────────────────────────

    private void requestLocationPermissionAndFetch() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            fetchLocation();
        } else {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
        }
    }

    // La permission ACCESS_FINE_LOCATION est toujours vérifiée avant d'appeler cette méthode
    @SuppressLint("MissingPermission")
    private void fetchLocation() {
        tvGpsCoords.setText("📍 Récupération en cours…");
        // PRIORITY_HIGH_ACCURACY = préfère GPS matériel (meilleure précision)
        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        currentLatitude = location.getLatitude();
                        currentLongitude = location.getLongitude();
                        tvGpsCoords.setText(String.format(Locale.US,
                                "📍 %.4f, %.4f", currentLatitude, currentLongitude));
                    } else {
                        tvGpsCoords.setText("📍 Position indisponible (GPS éteint ?)");
                    }
                })
                .addOnFailureListener(e ->
                        tvGpsCoords.setText("📍 Erreur GPS : " + e.getMessage()));
    }

    // ── Enregistrement ────────────────────────────────────────────────────────

    private void onSaveClicked() {
        String title = etTitle.getText() != null ? etTitle.getText().toString().trim() : "";

        if (title.isEmpty()) {
            etTitle.setError("Le titre est obligatoire");
            etTitle.requestFocus();
            return;
        }

        if (selectedPhotoUri == null) {
            Toast.makeText(this, "Veuillez choisir une photo.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (currentLatitude == null || currentLongitude == null) {
            new MaterialAlertDialogBuilder(this)
                    .setTitle("Position GPS manquante")
                    .setMessage("La position GPS n'a pas encore été récupérée. Continuer quand même ?")
                    .setNegativeButton("Annuler", null)
                    .setPositiveButton("Continuer", (d, w) -> startUploadThenSave(title))
                    .show();
            return;
        }

        if (!NetworkUtils.isNetworkAvailable(this)) {
            NetworkUtils.showNoNetworkSnackbar(findViewById(android.R.id.content));
            return;
        }

        startUploadThenSave(title);
    }

    /**
     * Étape 1 : upload la photo si elle n'a pas déjà été uploadée.
     * Étape 2 : crée le souvenir avec le photoUrl obtenu.
     */
    private void startUploadThenSave(String title) {
        if (uploadedPhotoUrl != null) {
            // Photo déjà uploadée (l'utilisateur n'a pas changé de photo)
            saveMemory(title, uploadedPhotoUrl);
            return;
        }

        setLoading(true);

        // Résolution du chemin réel depuis l'URI
        File photoFile = resolveFileFromUri(selectedPhotoUri);
        if (photoFile == null || !photoFile.exists()) {
            setLoading(false);
            Toast.makeText(this, "Impossible de lire le fichier photo.", Toast.LENGTH_SHORT).show();
            return;
        }

        RequestBody requestBody = RequestBody.create(photoFile, MediaType.parse("image/*"));
        MultipartBody.Part part = MultipartBody.Part.createFormData(
                "file", photoFile.getName(), requestBody);

        apiService.uploadPhoto(part).enqueue(new Callback<UploadResponse>() {
            @Override
            public void onResponse(@NonNull Call<UploadResponse> call,
                                   @NonNull Response<UploadResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    uploadedPhotoUrl = response.body().getUrl();
                    saveMemory(title, uploadedPhotoUrl);
                } else {
                    setLoading(false);
                    Toast.makeText(AddMemoryActivity.this,
                            "Erreur upload : " + ApiErrorHandler.parseError(response),
                            Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<UploadResponse> call, @NonNull Throwable t) {
                setLoading(false);
                Toast.makeText(AddMemoryActivity.this,
                        "Erreur réseau : " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void saveMemory(String title, String photoUrl) {
        String description = etDescription.getText() != null
                ? etDescription.getText().toString().trim() : "";

        // Format ISO 8601 attendu par le backend : "2026-05-14T15:30:00"
        LocalDateTime visitedAt = LocalDateTime.of(selectedDate, selectedTime);
        String visitedAtStr = visitedAt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

        MemoryRequest request = new MemoryRequest(
                title,
                description.isEmpty() ? null : description,
                photoUrl,
                currentLatitude,
                currentLongitude,
                visitedAtStr
        );

        apiService.createMemory(tripId, request).enqueue(new Callback<Memory>() {
            @Override
            public void onResponse(@NonNull Call<Memory> call,
                                   @NonNull Response<Memory> response) {
                setLoading(false);
                if (response.isSuccessful()) {
                    AnalyticsHelper.logMemoryAdded(title);
                    Toast.makeText(AddMemoryActivity.this,
                            "Souvenir enregistré ✅", Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK);
                    finish();
                    overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
                } else {
                    Toast.makeText(AddMemoryActivity.this,
                            "Erreur sauvegarde : " + ApiErrorHandler.parseError(response),
                            Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<Memory> call, @NonNull Throwable t) {
                setLoading(false);
                Toast.makeText(AddMemoryActivity.this,
                        "Erreur réseau : " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    // ── Microphone / Dictée vocale ────────────────────────────────────────────

    private void requestMicAndSpeak() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED) {
            startSpeechRecognition();
        } else {
            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO);
        }
    }

    private void startSpeechRecognition() {
        AnalyticsHelper.logVoiceDictationUsed();
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "fr-FR");
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "fr-FR");
        intent.putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, false);
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Décrivez votre souvenir…");
        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);
        try {
            speechLauncher.launch(intent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this,
                    "Reconnaissance vocale non disponible sur cet appareil.",
                    Toast.LENGTH_LONG).show();
        }
    }

    // ── Utilitaires ───────────────────────────────────────────────────────────

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnSave.setEnabled(!loading);
    }

    /**
     * Résout un URI (content:// ou file://) vers un File.
     */
    private File resolveFileFromUri(Uri uri) {
        try {
            File tempFile = File.createTempFile("upload_", ".jpg", getCacheDir());
            try (InputStream in = getContentResolver().openInputStream(uri);
                 OutputStream out = new FileOutputStream(tempFile)) {
                if (in == null) return null;
                byte[] buf = new byte[4096];
                int len;
                while ((len = in.read(buf)) > 0) out.write(buf, 0, len);
            }
            return tempFile;
        } catch (IOException e) {
            return null;
        }
    }
}
