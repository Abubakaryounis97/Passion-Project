package com.example.farm_planner.parcel.dto;


import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Minimal GeoJSON model for the ArcGIS query (f=geojson).
 * We only need properties + geometry from the first feature.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class GeoJsonFeatureCollection {
  public String type;
  public List<Feature> features;

  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class Feature {
    public String type;
    public Map<String, Object> properties;
    public Map<String, Object> geometry; // {type, coordinates}
  }
}
