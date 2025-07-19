package com.example.hackathon_ex.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class OptimalRouteResponse {
    private int totalDuration;
    private int totalDistance;
    private List<Point>path;
}
