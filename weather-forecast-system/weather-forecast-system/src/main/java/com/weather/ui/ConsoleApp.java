package com.weather.ui;

import com.weather.db.WeatherDao;
import com.weather.model.City;
import com.weather.model.Forecast;
import com.weather.model.WeatherCodes;
import com.weather.service.WeatherApiService;

import java.io.IOException;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;

/** Menu-driven console interface. */
public class ConsoleApp {
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("EEE dd-MMM-yyyy");

    private final Scanner in = new Scanner(System.in);
    private final WeatherApiService api = new WeatherApiService();
    private final WeatherDao dao = new WeatherDao();

    public void run() {
        System.out.println("=========================================");
        System.out.println("   CONSOLE WEATHER FORECASTING SYSTEM");
        System.out.println("=========================================");
        boolean running = true;
        while (running) {
            printMenu();
            if (!in.hasNextLine()) {
                break;
            }
            String choice = in.nextLine().trim();
            try {
                switch (choice) {
                    case "1" -> liveForecast();
                    case "2" -> savedForecast();
                    case "3" -> listCities();
                    case "4" -> history();
                    case "5" -> deleteCity();
                    case "0" -> running = false;
                    default -> System.out.println("Invalid choice, try again.");
                }
            } catch (SQLException e) {
                System.out.println("[Database error] " + e.getMessage());
                System.out.println("Check MySQL is running and db.properties is correct.");
            }
        }
        System.out.println("Goodbye!");
    }

    private void printMenu() {
        System.out.println();
        System.out.println("1. Live 7-day forecast (internet, saved to database)");
        System.out.println("2. Saved forecast (offline, from database)");
        System.out.println("3. List saved cities");
        System.out.println("4. Search history");
        System.out.println("5. Delete a saved city");
        System.out.println("0. Exit");
        System.out.print("Choose: ");
    }

    private void liveForecast() throws SQLException {
        String name = prompt("Enter city name: ");
        if (name.isEmpty()) {
            return;
        }
        try {
            System.out.println("Fetching live data...");
            City city = api.geocode(name);
            List<Forecast> forecasts = api.fetchForecast(city);
            int id = dao.upsertCity(city);
            dao.saveForecasts(id, forecasts, "API");
            dao.logSearch(id, "API");
            printForecast(city, forecasts, "LIVE (Open-Meteo)");
        } catch (IOException e) {
            System.out.println("[Online lookup failed] " + e.getMessage());
            System.out.println("Trying saved data instead...");
            showFromDatabase(name);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.out.println("Request interrupted.");
        }
    }

    private void savedForecast() throws SQLException {
        String name = prompt("Enter city name: ");
        if (!name.isEmpty()) {
            showFromDatabase(name);
        }
    }

    private void showFromDatabase(String name) throws SQLException {
        Optional<City> city = dao.findCity(name);
        if (city.isEmpty()) {
            System.out.println("No saved data for '" + name + "'. Use option 1 when online.");
            return;
        }
        List<Forecast> list = dao.getForecasts(city.get().id());
        if (list.isEmpty()) {
            System.out.println("Saved data for " + city.get() + " is out of date. Use option 1 when online.");
            return;
        }
        dao.logSearch(city.get().id(), "DATABASE");
        printForecast(city.get(), list, "SAVED (MySQL)");
    }

    private void listCities() throws SQLException {
        List<City> cities = dao.listCities();
        if (cities.isEmpty()) {
            System.out.println("No cities saved yet.");
            return;
        }
        System.out.println("\nSaved cities:");
        for (City c : cities) {
            System.out.printf("  %-3d %-25s (%.2f, %.2f)%n", c.id(), c, c.latitude(), c.longitude());
        }
    }

    private void history() throws SQLException {
        List<String> rows = dao.recentHistory(15);
        if (rows.isEmpty()) {
            System.out.println("No searches yet.");
            return;
        }
        System.out.println("\nLast 15 searches:");
        rows.forEach(r -> System.out.println("  " + r));
    }

    private void deleteCity() throws SQLException {
        String name = prompt("City to delete: ");
        Optional<City> city = dao.findCity(name);
        if (city.isEmpty()) {
            System.out.println("City not found.");
            return;
        }
        String ok = prompt("Delete " + city.get() + " and all its data? (y/n): ");
        if (ok.equalsIgnoreCase("y")) {
            dao.deleteCity(city.get().id());
            System.out.println("Deleted.");
        }
    }

    private void printForecast(City city, List<Forecast> list, String source) {
        System.out.println();
        System.out.println("Forecast for " + city + "   [" + source + "]");
        System.out.println("-".repeat(80));
        System.out.printf("%-17s %-22s %8s %8s %9s %10s%n",
                "Date", "Condition", "Max C", "Min C", "Rain mm", "Wind km/h");
        System.out.println("-".repeat(80));
        for (Forecast f : list) {
            System.out.printf("%-17s %-22s %8.1f %8.1f %9.1f %10.1f%n",
                    f.date().format(DAY), WeatherCodes.describe(f.weatherCode()),
                    f.tempMax(), f.tempMin(), f.precipitationMm(), f.windSpeedMax());
        }
        System.out.println("-".repeat(80));
    }

    private String prompt(String text) {
        System.out.print(text);
        return in.hasNextLine() ? in.nextLine().trim() : "";
    }
}
