package hackathon.chatbot;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpHeaders;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import reactor.core.publisher.Mono; // Mono 임포트 추가

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class KakaoMapClient {

    private final ObjectMapper objectMapper;
    // @Value("${kakao.api-key}") // application.properties에서 읽지 않으므로 주석 처리
    private String kakaoApiKey; // 필드 선언 유지

    private final WebClient webClient;

    public KakaoMapClient(WebClient.Builder webClientBuilder, ObjectMapper objectMapper, @Value("${kakao.api-key}") String kakaoApiKey) {
        this.kakaoApiKey = kakaoApiKey;
        this.webClient = webClientBuilder.baseUrl("https://dapi.kakao.com/v2/local")
                .defaultHeader(HttpHeaders.AUTHORIZATION, "KakaoAK " + this.kakaoApiKey)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();
        this.objectMapper = objectMapper;
    }

    // 반환 타입을 Mono<List<Map<String, String>>>으로 변경하고, .block() 제거
    public Mono<List<Map<String, String>>> searchPlace(String query) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder.path("/search/keyword.json")
                        .queryParam("query", query)
                        .queryParam("size", 5) // 최대 5개의 검색 결과 요청
                        .build())
                .retrieve()
                .bodyToMono(String.class) // Mono<String> 반환
                .map(jsonResponse -> { // Mono<String>을 Mono<List<Map<String, String>>>로 변환
                    List<Map<String, String>> places = new ArrayList<>();
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
                            // 에러 발생 시 빈 리스트 반환 (Mono.empty() 또는 Mono.error() 고려)
                        }
                    }
                    return places;
                })
                .defaultIfEmpty(new ArrayList<>()) // 응답이 비어있으면 빈 리스트 반환
                .onErrorResume(e -> { // WebClient 호출 자체의 오류 처리
                    System.err.println("KakaoMapClient WebClient call error: " + e.getMessage());
                    return Mono.just(new ArrayList<>()); // 오류 발생 시 빈 리스트 Mono 반환
                });
    }
}