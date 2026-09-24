package com.devcontext.integration.github;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GitHubWebhookControllerTest {
    @Mock ConnectedRepositoryRepository repositories;
    @Mock PullRequestRecordRepository pullRequests;

    @Test
    void acceptsSignedPullRequestEvent() throws Exception {
        String secret = "webhook-secret";
        String payload = "{\"repository\":{\"id\":42},\"pull_request\":{\"number\":7,\"title\":\"Fix retries\",\"user\":{\"login\":\"dev\"},\"state\":\"open\",\"html_url\":\"https://github.com/acme/api/pull/7\"}}";
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        String signature = "sha256=" + HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
        ConnectedRepository repository = new ConnectedRepository(UUID.randomUUID(), 42, "api", "acme/api", false, "https://github.com/acme/api");
        when(repositories.findAllByExternalId(42)).thenReturn(List.of(repository));
        when(pullRequests.findByRepositoryIdAndExternalNumber(any(), anyInt())).thenReturn(java.util.Optional.empty());
        when(pullRequests.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var result = new GitHubWebhookController(new ObjectMapper(), repositories, pullRequests, secret)
                .receive(signature, "pull_request", payload);

        assertThat(result.accepted()).isTrue();
        assertThat(result.updatedPullRequests()).isEqualTo(1);
    }
}
