package hackathon.chatbot;

import com.google.cloud.vertexai.VertexAI;
import com.google.cloud.vertexai.api.GenerateContentResponse;
import com.google.cloud.vertexai.api.Tool;
import com.google.cloud.vertexai.api.FunctionDeclaration;
import com.google.cloud.vertexai.api.Schema;
import com.google.cloud.vertexai.api.Type;
import com.google.cloud.vertexai.api.Part;
import com.google.cloud.vertexai.api.Content;
import com.google.cloud.vertexai.api.FunctionResponse;
import com.google.cloud.vertexai.generativeai.GenerativeModel;
import com.google.cloud.vertexai.generativeai.ResponseHandler;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;

import com.google.protobuf.Struct;
import com.google.protobuf.Value;
import com.google.protobuf.ListValue;

import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Component
public class GeminiClient {

    @org.springframework.beans.factory.annotation.Value("${gemini.api-key}")
    private final String geminiApiKey;

    @org.springframework.beans.factory.annotation.Value("${gemini.model-name}")
    private final String geminiModelName;

    @org.springframework.beans.factory.annotation.Value("${GOOGLE_CLOUD_PROJECT_ID}")
    private final String projectId;

    private VertexAI vertexAI;
    private GenerativeModel model;
    private final ObjectMapper objectMapper;

    public GeminiClient(
            ObjectMapper objectMapper,
            @org.springframework.beans.factory.annotation.Value("${gemini.api-key}") String geminiApiKey,
            @org.springframework.beans.factory.annotation.Value("${gemini.model-name}") String geminiModelName,
            @org.springframework.beans.factory.annotation.Value("${GOOGLE_CLOUD_PROJECT_ID}") String projectId
    ) {
        this.objectMapper = objectMapper;
        this.geminiApiKey = geminiApiKey;
        this.geminiModelName = geminiModelName;
        this.projectId = projectId;
    }

    @PostConstruct
    public void init() throws IOException {
        String location = "us-central1";

        try {
            this.vertexAI = new VertexAI(projectId, location);

            FunctionDeclaration searchPlacesFunction = FunctionDeclaration.newBuilder()
                    .setName("search_places")
                    .setDescription("주변의 장소(식당, 카페 등)를 검색합니다. 특정 장소 이름과 검색 쿼리(음식 종류, 카테고리 등)를 입력받습니다.")
                    .setParameters(Schema.newBuilder()
                            .setType(Type.OBJECT)
                            .putProperties("query", Schema.newBuilder()
                                    .setType(Type.STRING)
                                    .setDescription("검색할 장소의 이름과 종류 (예: '강남역 중식당', '홍대 카페', '판교역 맛집')")
                                    .build())
                            .addRequired("query")
                            .build())
                    .build();

            this.model = new GenerativeModel(geminiModelName, vertexAI)
                    .withTools(Arrays.asList(Tool.newBuilder().addFunctionDeclarations(searchPlacesFunction).build()));

        } catch (NoClassDefFoundError | NoSuchMethodError e) {
            System.err.println("VertexAI 생성자를 찾을 수 없습니다. SDK 버전을 확인하거나 종속성을 확인하세요.");
            throw new IOException("VertexAI 초기화 오류: " + e.getMessage(), e);
        }
    }

