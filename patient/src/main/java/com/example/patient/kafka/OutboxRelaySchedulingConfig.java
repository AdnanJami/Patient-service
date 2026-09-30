package com.example.patient.kafka;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

// Runs OutboxRelay in the background. Tests turn it off and call relay() themselves.
@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "outbox.relay.enabled", matchIfMissing = true)
public class OutboxRelaySchedulingConfig {
}
