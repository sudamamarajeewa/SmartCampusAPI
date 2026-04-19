package com.smartcampus.model;

/**
 * Represents a single historical reading captured by a sensor.
 */
public class SensorReading {

    private String id;        // Unique reading event ID (UUID recommended)
    private long timestamp;   // Epoch time in milliseconds when the reading was captured
    private double value;     // The actual metric value recorded by the hardware

    public SensorReading() {}

    public SensorReading(String id, long timestamp, double value) {
        this.id = id;
        this.timestamp = timestamp;
        this.value = value;
    }

    // Getters
    public String getId()        { return id; }
    public long getTimestamp()   { return timestamp; }
    public double getValue()     { return value; }

    // Setters
    public void setId(String id)         { this.id = id; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    public void setValue(double value)   { this.value = value; }
}
