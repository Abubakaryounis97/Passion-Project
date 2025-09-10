package com.example.farm_planner.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebConfig {

  @Bean
  WebClient webClient(@Value("${app.nominatimUserAgent}") String ua) {
    return WebClient.builder()
        // Nominatim requires a real User-Agent (ideally with an email)
        .defaultHeader("User-Agent", ua)
        // Allow slightly larger JSON payloads from GIS (polygons can be big)
        .exchangeStrategies(ExchangeStrategies.builder()
            .codecs(c -> c.defaultCodecs().maxInMemorySize(10 * 1024 * 1024)) // 10 MB
            .build())
        .build();
  }
}