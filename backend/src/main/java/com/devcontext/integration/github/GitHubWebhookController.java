package com.devcontext.integration.github;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

@RestController
@RequestMapping("/api/webhooks/github")
public class GitHubWebhookController {
    private final ObjectMapper objectMapper;
    private final ConnectedRepositoryRepository repositories;
    private final PullRequestRecordRepository pullRequests;
    private final String webhookSecret;

    public GitHubWebhookController(ObjectMapper objectMapper, ConnectedRepositoryRepository repositories,
                                   PullRequestRecordRepository pullRequests,
                                   @Value("${devcontext.integrations.github.webhook-secret:}") String webhookSecret) {
        this.objectMapper = objectMapper;
        this.repositories = repositories;
        this.pullRequests = pullRequests;
        this.webhookSecret = webhookSecret;
    }

    @PostMapping
    public WebhookResult receive(@RequestHeader(value = "X-Hub-Signature-256", required = false) String signature,
                                 @RequestHeader(value = "X-GitHub-Event", defaultValue = "unknown") String event,
                                 @RequestBody String payload) {
        if (!StringUtils.hasText(webhookSecret)) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "GitHub webhook is not configured");
        }
        if (!validSignature(signature, payload)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid webhook signature");
        }
        if (!"pull_request".equals(event)) return new WebhookResult(true, 0);
        try {
            JsonNode root = objectMapper.readTree(payload);
            long externalRepositoryId = root.path("repository").path("id").asLong(0);
            int number = root.path("pull_request").path("number").asInt(0);
            if (externalRepositoryId == 0 || number == 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Webhook is missing repository or pull request identity");
            }
            JsonNode pullRequest = root.path("pull_request");
            int updated = 0;
            for (ConnectedRepository repository : repositories.findAllByExternalId(externalRepositoryId)) {
                PullRequestRecord record = pullRequests.findByRepositoryIdAndExternalNumber(repository.getId(), number)
                        .orElseGet(() -> new PullRequestRecord(repository.getId(), number,
                                pullRequest.path("title").asText("Untitled"), pullRequest.path("user").path("login").asText("unknown"),
                                pullRequest.path("state").asText("unknown"), pullRequest.path("html_url").asText("")));
                record.refreshFromWebhook(pullRequest.path("title").asText("Untitled"),
                        pullRequest.path("user").path("login").asText("unknown"),
                        pullRequest.path("state").asText("unknown"), pullRequest.path("html_url").asText(""));
                pullRequests.save(record);
                updated++;
            }
            return new WebhookResult(true, updated);
        } catch (ResponseStatusException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid GitHub webhook payload");
        }
    }

    private boolean validSignature(String signature, String payload) {
        if (signature == null || !signature.startsWith("sha256=")) return false;
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(webhookSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            String expected = "sha256=" + HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
            return MessageDigest.isEqual(expected.getBytes(StandardCharsets.US_ASCII), signature.getBytes(StandardCharsets.US_ASCII));
        } catch (Exception exception) {
            return false;
        }
    }

    public record WebhookResult(boolean accepted, int updatedPullRequests) {}
}
