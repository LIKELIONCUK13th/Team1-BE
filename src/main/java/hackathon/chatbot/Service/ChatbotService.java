package hackathon.chatbot.Service;

import hackathon.chatbot.GeminiClient;
import hackathon.chatbot.KakaoMapClient;
import org.springframework.stereotype.Service;

import reactor.core.publisher.Mono; // Mono 임포트
import com.google.cloud.vertexai.api.Content; // Content 임포트
import java.util.ArrayList; // ArrayList 임포트
import java.util.List; // List 임포트
import java.util.Map;

@Service
public class ChatbotService {

    private final KakaoMapClient kakaoMapClient;
    private final GeminiClient geminiClient;

    private final Map<String, List<Content>> conversationHistories = new java.util.HashMap<>();


    public ChatbotService(KakaoMapClient kakaoMapClient, GeminiClient geminiClient) {
        this.kakaoMapClient = kakaoMapClient;
        this.geminiClient = geminiClient;
    }

    public Mono<String> getChatbotResponse(String placeName, String userQuestion) {
        String userId = "testUser1";
        List<Content> currentHistory = conversationHistories.computeIfAbsent(userId, k -> new ArrayList<>());


        String initialPrompt = String.format("'%s' 주변에 대해 '%s'라고 질문했습니다. 이 질문에 답하기 위해 적절한 장소를 검색해야 합니다." +
                        "\n\n장소 검색 결과가 있다면, 다음 양식에 맞춰 장소를 추천해주세요:" +
                        "\n\n[번호]. [장소 이름] ([장소 카테고리])" +
                        "\n주소: [장소 주소]" +
                        "\n전화번호: [장소 전화번호 (없으면 '정보 없음')]" +
                        "\n상세 정보: [장소 상세정보 URL (없으면 '정보 없음')]" +
                        "\n설명: [해당 장소에 대한 간략한 특징, 분위기, 추천 메뉴 등을 모델이 직접 생성하여 설명]" +
                        "\n\n각 추천 장소는 서로 다른 단락(두 줄 바꿈)으로 구분해주세요." +
                        "\n\n만약 검색 결과가 없다면, '죄송합니다. 요청하신 조건에 맞는 장소를 찾을 수 없습니다. 다른 검색 조건을 알려주시겠어요?'와 같이 친절하게 응답해주세요.",
                placeName, userQuestion);

        return geminiClient.getGeminiResponse(initialPrompt, kakaoMapClient, currentHistory);
    }
}