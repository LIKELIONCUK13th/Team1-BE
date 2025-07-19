package hackathon.chatbot.Service;

import hackathon.chatbot.GeminiClient;
import hackathon.chatbot.KakaoMapClient;
import org.springframework.stereotype.Service;

@Service
public class ChatbotService {

    private final KakaoMapClient kakaoMapClient;
    private final GeminiClient geminiClient;

    public ChatbotService(KakaoMapClient kakaoMapClient, GeminiClient geminiClient) {
        this.kakaoMapClient = kakaoMapClient;
        this.geminiClient = geminiClient;
    }

    public String getChatbotResponse(String placeName, String userQuestion) {
        String geminiResponse = geminiClient.getGeminiResponse(placeName, userQuestion, kakaoMapClient);

        return geminiResponse;
    }
}