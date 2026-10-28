package com.mahmoud.carnetvoyage.model;

public class ShareResponse {

    private Long id;
    private Long tripId;
    private Long sharedWithUserId;
    private String sharedWithUsername;
    private String sharedWithEmail;
    private String sharedAt;

    public Long getId() { return id; }
    public Long getTripId() { return tripId; }
    public Long getSharedWithUserId() { return sharedWithUserId; }
    public String getSharedWithUsername() { return sharedWithUsername; }
    public String getSharedWithEmail() { return sharedWithEmail; }
    public String getSharedAt() { return sharedAt; }
}
