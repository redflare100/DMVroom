package com.example.demo.TransportComponent.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.example.demo.TransportComponent.model.Stop;
import com.example.demo.TransportComponent.model.Transport;

import org.springframework.http.*;

@Service
public class WmataService {

    @Value("${wmata.api.key}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    public List<Transport> parseTransports() {
        List<Transport> transports = new ArrayList<>();
    
        // -- Bus Positions --
        String busUrl = "https://api.wmata.com/Bus.svc/json/jBusPositions";
        HttpHeaders headers = new HttpHeaders();
        headers.set("api_key", apiKey);
        HttpEntity<String> entity = new HttpEntity<>(headers);
    
        ResponseEntity<String> busResponse = restTemplate.exchange(
            busUrl, HttpMethod.GET, entity, String.class
        );
    
        if (busResponse.getStatusCode() == HttpStatus.OK) {
            JSONObject json = new JSONObject(busResponse.getBody());
            JSONArray buses = json.getJSONArray("BusPositions");
    
            for (int i = 0; i < buses.length(); i++) {
                JSONObject bus = buses.getJSONObject(i);
                Transport t = new Transport();
    
                t.setTransportName(bus.getString("VehicleID"));
                t.setTransportType("Bus");
    
                int deviation = bus.getInt("Deviation");
                t.setDelayTime(Math.abs(deviation));
                if (deviation > 0) {
                    t.setStatus("Delayed");
                } else if (deviation < 0) {
                    t.setStatus("Arriving Early");
                } else {
                    t.setStatus("On-Time");
                }
    
                t.setCapacity(0); // Default value
                t.setEtaTime(0);  // Placeholder
    
                transports.add(t);
            }
        }
    
        // -- Metro Train Positions --
        String trainUrl = "https://api.wmata.com/TrainPositions/TrainPositions?contentType=json";
        ResponseEntity<String> trainResponse = restTemplate.exchange(
            trainUrl, HttpMethod.GET, entity, String.class
        );
    
        if (trainResponse.getStatusCode() == HttpStatus.OK) {
            JSONObject trainJson = new JSONObject(trainResponse.getBody());
            JSONArray trains = trainJson.getJSONArray("TrainPositions");
        
            for (int i = 0; i < trains.length(); i++) {
                JSONObject train = trains.getJSONObject(i);
        
                if (!train.optBoolean("IsRevenueService", true)) {
                    continue; // Skip non-passenger trains if that field exists
                }
        
                Transport t = new Transport();
        
                t.setTransportName(train.optString("TrainId", "Unknown"));
                t.setTransportType("Metro");
        
                int deviation = train.optInt("ScheduleDeviation", 0);
                t.setDelayTime(Math.abs(deviation));
        
                if (deviation > 0) {
                    t.setStatus("Delayed");
                } else if (deviation < 0) {
                    t.setStatus("Arriving Early");
                } else {
                    t.setStatus("On-Time");
                }
        
                // Optional: Map car count as capacity (or store separately)
                t.setCapacity(train.optInt("CarCount", 0));
        
                // Still a placeholder
                t.setEtaTime(0);
        
                transports.add(t);
            }
        }
        
    
        return transports;
    }
    

    public List<Stop> parseStops() {
        String busUrl = "https://api.wmata.com/Bus.svc/json/jStops";
        String railUrl = "https://api.wmata.com/Rail.svc/json/jStations";
        String predictionUrlBase = "https://api.wmata.com/NextBusService.svc/json/jPredictions?StopID=";

        HttpHeaders headers = new HttpHeaders();
        headers.set("api_key", apiKey);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        List<Stop> stopList = new ArrayList<>();
        List<Transport> transports = parseTransports(); // Step 1: all transports

        // Step 2: Build map for quick lookup by VehicleID
        Map<String, Transport> vehicleMap = new HashMap<>();
        for (Transport t : transports) {
            if ("Bus".equals(t.getTransportType())) {
                vehicleMap.put(t.getTransportName(), t);
            }
        }

        try {
            // --- Fetch Bus Stops ---
            ResponseEntity<String> busResponse = restTemplate.exchange(busUrl, HttpMethod.GET, entity, String.class);
            if (busResponse.getStatusCode() == HttpStatus.OK) {
                JSONObject busJson = new JSONObject(busResponse.getBody());
                JSONArray stops = busJson.getJSONArray("Stops");

                // currently restricted to 100 bus stops as theres over 9,000 stops in the dmv area and relating all of the stops to a Transport is too much
                for (int i = 0; i < 100; i++) {
                    JSONObject stopObj = stops.getJSONObject(i);
                    String stopId = stopObj.getString("StopID");
                    String name = stopObj.getString("Name") + " - Bus Stop";
                    double lat = stopObj.getDouble("Lat");
                    double lon = stopObj.getDouble("Lon");

                    Stop stop = new Stop(name, List.of(lat, lon));

                    // --- Call Prediction API for this StopID ---
                    try {
                        String predictionUrl = predictionUrlBase + stopId;
                        ResponseEntity<String> predictionResponse = restTemplate.exchange(predictionUrl, HttpMethod.GET, entity, String.class);

                        if (predictionResponse.getStatusCode() == HttpStatus.OK) {
                            JSONObject predictionJson = new JSONObject(predictionResponse.getBody());
                            JSONArray predictions = predictionJson.optJSONArray("Predictions");

                            if (predictions != null && predictions.length() > 0) {
                                JSONObject nextBus = predictions.getJSONObject(0);
                                String vehicleId = nextBus.optString("VehicleID");
                                int minutes = nextBus.optInt("Minutes", 0);

                                if (vehicleMap.containsKey(vehicleId)) {
                                    Transport match = vehicleMap.get(vehicleId);
                                    match.setEtaTime(minutes);
                                    stop.setNextArrival(match); // Set the next arrival
                                }
                            }
                        }
                        // Optional: avoid getting rate-limited
                        Thread.sleep(200);
                    } catch (Exception ex) {
                        System.err.println("Prediction failed for StopID " + stopId + ": " + ex.getMessage());
                    }

                    stopList.add(stop);
                }
            }

            // --- Fetch Train Stations (no nextArrival for trains) ---
            ResponseEntity<String> railResponse = restTemplate.exchange(railUrl, HttpMethod.GET, entity, String.class);
            if (railResponse.getStatusCode() == HttpStatus.OK) {
                JSONObject railJson = new JSONObject(railResponse.getBody());
                JSONArray stations = railJson.getJSONArray("Stations");

                for (int i = 0; i < stations.length(); i++) {
                    JSONObject station = stations.getJSONObject(i);
                    String name = station.getString("Name") + " - Metro Station";
                    double lat = station.getDouble("Lat");
                    double lon = station.getDouble("Lon");

                    Stop stop = new Stop(name, List.of(lat, lon));
                    stopList.add(stop);
                }
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }

        return stopList;
    }
}
