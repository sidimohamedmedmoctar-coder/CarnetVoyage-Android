package com.mahmoud.carnetvoyage.util;

import android.content.Context;
import android.os.Bundle;

import com.google.firebase.analytics.FirebaseAnalytics;

/**
 * Centralise tous les événements Firebase Analytics du Carnet de Voyage.
 * Chaque méthode correspond à une action significative de l'utilisateur.
 */
public class AnalyticsHelper {

    private static FirebaseAnalytics firebaseAnalytics;

    public static void init(Context context) {
        firebaseAnalytics = FirebaseAnalytics.getInstance(context.getApplicationContext());
    }

    /** Voyage créé */
    public static void logTripCreated(String tripTitle) {
        if (firebaseAnalytics == null) return;
        Bundle bundle = new Bundle();
        bundle.putString("trip_title", tripTitle);
        firebaseAnalytics.logEvent("trip_created", bundle);
    }

    /** Détail d'un voyage ouvert */
    public static void logTripViewed(String tripTitle) {
        if (firebaseAnalytics == null) return;
        Bundle bundle = new Bundle();
        bundle.putString("trip_title", tripTitle);
        firebaseAnalytics.logEvent("trip_viewed", bundle);
    }

    /** Souvenir ajouté */
    public static void logMemoryAdded(String memoryTitle) {
        if (firebaseAnalytics == null) return;
        Bundle bundle = new Bundle();
        bundle.putString("memory_title", memoryTitle);
        firebaseAnalytics.logEvent("memory_added", bundle);
    }

    /** Carte globale ouverte */
    public static void logGlobalMapViewed() {
        if (firebaseAnalytics == null) return;
        firebaseAnalytics.logEvent("global_map_viewed", null);
    }

    /** Voyage partagé avec un ami */
    public static void logTripShared() {
        if (firebaseAnalytics == null) return;
        firebaseAnalytics.logEvent("trip_shared", null);
    }

    /** Dictée vocale utilisée */
    public static void logVoiceDictationUsed() {
        if (firebaseAnalytics == null) return;
        firebaseAnalytics.logEvent("voice_dictation_used", null);
    }
}
