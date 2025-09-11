package com.example.farm_planner.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.web.cors.CorsConfiguration;

@Configuration
public class SecurityConfig {

  @Value("${app.cors.allowedOrigins:http://localhost:5173,http://localhost:3000}")
  private List<String> allowedOrigins;

  @Bean
  SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
      // CORS for the frontend dev server
      .cors(c -> c.configurationSource(request -> {
        CorsConfiguration cfg = new CorsConfiguration();
        cfg.setAllowedOrigins(allowedOrigins);
        cfg.setAllowedMethods(List.of("GET","POST","PUT","DELETE","OPTIONS"));
        cfg.setAllowedHeaders(List.of("Content-Type","Authorization","X-CSRF-Token"));
        cfg.setAllowCredentials(true);
        return cfg;
      }))
      // Dev: ignore CSRF for API routes so static test.html can POST without token
      .csrf(csrf -> csrf
        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
        .ignoringRequestMatchers("/api/**")
      )
      // Session-based auth (cookie)
      .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
      // URL rules
      .authorizeHttpRequests(auth -> auth
        .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
        .requestMatchers(HttpMethod.POST, "/api/parcels/search").permitAll() // keep open for now
        .requestMatchers(HttpMethod.GET, "/api/parcels/**").permitAll()      // keep open for now
        .requestMatchers(HttpMethod.POST, "/api/analysis/**").permitAll()    // allow analysis POSTs during dev
        // static assets and test page
        .requestMatchers(HttpMethod.GET, "/", "/index.html", "/test.html", "/favicon.ico",
            "/**/*.css", "/**/*.js", "/**/*.map", "/**/*.png", "/**/*.jpg", "/**/*.svg").permitAll()
        .requestMatchers("/actuator/health", "/error").permitAll()
        .anyRequest().authenticated()
      )
      // We’re doing our own JSON login endpoint; no default form
      .httpBasic(Customizer.withDefaults())
      .formLogin(f -> f.disable())
      .logout(lo -> lo.logoutUrl("/api/auth/logout"));

    return http.build();
  }

  // Simple in-memory user for now; later swap to DB-backed service
  @Bean
  UserDetailsService userDetailsService(
      @Value("${app.auth.username:user}") String username,
      @Value("${app.auth.password:user}") String rawPassword,
      PasswordEncoder encoder
  ) {
    var u = User.withUsername(username)
                .password(encoder.encode(rawPassword))
                .roles("USER")
                .build();
    return new InMemoryUserDetailsManager(u);
  }

  @Bean
  PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  AuthenticationManager authenticationManager(UserDetailsService uds, PasswordEncoder enc) {
    var provider = new DaoAuthenticationProvider();
    provider.setUserDetailsService(uds);
    provider.setPasswordEncoder(enc);
    return new ProviderManager(provider);
  }
}
