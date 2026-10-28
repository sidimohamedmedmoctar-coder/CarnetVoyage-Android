package com.mahmoud.carnetvoyage.util;

import android.content.Context;
import android.content.SharedPreferences;

import com.mahmoud.carnetvoyage.model.AuthResponse;

public class SessionManager {

    private final SharedPreferences pref;

    public SessionManager(Context context) {
        pref = context.getSharedPreferences(Constants.PREF_NAME, Context.MODE_PRIVATE);
    }

    // Sauvegarder le token + infos user après login/register
    public void saveAuth(AuthResponse response) {
        pref.edit()
            .putString(Constants.KEY_TOKEN, response.getToken())
            .putLong(Constants.KEY_USER_ID, response.getUserId())
            .putString(Constants.KEY_USERNAME, response.getUsername())
            .putString(Constants.KEY_EMAIL, response.getEmail())
            .apply();
    }

    public String getToken() {
        return pref.getString(Constants.KEY_TOKEN, null);
    }

    public Long getUserId() {
        return pref.getLong(Constants.KEY_USER_ID, -1L);
    }

    public String getUsername() {
        return pref.getString(Constants.KEY_USERNAME, null);
    }

    public String getEmail() {
        return pref.getString(Constants.KEY_EMAIL, null);
    }

    public boolean isLoggedIn() {
        return getToken() != null;
    }

    public void logout() {
        // On utilise commit() ici pour garantir que le nettoyage est terminé 
        // avant la redirection vers l'écran de Login
        pref.edit().clear().commit();
    }
}