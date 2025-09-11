package com.example.farm_planner.rules;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.Yaml;

@Service
public class RulesService {

    private final Map<String, Object> rules = new HashMap<>();

    public RulesService(
            // Defaults from application.yml (can be overridden by worcester.yml)
            @Value("${app.rules.houseWidthFt:66}") double houseWidthFt,
            @Value("${app.rules.houseLengthFt:650}") double houseLengthFt,
            @Value("${app.rules.perHouseAreaLimitSqFt:40000}") double perHouseAreaLimitSqFt,
            @Value("${app.rules.maxHousesPerParcel:8}") int maxHousesPerParcel,
            @Value("${app.rules.setbackLossPct:0.20}") double setbackLossPct,
            @Value("${app.rules.infraLossPct:0.10}") double infraLossPct,
            @Value("${app.rules.minParcelAcres:5.0}") double minParcelAcres,

            // County file (optional, overrides the above when present)
            @Value("classpath:rules/Worcester.yml") Resource countyFile
    ) {
        // seed with app defaults
        rules.put("houseWidthFt", houseWidthFt);
        rules.put("houseLengthFt", houseLengthFt);
        rules.put("perHouseAreaLimitSqFt", perHouseAreaLimitSqFt);
        rules.put("maxHousesPerParcel", maxHousesPerParcel);
        rules.put("setbackLossPct", setbackLossPct);
        rules.put("infraLossPct", infraLossPct);
        rules.put("minParcelAcres", minParcelAcres);

        // merge county overrides
        try {
            if (countyFile.exists()) {
                try (InputStream in = countyFile.getInputStream()) {
                    Object data = new Yaml().load(in);
                    if (data instanceof Map<?,?> m) {
                        //noinspection unchecked
                        rules.putAll((Map<String, Object>) m);
                    }
                }
            }
        } catch (Exception ignored) {
            // keep defaults if file missing/invalid
        }
    }

    // Accessors
    public double houseWidthFt()          { return d("houseWidthFt"); }
    public double houseLengthFt()         { return d("houseLengthFt"); }
    public double perHouseAreaLimitSqFt() { return d("perHouseAreaLimitSqFt"); }
    public int    maxHousesPerParcel()    { return (int) Math.round(d("maxHousesPerParcel")); }
    public double setbackLossPct()        { return d("setbackLossPct"); }
    public double infraLossPct()          { return d("infraLossPct"); }
    public double minParcelAcres()        { return d("minParcelAcres"); }

    // Helpers
    public double acreToSqFt(double acres) { return acres * 43_560.0; }

    private double d(String key) {
        return Double.parseDouble(String.valueOf(rules.get(key)));
    }

    public Map<String,Object> asMap() { return Map.copyOf(rules); }
}
