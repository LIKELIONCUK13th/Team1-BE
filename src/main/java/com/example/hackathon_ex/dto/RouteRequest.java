package com.example.hackathon_ex.dto;

import lombok.Data;

import java.util.List;

@Data
public class RouteRequest {
    private List<LocationRequest> locations;
}
