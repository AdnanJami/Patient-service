package com.example.patient;

import com.example.patient.grpc.BillingServiceGrpcClient;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.ServerSocket;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

class BillingServiceGrpcClientTests {

    @Test
    void unreachableBillingDoesNotThrowAndGivesUpWithinDeadline() throws IOException {
        int unusedPort;
        try (ServerSocket socket = new ServerSocket(0)) {
            unusedPort = socket.getLocalPort();
        }
        BillingServiceGrpcClient client = new BillingServiceGrpcClient("localhost", unusedPort);

        try {
            long start = System.nanoTime();
            assertThatNoException().isThrownBy(() ->
                    assertThat(client.createBillingAccount("p-1", "Jane", "jane@example.com")).isFalse());
            assertThat(Duration.ofNanos(System.nanoTime() - start)).isLessThan(Duration.ofSeconds(5));
        } finally {
            client.shutdown();
        }
    }
}
