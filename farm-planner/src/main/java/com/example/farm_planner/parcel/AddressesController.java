package com.example.farm_planner.parcel;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

import com.example.farm_planner.parcel.dto.ArcJsonFeatureSet;

import reactor.core.publisher.Mono;

/**
 * Helper endpoint to fetch a list of parcel addresses from the county layer
 * so you can quickly test the app with real data.
 */
@RestController
@RequestMapping("/api/parcels")
public class AddressesController {

  private final WebClient http;
  private final String layerUrl;

  public AddressesController(WebClient http,
                             @Value("${app.worcesterParcelsLayerUrl}") String layerUrl) {
    this.http = http;
    this.layerUrl = layerUrl;
  }

  /**
   * GET /api/parcels/addresses?minAcres=10&limit=50[&town=Snow%20Hill]
   * Returns a list of {acctId, acres, siteAddress, situs}
   */
  @GetMapping("/addresses")
  public Mono<ResponseEntity<List<Map<String,Object>>>> list(
      @RequestParam(defaultValue = "10") double minAcres,
      @RequestParam(defaultValue = "50") int limit,
      @RequestParam(required = false) String town
  ) {
    StringBuilder where = new StringBuilder();
    where.append("ACRES >= ").append(minAcres)
         .append(" AND SITEADDRESS IS NOT NULL AND SITEADDRESS <> ''");
    if (town != null && !town.isBlank()) {
      String t = town.replace("'", "''");
      where.append(" AND UPPER(SITUS) LIKE UPPER('%").append(t).append("%')");
    }

    MultiValueMap<String,String> q = new LinkedMultiValueMap<>();
    q.add("where", where.toString());
    q.add("outFields", "ACCTID,ACRES,SITEADDRESS,SITUS");
    q.add("returnGeometry", "false");
    q.add("resultRecordCount", String.valueOf(Math.max(1, Math.min(limit, 500))));
    q.add("f", "json");

    URI uri = UriComponentsBuilder.fromUriString(layerUrl + "/query")
        .queryParams(q)
        .build()
        .encode()
        .toUri();

    return http.get()
        .uri(uri)
        .exchangeToMono(resp -> resp.statusCode().is2xxSuccessful()
            ? resp.bodyToMono(ArcJsonFeatureSet.class)
            : Mono.just(new ArcJsonFeatureSet()))
        .map(fs -> {
          List<Map<String,Object>> out = new ArrayList<>();
          if (fs != null && fs.features != null) {
            for (var f : fs.features) {
              if (f.attributes == null) continue;
              Object acct = f.attributes.get("ACCTID");
              Object acres = f.attributes.get("ACRES");
              Object addr = f.attributes.get("SITEADDRESS");
              Object situs = f.attributes.get("SITUS");
              out.add(Map.of(
                  "acctId", acct,
                  "acres", acres,
                  "siteAddress", addr,
                  "situs", situs
              ));
            }
          }
          return ResponseEntity.ok(out);
        });
  }
}

