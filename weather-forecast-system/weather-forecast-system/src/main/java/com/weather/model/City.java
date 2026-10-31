package com.weather.model;

/** A city with coordinates. id is 0 when not yet stored in the database. */
public record City(int id, String name, String country, double latitude, double longitude) {
    public City withId(int newId) {
        return new City(newId, name, country, latitude, longitude);
    }

    @Override
    public String toString() {
        return country == null || country.isBlank() ? name : name + ", " + country;
    }
}
