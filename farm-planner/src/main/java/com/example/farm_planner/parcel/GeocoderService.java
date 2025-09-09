package com.example.farm_planner.parcel;

import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.example.farm_planner.parcel.dto.NominatimResult;

import reactor.core.publisher.Mono;

/**
 * Turns a street address into [lat, lon] using Nominatim (OpenStreetMap).
 * We return a Mono<double[]> with {lat, lon}.
 */
@Service
public class GeocoderService {

  private final WebClient http;

  public GeocoderService(WebClient http) {
    this.http = http;
  }

  /**
   * Geocode an address and return the first result as [lat, lon].
   * If no result is found, the Mono completes empty.
   */
  public Mono<double[]> geocodeOne(String address) {
    // Nominatim Search API docs: https://nominatim.org/release-docs/latest/api/Search/
    return http.get()
        .uri(uri -> uri
            .scheme("https")
            .host("nominatim.openstreetmap.org")
            .path("/search")
            .queryParam("format", "jsonv2")
            .queryParam("limit", "1")
            .queryParam("addressdetails", "0")
            .queryParam("q", address)
            .build())
        .retrieve()
        .bodyToFlux(NominatimResult.class)
        .next() // first item or empty if none
        .map(r -> new double[] {
            Double.parseDouble(r.lat()),
            Double.parseDouble(r.lon())
        });
  }
}
