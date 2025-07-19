package hackathon.chatbot;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpHeaders;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class KakaoMapClient {

    private final ObjectMapper objectMapper;
    private String kakaoApiKey;

    private final WebClient webClient;

    public KakaoMapClient(WebClient.Builder webClientBuilder, ObjectMapper objectMapper, @Value("${kakao.api-key}") String kakaoApiKey) {
        this.kakaoApiKey = kakaoApiKey;
        this.webClient = webClientBuilder.baseUrl("https://dapi.kakao.com/v2/local")
                .defaultHeader(HttpHeaders.AUTHORIZATION, "KakaoAK " + this.kakaoApiKey)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();
        this.objectMapper = objectMapper;
    }

    // 변경된 searchPlace 메서드: List<Map<String, String>> 형태로 여러 장소 정보를 반환
    public List<Map<String, String>> searchPlace(String query) {
        List<Map<String, String>> places = new ArrayList<>();
        String jsonResponse = webClient.get()
                .uri(uriBuilder -> uriBuilder.path("/search/keyword.json")
                        .queryParam("query", query)
                        .queryParam("size", 5) // 최대 5개의 검색 결과 요청
                        .build())
                .retrieve()
                .bodyToMono(String.class)
                .block();

        if (jsonResponse != null) {
            try {
                JsonNode root = objectMapper.readTree(jsonResponse);
                JsonNode documents = root.path("documents");
                if (documents.isArray()) {
                    for (JsonNode placeNode : documents) {
                        Map<String, String> placeInfo = new HashMap<>();
                        placeInfo.put("place_name", placeNode.path("place_name").asText());
                        placeInfo.put("address_name", placeNode.path("address_name").asText());
                        placeInfo.put("category_name", placeNode.path("category_name").asText());
                        placeInfo.put("phone", placeNode.path("phone").asText("정보 없음"));
                        placeInfo.put("place_url", placeNode.path("place_url").asText("정보 없음"));
                        places.add(placeInfo);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                // 에러 발생 시 빈 리스트 반환
                return new ArrayList<>();
            }
        }
        return places;
    }
}