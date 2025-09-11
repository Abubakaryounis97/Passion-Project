package com.example.farm_planner.parcel.dto;

import java.util.List;
import java.util.Map;

/** Minimal model for ArcGIS f=json (returnGeometry=false) result. */
public class ArcJsonFeatureSet {
  public List<Feature> features;

  public static class Feature {
    public Map<String, Object> attributes;
  }
}

