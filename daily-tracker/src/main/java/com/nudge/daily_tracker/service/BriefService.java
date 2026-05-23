package com.nudge.daily_tracker.service;


import com.nudge.daily_tracker.model.BriefRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

@Service
public class BriefService {

    @Value("${openrouter.api.key}")
    private String apiKey;

    public String generateBrief(BriefRequest request) {

        String prompt = String.format(
                "You are a personal assistant. Give a short, motivating daily brief " +
                        "for someone named %s whose goal is: %s. " +
                        "Give them exactly 3 tasks for today that will move them closer to their goal. " +
                        "Keep it under 100 words. Be specific and actionable.",
                request.getName(),
                request.getGoal()
        );

        Map<String, Object> requestBody = Map.of(
                "model", "meta-llama/llama-3.2-3b-instruct:free",
                "messages", List.of(
                        Map.of("role", "user", "content", prompt)
                )
        );

        WebClient client = WebClient.create();

        for (int attempt = 1; attempt <= 3; attempt++) {
            try {
                Map response = client.post()
                        .uri("https://openrouter.ai/api/v1/chat/completions")
                        .header("Content-Type", "application/json")
                        .header("Authorization", "Bearer " + apiKey)
                        .bodyValue(requestBody)
                        .retrieve()
                        .onStatus(status -> status.value() == 429, clientResponse ->
                                reactor.core.publisher.Mono.error(new RuntimeException("RATE_LIMIT")))
                        .onStatus(status -> status.is4xxClientError(), clientResponse ->
                                reactor.core.publisher.Mono.error(new RuntimeException("API_ERROR: " + clientResponse.statusCode())))
                        .bodyToMono(Map.class)
                        .block();

                // Extract response
                List choices = (List) response.get("choices");
                Map firstChoice = (Map) choices.get(0);
                Map message = (Map) firstChoice.get("message");
                return (String) message.get("content");

            } catch (RuntimeException e) {
                System.out.println("=== ATTEMPT " + attempt + " FAILED: " + e.getMessage());
                if (e.getMessage().equals("RATE_LIMIT") && attempt < 3) {
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                } else if (attempt == 3) {
                    return "Could not generate brief. Please try again.";
                }
            }
        }

        return "Could not generate brief. Please try again.";
    }
}
