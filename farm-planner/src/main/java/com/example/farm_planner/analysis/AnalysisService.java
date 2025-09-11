package com.example.farm_planner.analysis;

import static java.lang.Math.floor;
import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.lang.Math.pow;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.example.farm_planner.analysis.model.EconResult;
import com.example.farm_planner.analysis.model.EconomicInputs;
import com.example.farm_planner.analysis.model.QuickAnalysisResult;
import com.example.farm_planner.parcel.dto.ParcelResponse;
import com.example.farm_planner.rules.RulesService;

@Service
public class AnalysisService {

    private final RulesService rules;

    public AnalysisService(RulesService rules) {
        this.rules = rules;
    }

    /* =========================================================
       STEP 1: FIT — Given a parcel, decide how many houses fit
       ========================================================= */
    public QuickAnalysisResult fit(ParcelResponse parcel) {
        List<String> notes = new ArrayList<>();

        if (parcel == null) {
            return new QuickAnalysisResult(false, null, 0, 0, 0, 0,
                    List.of("Parcel not found."));
        }

        double acres = round2(nz(parcel.acres()));
        if (acres <= 0) {
            return new QuickAnalysisResult(true, parcel.acctId(), 0, 0, perHouseSqFtCapped(), 0,
                    List.of("Parcel area is missing or zero."));
        }
        if (acres < rules.minParcelAcres()) {
            notes.add("Parcel below minimum size (" + rules.minParcelAcres() + " acres).");
            return new QuickAnalysisResult(true, parcel.acctId(), acres, 0, perHouseSqFtCapped(), 0, notes);
        }

        // Usable acreage after losses
        double usable = acres;
        usable *= (1 - clampPct(rules.setbackLossPct(), notes, "setbackLossPct"));
        usable *= (1 - clampPct(rules.infraLossPct(), notes, "infraLossPct"));
        usable = max(0, round2(usable));

        // Per-house size (capped by county to 40,000 ft²)
        double perHouseSqFt = perHouseSqFtCapped();
        if (perHouseSqFt <= 0) {
            notes.add("Invalid house dimensions/cap.");
            return new QuickAnalysisResult(true, parcel.acctId(), acres, usable, 0, 0, notes);
        }

        // How many houses fit?
        double usableSqFt = acresToSqFt(usable);
        int theoretical = (int) floor(usableSqFt / perHouseSqFt);
        int houses = min(theoretical, rules.maxHousesPerParcel());
        houses = max(0, houses);

        if (houses == 0) notes.add("Usable area too small for one house (<= 40,000 ft² cap).");

        double rawHouseSqFt = rules.houseWidthFt() * rules.houseLengthFt();
        if (perHouseSqFt < rawHouseSqFt) {
            notes.add("Per-house area limited to 40,000 ft² by county rules.");
        }
        if (theoretical > houses) {
            notes.add("Capped by county max of " + rules.maxHousesPerParcel() + " houses.");
        }

        return new QuickAnalysisResult(true, parcel.acctId(), acres, usable, perHouseSqFt, houses, notes);
    }

    /* ==================================================================
       STEP 2: ECON — Given a fit result + user inputs, compute finances
       ================================================================== */
    public EconResult econ(QuickAnalysisResult fit, EconomicInputs econ) {
        List<String> notes = new ArrayList<>(fit.messages() == null ? List.of() : fit.messages());

        if (!fit.found()) {
            return new EconResult(false, 0, 0, 0, 0, 0, 0, 0,
                    List.of("Parcel not found. Run /api/parcels/search first."));
        }
        if (fit.maxHouses() <= 0) {
            notes.add("No houses fit on this parcel — skipping economics.");
            double workers = workerCost(econ);
            double loan = loanAnnual(econ);
            return new EconResult(false, 0, 0, 0, 0, workers, loan,
                    round2(-(workers + loan)), notes);
        }

        int houses = fit.maxHouses();
        double totalCoveredSqFt = houses * fit.perHouseSqFt();

        // Your given rules
        double annualIncome      = round2(totalCoveredSqFt * 2.5); // income = area * 2.5
        double annualOpsExpense  = round2(totalCoveredSqFt * 0.5); // ops = area * 0.5
        double annualWorkerCost  = workerCost(econ);               // workers * weekly * 52
        double annualLoanPayment = loanAnnual(econ);               // annuity (annual compounding)
        double annualNetRevenue  = round2(annualIncome - (annualOpsExpense + annualWorkerCost + annualLoanPayment));

        return new EconResult(true, houses, totalCoveredSqFt, annualIncome, annualOpsExpense,
                annualWorkerCost, annualLoanPayment, annualNetRevenue, notes);
    }

    /* ======================
       Helpers / calculations
       ====================== */

    private double perHouseSqFtCapped() {
        return min(rules.houseWidthFt() * rules.houseLengthFt(),
                   rules.perHouseAreaLimitSqFt());
    }

    private double clampPct(double v, List<String> notes, String name) {
        if (v < 0) { notes.add(name + " < 0; clamped to 0."); return 0; }
        if (v > 0.95) { notes.add(name + " > 0.95; clamped to 0.95."); return 0.95; }
        return v;
    }

    private double workerCost(EconomicInputs econ) {
        int workers = max(0, econ.workers());
        double weekly = max(0, econ.weeklyPayPerWorker());
        return round2(workers * weekly * 52.0);
    }

    /** Annual amortized payment with annual compounding & payments (demo-friendly). */
    private double loanAnnual(EconomicInputs econ) {
        double price = max(0, econ.landPrice());
        double down = 0;
        if (econ.downPaymentAmount() != null) {
            down = max(0, econ.downPaymentAmount());
        } else if (econ.downPaymentPct() != null) {
            down = price * clamp01(econ.downPaymentPct());
        }
        double principal = max(0, price - down);

        int years = max(1, econ.years());
        double r = max(0, econ.annualInterestRatePct()) / 100.0;

        if (r == 0) return round2(principal / years);

        double annuity = principal * r / (1 - pow(1 + r, -years));
        return round2(annuity);
    }

    private double clamp01(double v) { return max(0, min(1, v)); }
    private double acresToSqFt(double a) { return a * 43_560.0; }
    private double nz(Double d) { return d == null ? 0.0 : d; }
    private double round2(double v) { return Math.round(v * 100.0) / 100.0; }
}
