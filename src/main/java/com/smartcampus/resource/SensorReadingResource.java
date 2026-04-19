package com.smartcampus.resource;

import com.smartcampus.data.DataStore;
import com.smartcampus.model.ApiError;
import com.smartcampus.model.Sensor;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

/**
 * Sub-resource for sensor readings — /api/v1/sensors/{sensorId}/readings
 *
 * This class is instantiated via the Sub-Resource Locator in SensorResource.
 * It receives the sensorId from the parent locator and uses it for all operations.
 *
 * NOTE: Full implementation is completed in Day 4.
 *       This stub ensures the project compiles cleanly after Day 3.
 */
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SensorReadingResource {

    private final String sensorId;

    public SensorReadingResource(String sensorId) {
        this.sensorId = sensorId;
    }

    /**
     * GET /api/v1/sensors/{sensorId}/readings
     * Returns all historical readings for the given sensor.
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

        List<?> readings = DataStore.getReadingsForSensor(sensorId);
        return Response.ok(readings).build();
    }

    /**
     * POST /api/v1/sensors/{sensorId}/readings
     * Full implementation in Day 4 (includes MAINTENANCE check + currentValue update).
     */
    @POST
    public Response addReading(Object body) {
        ApiError err = new ApiError(501, "Not Implemented",
                "POST /readings will be fully implemented in Day 4.");
        return Response.status(501).entity(err).build();
    }
}
