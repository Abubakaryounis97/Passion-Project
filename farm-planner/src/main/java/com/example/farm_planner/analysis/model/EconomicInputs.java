package com.example.farm_planner.analysis.model;

public record EconomicInputs(
    int workers,
    double weeklyPayPerWorker,
    double landPrice,
    Double downPaymentAmount,   // optional; if set, used over pct
    Double downPaymentPct,      // optional (0..1)
    double annualInterestRatePct,
    int years
) {}
