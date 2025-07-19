package hackathon.chatbot.Contorller;

import hackathon.chatbot.Dto.LocationRequest;
import hackathon.chatbot.Dto.RouteRequest;
import hackathon.chatbot.Dto.RouteResult;
import hackathon.chatbot.Service.RouteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/map")
@RequiredArgsConstructor
public class RouteController {

    private final RouteService routeService;

    @PostMapping("/route")
    public ResponseEntity<List<RouteResult>> getRoutes(@RequestBody RouteRequest request){
        List<LocationRequest> origins = request.getLocations();
        List<RouteResult> results = routeService.calculateRouteToMiddle(origins);
        return ResponseEntity.ok(results);
    }
}
