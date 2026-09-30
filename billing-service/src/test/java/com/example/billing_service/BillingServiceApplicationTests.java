package com.example.billing_service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "spring.grpc.server.port=0")
class BillingServiceApplicationTests {

	@Test
	void contextLoads() {
	}

}
