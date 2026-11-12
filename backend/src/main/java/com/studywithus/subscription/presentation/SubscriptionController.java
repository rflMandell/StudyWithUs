package com.studywithus.subscription.presentation;

import com.studywithus.identity.infrastructure.SecurityUserPrincipal;
import com.studywithus.subscription.application.CancelSubscriptionUseCase;
import com.studywithus.subscription.application.GetSubscriptionUseCase;
import com.studywithus.subscription.application.InitiateSubscriptionCommand;
import com.studywithus.subscription.application.InitiateSubscriptionUseCase;
import com.studywithus.subscription.domain.SubscriptionPlan;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/v1/subscription", "/api/v1/subscriptions"})
@RequiredArgsConstructor
public class SubscriptionController {

    private final GetSubscriptionUseCase getSubscriptionUseCase;
    private final InitiateSubscriptionUseCase initiateSubscriptionUseCase;
    private final CancelSubscriptionUseCase cancelSubscriptionUseCase;

    @GetMapping
    public ResponseEntity<SubscriptionResponse> getSubscription(
            @AuthenticationPrincipal SecurityUserPrincipal principal
    ) {
        var result = getSubscriptionUseCase.execute(principal.getId());
        return ResponseEntity.ok(SubscriptionResponse.from(result));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<InitiateSubscriptionResponse> initiateSubscription(
            @AuthenticationPrincipal SecurityUserPrincipal principal,
            @Valid @RequestBody(required = false) InitiateSubscriptionRequest request
    ) {
        SubscriptionPlan plan = request != null && request.plan() != null ? request.plan() : SubscriptionPlan.PREMIUM;
        String returnUrl = request != null ? request.returnUrl() : null;

        var command = new InitiateSubscriptionCommand(principal.getId(), plan, returnUrl);
        var result = initiateSubscriptionUseCase.execute(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(InitiateSubscriptionResponse.from(result));
    }

    @PostMapping("/cancel")
    public ResponseEntity<SubscriptionResponse> cancelSubscription(
            @AuthenticationPrincipal SecurityUserPrincipal principal
    ) {
        var result = cancelSubscriptionUseCase.execute(principal.getId());
        return ResponseEntity.ok(SubscriptionResponse.from(result));
    }
}
