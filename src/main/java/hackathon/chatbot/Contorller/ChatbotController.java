package hackathon.chatbot.Contorller;

import hackathon.chatbot.Dto.ChatRequest;
import hackathon.chatbot.Dto.ChatResponse;
import hackathon.chatbot.Service.ChatbotService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/chatbot")
public class ChatbotController {

    private final ChatbotService chatbotService;

    public ChatbotController(ChatbotService chatbotService) {
        this.chatbotService = chatbotService;
    }

    @PostMapping("/ask")
    public ChatResponse ask(@RequestBody ChatRequest request) {
        String answer = chatbotService.getChatbotResponse(request.getPlaceName(), request.getQuestion());
        return new ChatResponse(answer);
    }
}
