package com.example.ubercus.History;

public class HistoryObject {
    private String rideId;
    private String time;

    public HistoryObject (String rideId, String time) {
        this.rideId = rideId;
        this.time = time;
    }
    public HistoryObject(String rideId){ this.rideId = rideId; };
    public void setRideId(String rideId) { this.rideId = rideId; };
}
