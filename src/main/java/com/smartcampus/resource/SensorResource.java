package com.smartcampus.resource;

import com.smartcampus.data.DataStore;
import com.smartcampus.exception.LinkedResourceNotFoundException;
import com.smartcampus.model.ApiError;
import com.smartcampus.model.Room;
import com.smartcampus.model.Sensor;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Sensor Resource — manages the /api/v1/sensors collection.
 *
 * Endpoints:
 *   GET  /api/v1/sensors            → list all sensors (optionally filtered by ?type=)
 *   POST /api/v1/sensors            → register a new sensor (roomId must exist)
 *   GET  /api/v1/sensors/{sensorId} → get a specific sensor
 *
 * Sub-resource locator (Part 4):
 *   ANY  /api/v1/sensors/{sensorId}/readings → delegates to SensorReadingResource
 */
@Path("/sensors")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SensorResource {

    // ─── GET /api/v1/sensors ─────────────────────────────────────────────────
    /**
     * Returns all sensors in the system.
     * Supports optional filtering via ?type=  e.g. GET /api/v1/sensors?type=CO2
     *
     * Using @QueryParam for filtering (not path-based e.g. /sensors/type/CO2)
     * because query parameters are the standard REST convention for optional
     * filters on a collection. Path segments should only represent resources,
     * not filter criteria.
     */
    @GET
    public Response getAllSensors(@QueryParam("type") String type) {
        List<Sensor> sensorList = new ArrayList<>(DataStore.getSensors().values());

        // Apply filter if ?type= was provided
        if (type != null && !type.trim().isEmpty()) {
            sensorList = sensorList.stream()
                    .filter(s -> s.getType() != null &&
                                 s.getType().equalsIgnoreCase(type.trim()))
                    .collect(Collectors.toList());
        }

        return Response.ok(sensorList).build();
    }

    // ─── POST /api/v1/sensors ────────────────────────────────────────────────
    /**
     * Registers a new sensor.
     *
     * Validation rules:
     *  - Body must not be null
     *  - 'type' is required
     *  - 'roomId' is required AND must reference an existing room
     *  - If roomId does not exist → throws LinkedResourceNotFoundException (422)
     *
     * Side effects on success:
     *  - Sensor is added to DataStore
     *  - Sensor's ID is appended to the parent Room's sensorIds list
     *
     * @Consumes(APPLICATION_JSON) means if client sends text/plain or application/xml,
     * JAX-RS automatically returns 415 Unsupported Media Type before this method is called.
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createSensor(Sensor sensor) {
        // Null body guard
        if (sensor == null) {
            ApiError err = new ApiError(400, "Bad Request", "Request body is missing or malformed.");
            return Response.status(Response.Status.BAD_REQUEST).entity(err).build();
        }

        // Validate required fields
        if (sensor.getType() == null || sensor.getType().trim().isEmpty()) {
            ApiError err = new ApiError(400, "Bad Request", "Field 'type' is required (e.g. Temperature, CO2, Occupancy).");
            return Response.status(Response.Status.BAD_REQUEST).entity(err).build();
        }

        if (sensor.getRoomId() == null || sensor.getRoomId().trim().isEmpty()) {
            ApiError err = new ApiError(400, "Bad Request", "Field 'roomId' is required.");
            return Response.status(Response.Status.BAD_REQUEST).entity(err).build();
        }

        // ── Key integrity check: roomId must exist ──────────────────────────
        Room parentRoom = DataStore.getRoom(sensor.getRoomId());
        if (parentRoom == null) {
            // 422 Unprocessable Entity — JSON is valid, but the referenced resource doesn't exist
            throw new LinkedResourceNotFoundException("roomId", sensor.getRoomId());
        }

        // Auto-generate ID if not provided
        if (sensor.getId() == null || sensor.getId().trim().isEmpty()) {
            sensor.setId(UUID.randomUUID().toString());
        }

        // Reject duplicate sensor IDs
        if (DataStore.sensorExists(sensor.getId())) {
            ApiError err = new ApiError(409, "Conflict",
                    "A sensor with id '" + sensor.getId() + "' already exists.");
            return Response.status(Response.Status.CONFLICT).entity(err).build();
        }

        // Default status to ACTIVE if not specified
        if (sensor.getStatus() == null || sensor.getStatus().trim().isEmpty()) {
            sensor.setStatus("ACTIVE");
        }

        // Validate status value
        String status = sensor.getStatus().toUpperCase();
        if (!status.equals("ACTIVE") && !status.equals("MAINTENANCE") && !status.equals("OFFLINE")) {
            ApiError err = new ApiError(400, "Bad Request",
                    "Field 'status' must be one of: ACTIVE, MAINTENANCE, OFFLINE.");
            return Response.status(Response.Status.BAD_REQUEST).entity(err).build();
        }
        sensor.setStatus(status);

        // ── Persist the sensor ──────────────────────────────────────────────
        DataStore.putSensor(sensor);

        // ── Side effect: link sensor to its parent room ─────────────────────
        parentRoom.addSensorId(sensor.getId());

        // 201 Created with the new sensor in the body
        return Response.status(Response.Status.CREATED).entity(sensor).build();
    }

    // ─── GET /api/v1/sensors/{sensorId} ──────────────────────────────────────
    /**
     * Returns detailed info for a specific sensor.
     * Returns 404 if no sensor with that ID exists.
     */
    @GET
    @Path("/{sensorId}")
    public Response getSensorById(@PathParam("sensorId") String sensorId) {
        Sensor sensor = DataStore.getSensor(sensorId);

        if (sensor == null) {
            ApiError err = new ApiError(404, "Not Found",
                    "No sensor found with id '" + sensorId + "'.");
            return Response.status(Response.Status.NOT_FOUND).entity(err).build();
        }

        return Response.ok(sensor).build();
    }

    // ─── Sub-resource Locator: /api/v1/sensors/{sensorId}/readings ───────────
    /**
     * Delegates all /readings sub-paths to SensorReadingResource.
     *
     * This is the Sub-Resource Locator pattern (Part 4).
     * Instead of defining every readings path here, we hand off to a dedicated class,
     * keeping this controller focused and clean.
     *
     * Note: No HTTP method annotation — JAX-RS uses this purely as a locator.
     */
    @Path("/{sensorId}/readings")
    public SensorReadingResource getReadingsResource(@PathParam("sensorId") String sensorId) {
        return new SensorReadingResource(sensorId);
    }
}
