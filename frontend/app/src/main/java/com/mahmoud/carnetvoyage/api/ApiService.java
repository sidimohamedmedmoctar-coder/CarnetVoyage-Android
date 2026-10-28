package com.mahmoud.carnetvoyage.api;

import com.mahmoud.carnetvoyage.model.AuthResponse;
import com.mahmoud.carnetvoyage.model.LoginRequest;
import com.mahmoud.carnetvoyage.model.Memory;
import com.mahmoud.carnetvoyage.model.MemoryRequest;
import com.mahmoud.carnetvoyage.model.RegisterRequest;
import com.mahmoud.carnetvoyage.model.ShareRequest;
import com.mahmoud.carnetvoyage.model.ShareResponse;
import com.mahmoud.carnetvoyage.model.Trip;
import com.mahmoud.carnetvoyage.model.TripRequest;
import com.mahmoud.carnetvoyage.model.UploadResponse;

import java.util.List;

import okhttp3.MultipartBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Part;
import retrofit2.http.Path;

public interface ApiService {

    // === Authentification ===

    @POST("api/auth/register")
    Call<AuthResponse> register(@Body RegisterRequest request);

    @POST("api/auth/login")
    Call<AuthResponse> login(@Body LoginRequest request);

    // === Voyages ===

    @GET("api/trips")
    Call<List<Trip>> getMyTrips();

    @GET("api/trips/shared")
    Call<List<Trip>> getSharedTrips();

    @GET("api/trips/{id}")
    Call<Trip> getTripById(@Path("id") Long id);

    @POST("api/trips")
    Call<Trip> createTrip(@Body TripRequest request);

    @PUT("api/trips/{id}")
    Call<Trip> updateTrip(@Path("id") Long id, @Body TripRequest request);

    @DELETE("api/trips/{id}")
    Call<Void> deleteTrip(@Path("id") Long id);

    // === Souvenirs ===

    @GET("api/trips/{tripId}/memories")
    Call<List<Memory>> getMemoriesByTrip(@Path("tripId") Long tripId);

    @POST("api/trips/{tripId}/memories")
    Call<Memory> createMemory(@Path("tripId") Long tripId, @Body MemoryRequest request);

    @DELETE("api/memories/{id}")
    Call<Void> deleteMemory(@Path("id") Long id);

    // === Partage de voyages ===

    @POST("api/trips/{tripId}/share")
    Call<ShareResponse> shareTrip(@Path("tripId") Long tripId, @Body ShareRequest request);

    @GET("api/trips/{tripId}/share")
    Call<List<ShareResponse>> getSharesOfTrip(@Path("tripId") Long tripId);

    @DELETE("api/trips/{tripId}/share/{userId}")
    Call<Void> unshareTrip(@Path("tripId") Long tripId, @Path("userId") Long userId);

    // === Upload photo ===

    @Multipart
    @POST("api/upload")
    Call<UploadResponse> uploadPhoto(@Part MultipartBody.Part file);
}