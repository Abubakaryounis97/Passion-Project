package com.example.farm_planner.parcel.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request body for POST /api/parcels/search
 * Example: { "address": "123 Main St, Pocomoke City, MD" }
 */
public record AddressSearchRequest(
    @NotBlank String address
) {}

