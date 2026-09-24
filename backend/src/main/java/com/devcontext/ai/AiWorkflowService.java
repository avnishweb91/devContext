package com.devcontext.ai;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

import static org.springframework.http.HttpStatus.BAD_GATEWAY;
import static org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE;

@Service
public class AiWorkflowService {
    private final RestClient client;
    private final String apiKey;
    private final String model;
    private final String endpoint;

    public AiWorkflowService(RestClient.Builder builder,
                             @Value("${devcontext.ai.api-key:}") String apiKey,
                             @Value("${devcontext.ai.model:gpt-4o-mini}") String model,
                             @Value("${devcontext.ai.endpoint:https://api.openai.com/v1/chat/completions}") String endpoint) {
        this.client = builder.build();
        this.apiKey = apiKey;
        this.model = model;
        this.endpoint = endpoint;
    }

    public String summarize(String title, String context) {
        if (apiKey.isBlank()) {
            throw new ResponseStatusException(SERVICE_UNAVAILABLE, "AI workflow is not configured");
        }
        Map<String, Object> request = Map.of(
                "model", model,
                "temperature", 0.1,
                "messages", new Object[]{
                        Map.of("role", "system", "content", "You are DevContext, an engineering release assistant. Summarize evidence, risks, missing checks, and a safe next action. Do not invent evidence."),
                        Map.of("role", "user", "content", "Change title: " + title + "\nEngineering context:\n" + context)
                }
        );
        try {
            JsonNode response = client.post().uri(endpoint).headers(headers -> headers.setBearerAuth(apiKey))
                    .body(request).retrieve().onStatus(HttpStatusCode::isError,
                            (req, responseBody) -> { throw new ResponseStatusException(BAD_GATEWAY, "AI provider request failed"); })
                    .body(JsonNode.class);
            String result = response == null ? "" : response.path("choices").path(0).path("message").path("content").asText("");
            if (result.isBlank()) throw new ResponseStatusException(BAD_GATEWAY, "AI provider returned no summary");
            return result;
        } catch (ResponseStatusException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new ResponseStatusException(BAD_GATEWAY, "AI provider is unavailable");
        }
    }
}
