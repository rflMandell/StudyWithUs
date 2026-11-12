package com.studywithus.subscription.presentation;

import com.studywithus.subscription.application.ProcessWebhookCommand;
import com.studywithus.subscription.application.ProcessWebhookUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/webhooks/subscription")
@RequiredArgsConstructor
public class SubscriptionWebhookController {

    private final ProcessWebhookUseCase processWebhookUseCase;

    @PostMapping
    public ResponseEntity<Map<String, Object>> handleWebhook(
            @RequestHeader(value = "X-Webhook-Signature", required = false) String signature,
            @RequestBody String rawPayload
    ) {
        var command = new ProcessWebhookCommand(signature, rawPayload);
        processWebhookUseCase.execute(command);
        return ResponseEntity.ok(Map.of("received", true));
    }
}
