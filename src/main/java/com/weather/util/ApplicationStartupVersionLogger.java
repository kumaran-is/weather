package com.weather.util;

import org.springframework.boot.info.BuildProperties;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Component that logs application build information during startup.
 * Also ensures build information is available via /management/info endpoint.
 */
@Component
public class ApplicationStartupVersionLogger {

    private static final Logger logger = LogManager.getLogger(ApplicationStartupVersionLogger.class);

    private final BuildProperties buildProperties;

    public ApplicationStartupVersionLogger(BuildProperties buildProperties) {
        this.buildProperties = buildProperties;
    }

    @EventListener
    public void onApplicationEvent(ContextRefreshedEvent event) {
        // Ensure we only log once for the root application context.
        if (event.getApplicationContext().getParent() == null) {
            if (buildProperties != null) {
                logger.info("========================================================================");
                logger.info("Application Name     : {}", buildProperties.getName());
                logger.info("Application Version  : {}", buildProperties.getVersion());
                logger.info("Build Time           : {}", buildProperties.getTime());
                logger.info("Artifact ID          : {}", buildProperties.getArtifact());
                logger.info("Group ID             : {}", buildProperties.getGroup());
                logger.info("Java Version         : {}", buildProperties.get("java.source"));
                logger.info("Spring Boot Version  : {}", buildProperties.get("spring-boot.version"));
                logger.info("Spring Framework Ver : {}", buildProperties.get("spring-framework.version"));
                logger.info("========================================================================");
            } else {
                logger.warn("BuildProperties not available. Ensure spring-boot-maven-plugin's build-info goal is executed.");
            }
        }
    }
}