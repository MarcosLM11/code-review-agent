package com.marcos.codereviewagent.application;

import com.marcos.codereviewagent.domain.port.PullRequestProvider;
import com.marcos.codereviewagent.domain.service.ReviewFormatter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.json.JsonMapper;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

@Slf4j
@RestController
@RequestMapping("/api/v1/webhook")
@RequiredArgsConstructor
public class WebhookController {

    private static final String HMAC_SHA256 = "HmacSHA256";
    private static final String SIGNATURE_PREFIX = "sha256=";
    private static final JsonMapper JSON_MAPPER = JsonMapper.builder().build();

    private final PullRequestProvider pullRequestProvider;
    private final ReviewFormatter reviewFormatter;

    @Value("${app.webhook.secret:}")
    private String webhookSecret;

    @PostMapping
    public ResponseEntity<String> handleWebhook(
            @RequestHeader(value = "X-GitHub-Event", required = false) String event,
            @RequestHeader(value = "X-Hub-Signature-256", required = false) String signature,
            @RequestBody String body) {

        if (!"pull_request".equals(event)) {
            log.debug("Ignoring non-PR event: {}", event);
            return ResponseEntity.ok("ignored");
        }

        if (!verifySignature(body, signature)) {
            log.warn("Invalid webhook signature");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("invalid signature");
        }

        var payload = JSON_MAPPER.readTree(body);
        var action = payload.path("action").asString();

        if (!"opened".equals(action) && !"synchronize".equals(action)) {
            log.debug("Ignoring PR action: {}", action);
            return ResponseEntity.ok("ignored");
        }

        var owner = payload.path("repository").path("owner").path("login").asString();
        var repo = payload.path("repository").path("name").asString();
        var prNumber = payload.path("pull_request").path("number").asInt();

        log.info("Processing PR webhook: {}/{} #{} ({})", owner, repo, prNumber, action);

        var diff = pullRequestProvider.fetchPullRequest(owner, repo, prNumber);
        var reviewBody = reviewFormatter.formatSummary(diff);
        var posted = pullRequestProvider.postReview(owner, repo, prNumber, reviewBody);

        if (posted) {
            log.info("Review posted to {}/{} #{}", owner, repo, prNumber);
            return ResponseEntity.ok("review posted");
        }

        log.warn("Failed to post review to {}/{} #{}", owner, repo, prNumber);
        return ResponseEntity.ok("review failed");
    }

    boolean verifySignature(String payload, String signature) {
        if (webhookSecret == null || webhookSecret.isBlank()) {
            return true;
        }
        if (signature == null || !signature.startsWith(SIGNATURE_PREFIX)) {
            return false;
        }

        try {
            var mac = Mac.getInstance(HMAC_SHA256);
            mac.init(new SecretKeySpec(webhookSecret.getBytes(StandardCharsets.UTF_8), HMAC_SHA256));
            var expectedHash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            var expectedSignature = SIGNATURE_PREFIX + HexFormat.of().formatHex(expectedHash);

            return MessageDigest.isEqual(
                    expectedSignature.getBytes(StandardCharsets.UTF_8),
                    signature.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            log.error("Signature verification failed", e);
            return false;
        }
    }
}
