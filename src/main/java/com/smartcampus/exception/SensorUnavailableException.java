package com.smartcampus.exception;

/**
 * Thrown when a POST /readings is attempted on a sensor in MAINTENANCE status.
 * A sensor under maintenance is physically disconnected and cannot accept new data.
 * Mapped to HTTP 403 Forbidden by SensorUnavailableExceptionMapper.
 */
public class SensorUnavailableException extends RuntimeException {

    private final String sensorId;
    private final String status;

    public SensorUnavailableException(String sensorId, String status) {
        super("Sensor '" + sensorId + "' is currently in '"
                + status + "' status and cannot accept new readings.");
        this.sensorId = sensorId;
        this.status   = status;
    }

    public String getSensorId() { return sensorId; }
    public String getStatus()   { return status; }
}
