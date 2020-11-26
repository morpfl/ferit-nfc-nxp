package com.ferit.temp_reader.types;

import android.os.Parcel;
import android.os.Parcelable;

public class Temperature implements Parcelable {
    private final String timestamp;
    private final String temperatureValue;
    private boolean seriesTemp;

    public Temperature(String timestamp, String temperatureValue, boolean seriesTemp){
        this.timestamp = timestamp;
        this.temperatureValue = temperatureValue;
        this.seriesTemp = seriesTemp;
    }

    public Temperature(Parcel in){
        this.timestamp = in.readString();
        this.temperatureValue = in.readString();
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
    public boolean getSeriesTemp() {return this.seriesTemp; }

    public static final Parcelable.Creator CREATOR = new Parcelable.Creator() {
        public Temperature createFromParcel(Parcel in) {
            return new Temperature(in);
        }

        public Temperature[] newArray(int size) {
            return new Temperature[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(this.timestamp);
        parcel.writeString(this.temperatureValue);
    }
}
