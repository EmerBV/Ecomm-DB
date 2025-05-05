package com.emerbv.ecommdb.security.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.RetryPolicy;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class RetryConfig {

    @Bean
    public RetryTemplate stripeRetryTemplate() {
        RetryTemplate retryTemplate = new RetryTemplate();

        // Configuración del backoff exponencial
        ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
        backOffPolicy.setInitialInterval(1000); // 1 segundo inicial
        backOffPolicy.setMultiplier(2.0); // Duplica el tiempo en cada intento
        backOffPolicy.setMaxInterval(30000); // Máximo 30 segundos entre intentos
        retryTemplate.setBackOffPolicy(backOffPolicy);

        // Configuración de la política de reintentos
        Map<Class<? extends Throwable>, Boolean> retryableExceptions = new HashMap<>();

        // Excepciones de Stripe que deberían ser reintentadas
        retryableExceptions.put(com.stripe.exception.ApiConnectionException.class, true);
        retryableExceptions.put(com.stripe.exception.ApiException.class, true);
        retryableExceptions.put(com.stripe.exception.RateLimitException.class, true);
        retryableExceptions.put(com.stripe.exception.IdempotencyException.class, true);

        // No reintentamos errores de autenticación o validación, pues repetirlos no ayudará
        retryableExceptions.put(com.stripe.exception.AuthenticationException.class, false);
        retryableExceptions.put(com.stripe.exception.InvalidRequestException.class, false);
        retryableExceptions.put(com.stripe.exception.CardException.class, false);

        RetryPolicy retryPolicy = new SimpleRetryPolicy(3, retryableExceptions, true);
        retryTemplate.setRetryPolicy(retryPolicy);

        return retryTemplate;
    }
}
