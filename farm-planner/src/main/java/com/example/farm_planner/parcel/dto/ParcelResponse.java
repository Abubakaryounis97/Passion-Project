package com.example.farm_planner.parcel.dto;

import java.util.Map;

/**
 * Standard response for parcel lookups.
 * geometry: raw GeoJSON geometry object: { "type": "Polygon", "coordinates": [...] }
 */
public record ParcelResponse(
    String acctId,
    Double acres,
    Map<String, Object> geometry
) {}