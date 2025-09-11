package com.example.farm_planner.parcel;

import java.net.URI;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

import com.example.farm_planner.parcel.dto.GeoJsonFeatureCollection;
import com.example.farm_planner.parcel.dto.ParcelResponse;
import com.fasterxml.jackson.databind.ObjectMapper;

import reactor.core.publisher.Mono;

/** Queries the county ArcGIS layer and maps results to ParcelResponse. */
@Service
public class ParcelService {

  private final WebClient http;
  private final String layerUrl;
  private static final ObjectMapper M = new ObjectMapper();

  public ParcelService(WebClient http,
                       @Value("${app.worcesterParcelsLayerUrl}") String layerUrl) {
    this.http = http;
    this.layerUrl = layerUrl; // e.g. .../MapServer/5
  }

  /** Find the parcel intersecting the given WGS84 point (lat, lon). */
  public Mono<ParcelResponse> findByPoint(double lat, double lon) {
    // ArcGIS expects x = lon, y = lat, wkid 4326
    var geometry = Map.of(
        "x", lon,
        "y", lat,
        "spatialReference", Map.of("wkid", 4326)
    );

    MultiValueMap<String, String> q = new LinkedMultiValueMap<>();
    q.add("f", "geojson");
    q.add("where", "1=1");
    q.add("geometry", toJson(geometry));
    q.add("geometryType", "esriGeometryPoint");
    q.add("inSR", "4326");
    q.add("outSR", "4326");
    q.add("spatialRel", "esriSpatialRelIntersects");
    q.add("returnGeometry", "true");
    q.add("outFields", "*");

    URI uri = UriComponentsBuilder.fromUriString(layerUrl + "/query")
        .queryParams(q)
        .build()
        .encode()
        .toUri();

    return http.get()
        .uri(uri)
        .exchangeToMono(resp -> {
          HttpStatusCode sc = resp.statusCode();
          if (sc.is2xxSuccessful()) {
            return resp.bodyToMono(GeoJsonFeatureCollection.class);
          }
          return Mono.empty();
        })
        .flatMap(fc -> {
          if (fc == null || fc.features == null || fc.features.isEmpty()) return Mono.empty();
          var f = fc.features.get(0);
          return Mono.just(toParcelResponse(f));
        });
  }

  /** Lookup by (partial) account id. Adjust the field name as needed for your layer. */
  public Mono<ParcelResponse> findByAcctId(String acctId) {
    // TODO: Confirm the actual field name in your layer (e.g., ACCOUNTID, ACCOUNTNO, ACCT).
    String where = "UPPER(COALESCE(ACCOUNTID, COALESCE(ACCOUNT, COALESCE(ACCT, AcctId)))) LIKE UPPER('%" + sanitize(acctId) + "%')";

    MultiValueMap<String, String> q = new LinkedMultiValueMap<>();
    q.add("f", "geojson");
    q.add("where", where);
    q.add("returnGeometry", "true");
    q.add("outFields", "*");
    q.add("outSR", "4326");

    URI uri = UriComponentsBuilder.fromUriString(layerUrl + "/query")
        .queryParams(q)
        .build()
        .encode()
        .toUri();

    return http.get()
        .uri(uri)
        .exchangeToMono(resp -> {
          HttpStatusCode sc = resp.statusCode();
          if (sc.is2xxSuccessful()) {
            return resp.bodyToMono(GeoJsonFeatureCollection.class);
          }
          return Mono.empty();
        })
        .flatMap(fc -> {
          if (fc == null || fc.features == null || fc.features.isEmpty()) return Mono.empty();
          var f = fc.features.get(0);
          return Mono.just(toParcelResponse(f));
        });
  }

  private ParcelResponse toParcelResponse(GeoJsonFeatureCollection.Feature f) {
    Map<String, Object> props = f.properties;
    String acct = firstNonNullString(props, List.of("ACCOUNTID", "ACCOUNT", "ACCT", "AcctId"));
    Double acres = toDouble(firstNonNull(props, List.of("ACRES", "Acres")));
    // geometry is passed through as-is (GeoJSON shape)
    return new ParcelResponse(acct, acres, f.geometry);
  }

  /* ---------- helpers ---------- */

  private static String toJson(Object o) {
    try { return M.writeValueAsString(o); } catch (Exception e) { throw new RuntimeException(e); }
  }

  private static String sanitize(String s) {
    return s == null ? "" : s.replace("'", "''");
  }

  private static Object firstNonNull(Map<String, Object> props, List<String> keys) {
    for (String k : keys) {
      Object v = props.get(k);
      if (v != null) return v;
    }
    return null;
  }

  private static String firstNonNullString(Map<String, Object> props, List<String> keys) {
    Object v = firstNonNull(props, keys);
    return v == null ? null : v.toString();
  }

  private static Double toDouble(Object v) {
    if (v == null) return null;
    try { return Double.valueOf(v.toString()); } catch (NumberFormatException e) { return null; }
  }
}
