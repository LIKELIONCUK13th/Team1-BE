package hackathon.chatbot.Dto;

import lombok.Getter;

import java.util.List;

@Getter
public class RouteResult {
    private double endX;
    private double endY;
    private int duration;
    private int distance;
    private List<Point> path;
    public RouteResult(double endX, double endY, int duration, int distance,List<Point>path) {
        this.endX = endX;
        this.endY = endY;
        this.duration = duration;
        this.distance = distance;
        this.path = path;
    }
}
