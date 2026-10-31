package com.weather.service;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.weather.model.City;
import com.weather.model.Forecast;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Fetches live data from the free Open-Meteo APIs (no API key needed). */
public class WeatherApiService {
    private static final String GEO_URL = "https://geocoding-api.open-meteo.com/v1/search";
    private static final String FORECAST_URL = "https://api.open-meteo.com/v1/forecast";

    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10)).build();

    /** Looks up coordinates for a city name. */
    public City geocode(String name) throws IOException, InterruptedException {
        String url = GEO_URL + "?count=1&language=en&format=json&name="
                + URLEncoder.encode(name.trim(), StandardCharsets.UTF_8);
        JsonObject root = JsonParser.parseString(get(url)).getAsJsonObject();
        if (!root.has("results") || root.getAsJsonArray("results").isEmpty()) {
            throw new IOException("City not found: " + name);
        }
        JsonObject r = root.getAsJsonArray("results").get(0).getAsJsonObject();
        String country = r.has("country") ? r.get("country").getAsString() : "";
        return new City(0, r.get("name").getAsString(), country,
                r.get("latitude").getAsDouble(), r.get("longitude").getAsDouble());
    }

    /** Gets a 7-day daily forecast for the given coordinates. */
    public List<Forecast> fetchForecast(City city) throws IOException, InterruptedException {
        String url = FORECAST_URL + "?latitude=" + city.latitude() + "&longitude=" + city.longitude()
                + "&daily=weather_code,temperature_2m_max,temperature_2m_min,precipitation_sum,wind_speed_10m_max"
                + "&timezone=auto&forecast_days=7";
        JsonObject daily = JsonParser.parseString(get(url)).getAsJsonObject().getAsJsonObject("daily");
        JsonArray time = daily.getAsJsonArray("time");
        List<Forecast> out = new ArrayList<>();
        for (int i = 0; i < time.size(); i++) {
            out.add(new Forecast(
                    LocalDate.parse(time.get(i).getAsString()),
                    num(daily, "temperature_2m_max", i),
                    num(daily, "temperature_2m_min", i),
                    num(daily, "precipitation_sum", i),
                    num(daily, "wind_speed_10m_max", i),
                    (int) num(daily, "weather_code", i)));
        }
        return out;
    }

    private static double num(JsonObject o, String key, int i) {
        var el = o.getAsJsonArray(key).get(i);
        return el.isJsonNull() ? 0 : el.getAsDouble();
    }

    private String get(String url) throws IOException, InterruptedException {
        HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(15)).GET().build();
        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() != 200) {
            throw new IOException("HTTP " + resp.statusCode() + " from weather service");
        }
        return resp.body();
    }
}
