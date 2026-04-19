package com.smartcampus.exception.mapper;

import com.smartcampus.model.ApiError;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Global "safety net" exception mapper — catches ALL unhandled Throwables.
 *
 * This ensures the API never leaks a raw Java stack trace or server error page
 * to the client. Any unexpected runtime exception (NullPointerException,
 * IndexOutOfBoundsException, etc.) is caught here and converted to a clean
 * HTTP 500 response with a safe, generic message.
 *
 * Security note: Stack traces are logged server-side only (for debugging),
 * but the client response never includes internal implementation details,
 * preventing information disclosure attacks.
 */
@Provider
public class GenericExceptionMapper implements ExceptionMapper<Throwable> {

    private static final Logger LOGGER = Logger.getLogger(GenericExceptionMapper.class.getName());

    @Override
    public Response toResponse(Throwable exception) {
        // If it's a built-in JAX-RS exception (e.g. 404 Not Found, 405 Method Not Allowed),
        // keep its intended status code rather than forcing a 500.
        if (exception instanceof javax.ws.rs.WebApplicationException) {
            javax.ws.rs.WebApplicationException webEx = (javax.ws.rs.WebApplicationException) exception;
            Response r = webEx.getResponse();
            ApiError error = new ApiError(
                    r.getStatus(),
                    r.getStatusInfo().getReasonPhrase(),
                    exception.getMessage() != null ? exception.getMessage() : "HTTP Error " + r.getStatus()
            );
            return Response.status(r.getStatus())
                    .entity(error)
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }

        // Log the full stack trace server-side for debugging
        LOGGER.log(Level.SEVERE,
                "Unhandled exception caught by global safety net: " + exception.getMessage(),
                exception);

        // Return a safe, generic message to the client — no stack trace exposed
        ApiError error = new ApiError(
                500,
                "Internal Server Error",
                "An unexpected error occurred. Please try again later or contact the administrator."
        );
        return Response
                .status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(error)
                .type(MediaType.APPLICATION_JSON)
                .build();
    }
}
