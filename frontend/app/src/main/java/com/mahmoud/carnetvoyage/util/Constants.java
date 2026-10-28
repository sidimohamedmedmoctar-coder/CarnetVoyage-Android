package com.mahmoud.carnetvoyage.util;

public class Constants {

    // ⚠️ 10.0.2.2 = "localhost" du PC, vu depuis l'émulateur Android
    // Si tu utilises un téléphone physique, remplace par l'IP de ton PC (ex: "192.168.1.42")
    public static final String BASE_URL = "http://10.0.2.2:8080/";

    // SharedPreferences (stockage local sur l'appareil)
    public static final String PREF_NAME = "CarnetVoyagePrefs";
    public static final String KEY_TOKEN = "token";
    public static final String KEY_USER_ID = "userId";
    public static final String KEY_USERNAME = "username";
    public static final String KEY_EMAIL = "email";
}