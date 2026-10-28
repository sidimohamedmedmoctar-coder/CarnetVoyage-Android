package com.mahmoud.carnetvoyage.model;

import com.google.gson.annotations.SerializedName;

public class Trip {

    private Long id;
    private String title;
    private String description;
    private String startDate;   // "2026-07-15"
    private String endDate;     // "2026-07-25"
    private String createdAt;   // "2026-05-13T..."
    private Long ownerId;
    private String ownerUsername;
    @SerializedName("owner")
    private boolean isOwner;

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getStartDate() { return startDate; }
    public String getEndDate() { return endDate; }
    public String getCreatedAt() { return createdAt; }
    public Long getOwnerId() { return ownerId; }
    public String getOwnerUsername() { return ownerUsername; }
    public boolean isOwner() { return isOwner; }
}