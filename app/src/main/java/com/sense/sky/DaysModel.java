package com.sense.sky;

public class DaysModel {

    private String date;
    private String min_temperature;
    private String minFahrenheit;
    private String max_temperature;
    private String maxFahrenheit;
    private String currentTemperatureUnit;
    private String icon;
    private String humidity;
    private String windSpeed;

    public DaysModel() {}

    public DaysModel(String date, String min_temperature, String minFahrenheit, String max_temperature, String maxFahrenheit, String currentTemperatureUnit, String icon, String humidity, String windSpeed) {
        this.date = date;
        this.min_temperature = min_temperature;
        this.minFahrenheit = minFahrenheit;
        this.max_temperature = max_temperature;
        this.maxFahrenheit = maxFahrenheit;
        this.currentTemperatureUnit = currentTemperatureUnit;
        this.icon = icon;
        this.humidity = humidity;
        this.windSpeed = windSpeed;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getMin_temperature() {
        return min_temperature;
    }

    public void setMin_temperature(String min_temperature) {
        this.min_temperature = min_temperature;
    }

    public String getMinFahrenheit() {
        return minFahrenheit;
    }

    public void setMinFahrenheit(String minFahrenheit) {
        this.minFahrenheit = minFahrenheit;
    }

    public String getMax_temperature() {
        return max_temperature;
    }

    public void setMax_temperature(String max_temperature) {
        this.max_temperature = max_temperature;
    }

    public String getMaxFahrenheit() {
        return maxFahrenheit;
    }

    public void setMaxFahrenheit(String maxFahrenheit) {
        this.maxFahrenheit = maxFahrenheit;
    }

    public String getCurrentTemperatureUnit() {
        return currentTemperatureUnit;
    }

    public void setCurrentTemperatureUnit(String currentTemperatureUnit) {
        this.currentTemperatureUnit = currentTemperatureUnit;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getHumidity() {
        return humidity;
    }

    public void setHumidity(String humidity) {
        this.humidity = humidity;
    }

    public String getWindSpeed() {
        return windSpeed;
    }

    public void setWindSpeed(String windSpeed) {
        this.windSpeed = windSpeed;
    }
}
