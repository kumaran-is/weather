package com.weather.config;

import io.r2dbc.spi.ConnectionFactories;
import io.r2dbc.spi.ConnectionFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.r2dbc.config.AbstractR2dbcConfiguration;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;

@Configuration
public class DatabaseConfig extends AbstractR2dbcConfiguration {
    
    private static final Logger log = LogManager.getLogger(DatabaseConfig.class);
    
    @Value("${spring.r2dbc.url}")
    private String r2dbcUrl;

    @Override
    public ConnectionFactory connectionFactory() {
        ConnectionFactory factory = ConnectionFactories.get(r2dbcUrl);
        log.info("Created connection factory for URL: {}", r2dbcUrl);
        return factory;
    }

    @Bean
    public R2dbcEntityTemplate r2dbcEntityTemplate(ConnectionFactory connectionFactory) {
        log.info("Creating R2dbcEntityTemplate with connection factory: {}", 
                connectionFactory.getClass().getSimpleName());
        return new R2dbcEntityTemplate(connectionFactory);
    }
}