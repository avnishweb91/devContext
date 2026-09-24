package com.devcontext.integration.github;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.stream.StreamSupport;

@Component
public class GitHubApiClient {
    private final RestClient github;

    public GitHubApiClient(RestClient.Builder restClientBuilder) {
        this.github = restClientBuilder.baseUrl("https://api.github.com").build();
    }

    public List<GitHubRepositoryData> repositories(OAuth2AuthorizedClient client) {
        JsonNode response = github.get().uri(uri -> uri.path("/user/repos").queryParam("sort", "updated").queryParam("per_page", 100).build())
                .headers(headers -> headers.setBearerAuth(client.getAccessToken().getTokenValue()))
                .retrieve().body(JsonNode.class);
        if (response == null || !response.isArray()) return List.of();
        return StreamSupport.stream(response.spliterator(), false).map(repo -> new GitHubRepositoryData(
                repo.path("id").asLong(), repo.path("name").asText(), repo.path("full_name").asText(),
                repo.path("private").asBoolean(), repo.path("html_url").asText())).toList();
    }

    public List<GitHubPullRequestData> pullRequests(OAuth2AuthorizedClient client, String fullName) {
        String[] repository = fullName.split("/", 2);
        if (repository.length != 2) return List.of();
        JsonNode response = github.get().uri(uri -> uri.pathSegment("repos", repository[0], repository[1], "pulls").queryParam("state", "all").queryParam("sort", "updated").queryParam("per_page", 50).build())
                .headers(headers -> headers.setBearerAuth(client.getAccessToken().getTokenValue()))
                .retrieve().body(JsonNode.class);
        if (response == null || !response.isArray()) return List.of();
        return StreamSupport.stream(response.spliterator(), false).map(pr -> new GitHubPullRequestData(
                pr.path("number").asInt(), pr.path("title").asText(), pr.path("user").path("login").asText(),
                pr.path("state").asText(), pr.path("html_url").asText())).toList();
    }

    public record GitHubRepositoryData(long externalId, String name, String fullName, boolean privateRepository, String url) {}
    public record GitHubPullRequestData(int number, String title, String authorLogin, String state, String url) {}
}
