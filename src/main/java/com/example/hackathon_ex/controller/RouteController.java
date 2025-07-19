package com.example.hackathon_ex.controller;

import com.example.hackathon_ex.dto.*;
import com.example.hackathon_ex.service.RouteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/map")
@RequiredArgsConstructor
public class RouteController {

    private final RouteService routeService;

    @PostMapping("/route")
    public ResponseEntity<List<RouteResult>> getRoutes(@RequestBody RouteRequest request){
        List<LocationRequest> origins = request.getLocations();
        List<RouteResult> results = routeService.calculateRouteToMiddle(origins);
        return ResponseEntity.ok(results);
    }

    @PostMapping("/optimal")
    public ResponseEntity<OptimalRouteResponse> getOptimalRoute(@RequestBody OptimalRoute request){
        return ResponseEntity.ok(routeService.calculateOptimalRoute(request));
    }

}
