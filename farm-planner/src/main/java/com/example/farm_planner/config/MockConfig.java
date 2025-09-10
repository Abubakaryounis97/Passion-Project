package com.example.farm_planner.config;

import java.util.Map;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

import com.example.farm_planner.parcel.GeocoderService;
import com.example.farm_planner.parcel.ParcelService;
import com.example.farm_planner.parcel.dto.ParcelResponse;

import reactor.core.publisher.Mono;

/**
 * Provides mock services when the "mock" profile is active.
 * Run with:  -Dspring-boot.run.profiles=mock
 */
@Configuration
@Profile("mock")
public class MockConfig {

  /** Mock geocoder returns fixed coords near Snow Hill, MD. */
  @Bean
  @Primary
  GeocoderService mockGeocoderService() {
    return new GeocoderService(null, "FarmPlanner/0.1 (mock)") {
      @Override
      public Mono<double[]> geocodeOne(String address) {
        // Lat/Lon near Snow Hill
        return Mono.just(new double[] { 38.1779, -75.3924 });
      }
    };
  }

  /** Mock parcel service returns a tiny square polygon and fake attributes. */
  @Bean
  @Primary
  ParcelService mockParcelService() {
    return new ParcelService(null, "mock://layer") {
      @Override
      public Mono<ParcelResponse> findByPoint(double lat, double lon) {
        return Mono.just(mockParcel());
      }

      @Override
      public Mono<ParcelResponse> findByAcctId(String acctId) {
        return Mono.just(mockParcel());
      }

      private ParcelResponse mockParcel() {
        // Simple square polygon around a point
        Map<String, Object> geometry = Map.of(
            "type", "Polygon",
            "coordinates", new double[][][] {
                { { -75.3926, 38.1778 }, { -75.3922, 38.1778 }, { -75.3922, 38.1780 }, { -75.3926, 38.1780 }, { -75.3926, 38.1778 } }
            }
        );
        return new ParcelResponse("MOCK123", 5.25, geometry);
      }
    };
  }
}
