package com.mahmoud.carnetvoyage.model;

public class Memory {

    private Long id;
    private String title;
    private String description;
    private String photoUrl;
    private Double latitude;
    private Double longitude;
    private String visitedAt;
    private String createdAt;
    private Long tripId;

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getPhotoUrl() { return photoUrl; }
    public Double getLatitude() { return latitude; }
    public Double getLongitude() { return longitude; }
    public String getVisitedAt() { return visitedAt; }
    public String getCreatedAt() { return createdAt; }
    public Long getTripId() { return tripId; }
}
