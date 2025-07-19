package hackathon.chatbot.Dto;

import lombok.Data;

import java.util.List;

@Data
public class RouteRequest {
    private List<LocationRequest> locations;
}
