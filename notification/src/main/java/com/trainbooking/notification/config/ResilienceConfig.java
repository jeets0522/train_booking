package com.trainbooking.notification.config;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class ResilienceConfig {

    @Bean
    public CircuitBreakerRegistry circuitBreakerRegistry(
            @Value("${notification.circuit-breaker.failure-rate-threshold:50}") float failureRateThreshold,
            @Value("${notification.circuit-breaker.minimum-number-of-calls:10}") int minCalls,
            @Value("${notification.circuit-breaker.sliding-window-size:20}") int windowSize,
            @Value("${notification.circuit-breaker.wait-duration-in-open-state-seconds:30}") long waitSeconds,
            @Value("${notification.circuit-breaker.permitted-calls-in-half-open:3}") int halfOpenCalls) {

        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .failureRateThreshold(failureRateThreshold)
                .minimumNumberOfCalls(minCalls)
                .slidingWindowSize(windowSize)
                .waitDurationInOpenState(Duration.ofSeconds(waitSeconds))
                .permittedNumberOfCallsInHalfOpenState(halfOpenCalls)
                .build();
        return CircuitBreakerRegistry.of(config);
    }
}
