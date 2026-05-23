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

    public String getApiKey() {
        return apiKey;
    }

    public String generateBrief(BriefRequest request) {
        try {
            System.out.println("=== DEBUG: apiKey=" + apiKey);
            System.out.println("=== DEBUG: apiUrl=" + apiUrl);

            String prompt = String.format(
                    "You are a personal assistant. Give a short, motivating daily brief " +
                            "for someone named %s whose goal is: %s. " +
                            "Give them exactly 3 tasks for today that will move them closer to their goal. " +
                            "Keep it under 100 words. Be specific and actionable.",
                    request.getName(),
                    request.getGoal()
            );

            Map<String, Object> requestBody = Map.of(
                    "contents", List.of(
                            Map.of("parts", List.of(
                                    Map.of("text", prompt)
                            ))
                    )
            );

            WebClient client = WebClient.create();

            // Retry up to 3 times with 5 second delay between attempts
            for (int attempt = 1; attempt <= 3; attempt++) {
                try {
                    Map response = client.post()
                            .uri(apiUrl)
                            .header("Content-Type", "application/json")
                            .header("x-goog-api-key", apiKey)
                            .bodyValue(requestBody)
                            .retrieve()
                            .onStatus(status -> status.value() == 429, clientResponse ->
                                    reactor.core.publisher.Mono.error(new RuntimeException("RATE_LIMIT")))
                            .onStatus(status -> status.value() == 403, clientResponse ->
                                    reactor.core.publisher.Mono.error(new RuntimeException("RATE_LIMIT")))
                            .onStatus(status -> status.is4xxClientError(), clientResponse ->
                                    reactor.core.publisher.Mono.error(new RuntimeException("API_ERROR: " + clientResponse.statusCode())))
                            .bodyToMono(Map.class)
                            .block();

                    // Success — extract and return the text
                    List candidates = (List) response.get("candidates");
                    Map firstCandidate = (Map) candidates.get(0);
                    Map content = (Map) firstCandidate.get("content");
                    List parts = (List) content.get("parts");
                    Map firstPart = (Map) parts.get(0);
                    return (String) firstPart.get("text");

                } catch (RuntimeException e) {
                    System.out.println("=== ATTEMPT " + attempt + " FAILED: " + e.getMessage());
                    if (e.getMessage().equals("RATE_LIMIT") && attempt < 3) {
                        try {
                            Thread.sleep(5000); // wait 5 seconds before retry
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                        }
                    } else if (attempt == 3) {
                        return "Could not generate brief after multiple attempts. Please try again in a minute.";
                    }
                }
            }

        } catch (Exception e) {
            System.out.println("=== FULL ERROR: " + e.getMessage());
            e.printStackTrace();
            return "Error: " + e.getMessage();
        }

        return "Could not generate brief. Please try again.";
    }
}
