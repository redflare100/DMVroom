package com.example.demo.model;

import java.util.List;

public class Stop {
    private String stopName;
    private List<Double> geoLocation; // [Lat, Lon]
    private Transport nextArrival;

    public Stop() {}

    public Stop(String stopName, List<Double> geoLocation) {
        this.stopName = stopName;
        this.geoLocation = geoLocation;
        this.nextArrival = null;
    }

    public String getStopName() {
        return stopName;
    }

    public void setStopName(String stopName) {
        this.stopName = stopName;
    }

    public List<Double> getGeoLocation() {
        return geoLocation;
    }

    public void setGeoLocation(List<Double> geoLocation) {
        this.geoLocation = geoLocation;
    }
    public Transport getNextArrival(){
        return nextArrival;
    }
    public void setNextArrival(Transport nextArrival){
        this.nextArrival = nextArrival;
    }
}
