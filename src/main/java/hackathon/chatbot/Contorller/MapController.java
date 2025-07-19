package hackathon.chatbot.Contorller;

import hackathon.chatbot.Dto.LocationRequest;
import hackathon.chatbot.Dto.MiddlePointResponse;
import hackathon.chatbot.Service.MeetPointService;
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


public class MapController {
    private final hackathon.chatbot.Service.MeetPointService meetPointService;
    @PostMapping("/middle")
    public ResponseEntity<MiddlePointResponse> getMiddlePoint(@RequestBody List<LocationRequest> locations) {
        return ResponseEntity.ok(meetPointService.calculateMiddlePoint(locations));
    }
}
