package com.example.farm_planner.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.web.reactive.function.client.WebClient;

import com.example.farm_planner.parcel.GeocoderService;
import com.example.farm_planner.parcel.ParcelService;
import com.example.farm_planner.parcel.dto.ParcelResponse;

import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

@Configuration
@Profile("mock")
public class MockConfig {

  @Bean
  @Primary
  GeocoderService geocoderMock() {
    return new GeocoderService(WebClient.create()) {
      @Override
      public Mono<double[]> geocodeOne(String address) {
        // Return a fixed point (lat, lon)
        return Mono.just(new double[] { 38.075, -75.568 });
      }
    };
  }

  @Bean
  @Primary
  ParcelService parcelServiceMock() {
    return new ParcelService(WebClient.create()) {
      @Override
      public Mono<ParcelResponse> findByPoint(double lat, double lon) {
        return Mono.just(sampleResponse("MOCK123"));
      }

      @Override
      public Mono<ParcelResponse> findByAcctId(String acctId) {
        return Mono.just(sampleResponse(acctId));
      }

      private ParcelResponse sampleResponse(String acctId) {
        Map<String,Object> geom = new HashMap<>();
        geom.put("type", "Polygon");
        // Simple triangle polygon in lon/lat (GeoJSON standard)
        geom.put("coordinates", new Object[] { new Object[] {
          new double[] { -75.57, 38.07 },
          new double[] { -75.56, 38.07 },
          new double[] { -75.565, 38.075 },
          new double[] { -75.57, 38.07 }
        }});
        return new ParcelResponse(acctId, 12.34, geom);
      }
    };
  }
}

