package com.example.farm_planner.parcel;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.farm_planner.parcel.dto.AddressSearchRequest;
import com.example.farm_planner.parcel.dto.ParcelResponse;

import jakarta.validation.Valid;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/parcels")
public class ParcelController {

  private final GeocoderService geocoder;
  private final ParcelService parcels;

  public ParcelController(GeocoderService geocoder, ParcelService parcels) {
    this.geocoder = geocoder;
    this.parcels = parcels;
  }

  @PostMapping("/search")
  public Mono<ResponseEntity<ParcelResponse>> searchByAddress(
      @Valid @RequestBody AddressSearchRequest req) {

    return geocoder.geocodeOne(req.address())
        .flatMap(ll -> parcels.findByPoint(ll[0], ll[1]))
        .map(ResponseEntity::ok)
        .defaultIfEmpty(ResponseEntity.notFound().build());
  }

  @GetMapping("/{acctId}")
  public Mono<ResponseEntity<ParcelResponse>> byAcct(@PathVariable String acctId) {
    return parcels.findByAcctId(acctId)
        .map(ResponseEntity::ok)
        .defaultIfEmpty(ResponseEntity.notFound().build());
  }
}
