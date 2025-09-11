package com.example.farm_planner.analysis.model;

import java.util.List;

public record QuickAnalysisResult(
    boolean found,
    String acctId,
    double parcelAcres,
    double usableAcres,
    double perHouseSqFt,
    int maxHouses,
    List<String> messages
) {}
