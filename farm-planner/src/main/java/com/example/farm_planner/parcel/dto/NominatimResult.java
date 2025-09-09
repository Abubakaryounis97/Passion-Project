package com.example.farm_planner.parcel.dto;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Minimal fields from Nominatim's /search response.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record NominatimResult(
    String lat,
    String lon,
    String display_name
) {}