package com.emerbv.ecommdb.security.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class StripeSecurityConfig {

    @Bean
    @Order(1) // Darle mayor prioridad que la configuración de seguridad general
    public SecurityFilterChain stripeFilterChain(HttpSecurity http) throws Exception {
        // Configuración específica para el webhook de Stripe
        // Los webhooks de Stripe deben ser públicos para que Stripe pueda enviar eventos
        return http
                .securityMatcher("/ecommdb/api/v1/payments/webhook")
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .build();
    }
}
