package com.devcontext.integration;

import com.devcontext.memory.EngineeringMemory;
import com.devcontext.memory.EngineeringMemoryRepository;
import com.devcontext.workspace.WorkspaceAccessService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import static org.springframework.http.HttpStatus.BAD_GATEWAY;
import static org.springframework.http.HttpStatus.CONFLICT;

@Service
public class ProviderSyncService {
    private final WorkspaceAccessService access;
    private final WorkspaceIntegrationRepository integrations;
    private final EngineeringMemoryRepository memories;
    private final ObjectProvider<OAuth2AuthorizedClientService> authorizedClients;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;

    public ProviderSyncService(WorkspaceAccessService access, WorkspaceIntegrationRepository integrations,
                               EngineeringMemoryRepository memories,
                               ObjectProvider<OAuth2AuthorizedClientService> authorizedClients,
                               ObjectMapper objectMapper, RestClient.Builder restClientBuilder) {
        this.access = access;
        this.integrations = integrations;
        this.memories = memories;
        this.authorizedClients = authorizedClients;
        this.objectMapper = objectMapper;
        this.restClient = restClientBuilder.build();
    }

    public SyncResult sync(UUID workspaceId, String providerName, Authentication authentication) {
        access.requireMember(workspaceId, authentication);
        String provider = providerName.trim().toUpperCase(Locale.ROOT);
        if (!provider.equals("JIRA") && !provider.equals("SLACK")) {
            throw new ResponseStatusException(CONFLICT, "Only Jira and Slack provider sync is supported here");
        }
        OAuth2AuthorizedClientService clientService = authorizedClients.getIfAvailable();
        if (clientService == null) {
            throw new ResponseStatusException(CONFLICT, "OAuth security is not enabled for this environment");
        }
        OAuth2AuthorizedClient client = clientService.loadAuthorizedClient(provider.toLowerCase(Locale.ROOT), authentication.getName());
        if (client == null || client.getAccessToken() == null) {
            throw new ResponseStatusException(CONFLICT, "Connect " + provider + " before syncing it");
        }

        WorkspaceIntegration integration = integrations.findByWorkspaceIdAndProvider(workspaceId, provider)
                .orElseGet(() -> new WorkspaceIntegration(workspaceId, provider, "CONNECTED", null));
        try {
            List<EngineeringMemory> discovered = provider.equals("JIRA")
                    ? importJira(workspaceId, client.getAccessToken().getTokenValue())
                    : importSlack(workspaceId, client.getAccessToken().getTokenValue());
            List<EngineeringMemory> imported = discovered.stream()
                    .filter(memory -> memory.getSourceUrl() == null || !memories.existsByWorkspaceIdAndSourceUrl(workspaceId, memory.getSourceUrl()))
                    .toList();
            memories.saveAll(imported);
            Instant syncedAt = Instant.now();
            integration.markSynced(syncedAt);
            integrations.save(integration);
            return new SyncResult(provider, imported.size(), syncedAt, "SYNCED", null);
        } catch (Exception exception) {
            integration.markSyncFailed(exception.getMessage());
            integrations.save(integration);
            if (exception instanceof ResponseStatusException responseStatusException) throw responseStatusException;
            throw new ResponseStatusException(BAD_GATEWAY, provider + " sync failed", exception);
        }
    }

