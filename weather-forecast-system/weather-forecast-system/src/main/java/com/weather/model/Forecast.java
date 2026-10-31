package com.weather.model;

import java.time.LocalDate;

/** One day of forecast data. */
public record Forecast(LocalDate date, double tempMax, double tempMin,
                       double precipitationMm, double windSpeedMax, int weatherCode) {
}
