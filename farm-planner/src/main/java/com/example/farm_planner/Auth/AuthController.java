package com.example.farm_planner.Auth;

import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

record LoginRequest(String username, String password) {}
record LoginResponse(String username) {}

@RestController
@RequestMapping("/api/auth")
public class AuthController {

  private final AuthenticationManager authManager;

  public AuthController(AuthenticationManager authManager) {
    this.authManager = authManager;
  }

  @PostMapping("/login")
  public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest req) {
    Authentication auth = authManager.authenticate(
        new UsernamePasswordAuthenticationToken(req.username(), req.password()));
    return ResponseEntity.ok(new LoginResponse(auth.getName()));
  }

  // /api/auth/logout is handled by Spring Security (configured in SecurityConfig)
}