    public Mono<String> getGeminiResponse(String userPrompt, KakaoMapClient kakaoMapClient, List<Content> currentHistory) {
        Content userContent = Content.newBuilder()
                .addParts(Part.newBuilder().setText(userPrompt).build())
                .setRole("user")
                .build();
        currentHistory.add(userContent); // 사용자 질문을 기록에 추가

        // Mono를 반환하여 비동기 처리
        // Gemini API 호출은 네트워크 IO이므로, blocking 호출이 허용되는 스레드에서 실행되도록 fromCallable + publishOn을 사용합니다.
        return Mono.fromCallable(() -> model.generateContent(new ArrayList<>(currentHistory))) // history 복사본 전달
                .publishOn(Schedulers.boundedElastic()) // Gemini API 호출을 blocking 가능한 스레드로 전환
                .flatMap(response -> {
                    Content modelContent = response.getCandidates(0).getContent();
                    String modelResponseText = ResponseHandler.getText(response); // 기본 응답 텍스트

                    // 모델 응답을 history에 추가 (Function Call이 있는 경우와 없는 경우 모두)
                    currentHistory.add(modelContent); // 모델 응답 (함수 호출 포함 가능)을 기록에 추가

                    if (modelContent.getPartsCount() > 0 && modelContent.getParts(0).hasFunctionCall()) {
                        String functionName = modelContent.getParts(0).getFunctionCall().getName();

                        if ("search_places".equals(functionName)) {
                            Struct functionArgs = modelContent.getParts(0).getFunctionCall().getArgs();

                            final String extractedQuery;
                            if (functionArgs.getFieldsMap().containsKey("query")) {
                                Value queryValue = functionArgs.getFieldsMap().get("query");
                                if (queryValue.hasStringValue()) {
                                    extractedQuery = queryValue.getStringValue();
                                } else {
                                    extractedQuery = null;
                                }
                            } else {
                                extractedQuery = null;
                            }

                            if (extractedQuery == null || extractedQuery.isEmpty()) {
                                // Gemini가 검색 쿼리를 제대로 생성하지 못하면, 해당 오류 메시지를 history에 추가하고 반환
                                // 이 경우는 모델이 오류를 이해하고 답변하도록 하지 않고, 직접 오류 반환
                                // currentHistory.remove(currentHistory.size() - 1); // 잘못된 modelContent 제거
                                return Mono.just("Gemini가 검색 쿼리를 제대로 생성하지 못했습니다. 질문을 명확히 해주세요.");
                            }

                            System.out.println("Gemini가 카카오맵 검색을 제안했습니다. 검색 쿼리: " + extractedQuery);

                            return kakaoMapClient.searchPlace(extractedQuery) // extractedQuery 사용
                                    .flatMap(places -> {
                                        Struct.Builder responseStructBuilder = Struct.newBuilder();

                                        if (places == null || places.isEmpty()) {
                                            responseStructBuilder.putFields("status", Value.newBuilder().setStringValue("No results").build());
                                            responseStructBuilder.putFields("message", Value.newBuilder().setStringValue("No places found for query: " + extractedQuery).build());
                                        } else {
                                            List<Value> placeValueList = new java.util.ArrayList<>();
                                            for (Map<String, String> place : places) {
                                                Struct.Builder placeStructBuilder = Struct.newBuilder();
                                                place.forEach((key, val) -> placeStructBuilder.putFields(key, Value.newBuilder().setStringValue(val).build()));
                                                placeValueList.add(Value.newBuilder().setStructValue(placeStructBuilder.build()).build());
                                            }
                                            responseStructBuilder.putFields("places", Value.newBuilder().setListValue(ListValue.newBuilder().addAllValues(placeValueList).build()).build());
                                            responseStructBuilder.putFields("status", Value.newBuilder().setStringValue("success").build());
                                        }
                                        Struct toolOutputStruct = responseStructBuilder.build();

                                        // FunctionResponse Content를 생성하고 history에 추가
                                        // FunctionResponse를 담는 Content의 role은 'user'여야 합니다.
                                        Content toolResponseContent = Content.newBuilder()
                                                .addParts(Part.newBuilder()
                                                        .setFunctionResponse(
                                                                FunctionResponse.newBuilder()
                                                                        .setName(functionName)
                                                                        .setResponse(toolOutputStruct)
                                                                        .build())
                                                        .build())
                                                .setRole("user") // <<-- 이 부분이 중요합니다: 'user' 역할
                                                .build();
                                        currentHistory.add(toolResponseContent);

                                        // 도구 결과와 함께 대화 기록을 다시 모델에 전달하여 최종 응답 생성
                                        return Mono.fromCallable(() -> model.generateContent(new ArrayList<>(currentHistory)))
                                                .publishOn(Schedulers.boundedElastic())
                                                .map(finalModelResponse -> {
                                                    String finalModelResponseText = ResponseHandler.getText(finalModelResponse);
                                                    Content finalModelContent = Content.newBuilder()
                                                            .addParts(Part.newBuilder().setText(finalModelResponseText).build())
                                                            .setRole("model")
                                                            .build();
                                                    currentHistory.add(finalModelContent);
                                                    return finalModelResponseText;
                                                })
                                                .onErrorResume(e -> {
                                                    System.err.println("Error generating final content after tool call: " + e.getMessage());
                                                    return Mono.just("Gemini API 최종 응답 생성 중 오류가 발생했습니다: " + e.getMessage());
                                                });
                                    })
                                    .onErrorResume(e -> {
                                        System.err.println("Error calling KakaoMapClient: " + e.getMessage());
                                        return Mono.just("카카오맵 검색 중 오류가 발생했습니다: " + e.getMessage());
                                    });

                        } else {
                            return Mono.just("Gemini가 알 수 없는 함수를 호출하려 했습니다: " + functionName);
                        }
                    } else {
                        // Function Call이 없는 경우, 이미 modelContent가 history에 추가되었으므로 여기서 추가 작업 없음
                        return Mono.just(modelResponseText);
                    }
                })
                .onErrorResume(e -> {
                    System.err.println("Error in initial Gemini API call: " + e.getMessage());
                    return Mono.just("Gemini API 초기 호출 중 오류가 발생했습니다: " + e.getMessage());
                });
    }

    @PreDestroy
    public void cleanup() throws IOException {
        if (vertexAI != null) {
            vertexAI.close();
        }
    }
}