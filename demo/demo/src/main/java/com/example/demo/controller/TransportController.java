package com.example.demo.controller;

import com.example.demo.model.Stop;
import com.example.demo.model.Transport;
import com.example.demo.service.WmataService;

import java.util.List;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class TransportController {

    private final WmataService wmataService;

    public TransportController(WmataService wmataService) {
        this.wmataService = wmataService;
    }

    @GetMapping("/transport")
    public List<Transport> getBusTransports() {
        return wmataService.parseTransports();
    }

    @GetMapping("/stops")
    public List<Stop> getAllStops() {
        return wmataService.parseStops();
    }

}

