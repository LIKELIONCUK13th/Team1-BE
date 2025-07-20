package hackathon.chatbot.Contorller;

import hackathon.chatbot.Dto.ChatRequest;
import hackathon.chatbot.Dto.ChatResponse;
import hackathon.chatbot.Service.ChatbotService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import reactor.core.publisher.Mono; // Mono 임포트

@RestController
@RequestMapping("/chatbot")
public class ChatbotController {

    private final ChatbotService chatbotService;

    public ChatbotController(ChatbotService chatbotService) {
        this.chatbotService = chatbotService;
    }

    @PostMapping("/ask")
    // 반환 타입을 Mono<ChatResponse>으로 변경
    public Mono<ChatResponse> ask(@RequestBody ChatRequest request) {
        // Mono<String>을 Mono<ChatResponse>로 변환
        return chatbotService.getChatbotResponse(request.getPlaceName(), request.getQuestion())
                .map(ChatResponse::new); // String 결과를 ChatResponse 객체로 맵핑
    }
}