package com.surendra.priorauth.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Role-based access control:
 * <ul>
 *   <li>INTAKE - creates requests and runs documentation checks</li>
 *   <li>REVIEWER - reads requests (PHI masked) and runs checks</li>
 *   <li>AUDITOR - reads the audit trail only</li>
 * </ul>
 * Demo users are in-memory. A real deployment would use SSO (OIDC) instead.
 */
@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // Stateless API using HTTP Basic: no session cookie, so CSRF tokens are not needed.
            .csrf(csrf -> csrf.disable())
            .cors(Customizer.withDefaults())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers("/actuator/health").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/requests").hasRole("INTAKE")
                .requestMatchers(HttpMethod.POST, "/api/requests/*/check").hasAnyRole("INTAKE", "REVIEWER")
                .requestMatchers(HttpMethod.GET, "/api/requests/**").hasAnyRole("INTAKE", "REVIEWER")
                .requestMatchers(HttpMethod.GET, "/api/rules").hasAnyRole("INTAKE", "REVIEWER")
                .requestMatchers("/api/audit/**").hasRole("AUDITOR")
                .anyRequest().denyAll())
            .httpBasic(Customizer.withDefaults());
        return http.build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    UserDetailsService users(PasswordEncoder encoder) {
        return new InMemoryUserDetailsManager(
            User.withUsername("intake").password(encoder.encode("intake-demo")).roles("INTAKE").build(),
            User.withUsername("reviewer").password(encoder.encode("reviewer-demo")).roles("REVIEWER").build(),
            User.withUsername("auditor").password(encoder.encode("auditor-demo")).roles("AUDITOR").build());
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("http://localhost:5173"));
        config.setAllowedMethods(List.of("GET", "POST", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }
}
