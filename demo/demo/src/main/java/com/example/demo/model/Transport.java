package com.example.demo.model;

public class Transport {
    private String transportName;
    private String transportType;
    private String status; // On-Time, Delayed, Shut Down
    private int etaTime;
    private int delayTime;
    private int capacity;

    // Getters and Setters
    public String getTransportName() { return transportName; }
    public void setTransportName(String transportName) { this.transportName = transportName; }

    public String getTransportType() { return transportType; }
    public void setTransportType(String transportType) { this.transportType = transportType; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public int getEtaTime() { return etaTime; }
    public void setEtaTime(int etaTime) { this.etaTime = etaTime; }

    public int getDelayTime() { return delayTime; }
    public void setDelayTime(int delayTime) {
        this.delayTime = delayTime;
        if (delayTime > 0) {
            this.status = "Delayed";
        }
    }

    public int getCapacity() { return capacity; }
    public void setCapacity(int capacity) { this.capacity = capacity; }
}
