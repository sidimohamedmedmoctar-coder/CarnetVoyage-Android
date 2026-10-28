package com.mahmoud.carnetvoyage.util;

import android.graphics.Color;

public class MapColorUtils {

    private MapColorUtils() {}

    /**
     * Répartit les voyages uniformément sur le cercle HSV (0–360°).
     * L'offset golden ratio (~137.5°) garantit que deux IDs consécutifs
     * n'aient pas des teintes trop proches.
     */
    public static float getHueForTripId(long tripId) {
        return (float) ((tripId * 137.5) % 360);
    }

    /**
     * Renvoie un entier ARGB opaque à partir de la teinte HSV du voyage.
     * Saturation et valeur fixes pour rester lisible sur fond de carte.
     */
    public static int getColorForTripId(long tripId) {
        float hue = getHueForTripId(tripId);
        float[] hsv = { hue, 0.75f, 0.85f };
        return Color.HSVToColor(hsv);
    }
}
