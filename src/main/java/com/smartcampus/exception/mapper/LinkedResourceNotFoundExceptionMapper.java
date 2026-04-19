package com.smartcampus.exception.mapper;

import com.smartcampus.exception.LinkedResourceNotFoundException;
import com.smartcampus.model.ApiError;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

/**
 * Maps LinkedResourceNotFoundException → HTTP 422 Unprocessable Entity.
 *
 * Triggered when a POST /sensors body contains a roomId that does not exist.
 * 422 is more semantically precise than 404 here: the URL path is valid,
 * but the payload references a resource that cannot be resolved.
 */
@Provider
public class LinkedResourceNotFoundExceptionMapper
        implements ExceptionMapper<LinkedResourceNotFoundException> {

    @Override
    public Response toResponse(LinkedResourceNotFoundException ex) {
        ApiError error = new ApiError(
                422,
                "Unprocessable Entity",
                ex.getMessage()
        );
        return Response
                .status(422)
                .entity(error)
                .type(MediaType.APPLICATION_JSON)
                .build();
    }
}
