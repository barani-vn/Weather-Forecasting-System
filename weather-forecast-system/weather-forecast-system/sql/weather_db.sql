-- =====================================================================
--  Weather Forecasting System - MySQL 8.x script (run in MySQL Workbench)
--  Open this file in Workbench, then click the lightning-bolt "Execute".
-- =====================================================================

DROP DATABASE IF EXISTS weather_db;
CREATE DATABASE weather_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE weather_db;

-- Application user (matches src/main/resources/db.properties)
CREATE USER IF NOT EXISTS 'weather_user'@'localhost' IDENTIFIED BY 'Weather@123';
GRANT ALL PRIVILEGES ON weather_db.* TO 'weather_user'@'localhost';
FLUSH PRIVILEGES;

-- ---------------------------------------------------------------------
CREATE TABLE cities (
    id         INT AUTO_INCREMENT PRIMARY KEY,
    name       VARCHAR(100) NOT NULL,
    country    VARCHAR(100) NOT NULL DEFAULT '',
    latitude   DECIMAL(9,6) NOT NULL,
    longitude  DECIMAL(9,6) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uq_city (name, country)
) ENGINE=InnoDB;

CREATE TABLE forecasts (
    id               INT AUTO_INCREMENT PRIMARY KEY,
    city_id          INT NOT NULL,
    forecast_date    DATE NOT NULL,
    temp_max         DECIMAL(5,1) NOT NULL,
    temp_min         DECIMAL(5,1) NOT NULL,
    precipitation_mm DECIMAL(6,1) NOT NULL DEFAULT 0,
    wind_speed_max   DECIMAL(5,1) NOT NULL DEFAULT 0,
    weather_code     INT NOT NULL DEFAULT 0,
    source           ENUM('API','SAMPLE') NOT NULL DEFAULT 'API',
    fetched_at       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uq_city_date (city_id, forecast_date),
    CONSTRAINT fk_forecast_city FOREIGN KEY (city_id)
        REFERENCES cities(id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE search_history (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    city_id     INT NOT NULL,
    source      ENUM('API','DATABASE') NOT NULL,
    searched_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_history_city FOREIGN KEY (city_id)
        REFERENCES cities(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- Sample cities (so the offline/database option works immediately)
INSERT INTO cities (name, country, latitude, longitude) VALUES
 ('Chennai',   'India', 13.0827, 80.2707),
 ('Coimbatore','India', 11.0168, 76.9558),
 ('Mumbai',    'India', 19.0760, 72.8777),
 ('Delhi',     'India', 28.6139, 77.2090),
 ('Bengaluru', 'India', 12.9716, 77.5946);

-- 7 days of sample forecasts per city, starting today
INSERT INTO forecasts (city_id, forecast_date, temp_max, temp_min, precipitation_mm, wind_speed_max, weather_code, source)
SELECT c.id,
       CURDATE() + INTERVAL n.d DAY,
       CASE c.name WHEN 'Chennai' THEN 32 WHEN 'Coimbatore' THEN 30 WHEN 'Mumbai' THEN 31
                   WHEN 'Delhi' THEN 28 ELSE 27 END + (n.d % 3),
       CASE c.name WHEN 'Chennai' THEN 25 WHEN 'Coimbatore' THEN 21 WHEN 'Mumbai' THEN 24
                   WHEN 'Delhi' THEN 14 ELSE 19 END + (n.d % 2),
       ELT(n.d + 1, 0.0, 0.0, 2.5, 8.0, 0.4, 0.0, 12.3),
       10 + n.d,
       ELT(n.d + 1, 0, 1, 2, 61, 3, 1, 80),
       'SAMPLE'
FROM cities c
JOIN (SELECT 0 d UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3
      UNION ALL SELECT 4 UNION ALL SELECT 5 UNION ALL SELECT 6) n;

-- Quick check
SELECT c.name, f.forecast_date, f.temp_max, f.temp_min, f.source
FROM forecasts f JOIN cities c ON c.id = f.city_id
ORDER BY c.name, f.forecast_date;
