package com.smartcampus.resource;

import com.smartcampus.data.DataStore;
import com.smartcampus.exception.SensorUnavailableException;
import com.smartcampus.model.ApiError;
import com.smartcampus.model.Sensor;
import com.smartcampus.model.SensorReading;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.UUID;

/**
 * Sub-Resource for Sensor Readings — /api/v1/sensors/{sensorId}/readings
 *
 * This class is NOT annotated with @Path at the class level because it is
 * instantiated via the Sub-Resource Locator in SensorResource, not auto-discovered.
 * The path context is inherited from the parent locator method.
 *
 * Sub-Resource Locator Pattern benefits:
 *  - Separates reading logic from sensor logic → cleaner code
 *  - Each class has a single responsibility
 *  - Easier to test and maintain independently
 *  - Avoids a single "god class" with hundreds of endpoint methods
 *
 * Endpoints:
 *   GET  /api/v1/sensors/{sensorId}/readings       → list all historical readings
 *   POST /api/v1/sensors/{sensorId}/readings       → append a new reading
 */
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SensorReadingResource {

    private final String sensorId;

    /**
     * Instantiated by SensorResource's sub-resource locator.
     * The sensorId is passed in from the parent @PathParam.
     */
    public SensorReadingResource(String sensorId) {
        this.sensorId = sensorId;
    }

    // ─── GET /api/v1/sensors/{sensorId}/readings ─────────────────────────────
    /**
     * Returns the full historical readings log for the specified sensor.
     * Returns 404 if the parent sensor does not exist.
     */
    @GET
    public Response getReadings() {
        // Verify parent sensor exists
        Sensor sensor = DataStore.getSensor(sensorId);
        if (sensor == null) {
            ApiError err = new ApiError(404, "Not Found",
                    "No sensor found with id '" + sensorId + "'.");
            return Response.status(Response.Status.NOT_FOUND).entity(err).build();
        }

        List<SensorReading> readings = DataStore.getReadingsForSensor(sensorId);
        return Response.ok(readings).build();
    }

    // ─── POST /api/v1/sensors/{sensorId}/readings ────────────────────────────
    /**
     * Appends a new reading to the sensor's historical log.
     *
     * Validations:
     *  1. Sensor must exist → 404 if not
     *  2. Sensor status must NOT be "MAINTENANCE" → 403 via SensorUnavailableException
     *     (A sensor under maintenance is physically disconnected)
     *  3. Reading value is required
     *
     * Side effect (data consistency):
     *  - On success, updates the parent Sensor's currentValue field to match
     *    the newly recorded reading value, keeping the sensor's live state in sync.
     *
     * Auto-generation:
     *  - id: UUID generated if not provided
     *  - timestamp: current epoch milliseconds if not provided
     */
    @POST
    public Response addReading(SensorReading reading) {
        // ── 1. Verify parent sensor exists ───────────────────────────────────
        Sensor sensor = DataStore.getSensor(sensorId);
        if (sensor == null) {
            ApiError err = new ApiError(404, "Not Found",
                    "No sensor found with id '" + sensorId + "'.");
            return Response.status(Response.Status.NOT_FOUND).entity(err).build();
        }

        // ── 2. State constraint: block readings if sensor is NOT ACTIVE ──────────
        // Throws SensorUnavailableException → mapped to 403 Forbidden
        if (!"ACTIVE".equalsIgnoreCase(sensor.getStatus())) {
            throw new SensorUnavailableException(sensorId, sensor.getStatus());
        }

        // ── 3. Validate request body ─────────────────────────────────────────
        if (reading == null) {
            ApiError err = new ApiError(400, "Bad Request",
                    "Request body is missing or malformed.");
            return Response.status(Response.Status.BAD_REQUEST).entity(err).build();
        }

        // Auto-generate id if not provided
        if (reading.getId() == null || reading.getId().trim().isEmpty()) {
            reading.setId(UUID.randomUUID().toString());
        }

        // Auto-set timestamp to now if not provided (or if 0)
        if (reading.getTimestamp() <= 0) {
            reading.setTimestamp(System.currentTimeMillis());
        }

        // ── 4. Persist the reading ───────────────────────────────────────────
        DataStore.addReading(sensorId, reading);

        // ── 5. Side effect: update parent sensor's currentValue ──────────────
        // Ensures sensor.currentValue always reflects the most recent measurement
        sensor.setCurrentValue(reading.getValue());

        // 201 Created with the new reading in the body
        return Response.status(Response.Status.CREATED).entity(reading).build();
    }
}
