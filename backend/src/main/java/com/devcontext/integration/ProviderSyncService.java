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
            List<EngineeringMemory> imported = provider.equals("JIRA")
                    ? importJira(workspaceId, client.getAccessToken().getTokenValue())
                    : importSlack(workspaceId, client.getAccessToken().getTokenValue());
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
        for (JsonNode project : projects.path("values")) {
            String key = project.path("key").asText();
            String name = project.path("name").asText(key);
            result.add(new EngineeringMemory(workspaceId, "Jira project: " + name, "jira",
                    "Project " + key + " is available in " + siteName + " for engineering planning and delivery context.",
                    "https://api.atlassian.com/ex/jira/" + cloudId + "/projects/" + key));
        }
        return result;
    }

    private List<EngineeringMemory> importSlack(UUID workspaceId, String token) throws Exception {
        JsonNode channels = get("https://slack.com/api/conversations.list?limit=100&exclude_archived=true", token);
        if (!channels.path("ok").asBoolean(false)) throw new IllegalStateException(channels.path("error").asText("Slack API error"));
        List<EngineeringMemory> result = new ArrayList<>();
        for (JsonNode channel : channels.path("channels")) {
            String name = channel.path("name").asText();
            if (name.isBlank()) continue;
            String id = channel.path("id").asText();
            result.add(new EngineeringMemory(workspaceId, "Slack channel: #" + name, "slack",
                    "Shared engineering channel #" + name + " is available for team context and operational follow-up.",
                    id.isBlank() ? null : "slack://channel/" + id));
        }
        return result;
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
