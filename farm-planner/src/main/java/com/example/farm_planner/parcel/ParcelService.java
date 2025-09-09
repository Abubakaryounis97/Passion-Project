package com.example.farm_planner.parcel;

import com.example.farm_planner.parcel.dto.GeoJsonFeatureCollection;
import com.example.farm_planner.parcel.dto.ParcelResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * Calls Worcester County's ArcGIS Parcels layer to get
 * ACCTID, Acres, and Polygon geometry (GeoJSON).
 */
@Service
public class ParcelService {

  private final WebClient http;

  @Value("${app.worcesterParcelsLayerUrl}")
  private String layerUrl; // e.g., https://.../MapServer/5

  public ParcelService(WebClient http) {
    this.http = http;
  }

  /**
   * Find the parcel containing the given point (lat, lon).
   * Returns a Mono<ParcelResponse> or empty if none found.
   */
  public Mono<ParcelResponse> findByPoint(double lat, double lon) {
    var params = new LinkedMultiValueMap<String, String>();

    // ArcGIS expects geometry as JSON; outSR=4326 returns lon/lat
    String geometryJson = String.format(
        "{\"x\":%f,\"y\":%f,\"spatialReference\":{\"wkid\":4326}}",
        lon, lat // NOTE: x=lon, y=lat
    );

    params.add("geometry", geometryJson);
    params.add("geometryType", "esriGeometryPoint");
    params.add("inSR", "4326");
    params.add("spatialRel", "esriSpatialRelIntersects");
    params.add("outFields", "ACCTID,Acres");
    params.add("returnGeometry", "true");
    params.add("outSR", "4326");
    params.add("f", "geojson");

    return http.get()
        .uri(uri -> uri.path(layerUrl + "/query").queryParams(params).build())
        .retrieve()
        .bodyToMono(GeoJsonFeatureCollection.class)
        .flatMap(fc -> (fc.features == null || fc.features.isEmpty())
            ? Mono.empty()
            : Mono.just(fromFeature(fc.features.get(0))));
  }

  /**
   * Fetch a parcel directly by ACCTID.
   * Returns a Mono<ParcelResponse> or empty if not found.
   */
  public Mono<ParcelResponse> findByAcctId(String acctId) {
    var params = new LinkedMultiValueMap<String, String>();
    // Basic where-clause on ACCTID; quotes around value
    params.add("where", "ACCTID='" + acctId.replace("'", "''") + "'");
    params.add("outFields", "ACCTID,Acres");
    params.add("returnGeometry", "true");
    params.add("outSR", "4326");
    params.add("f", "geojson");

    return http.get()
        .uri(uri -> uri.path(layerUrl + "/query").queryParams(params).build())
        .retrieve()
        .bodyToMono(GeoJsonFeatureCollection.class)
        .flatMap(fc -> (fc.features == null || fc.features.isEmpty())
            ? Mono.empty()
            : Mono.just(fromFeature(fc.features.get(0))));
  }

  private ParcelResponse fromFeature(GeoJsonFeatureCollection.Feature f) {
    Map<String, Object> props = f.properties;
    String acct = props != null && props.get("ACCTID") != null
        ? props.get("ACCTID").toString()
        : null;
    Double acres = props != null && props.get("Acres") != null
        ? toDouble(props.get("Acres"))
        : null;

    return new ParcelResponse(acct, acres, f.geometry);
  }

  private static Double toDouble(Object v) {
    try {
      return v == null ? null : Double.valueOf(v.toString());
    } catch (NumberFormatException e) {
      return null;
    }
  }
}