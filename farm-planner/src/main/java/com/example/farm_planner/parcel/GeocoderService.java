package com.example.farm_planner.parcel;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

import com.example.farm_planner.parcel.dto.NominatimResult;

import reactor.core.publisher.Mono;

/** Wraps Nominatim (OpenStreetMap) geocoding. Returns [lat, lon] for a given address. */
@Service
public class GeocoderService {

  private final WebClient http;
  private final String userAgent;

  public GeocoderService(WebClient http,
                         @Value("${app.nominatimUserAgent:FarmPlanner/0.1 (dev@example.com)}") String userAgent) {
    this.http = http;
    this.userAgent = userAgent;
  }

  public Mono<double[]> geocodeOne(String address) {
    var uri = UriComponentsBuilder
        .fromUriString("https://nominatim.openstreetmap.org/search")
        .queryParam("format", "jsonv2")
        .queryParam("limit", "1")
        .queryParam("addressdetails", "0")
        .queryParam("q", address)
        .build(true)   // keep existing encoding
        .toUri();

    return http.get()
        .uri(uri)
        .header(HttpHeaders.USER_AGENT, userAgent)
        .retrieve()
        .bodyToFlux(NominatimResult.class)
        .next()
        .map(r -> new double[] { Double.parseDouble(r.lat()), Double.parseDouble(r.lon()) });
  }
}
