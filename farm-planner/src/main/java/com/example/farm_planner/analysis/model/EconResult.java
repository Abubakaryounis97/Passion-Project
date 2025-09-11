package com.example.farm_planner.analysis.model;
import java.util.List;

public record EconResult(
    boolean eligible,           // true if houses > 0
    int houses,
    double totalCoveredSqFt,
    double annualIncome,
    double annualOpsExpense,
    double annualWorkerCost,
    double annualLoanPayment,
    double annualNetRevenue,
    List<String> messages
) {}
