package com.mahmoud.carnetvoyage.model;

public class MemoryRequest {

    private String title;
    private String description;
    private String photoUrl;
    private Double latitude;
    private Double longitude;
    private String visitedAt;

    public MemoryRequest(String title, String description, String photoUrl,
                         Double latitude, Double longitude, String visitedAt) {
        this.title = title;
        this.description = description;
        this.photoUrl = photoUrl;
        this.latitude = latitude;
        this.longitude = longitude;
        this.visitedAt = visitedAt;
    }
}
