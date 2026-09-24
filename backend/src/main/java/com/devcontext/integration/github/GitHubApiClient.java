package com.devcontext.integration.github;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.ArrayList;
import java.util.stream.StreamSupport;

@Component
public class GitHubApiClient {
    private final RestClient github;

    public GitHubApiClient(RestClient.Builder restClientBuilder) {
        this.github = restClientBuilder.baseUrl("https://api.github.com").build();
    }

    public List<GitHubRepositoryData> repositories(OAuth2AuthorizedClient client) {
        List<GitHubRepositoryData> result = new ArrayList<>();
        for (int page = 1; page <= 10; page++) {
            int currentPage = page;
            JsonNode response = github.get().uri(uri -> uri.path("/user/repos").queryParam("sort", "updated")
                            .queryParam("per_page", 100).queryParam("page", currentPage).build())
                    .headers(headers -> headers.setBearerAuth(client.getAccessToken().getTokenValue()))
                    .retrieve().body(JsonNode.class);
            if (response == null || !response.isArray()) break;
            result.addAll(StreamSupport.stream(response.spliterator(), false).map(repo -> new GitHubRepositoryData(
                    repo.path("id").asLong(), repo.path("name").asText(), repo.path("full_name").asText(),
                    repo.path("private").asBoolean(), repo.path("html_url").asText())).toList());
            if (response.size() < 100) break;
        }
        return result;
    }

    public List<GitHubPullRequestData> pullRequests(OAuth2AuthorizedClient client, String fullName) {
        String[] repository = fullName.split("/", 2);
        if (repository.length != 2) return List.of();
        List<GitHubPullRequestData> result = new ArrayList<>();
        for (int page = 1; page <= 10; page++) {
            int currentPage = page;
            JsonNode response = github.get().uri(uri -> uri.pathSegment("repos", repository[0], repository[1], "pulls")
                            .queryParam("state", "all").queryParam("sort", "updated").queryParam("per_page", 50)
                            .queryParam("page", currentPage).build())
                    .headers(headers -> headers.setBearerAuth(client.getAccessToken().getTokenValue()))
                    .retrieve().body(JsonNode.class);
            if (response == null || !response.isArray()) break;
            result.addAll(StreamSupport.stream(response.spliterator(), false).map(pr -> new GitHubPullRequestData(
                    pr.path("number").asInt(), pr.path("title").asText(), pr.path("user").path("login").asText(),
                    pr.path("state").asText(), pr.path("html_url").asText())).toList());
            if (response.size() < 50) break;
        }
        return result;
    }

    public record GitHubRepositoryData(long externalId, String name, String fullName, boolean privateRepository, String url) {}
    public record GitHubPullRequestData(int number, String title, String authorLogin, String state, String url) {}
}
