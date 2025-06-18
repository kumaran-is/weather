-- SQL Server Database Schema for Weather Service
IF NOT EXISTS (SELECT * FROM sysobjects WHERE name='weather_data' AND xtype='U')
CREATE TABLE weather_data (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    city NVARCHAR(100) NOT NULL,
    country NVARCHAR(100),
    temperature DECIMAL(5,2) NOT NULL,
    humidity INT,
    pressure DECIMAL(7,2),
    wind_speed DECIMAL(5,2),
    wind_direction NVARCHAR(3),
    weather_condition NVARCHAR(50),
    description NVARCHAR(MAX),
    recorded_at DATETIME2 NOT NULL,
    created_at DATETIME2 DEFAULT GETDATE(),
    updated_at DATETIME2 DEFAULT GETDATE()
);

-- Create indexes for better query performance
IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'idx_weather_city')
CREATE INDEX idx_weather_city ON weather_data(city);

IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'idx_weather_recorded_at')
CREATE INDEX idx_weather_recorded_at ON weather_data(recorded_at);

IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'idx_weather_city_recorded_at')
CREATE INDEX idx_weather_city_recorded_at ON weather_data(city, recorded_at);

IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'idx_weather_created_at')
CREATE INDEX idx_weather_created_at ON weather_data(created_at);