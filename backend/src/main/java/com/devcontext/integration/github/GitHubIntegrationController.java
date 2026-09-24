package com.devcontext.integration.github;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.StreamSupport;

@RestController
@RequestMapping("/api/integrations/github")
public class GitHubIntegrationController {
    private final OAuth2AuthorizedClientService authorizedClients;
    private final RestClient github;

    public GitHubIntegrationController(OAuth2AuthorizedClientService authorizedClients, RestClient.Builder restClientBuilder) {
        this.authorizedClients = authorizedClients;
        this.github = restClientBuilder.baseUrl("https://api.github.com").build();
    }

    @GetMapping(value = "/repositories", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<RepositorySummary> repositories(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sign in with GitHub first");
        }
        OAuth2AuthorizedClient client = authorizedClients.loadAuthorizedClient("github", authentication.getName());
        if (client == null || client.getAccessToken() == null) {
            throw new ResponseStatusException(HttpStatus.PRECONDITION_REQUIRED, "GitHub is not connected");
        }
        JsonNode response = github.get()
                .uri(uriBuilder -> uriBuilder.path("/user/repos").queryParam("sort", "updated").queryParam("per_page", 100).build())
                .headers(headers -> headers.setBearerAuth(client.getAccessToken().getTokenValue()))
                .retrieve()
                .body(JsonNode.class);
        if (response == null || !response.isArray()) return List.of();
        return StreamSupport.stream(response.spliterator(), false).map(repo -> new RepositorySummary(
                repo.path("id").asLong(),
                repo.path("name").asText(),
                repo.path("full_name").asText(),
                repo.path("private").asBoolean(),
                repo.path("html_url").asText()
        )).toList();
    }

    public record RepositorySummary(long id, String name, String fullName, boolean privateRepository, String url) {}
}
