package com.example.api_gateway.config;

import java.time.Duration;

import org.springframework.boot.http.client.autoconfigure.reactive.ClientHttpConnectorBuilderCustomizer;
import org.springframework.boot.http.client.reactive.ReactorClientHttpConnectorBuilder;
import org.springframework.cloud.gateway.config.HttpClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.netty.http.client.HttpClient;

/**
 * Caps how long the gateway caches DNS answers for downstream services.
 *
 * Reactor Netty caches each answer for its DNS TTL, and Docker's embedded DNS
 * hands out a TTL of 600s. When a container is recreated with a new IP, the
 * gateway kept connecting to the old one for up to 10 minutes.
 */
@Configuration
public class DnsCacheConfig {

    static final Duration DNS_CACHE_MAX_TTL = Duration.ofSeconds(5);

    static HttpClient withShortDnsCache(HttpClient httpClient) {
        return httpClient.resolver(spec -> spec.cacheMaxTimeToLive(DNS_CACHE_MAX_TTL));
    }

    // HttpClient used to proxy routed requests
    @Bean
    public HttpClientCustomizer shortDnsCacheHttpClientCustomizer() {
        return DnsCacheConfig::withShortDnsCache;
    }

    // HttpClient behind the auto-configured WebClient.Builder (JWT validation calls)
    @Bean
    public ClientHttpConnectorBuilderCustomizer<ReactorClientHttpConnectorBuilder> shortDnsCacheConnectorCustomizer() {
        return builder -> builder.withHttpClientCustomizer(DnsCacheConfig::withShortDnsCache);
    }
}
