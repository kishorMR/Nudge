package com.nudge.daily_tracker.service;


import com.nudge.daily_tracker.model.BriefRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

@Service
public class BriefService {

    @Value("${gemini.api.key}")
    private String apiKey;

    @Value("${gemini.api.url}")
    private String apiUrl;

    public String generateBrief(BriefRequest request) {

        // Build the prompt
        String prompt = String.format(
                "You are a personal assistant. Give a short, motivating daily brief " +
                        "for someone named %s whose goal is: %s. " +
                        "Give them exactly 3 tasks for today that will move them closer to their goal. " +
                        "Keep it under 100 words. Be specific and actionable.",
                request.getName(),
                request.getGoal()
        );

        // Build request body for Gemini API
        Map<String, Object> requestBody = Map.of(
                "contents", List.of(
                        Map.of("parts", List.of(
                                Map.of("text", prompt)
                        ))
                )
        );

        // Call Gemini API
        WebClient client = WebClient.create();

        Map response = client.post()
                .uri(apiUrl)
                .header("Content-Type", "application/json")
                .header("x-goog-api-key", apiKey)   // ← key goes here now
                .bodyValue(requestBody)
                .retrieve()
                .onStatus(status -> status.value() == 429, clientResponse ->
                        reactor.core.publisher.Mono.error(new RuntimeException("Rate limit hit, try again")))
                .onStatus(status -> status.is4xxClientError(), clientResponse ->
                        reactor.core.publisher.Mono.error(new RuntimeException("API error: " + clientResponse.statusCode())))
                .bodyToMono(Map.class)
                .block();

        // Extract the text from response
        try {
            List candidates = (List) response.get("candidates");
            Map firstCandidate = (Map) candidates.get(0);
            Map content = (Map) firstCandidate.get("content");
            List parts = (List) content.get("parts");
            Map firstPart = (Map) parts.get(0);
            return (String) firstPart.get("text");
        } catch (Exception e) {
            return "Could not generate brief. Please try again.";
        }
    }
}