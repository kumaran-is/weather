package com.weather.config;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import jakarta.annotation.PostConstruct;

/**
 * BlockHound configuration for detecting blocking calls in reactive code.
 * 
 * <p>BlockHound is a Java agent that detects blocking calls from non-blocking threads.
 * This is crucial for maintaining the non-blocking nature of reactive applications.
 * 
 * <p>Enabled for all profiles except 'prod' to avoid production performance overhead.
 * 
 * @author Weather Service Team
 */
@Configuration
@Profile("!prod")  // Active for all profiles except production
public class BlockHoundConfig {

    private static final Logger log = LogManager.getLogger(BlockHoundConfig.class);

    /**
     * Initialize BlockHound with configuration for the Weather Service.
     * Uses reflection to avoid compile-time dependency on BlockHound classes.
     * Configured in LOGGING-ONLY mode to warn about blocking calls without stopping the app.
     */
    @PostConstruct
    public void setupBlockHound() {
        try {
            log.info("Installing BlockHound for reactive blocking detection");
            
            // Use reflection to avoid compile-time dependency
            Class<?> blockHoundClass = Class.forName("reactor.blockhound.BlockHound");
            Object builder = blockHoundClass.getMethod("builder").invoke(null);
            Class<?> builderClass = builder.getClass();
            
            // Set up WARNING-ONLY mode instead of throwing exceptions
            setupLoggingOnlyMode(builder, builderClass);
            
            // Set up allowlist for safe operations
            setupAllowList(builder, builderClass);
            
            // Install BlockHound
            builderClass.getMethod("install").invoke(builder);
            
            log.info("✅ BlockHound successfully installed in WARNING-ONLY mode");
            log.info("📝 Blocking calls will be logged as warnings, not throw exceptions");
            
        } catch (ClassNotFoundException e) {
            log.info("BlockHound not found on classpath - skipping reactive blocking detection");
        } catch (Exception e) {
            log.error("Failed to install BlockHound - continuing without blocking detection", e);
        }
    }
    
    /**
     * Configure BlockHound to log warnings instead of throwing exceptions.
     */
    private void setupLoggingOnlyMode(Object builder, Class<?> builderClass) {
        try {
            // Create consumer that logs warnings instead of throwing
            java.util.function.Consumer<Object> loggingCallback = method -> {
                // Add special tag for Grafana/monitoring pickup
                log.warn("[BLOCKHOUND_VIOLATION] 🚫 REACTIVE VIOLATION: Blocking call detected: {}", method);
                log.warn("[BLOCKHOUND_VIOLATION] 💡 Consider making this operation reactive for better performance");
                log.warn("[BLOCKHOUND_VIOLATION] 📍 This call should be moved to a separate thread or made non-blocking");
                
                // Additional structured log for metrics/alerting
                log.warn("marker=BLOCKHOUND_VIOLATION severity=HIGH component=reactive-compliance method={}", method);
            };
            
            builderClass.getMethod("blockingMethodCallback", java.util.function.Consumer.class)
                       .invoke(builder, loggingCallback);
                       
        } catch (Exception e) {
            log.debug("Could not set up logging-only mode, using default behavior: {}", e.getMessage());
        }
    }
    
    /**
     * Set up allowlist for operations that are safe to block.
     */
    private void setupAllowList(Object builder, Class<?> builderClass) {
        // Allow specific blocking operations that are safe
        allowBlockingCall(builder, builderClass, "java.util.UUID", "randomUUID");
        allowBlockingCall(builder, builderClass, "java.security.SecureRandom", "nextBytes");
        
        // Allow Spring Framework internal operations
        allowBlockingCall(builder, builderClass, "org.springframework.util.ClassUtils", "forName");
        allowBlockingCall(builder, builderClass, "org.springframework.boot.context.properties.bind.Binder", "bind");
        
        // Allow Jackson JSON operations
        allowBlockingCall(builder, builderClass, "com.fasterxml.jackson.databind.ObjectMapper", "writeValueAsString");
        allowBlockingCall(builder, builderClass, "com.fasterxml.jackson.databind.ObjectMapper", "readValue");
        
        // Allow Log4j2 operations
        allowBlockingCall(builder, builderClass, "org.apache.logging.log4j.core.Logger", "logMessage");
        
        // Allow R2DBC operations that might appear blocking but are reactive
       // allowBlockingCall(builder, builderClass, "io.r2dbc.h2.H2Connection", "createStatement");
       // allowBlockingCall(builder, builderClass, "io.r2dbc.mssql.MssqlConnection", "createStatement");
        
        // Allow Weather Service specific operations
       // allowBlockingCall(builder, builderClass, "com.weather.service.WeatherDataServiceImpl", "validateRequest");
       // allowBlockingCall(builder, builderClass, "com.weather.mapper.WeatherDataMapper", "toEntity");
       // allowBlockingCall(builder, builderClass, "com.weather.mapper.WeatherDataMapper", "toResponse");
    }
    
    /**
     * Helper method to allow blocking calls using reflection.
     */
    private void allowBlockingCall(Object builder, Class<?> builderClass, String className, String methodName) {
        try {
            builderClass.getMethod("allowBlockingCallsInside", String.class, String.class)
                       .invoke(builder, className, methodName);
        } catch (Exception e) {
            log.debug("Failed to allow blocking call for {}.{}: {}", className, methodName, e.getMessage());
        }
    }
}