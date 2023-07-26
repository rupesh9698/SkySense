package com.sense.sky;

public class HoursModel {
    
    private String time;
    private String temperature;
    private String fahrenheit;
    private String currentTemperatureUnit;
    private String icon;
    private String humidity;
    private String windSpeed;

    public HoursModel() {}

    public HoursModel(String time, String temperature, String fahrenheit, String currentTemperatureUnit, String icon, String humidity, String windSpeed) {
        this.time = time;
        this.temperature = temperature;
        this.fahrenheit = fahrenheit;
        this.currentTemperatureUnit = currentTemperatureUnit;
        this.icon = icon;
        this.humidity = humidity;
        this.windSpeed = windSpeed;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getTemperature() {
        return temperature;
    }

    public void setTemperature(String temperature) {
        this.temperature = temperature;
    }

    public String getFahrenheit() {
        return fahrenheit;
    }

    public void setFahrenheit(String fahrenheit) {
        this.fahrenheit = fahrenheit;
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
