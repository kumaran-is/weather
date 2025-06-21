package com.weather.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.SignalType;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Configuration for reactive stream metrics and monitoring.
 * Provides custom metrics for reactive patterns like subscription rates, backpressure, and memory usage.
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class ReactiveMetricsConfig {
    
    private final MeterRegistry meterRegistry;
    
    /**
     * Reactive metrics collector for monitoring reactive stream patterns.
     */
    @Bean
    public ReactiveMetricsCollector reactiveMetricsCollector() {
        return new ReactiveMetricsCollector(meterRegistry);
    }
    
    /**
     * Utility class for collecting reactive-specific metrics.
     */
    public static class ReactiveMetricsCollector {
        
        private final MeterRegistry meterRegistry;
        private final AtomicLong activeSubscriptions = new AtomicLong(0);
        private final AtomicLong totalSubscriptions = new AtomicLong(0);
        
        public ReactiveMetricsCollector(MeterRegistry meterRegistry) {
            this.meterRegistry = meterRegistry;
            
            // Register gauges for active metrics
            meterRegistry.gauge("reactive.subscriptions.active", activeSubscriptions);
            meterRegistry.gauge("reactive.subscriptions.total", totalSubscriptions);
            
            log.info("Initialized reactive metrics collector");
        }
        
        /**
         * Add timing metrics to a Mono with automatic subscription tracking.
         */
        public <T> Mono<T> timed(Mono<T> mono, String operationName) {
            Timer timer = Timer.builder("reactive.operation.timer")
                    .tag("operation", operationName)
                    .register(meterRegistry);
            
            return Mono.fromCallable(() -> System.nanoTime())
                    .flatMap(startTime -> mono
                            .doOnSubscribe(subscription -> {
                                activeSubscriptions.incrementAndGet();
                                totalSubscriptions.incrementAndGet();
                                meterRegistry.counter("reactive.subscription.started", 
                                        Tags.of("operation", operationName)).increment();
                            })
                            .doOnSuccess(result -> {
                                timer.record(System.nanoTime() - startTime, java.util.concurrent.TimeUnit.NANOSECONDS);
                            })
                            .doOnError(error -> {
                                timer.record(System.nanoTime() - startTime, java.util.concurrent.TimeUnit.NANOSECONDS);
                            })
                            .doFinally(signalType -> {
                                activeSubscriptions.decrementAndGet();
                                recordSignalType(operationName, signalType);
                            }));
        }
        
        /**
         * Add metrics to a Flux with backpressure monitoring.
         */
        public <T> Flux<T> timedFlux(Flux<T> flux, String operationName) {
            Timer timer = Timer.builder("reactive.flux.timer")
                    .tag("operation", operationName)
                    .register(meterRegistry);
            
            return Mono.fromCallable(() -> System.nanoTime())
                    .flatMapMany(startTime -> flux
                            .doOnSubscribe(subscription -> {
                                activeSubscriptions.incrementAndGet();
                                totalSubscriptions.incrementAndGet();
                                meterRegistry.counter("reactive.flux.subscription.started", 
                                        Tags.of("operation", operationName)).increment();
                            })
                            .doOnNext(item -> {
                                meterRegistry.counter("reactive.flux.items.emitted", 
                                        Tags.of("operation", operationName)).increment();
                            })
                            .doOnRequest(n -> {
                                meterRegistry.counter("reactive.flux.items.requested", 
                                        Tags.of("operation", operationName)).increment(n);
                                log.debug("Flux requested {} items for operation: {}", n, operationName);
                            })
                            .doOnComplete(() -> {
                                timer.record(System.nanoTime() - startTime, java.util.concurrent.TimeUnit.NANOSECONDS);
                            })
                            .doOnError(error -> {
                                timer.record(System.nanoTime() - startTime, java.util.concurrent.TimeUnit.NANOSECONDS);
                            })
                            .doFinally(signalType -> {
                                activeSubscriptions.decrementAndGet();
                                recordSignalType(operationName, signalType);
                            }));
        }
        
        /**
         * Monitor cache hit rates for cached reactive streams.
         */
        public <T> Mono<T> monitorCache(Mono<T> cachedMono, String cacheName) {
            return cachedMono
                    .doOnNext(item -> meterRegistry.counter("reactive.cache.hit", 
                            Tags.of("cache", cacheName)).increment())
                    .doOnSuccess(item -> {
                        if (item == null) {
                            meterRegistry.counter("reactive.cache.miss", 
                                    Tags.of("cache", cacheName)).increment();
                        }
                    });
        }
        
        /**
         * Record memory usage patterns specific to reactive streams.
         */
        public void recordMemoryUsage(String operationName) {
            Runtime runtime = Runtime.getRuntime();
            long usedMemory = runtime.totalMemory() - runtime.freeMemory();
            long maxMemory = runtime.maxMemory();
            
            meterRegistry.gauge("reactive.memory.used", 
                    Tags.of("operation", operationName), usedMemory);
            meterRegistry.gauge("reactive.memory.usage.percentage", 
                    Tags.of("operation", operationName), (double) usedMemory / maxMemory * 100);
        }
        
        /**
         * Create a timer for measuring reactive operation durations.
         */
        public long startTimer() {
            return System.nanoTime();
        }
        
        /**
         * Stop timer and record the duration.
         */
        public void stopTimer(long startTime, String operationName, String result) {
            Timer.builder("reactive.operation.timer")
                    .tag("operation", operationName)
                    .tag("result", result)
                    .register(meterRegistry)
                    .record(System.nanoTime() - startTime, java.util.concurrent.TimeUnit.NANOSECONDS);
        }
        
        /**
         * Record backpressure events when they occur.
         */
        public void recordBackpressure(String operationName, long requestedItems, long availableItems) {
            if (requestedItems > availableItems) {
                meterRegistry.counter("reactive.backpressure.event", 
                        Tags.of("operation", operationName)).increment();
                
                double backpressureRatio = (double) availableItems / requestedItems;
                meterRegistry.gauge("reactive.backpressure.ratio", 
                        Tags.of("operation", operationName), backpressureRatio);
            }
        }
        
        /**
         * Record different signal types (completion, error, cancel).
         */
        private void recordSignalType(String operationName, SignalType signalType) {
            meterRegistry.counter("reactive.signal", 
                    Tags.of("operation", operationName, "type", signalType.name().toLowerCase())).increment();
        }
        
        /**
         * Monitor subscription timing patterns.
         */
        public <T> Mono<T> monitorSubscriptionTiming(Mono<T> mono, String operationName) {
            long subscriptionTime = System.currentTimeMillis();
            
            return mono
                    .doOnSubscribe(subscription -> {
                        long delay = System.currentTimeMillis() - subscriptionTime;
                        Timer.builder("reactive.subscription.delay")
                                .tag("operation", operationName)
                                .register(meterRegistry)
                                .record(Duration.ofMillis(delay));
                    });
        }
    }
}