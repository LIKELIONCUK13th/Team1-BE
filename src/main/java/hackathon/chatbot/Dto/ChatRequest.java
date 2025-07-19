package hackathon.chatbot.Dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChatRequest {
    private String placeName;
    private String question;
}
