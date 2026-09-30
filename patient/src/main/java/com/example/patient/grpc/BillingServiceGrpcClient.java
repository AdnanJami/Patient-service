package com.example.patient.grpc;

import billing.BillingRequest;
import billing.BillingResponse;
import billing.BillingServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class BillingServiceGrpcClient {
    private static final Logger log = LoggerFactory.getLogger(BillingServiceGrpcClient.class);

    // Upper bound on how long patient creation waits for billing, including a brief reconnect.
    private static final long DEADLINE_SECONDS = 3;

    private final ManagedChannel channel;
    private final BillingServiceGrpc.BillingServiceBlockingStub blockingStub;
    public BillingServiceGrpcClient(
            @Value("${billing.service.address:localhost}") String serverAddress,
            @Value("${billing.service.grpc.port:9001}") int serverPort
    ){
        log.info("Connecting to Billing Service gpc at {} :{}",serverAddress,serverPort);
        channel = ManagedChannelBuilder.forAddress(serverAddress,serverPort).usePlaintext().build();
        blockingStub =BillingServiceGrpc.newBlockingStub(channel);


    }

    /**
     * Best effort: a patient must still be created when billing is unavailable. The account can be
     * set up later through the billing API (POST /billing/accounts).
     *
     * @return true if billing confirmed the account
     */
    public boolean createBillingAccount(String patientId, String name, String email){
        BillingRequest request = BillingRequest.newBuilder().setPatientId(patientId)
                .setName(name).setEmail(email).build();
        try {
            BillingResponse response = blockingStub
                    .withWaitForReady()
                    .withDeadlineAfter(DEADLINE_SECONDS, TimeUnit.SECONDS)
                    .createBillingAccount(request);
            log.info("Received response from billing service via GRPC :{}",response);
            return true;
        } catch (StatusRuntimeException e) {
            log.warn("Billing account not created for patient {} ({}); it can be set up later",
                    patientId, e.getStatus());
            return false;
        }
    }

    @PreDestroy
    public void shutdown() {
        channel.shutdown();
    }
}
