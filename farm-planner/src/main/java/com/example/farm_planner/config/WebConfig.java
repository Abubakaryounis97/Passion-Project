package com.example.farm_planner.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebConfig {

  @Bean
  WebClient webClient(WebClient.Builder builder) {
    // Ensure a proper User-Agent for Nominatim and bump codecs if needed
    return builder
        .defaultHeader("User-Agent", "farm-planner/0.0.1 (+https://example.com)")
        .exchangeStrategies(ExchangeStrategies.builder()
            .codecs(c -> c.defaultCodecs().maxInMemorySize(4 * 1024 * 1024))
            .build())
        .build();
  }
}
