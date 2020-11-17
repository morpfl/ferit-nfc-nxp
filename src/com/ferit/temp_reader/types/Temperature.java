package com.ferit.temp_reader.types;

public class Temperature {
    private String timestamp;
    private String temperatureValue;

    public Temperature(String timestamp, String temperatureValue){
        this.timestamp = timestamp;
        this.temperatureValue = temperatureValue;
    }

    @Override
    public String toString(){
        return temperatureValue + "°C" + " - " + timestamp;
    }

    public String getTimestamp(){
        return this.timestamp;
    }

    public String getTemperatureValue(){
        return this.temperatureValue;
    }
}
