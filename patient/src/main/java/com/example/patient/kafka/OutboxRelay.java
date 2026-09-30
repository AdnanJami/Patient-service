package com.example.patient.kafka;

import com.example.patient.model.OutboxEvent;
import com.example.patient.repository.OutboxEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

// Publishes outbox events to Kafka in the order they were written. An event is removed only after
// Kafka acknowledges it; if publishing fails (e.g. Kafka is down) the relay stops and retries on the
// next run, so later events never overtake an unpublished one. Delivery is at-least-once: a crash
// between publishing and removing an event republishes it, and the consumers handle duplicates.
@Component
public class OutboxRelay {
    private static final Logger log = LoggerFactory.getLogger(OutboxRelay.class);

    private final OutboxEventRepository outboxEventRepository;
    private final kafkaProducer kafkaProducer;
    private boolean failing = false;

    public OutboxRelay(OutboxEventRepository outboxEventRepository, kafkaProducer kafkaProducer) {
        this.outboxEventRepository = outboxEventRepository;
        this.kafkaProducer = kafkaProducer;
    }

    @Scheduled(fixedDelayString = "${outbox.relay.interval-ms:1000}")
    public synchronized void relay() {
        List<OutboxEvent> batch;
        while (!(batch = outboxEventRepository.findTop100ByOrderByIdAsc()).isEmpty()) {
            for (OutboxEvent event : batch) {
                try {
                    kafkaProducer.publish(event);
                } catch (Exception e) {
                    if (e instanceof InterruptedException) {
                        Thread.currentThread().interrupt();
                    }
                    // Log once per outage rather than on every retry.
                    if (!failing) {
                        failing = true;
                        log.warn("Can't publish {} event for patient {}, will retry; {} event(s) waiting in the outbox: {}",
                                event.getEventType(), event.getPatientId(), outboxEventRepository.count(),
                                NestedExceptionUtils.getMostSpecificCause(e).getMessage());
                    }
                    return;
                }
                outboxEventRepository.delete(event);
                if (failing) {
                    failing = false;
                    log.info("Publishing to Kafka again, sending the waiting outbox events");
                }
            }
        }
    }
}
