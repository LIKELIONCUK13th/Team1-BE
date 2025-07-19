package com.example.hackathon_ex.service;

import com.example.hackathon_ex.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RouteService {

    private final MeetPointService meetPointService;

    @Value("${kakao.api.key}")
    private String kakaoApiKey;

    private WebClient getWebClient() {
        return WebClient.builder()
                .baseUrl("https://apis-navi.kakaomobility.com")
                .defaultHeader("Authorization", "KakaoAK " + kakaoApiKey)
                .build();
    }

    public List<RouteResult> calculateRouteToMiddle(List<LocationRequest> origins) {
        MiddlePointResponse middle = meetPointService.calculateMiddlePoint(origins);
        double midX = middle.getX();
        double midY = middle.getY();

        List<RouteResult> results = new ArrayList<>();
        for (LocationRequest origin : origins) {
            results.add(callKakaoRouteApi(origin.getX(), origin.getY(), midX, midY));
        }
        return results;
    }

    private RouteResult callKakaoRouteApi(double startX, double startY, double endX, double endY) {
        KakaoRouteResponse response = getWebClient().get()
                .uri(uriBuilder -> uriBuilder
                        .path("/v1/directions")
                        .queryParam("origin", startX + "," + startY)
                        .queryParam("destination", endX + "," + endY)
                        .queryParam("priority", "DISTANCE")
                        .build())
                .retrieve()
                .bodyToMono(KakaoRouteResponse.class)
                .block();

        int duration = response.getRoutes().get(0).getSummary().getDuration();
        int distance = response.getRoutes().get(0).getSummary().getDistance();
        List<Double>vertexes = response.getRoutes()
                .get(0).getSections()
                .get(0).getRoads()
                .stream()
                .flatMap(road->road.getVertexes().stream())
                .toList();
        List<Point>path = new ArrayList<>();
        for(int i=0;i<vertexes.size();i+=2){
            double x = vertexes.get(i);
            double y = vertexes.get(i+1);
            path.add(new Point(x, y));
        }
        return new RouteResult(endX, endY, duration, distance,path);
    }
    public OptimalRouteResponse calculateOptimalRoute(OptimalRoute request) {
        List<LocationRequest> waypoints = request.getWaypoints();
        if (waypoints == null || waypoints.size() < 2) {
            throw new IllegalArgumentException("Waypoints must contain at least two points");
        }
        int totalDistance = 0;
        int totalDuration = 0;
        List<Point> fullPath = new ArrayList<>();
        for (int i = 0; i < waypoints.size() - 1; i++) {
            LocationRequest origin = waypoints.get(i);
            LocationRequest destination = waypoints.get(i + 1);
            KakaoRouteResponse response = getWebClient().get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/v1/directions")
                            .queryParam("origin", origin.getX() + "," + origin.getY())
                            .queryParam("destination", destination.getX() + "," + destination.getY())
                            .queryParam("priority", "DISTANCE")
                            .build())
                    .retrieve()
                    .bodyToMono(KakaoRouteResponse.class)
                    .block();
            if (response != null) {
                int duration = response.getRoutes().get(0).getSummary().getDuration();
                int distance = response.getRoutes().get(0).getSummary().getDistance();
                totalDistance += distance;
                totalDuration += duration;
                List<Double> vertexes = response.getRoutes().get(0).getSections().get(0).getRoads()
                        .stream().flatMap(road -> road.getVertexes().stream())
                        .toList();
                for (int j = 0; j < vertexes.size(); j += 2) {
                    double x = vertexes.get(j);
                    double y = vertexes.get(j + 1);
                    fullPath.add(new Point(x, y));
                }
            }
        }
        return new OptimalRouteResponse(totalDuration, totalDistance, fullPath);
    }
}
