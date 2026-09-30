package com.example.patient;

import com.example.patient.kafka.OutboxRelay;
import com.example.patient.kafka.kafkaProducer;
import com.example.patient.model.OutboxEvent;
import com.example.patient.repository.OutboxEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;

import static com.example.patient.kafka.kafkaProducer.PATIENT_CREATED;
import static com.example.patient.kafka.kafkaProducer.PATIENT_DELETED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest(properties = "outbox.relay.enabled=false")
class OutboxRelayTests {

    @Autowired
    private OutboxRelay outboxRelay;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @MockitoBean
    private kafkaProducer kafkaProducer;

    @BeforeEach
    void emptyOutbox() {
        outboxEventRepository.deleteAll();
    }

    @Test
    void keepsEventsWhileKafkaIsDownThenPublishesThemInOrder() throws Exception {
        outboxEventRepository.save(new OutboxEvent("p-1", PATIENT_CREATED, new byte[]{1}));
        outboxEventRepository.save(new OutboxEvent("p-2", PATIENT_CREATED, new byte[]{2}));
        outboxEventRepository.save(new OutboxEvent("p-1", PATIENT_DELETED, new byte[]{3}));

        // Kafka down: the first event fails, nothing is removed and nothing later is attempted.
        doThrow(new IllegalStateException("broker unavailable")).when(kafkaProducer).publish(any());
        outboxRelay.relay();
        verify(kafkaProducer, times(1)).publish(any());
        assertThat(outboxEventRepository.count()).isEqualTo(3);

        // Kafka back: everything is published in the original order and removed.
        reset(kafkaProducer);
        doNothing().when(kafkaProducer).publish(any());
        outboxRelay.relay();

        ArgumentCaptor<OutboxEvent> published = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(kafkaProducer, times(3)).publish(published.capture());
        assertThat(published.getAllValues())
                .extracting(event -> event.getPayload()[0])
                .containsExactly((byte) 1, (byte) 2, (byte) 3);
        assertThat(outboxEventRepository.count()).isZero();
    }

    @Test
    void failureMidwayKeepsTheFailedEventAndEverythingAfterIt() throws Exception {
        OutboxEvent first = outboxEventRepository.save(new OutboxEvent("p-1", PATIENT_CREATED, new byte[]{1}));
        OutboxEvent second = outboxEventRepository.save(new OutboxEvent("p-2", PATIENT_CREATED, new byte[]{2}));
        OutboxEvent third = outboxEventRepository.save(new OutboxEvent("p-3", PATIENT_CREATED, new byte[]{3}));

        doNothing().when(kafkaProducer).publish(any());
        doThrow(new IllegalStateException("broker unavailable"))
                .when(kafkaProducer).publish(argThat(e -> e.getId().equals(second.getId())));

        outboxRelay.relay();

        List<Long> remaining = outboxEventRepository.findTop100ByOrderByIdAsc().stream().map(OutboxEvent::getId).toList();
        assertThat(remaining).containsExactly(second.getId(), third.getId());
        assertThat(remaining).doesNotContain(first.getId());
    }
}
