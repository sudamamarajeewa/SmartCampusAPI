package com.smartcampus.data;

import com.smartcampus.model.Room;
import com.smartcampus.model.Sensor;
import com.smartcampus.model.SensorReading;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Singleton in-memory data store for the Smart Campus API.
 * Uses ConcurrentHashMap to ensure thread-safe access across concurrent
 * requests.
 * In JAX-RS, resource classes are instantiated per request by default, but they
 * all
 * share these static maps so all data persists for the lifetime of the server.
 *
 * Data is stored in memory only. All data is lost if the server restarts.
 */
public class DataStore {

    // Rooms
    private static final ConcurrentHashMap<String, Room> rooms = new ConcurrentHashMap<>();

    // Sensors
    private static final ConcurrentHashMap<String, Sensor> sensors = new ConcurrentHashMap<>();

    // Sensor Readings — keyed by sensorId, each value is a list of readings
    private static final ConcurrentHashMap<String, List<SensorReading>> sensorReadings = new ConcurrentHashMap<>();

    // Private constructor — this is a utility class (all static), not instantiated
    private DataStore() {
    }

    // Room Operations

    public static ConcurrentHashMap<String, Room> getRooms() {
        return rooms;
    }

    public static Room getRoom(String id) {
        return rooms.get(id);
    }

    public static void putRoom(Room room) {
        rooms.put(room.getId(), room);
    }

    public static boolean roomExists(String id) {
        return rooms.containsKey(id);
    }

    public static Room removeRoom(String id) {
        return rooms.remove(id);
    }

    // Sensor Operations

    public static ConcurrentHashMap<String, Sensor> getSensors() {
        return sensors;
    }

    public static Sensor getSensor(String id) {
        return sensors.get(id);
    }

    public static void putSensor(Sensor sensor) {
        sensors.put(sensor.getId(), sensor);
    }

    public static boolean sensorExists(String id) {
        return sensors.containsKey(id);
    }

    public static Sensor removeSensor(String id) {
        return sensors.remove(id);
    }

    // Sensor Reading Operations

    public static List<SensorReading> getReadingsForSensor(String sensorId) {
        // Return existing list or create a new one for this sensor
        return sensorReadings.computeIfAbsent(sensorId, k -> new ArrayList<>());
    }

    public static void addReading(String sensorId, SensorReading reading) {
        sensorReadings.computeIfAbsent(sensorId, k -> new ArrayList<>()).add(reading);
    }
}
