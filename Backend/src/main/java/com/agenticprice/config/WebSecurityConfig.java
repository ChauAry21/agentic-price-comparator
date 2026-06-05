package com.agenticprice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
public class WebSecurityConfig {

    @Bean
    public SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            // 1. Enable CORS Configuration Bean
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            // 2. Disable CSRF so we can transmit post mapping JSON objects locally
            .csrf(csrf -> csrf.disable()) 
            // 3. Define open access routes vs protected areas
            .authorizeHttpRequests(authorize -> authorize
                .requestMatchers("/api/auth/signup",
                    "/api/auth/login",
                    "/api/auth/verify-2fa",
                    "/api/prices/**",
                    "/api/alerts/**"
                ).permitAll() 
                .anyRequest().authenticated()
            )
            .formLogin(Customizer.withDefaults());
            
        return http.build();
    }

    // Retaining basic default user configuration rule block requirements
    @Bean
    public UserDetailsService userDetailsService() {
        UserDetails testUser = User.withUsername("mahima@test.com")
                .password("{noop}password123")
                .roles("USER")
                .build();
        return new InMemoryUserDetailsManager(testUser);
    }

    // CORS Mapping configurations to explicitly allow requests from Vite dev servers
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("http://localhost:5173")); 
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        configuration.setAllowCredentials(true);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}