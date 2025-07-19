package com.example.hackathon_ex.dto;

import lombok.Getter;

import java.util.List;

@Getter
public class OptimalRoute {
    private List<LocationRequest> waypoints;
}
