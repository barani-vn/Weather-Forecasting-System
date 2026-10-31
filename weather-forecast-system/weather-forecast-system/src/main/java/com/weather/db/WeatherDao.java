package com.weather.db;

import com.weather.model.City;
import com.weather.model.Forecast;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** All SQL for the application lives here. */
public class WeatherDao {

    /** Inserts the city or updates its coordinates; returns the row id. */
    public int upsertCity(City c) throws SQLException {
        String sql = "INSERT INTO cities (name, country, latitude, longitude) VALUES (?,?,?,?) "
                + "ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id), "
                + "latitude = VALUES(latitude), longitude = VALUES(longitude)";
        try (Connection con = Database.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, c.name());
            ps.setString(2, c.country() == null ? "" : c.country());
            ps.setDouble(3, c.latitude());
            ps.setDouble(4, c.longitude());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
        }
        throw new SQLException("Could not obtain city id");
    }

    public void saveForecasts(int cityId, List<Forecast> list, String source) throws SQLException {
        String sql = "INSERT INTO forecasts (city_id, forecast_date, temp_max, temp_min, "
                + "precipitation_mm, wind_speed_max, weather_code, source) VALUES (?,?,?,?,?,?,?,?) "
                + "ON DUPLICATE KEY UPDATE temp_max=VALUES(temp_max), temp_min=VALUES(temp_min), "
                + "precipitation_mm=VALUES(precipitation_mm), wind_speed_max=VALUES(wind_speed_max), "
                + "weather_code=VALUES(weather_code), source=VALUES(source), fetched_at=CURRENT_TIMESTAMP";
        try (Connection con = Database.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            con.setAutoCommit(false);
            for (Forecast f : list) {
                ps.setInt(1, cityId);
                ps.setDate(2, Date.valueOf(f.date()));
                ps.setDouble(3, f.tempMax());
                ps.setDouble(4, f.tempMin());
                ps.setDouble(5, f.precipitationMm());
                ps.setDouble(6, f.windSpeedMax());
                ps.setInt(7, f.weatherCode());
                ps.setString(8, source);
                ps.addBatch();
            }
            ps.executeBatch();
            con.commit();
        }
    }

    public Optional<City> findCity(String name) throws SQLException {
        String sql = "SELECT id, name, country, latitude, longitude FROM cities "
                + "WHERE LOWER(name) = LOWER(?) LIMIT 1";
        try (Connection con = Database.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, name.trim());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapCity(rs)) : Optional.empty();
            }
        }
    }

    public List<City> listCities() throws SQLException {
        List<City> out = new ArrayList<>();
        try (Connection con = Database.getConnection();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(
                     "SELECT id, name, country, latitude, longitude FROM cities ORDER BY name")) {
            while (rs.next()) {
                out.add(mapCity(rs));
            }
        }
        return out;
    }

    /** Upcoming (today onwards) forecasts stored for a city. */
    public List<Forecast> getForecasts(int cityId) throws SQLException {
        String sql = "SELECT forecast_date, temp_max, temp_min, precipitation_mm, wind_speed_max, weather_code "
                + "FROM forecasts WHERE city_id = ? AND forecast_date >= CURDATE() ORDER BY forecast_date";
        List<Forecast> out = new ArrayList<>();
        try (Connection con = Database.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, cityId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(new Forecast(rs.getDate(1).toLocalDate(), rs.getDouble(2), rs.getDouble(3),
                            rs.getDouble(4), rs.getDouble(5), rs.getInt(6)));
                }
            }
        }
        return out;
    }

    public void logSearch(int cityId, String source) throws SQLException {
        try (Connection con = Database.getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "INSERT INTO search_history (city_id, source) VALUES (?, ?)")) {
            ps.setInt(1, cityId);
            ps.setString(2, source);
            ps.executeUpdate();
        }
    }

    public List<String> recentHistory(int limit) throws SQLException {
        String sql = "SELECT c.name, c.country, h.source, h.searched_at FROM search_history h "
                + "JOIN cities c ON c.id = h.city_id ORDER BY h.searched_at DESC, h.id DESC LIMIT ?";
        List<String> out = new ArrayList<>();
        try (Connection con = Database.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(String.format("%s | %-22s | %s", rs.getTimestamp(4).toString().substring(0, 19),
                            rs.getString(1) + ", " + rs.getString(2), rs.getString(3)));
                }
            }
        }
        return out;
    }

    public boolean deleteCity(int cityId) throws SQLException {
        try (Connection con = Database.getConnection();
             PreparedStatement ps = con.prepareStatement("DELETE FROM cities WHERE id = ?")) {
            ps.setInt(1, cityId);
            return ps.executeUpdate() > 0;
        }
    }

    private City mapCity(ResultSet rs) throws SQLException {
        return new City(rs.getInt("id"), rs.getString("name"), rs.getString("country"),
                rs.getDouble("latitude"), rs.getDouble("longitude"));
    }
}
