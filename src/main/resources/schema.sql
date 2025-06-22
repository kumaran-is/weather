-- H2 Database Schema for Weather Service
DROP TABLE IF EXISTS weather_data;

CREATE TABLE IF NOT EXISTS weather_data (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    city VARCHAR(100) NOT NULL,
    country VARCHAR(100),
    temperature DECIMAL(5,2) NOT NULL,
    humidity INTEGER,
    pressure DECIMAL(7,2),
    wind_speed DECIMAL(5,2),
    wind_direction VARCHAR(3),
    weather_condition VARCHAR(50),
    description TEXT,
    recorded_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for better query performance
CREATE INDEX IF NOT EXISTS idx_weather_city ON weather_data(city);
CREATE INDEX IF NOT EXISTS idx_weather_recorded_at ON weather_data(recorded_at);
CREATE INDEX IF NOT EXISTS idx_weather_city_recorded_at ON weather_data(city, recorded_at);
CREATE INDEX IF NOT EXISTS idx_weather_created_at ON weather_data(created_at);