    private List<EngineeringMemory> importJira(UUID workspaceId, String token) throws Exception {
        JsonNode resources = get("https://api.atlassian.com/oauth/token/accessible-resources", token);
        JsonNode resource = resources.isArray() && resources.size() > 0 ? resources.get(0) : null;
        if (resource == null || resource.path("id").asText().isBlank()) {
            throw new IllegalStateException("No accessible Jira site was returned");
        }
        String cloudId = resource.path("id").asText();
        String siteName = resource.path("name").asText("Jira site");
        JsonNode projects = get("https://api.atlassian.com/ex/jira/" + cloudId + "/rest/api/3/project/search?maxResults=100", token);
        List<EngineeringMemory> result = new ArrayList<>();
        String siteUrl = resource.path("url").asText("https://" + siteName);
        for (JsonNode project : projects.path("values")) {
            String key = project.path("key").asText();
            String name = project.path("name").asText(key);
            result.add(new EngineeringMemory(workspaceId, "Jira project: " + name, "jira",
                    "Project " + key + " is available in " + siteName + " for engineering planning and delivery context.",
                    siteUrl + "/plugins/servlet/project-config/" + key));
            if (key.isBlank()) continue;
            JsonNode issues = get("https://api.atlassian.com/ex/jira/" + cloudId
                    + "/rest/api/3/search?jql=project%3D" + key + "%20ORDER%20BY%20updated%20DESC&maxResults=50&fields=summary,status,description,updated", token);
            for (JsonNode issue : issues.path("issues")) {
                String issueKey = issue.path("key").asText();
                if (issueKey.isBlank()) continue;
                JsonNode fields = issue.path("fields");
                String summary = fields.path("summary").asText(issueKey);
                String status = fields.path("status").path("name").asText("Unknown");
                String description = flattenJiraText(fields.path("description"));
                String content = "Status: " + status + ". " + (description.isBlank() ? "No description provided." : description);
                result.add(new EngineeringMemory(workspaceId, "Jira " + issueKey + ": " + summary, "jira", content,
                        siteUrl + "/browse/" + issueKey));
            }
        }
        return result;
    }

    private List<EngineeringMemory> importSlack(UUID workspaceId, String token) throws Exception {
        JsonNode channels = get("https://slack.com/api/conversations.list?limit=100&exclude_archived=true", token);
        if (!channels.path("ok").asBoolean(false)) throw new IllegalStateException(channels.path("error").asText("Slack API error"));
        List<EngineeringMemory> result = new ArrayList<>();
        int channelCount = 0;
        for (JsonNode channel : channels.path("channels")) {
            String name = channel.path("name").asText();
            if (name.isBlank()) continue;
            String id = channel.path("id").asText();
            result.add(new EngineeringMemory(workspaceId, "Slack channel: #" + name, "slack",
                    "Shared engineering channel #" + name + " is available for team context and operational follow-up.",
                    id.isBlank() ? null : "slack://channel/" + id));
            if (id.isBlank() || channelCount++ >= 20) continue;
            JsonNode history = get("https://slack.com/api/conversations.history?channel=" + id + "&limit=50", token);
            if (!history.path("ok").asBoolean(false)) continue;
            for (JsonNode message : history.path("messages")) {
                String text = message.path("text").asText("").trim();
                String timestamp = message.path("ts").asText();
                if (text.isBlank() || timestamp.isBlank()) continue;
                result.add(new EngineeringMemory(workspaceId, "Slack #" + name + " message", "slack", text,
                        "slack://channel/" + id + "/message/" + timestamp));
            }
        }
        return result;
    }

    private String flattenJiraText(JsonNode node) {
        if (node == null || node.isNull()) return "";
        if (node.isTextual()) return node.asText();
        if (node.isArray()) {
            StringBuilder result = new StringBuilder();
            node.forEach(child -> {
                String value = flattenJiraText(child);
                if (!value.isBlank()) {
                    if (result.length() > 0) result.append(' ');
                    result.append(value);
                }
            });
            return result.toString();
        }
        if (node.isObject()) {
            StringBuilder result = new StringBuilder();
            node.fields().forEachRemaining(entry -> {
                String value = flattenJiraText(entry.getValue());
                if (!value.isBlank() && !entry.getKey().equals("type") && !entry.getKey().equals("version")) {
                    if (result.length() > 0) result.append(' ');
                    result.append(value);
                }
            });
            return result.toString();
        }
        return node.asText("");
    }

    private JsonNode get(String uri, String token) throws Exception {
        String body = restClient.get().uri(uri)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve().body(String.class);
        return objectMapper.readTree(body == null ? "{}" : body);
    }

    public record SyncResult(String provider, int importedMemories, Instant syncedAt, String status, String error) {}
}
