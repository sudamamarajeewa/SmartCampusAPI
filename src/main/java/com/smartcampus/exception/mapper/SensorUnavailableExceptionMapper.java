package com.smartcampus.exception.mapper;

import com.smartcampus.exception.SensorUnavailableException;
import com.smartcampus.model.ApiError;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

/**
 * Maps SensorUnavailableException → HTTP 403 Forbidden.
 *
 * Triggered when a POST /readings is sent to a sensor with status "MAINTENANCE".
 * 403 Forbidden is appropriate here: the server understands the request but
 * refuses to process it due to the sensor's current operational state.
 */
@Provider
public class SensorUnavailableExceptionMapper
        implements ExceptionMapper<SensorUnavailableException> {

    @Override
    public Response toResponse(SensorUnavailableException ex) {
        ApiError error = new ApiError(
                403,
                "Forbidden",
                ex.getMessage()
        );
        return Response
                .status(Response.Status.FORBIDDEN)
                .entity(error)
                .type(MediaType.APPLICATION_JSON)
                .build();
    }
}
