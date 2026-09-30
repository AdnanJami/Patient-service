package com.example.api_gateway.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import org.junit.jupiter.api.Test;
import reactor.netty.http.client.HttpClient;

class DnsCacheConfigTest {

    @Test
    void capsDnsCacheWellBelowDockerTtl() {
        HttpClient httpClient = new DnsCacheConfig().shortDnsCacheHttpClientCustomizer()
                .customize(HttpClient.create());

        Duration maxTtl = httpClient.configuration().getNameResolverProvider().cacheMaxTimeToLive();

        assertThat(maxTtl).isEqualTo(DnsCacheConfig.DNS_CACHE_MAX_TTL);
        assertThat(maxTtl).isLessThan(Duration.ofSeconds(600));
    }
}
