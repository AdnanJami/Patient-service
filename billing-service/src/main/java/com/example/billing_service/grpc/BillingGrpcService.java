package com.example.billing_service.grpc;

import billing.BillingRequest;
import billing.BillingResponse;
import billing.BillingServiceGrpc;
import com.example.billing_service.dto.AccountResponseDTO;
import com.example.billing_service.service.BillingService;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

@Service
public class BillingGrpcService extends BillingServiceGrpc.BillingServiceImplBase {
    private static final Logger log = LoggerFactory.getLogger(BillingGrpcService.class);

    private final BillingService billingService;

    public BillingGrpcService(BillingService billingService) {
        this.billingService = billingService;
    }

    @Override
    public void createBillingAccount(BillingRequest billingRequest,
                                     StreamObserver<BillingResponse> responseObserver) {

        log.info("createBillingAccount request received {}", billingRequest.toString());

        UUID patientId;
        try {
            patientId = UUID.fromString(billingRequest.getPatientId());
        } catch (IllegalArgumentException e) {
            responseObserver.onError(Status.INVALID_ARGUMENT
                    .withDescription("patientId must be a UUID").asRuntimeException());
            return;
        }

        AccountResponseDTO account = billingService.getOrCreateAccount(
                patientId, billingRequest.getName(), billingRequest.getEmail());

        BillingResponse response = BillingResponse.newBuilder()
                .setAccountId(account.id().toString())
                .setStatus(account.status())
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();

    }
}
