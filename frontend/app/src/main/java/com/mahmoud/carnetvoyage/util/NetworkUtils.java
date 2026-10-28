package com.mahmoud.carnetvoyage.util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.view.View;

import com.google.android.material.snackbar.Snackbar;

public class NetworkUtils {

    private NetworkUtils() {}

    public static boolean isNetworkAvailable(Context context) {
        ConnectivityManager cm =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;
        android.net.Network network = cm.getActiveNetwork();
        if (network == null) return false;
        NetworkCapabilities caps = cm.getNetworkCapabilities(network);
        return caps != null && (
                caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET));
    }

    public static void showNoNetworkSnackbar(View rootView) {
        Snackbar snackbar = Snackbar.make(rootView,
                "Pas de connexion internet", Snackbar.LENGTH_LONG);
        // Rouge Material Design (B3261E = colorError Material 3 par défaut)
        snackbar.getView().setBackgroundColor(0xFFB3261E);
        snackbar.setTextColor(android.graphics.Color.WHITE);
        snackbar.show();
    }
}
