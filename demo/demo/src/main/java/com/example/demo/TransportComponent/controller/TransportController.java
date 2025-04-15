package com.example.demo.TransportComponent.controller;

import com.example.demo.TransportComponent.model.Stop;
import com.example.demo.TransportComponent.model.Transport;
import com.example.demo.TransportComponent.service.WmataService;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/api")
public class TransportController {

    private final WmataService wmataService;

    public TransportController(WmataService wmataService) {
        this.wmataService = wmataService;
    }

    @ResponseBody
    @GetMapping("/transport")
    public List<Transport> getTransports() {
        return wmataService.parseTransports();
    }

    @ResponseBody
    @GetMapping("/stops")
    public List<Stop> getAllStops() {
        return wmataService.parseStops();
    }

    @GetMapping("/map")
    public String showMapPage() {
        return "transport/map"; 
    }
}
