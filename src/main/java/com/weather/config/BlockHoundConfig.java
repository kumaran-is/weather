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
     */
    @PostConstruct
    public void setupBlockHound() {
        try {
            log.info("Installing BlockHound for reactive blocking detection");
            
            // Use reflection to avoid compile-time dependency
            Class<?> blockHoundClass = Class.forName("reactor.blockhound.BlockHound");
            Object builder = blockHoundClass.getMethod("builder").invoke(null);
            Class<?> builderClass = builder.getClass();
            
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
            allowBlockingCall(builder, builderClass, "io.r2dbc.h2.H2Connection", "createStatement");
            allowBlockingCall(builder, builderClass, "io.r2dbc.mssql.MssqlConnection", "createStatement");
            
            // Allow Weather Service specific operations
            allowBlockingCall(builder, builderClass, "com.weather.service.WeatherDataServiceImpl", "validateRequest");
            allowBlockingCall(builder, builderClass, "com.weather.mapper.WeatherDataMapper", "toEntity");
            allowBlockingCall(builder, builderClass, "com.weather.mapper.WeatherDataMapper", "toResponse");
            
            // Install BlockHound
            builderClass.getMethod("install").invoke(builder);
            
            log.info("BlockHound successfully installed");
            log.warn("BlockHound is active - blocking calls will throw BlockingOperationError in reactive contexts");
            
        } catch (ClassNotFoundException e) {
            log.info("BlockHound not found on classpath - skipping reactive blocking detection");
        } catch (Exception e) {
            log.error("Failed to install BlockHound - continuing without blocking detection", e);
        }
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