package hackathon.chatbot.Dto;

import lombok.Data;

import java.util.List;

@Data
public class KakaoRouteResponse {
    private List<Route> routes;
}
