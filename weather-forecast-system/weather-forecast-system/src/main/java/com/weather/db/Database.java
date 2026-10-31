package com.weather.db;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/** Opens JDBC connections using settings from db.properties. */
public final class Database {
    private static final Properties PROPS = new Properties();

    static {
        try (InputStream in = Database.class.getResourceAsStream("/db.properties")) {
            if (in == null) {
                throw new IllegalStateException("db.properties not found on classpath");
            }
            PROPS.load(in);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read db.properties", e);
        }
    }

    private Database() {}

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(
                PROPS.getProperty("db.url"),
                PROPS.getProperty("db.user"),
                PROPS.getProperty("db.password"));
    }
}
