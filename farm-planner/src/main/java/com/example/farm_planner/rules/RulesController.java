package com.example.farm_planner.rules;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RulesController {
    private final RulesService rules;

    public RulesController(RulesService rules) { this.rules = rules; }

    @GetMapping("/api/rules")
    public Map<String, Object> get() {
        return rules.asMap();
    }
}
