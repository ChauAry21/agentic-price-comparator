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

@Configuration
public class WebSecurityConfig {

    // 1. Protect standard APIs, but allow standard login UI
    @Bean
    public SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(authorize -> authorize
                .anyRequest().authenticated()
            )
            .formLogin(Customizer.withDefaults()); // Uses Spring's default built-in login form for now
        return http.build();
    }

    // 2. Naive hardcoded test user until we wire your database repositories!
    @Bean
    public UserDetailsService userDetailsService() {
        UserDetails testUser = User.withUsername("mahima@test.com")
                .password("{noop}password123") // simple matching rule for testing
                .roles("USER")
                .build();
        return new InMemoryUserDetailsManager(testUser);
    }
}