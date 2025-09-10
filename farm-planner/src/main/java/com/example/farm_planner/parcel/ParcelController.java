package com.example.farm_planner.parcel;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
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
@Validated
public class ParcelController {

  private final GeocoderService geocoder;
  private final ParcelService parcels;

  public ParcelController(GeocoderService geocoder, ParcelService parcels) {
    this.geocoder = geocoder;
    this.parcels = parcels;
  }

  /** POST /api/parcels/search  { "address": "217 W Green St, Snow Hill, MD" } */
  @PostMapping("/search")
  public Mono<ResponseEntity<ParcelResponse>> search(@Valid @RequestBody AddressSearchRequest req) {
    return geocoder.geocodeOne(req.address())
        .flatMap(ll -> parcels.findByPoint(ll[0], ll[1]))
        .map(ResponseEntity::ok)
        .defaultIfEmpty(ResponseEntity.notFound().build());
  }

  /** GET /api/parcels/{acctId} – lookup by account id (adjust field name in service if needed) */
  @GetMapping("/{acctId}")
  public Mono<ResponseEntity<ParcelResponse>> findByAccount(@PathVariable String acctId) {
    return parcels.findByAcctId(acctId)
        .map(ResponseEntity::ok)
        .defaultIfEmpty(ResponseEntity.notFound().build());
  }
}
