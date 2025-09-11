package com.example.farm_planner.analysis;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.farm_planner.analysis.model.EconResult;
import com.example.farm_planner.analysis.model.EconomicInputs;
import com.example.farm_planner.analysis.model.QuickAnalysisResult;
import com.example.farm_planner.parcel.GeocoderService;
import com.example.farm_planner.parcel.ParcelService;
import com.example.farm_planner.parcel.dto.ParcelResponse;

import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/analysis")
public class AnalysisController {

    private final ParcelService parcelService;
    private final GeocoderService geocoderService;
    private final AnalysisService analysisService;

    public AnalysisController(ParcelService parcelService,
                              GeocoderService geocoderService,
                              AnalysisService analysisService) {
        this.parcelService = parcelService;
        this.geocoderService = geocoderService;
        this.analysisService = analysisService;
    }

    /** STEP 1: Address -> Parcel -> Fit analysis (reactive) */
    @PostMapping("/quick")
    public Mono<ResponseEntity<?>> search(@RequestBody Map<String, String> body) {
        String address = body.get("address");
        if (address == null || address.isBlank()) {
            return Mono.just(ResponseEntity.badRequest()
                    .body(Map.of("error", "Missing 'address'")));
        }

        return geocoderService.geocode(address)          // Mono<double[]> (lat, lon)
            .flatMap(this::toLatLon)                     // Mono<LatLon>
            .flatMap(latlon -> parcelService.findByPoint(latlon.lat(), latlon.lon())
                .map(parcel -> {
                    QuickAnalysisResult fit = analysisService.fit(parcel);
                    if (!fit.found()) {
                        return ResponseEntity.ok(Map.of(
                            "found", false,
                            "address", address,
                            "messages", fit.messages()
                        ));
                    }
                    return ResponseEntity.ok(fit);
                })
                .defaultIfEmpty(ResponseEntity.ok(Map.of(
                    "found", false,
                    "address", address,
                    "messages", List.of("Parcel not found.")
                )))
            )
            .onErrorResume(ex -> Mono.just(ResponseEntity.ok(Map.of(
                "found", false,
                "address", address,
                "messages", List.of("Geocoding/parcel lookup failed: " + ex.getMessage())
            ))))
            .defaultIfEmpty(ResponseEntity.ok(Map.of(
                "found", false,
                "address", address,
                "messages", List.of("Address could not be geocoded.")
            )));
    }

    /** STEP 2: Fit result + user inputs -> Economics analysis */
    @PostMapping("/econ/assess")
    public ResponseEntity<EconResult> assess(@RequestBody Map<String, Object> body) {
        Map<String, Object> fitMap = (Map<String, Object>) body.get("fit");
        if (fitMap == null) {
            return ResponseEntity.badRequest().build();
        }

        QuickAnalysisResult fit = new QuickAnalysisResult(
                (boolean) fitMap.getOrDefault("found", false),
                (String) fitMap.get("acctId"),
                toD(fitMap.get("parcelAcres")),
                toD(fitMap.get("usableAcres")),
                toD(fitMap.get("perHouseSqFt")),
                ((Number) fitMap.getOrDefault("maxHouses", 0)).intValue(),
                (List<String>) fitMap.getOrDefault("messages", List.of())
        );

        EconomicInputs econ = new EconomicInputs(
                ((Number) body.getOrDefault("workers", 0)).intValue(),
                toD(body.getOrDefault("weeklyPayPerWorker", 0)),
                toD(body.getOrDefault("landPrice", 0)),
                body.containsKey("downPaymentAmount") ? toD(body.get("downPaymentAmount")) : null,
                body.containsKey("downPaymentPct") ? toD(body.get("downPaymentPct")) : null,
                toD(body.getOrDefault("annualInterestRatePct", 0)),
                ((Number) body.getOrDefault("years", 1)).intValue()
        );

        return ResponseEntity.ok(analysisService.econ(fit, econ));
    }

    /* ---------------- Helpers ---------------- */

    // Convert Mono<double[]> from geocoder into a typed LatLon, with range checks.
    private Mono<LatLon> toLatLon(double[] coords) {
        if (coords == null || coords.length < 2) return Mono.empty();
        double lat = coords[0], lon = coords[1];
        if (lat < -90 || lat > 90 || lon < -180 || lon > 180) return Mono.empty();
        return Mono.just(new LatLon(lat, lon));
    }

    private static double toD(Object o) {
        if (o instanceof Integer i) return i.doubleValue();
        if (o instanceof Long l) return l.doubleValue();
        if (o instanceof Float f) return f.doubleValue();
        if (o instanceof Double d) return d;
        if (o instanceof String s) try { return Double.parseDouble(s); } catch (Exception ignored) {}
        return 0.0;
    }

    private record LatLon(double lat, double lon) {}
}
