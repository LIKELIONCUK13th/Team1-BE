package hackathon.chatbot.Dto;

import lombok.Getter;
import java.util.List;

@Getter
public class Route {
    private Summary summary;
    private List<Section> sections;
}
