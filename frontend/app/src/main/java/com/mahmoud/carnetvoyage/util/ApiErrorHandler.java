package com.mahmoud.carnetvoyage.util;

import org.json.JSONObject;

import retrofit2.Response;

public class ApiErrorHandler {

    private ApiErrorHandler() {}

    /**
     * Lit l'errorBody de la réponse et extrait le champ "error" du JSON
     * renvoyé par le GlobalExceptionHandler du backend.
     * Retourne un message générique si l'extraction échoue.
     */
    public static String parseError(Response<?> response) {
        if (response == null) return "Erreur inconnue";
        try {
            if (response.errorBody() != null) {
                String raw = response.errorBody().string();
                JSONObject json = new JSONObject(raw);
                if (json.has("error")) return json.getString("error");
                if (json.has("message")) return json.getString("message");
            }
        } catch (Exception ignored) {}
        return "Erreur " + response.code();
    }

    public static boolean isUnauthorized(Response<?> response) {
        return response != null && response.code() == 401;
    }
}
