package com.marcos.codereviewagent.application;

import com.marcos.codereviewagent.domain.port.PullRequestProvider;
import com.marcos.codereviewagent.domain.service.ReviewFormatter;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class WebhookSignatureTest {

    private final WebhookController controller = new WebhookController(
            mock(PullRequestProvider.class),
            mock(ReviewFormatter.class));

    @Test
    void shouldAcceptValidSignature() {
        // given
        var secret = "test-secret";
        var payload = "Hello, World!";
        ReflectionTestUtils.setField(controller, "webhookSecret", secret);

        var signature = computeSignature(secret, payload);

        // when / then
        assertThat(controller.verifySignature(payload, signature)).isTrue();
    }

    @Test
    void shouldRejectInvalidSignature() {
        // given
        ReflectionTestUtils.setField(controller, "webhookSecret", "test-secret");

        // when / then
        assertThat(controller.verifySignature("payload", "sha256=invalid")).isFalse();
    }

    @Test
    void shouldAcceptAnyPayloadWhenNoSecretConfigured() {
        // given — empty secret (default)
        ReflectionTestUtils.setField(controller, "webhookSecret", "");

        // when / then
        assertThat(controller.verifySignature("payload", null)).isTrue();
    }

    @Test
    void shouldRejectMissingSignatureWhenSecretConfigured() {
        // given
        ReflectionTestUtils.setField(controller, "webhookSecret", "secret");

        // when / then
        assertThat(controller.verifySignature("payload", null)).isFalse();
    }

    private String computeSignature(String secret, String payload) {
        try {
            var mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            var hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return "sha256=" + HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException(e);
        }
    }
}
