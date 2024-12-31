package com.example.ubercus.History;

public class HistoryObject {
    private String rideId;
    private String prices;
    private String time;

    public HistoryObject (String rideId, String time, String prices) {
        this.rideId = rideId;
        this.time = time;
        this.prices = prices;
    }

    public String getRideId() { return rideId; }
    public String getPrices() { return prices; }
    public String getTime() { return time; }

    public void setRideId(String rideId) { this.rideId = rideId; };
    public void setTime(String prices) { this.time = time; };
    public void setPrices(String prices) { this.prices = prices; };

}
